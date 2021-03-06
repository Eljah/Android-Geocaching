package su.geocaching.android.ui.map;

import android.location.Location;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import su.geocaching.android.model.MapInfo;

public interface IMapWrapper {

    void animateToLocation(Location location);

    void animateToGeoPoint(LatLng geoPoint);

    MapInfo getMapState();

    void restoreMapState(MapInfo lastMapInfo);

    void setZoomControlsEnabled(boolean zoomControlsEnabled);

    void setViewPortChangeListener(ViewPortChangeListener listener);

    Projection getProjection();

    void setupMyLocationLayer();

    void updateLocationMarker(Location location);

    void setLocationMarkerTapListener(LocationMarkerTapListener listener);

    void updateMapLayer();
}