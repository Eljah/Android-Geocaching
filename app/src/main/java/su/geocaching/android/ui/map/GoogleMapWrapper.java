package su.geocaching.android.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import su.geocaching.android.controller.Controller;
import su.geocaching.android.controller.apimanager.GeoRect;
import su.geocaching.android.model.MapInfo;
import su.geocaching.android.ui.map.providers.*;

import static com.google.android.gms.maps.GoogleMap.*;

public class GoogleMapWrapper implements IMapWrapper {

    protected Context context;
    protected GoogleMap googleMap;

    protected Location currentUserLocation;
    protected TileOverlay customTileOverlay;
    protected MapType currentMapType;

    protected LocationSource.OnLocationChangedListener locationChangedListener;
    private static final Bitmap clickableBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ALPHA_8);
    private Marker userPositionClickArea; // hack to make user position clickable

    private Marker userPositionClickArea2; // hack to make user position clickable
    private LocationMarkerTapListener locationMarkerTapListener;

    public GoogleMapWrapper(Context context, GoogleMap map) {
        this.context = context;
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        //clickableBitmap.eraseColor(Color.RED);

        map.setOnMarkerClickListener(
                new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        return onLocationMarkerTap(marker) || onMarkerTap(marker);
                    }
                });
    }

    private boolean onLocationMarkerTap(Marker marker) {
        if (userPositionClickArea == null) return false;
        if (userPositionClickArea2 == null) return false;

        if (marker.getId().equals(userPositionClickArea.getId()) ||
                marker.getId().equals(userPositionClickArea2.getId())) {
            if (locationMarkerTapListener != null)
                locationMarkerTapListener.onMarkerTapped();
            return true;
        }

        return false;
    }

    protected boolean onMarkerTap(Marker marker) {
        return false;
    }

    @Override
    public void animateToLocation(Location location) {
        LatLng center = new LatLng(location.getLongitude(), location.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(center));
    }

    @Override
    public void animateToGeoPoint(LatLng geoPoint) {
        LatLng center = new LatLng(geoPoint.latitude, geoPoint.longitude);
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(center));
    }

    @Override
    public MapInfo getMapState() {
        CameraPosition cameraPosition = googleMap.getCameraPosition();
        return new MapInfo(cameraPosition.target.latitude, cameraPosition.target.longitude, cameraPosition.zoom);
    }

    @Override
    public void restoreMapState(MapInfo lastMapInfo) {
        LatLng center = new LatLng(lastMapInfo.getCenterX(), lastMapInfo.getCenterY());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(center, lastMapInfo.getZoom());
        googleMap.moveCamera(cameraUpdate);
    }

    @Override
    public void setZoomControlsEnabled(boolean zoomControlEnabled) {
        googleMap.getUiSettings().setZoomControlsEnabled(zoomControlEnabled);
    }

    @Override
    public void setViewPortChangeListener(final ViewPortChangeListener listener) {
        googleMap.setOnCameraChangeListener(new OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        GoogleMapWrapper.this.onCameraChange(cameraPosition, listener);

                    }
                }
        );
    }

    protected void onCameraChange(CameraPosition cameraPosition, ViewPortChangeListener listener) {
        // If custom tile overlay is enabled, use rounded zoom to avoid
        // tiles blurring
        if (customTileOverlay != null) {
            int roundZoom = Math.round(cameraPosition.zoom);
            if (Math.abs(cameraPosition.zoom - roundZoom) > 0.01) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(roundZoom);
                googleMap.animateCamera(cameraUpdate);
                return;
            }
        }
        GeoRect viewPort = getViewPortGeoRect();
        listener.onViewPortChanged(viewPort);
    }

    @Override
    public Projection getProjection() {
        return googleMap.getProjection();
    }

    private GeoRect getViewPortGeoRect() {
        LatLngBounds viewPortBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng tl = new LatLng(viewPortBounds.northeast.latitude, viewPortBounds.southwest.longitude);
        LatLng br = new LatLng(viewPortBounds.southwest.latitude, viewPortBounds.northeast.longitude);
        return new GeoRect(tl, br);
    }

    @Override
    public void updateLocationMarker(Location location) {
        if (locationChangedListener != null) {
            currentUserLocation = location;
            locationChangedListener.onLocationChanged(location);
        }

        // Update clickable area
        LatLng userPosition = getUserLocation(location);
        if (userPositionClickArea == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(userPosition);
            markerOptions.anchor(0.4f, 0.4f); // strange google maps bug
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(clickableBitmap));
            userPositionClickArea = googleMap.addMarker(markerOptions);
        } else {
            userPositionClickArea.setPosition(userPosition);
        }
        if (userPositionClickArea2 == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(userPosition);
            markerOptions.anchor(0.6f, 0.6f); // strange google maps bug
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(clickableBitmap));
            userPositionClickArea2 = googleMap.addMarker(markerOptions);
        } else {
            userPositionClickArea2.setPosition(userPosition);
        }
    }

    @Override
    public void setLocationMarkerTapListener(LocationMarkerTapListener listener) {
        locationMarkerTapListener = listener;
    }

    @Override
    public void updateMapLayer() {
        MapType mapType = Controller.getInstance().getPreferencesManager().getMapType();
        if (mapType == currentMapType) return;
        currentMapType = mapType;

        if (customTileOverlay != null) customTileOverlay.remove();
        customTileOverlay = null;

        switch (mapType) {
            case GoogleNormal: googleMap.setMapType(MAP_TYPE_NORMAL); return;
            case GoogleSatellite: googleMap.setMapType(MAP_TYPE_SATELLITE); return;
            case GoogleTerrain: googleMap.setMapType(MAP_TYPE_TERRAIN); return;
            case GoogleHybrid: googleMap.setMapType(MAP_TYPE_HYBRID); return;
        }

        googleMap.setMapType(MAP_TYPE_NONE);// Don't display any google layer

        UrlTileProvider provider = getTileProvider(mapType);
        if (provider != null) {
            customTileOverlay = googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            customTileOverlay.setZIndex(-100);
        }
    }

    private UrlTileProvider getTileProvider(MapType mapType) {
        //TODO: yandex provider
        /**
            It's not possible to create yandex provider because they use different projection and it's impossible to use
            custom projection with the current state of google maps api.

            EPSG:3395 - WGS 84 / World Mercator  на сфероиде. Эта проекция используется такими сервисами как Космоснимки, Яндекс карты, Карты mail.ru (спутник) и др.
            EPSG:3857 - WGS 84 / Pseudo-Mercator (Spherical Mercator) на сфере. Эта проекция используется такими сервисами как Google, Virtualearth, Maps-For-Free, Wikimapia, OpenStreetMap, Роскосмос, Навител, Nokia и др.
        */
        switch (mapType) {
            case OsmMapnik:
                return new MapnikOsmUrlTileProvider();
            case OsmCylcle:
                return new OpenCycleMapOsmUrlTileProvider();
            case OsmMapQuest:
                return new MapQuestOsmUrlTileProvider();
            case MarshrutyRu:
                return new MarshrutyRuUrlTileProvider();
            case MapBox:
                return new MapBoxStandardProvider();
        }
        return null;
    }

    @Override
    public void setupMyLocationLayer() {
        googleMap.setMyLocationEnabled(true);
        googleMap.setLocationSource(new LocationSource() {

            @Override
            public void activate(OnLocationChangedListener onLocationChangedListener) {
                locationChangedListener = onLocationChangedListener;
            }

            @Override
            public void deactivate() {
                locationChangedListener = null;
            }
        });
    }

    protected static LatLng getUserLocation(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
}