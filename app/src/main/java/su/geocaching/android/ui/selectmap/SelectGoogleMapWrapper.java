package su.geocaching.android.ui.selectmap;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import su.geocaching.android.controller.Controller;
import su.geocaching.android.controller.managers.NavigationManager;
import su.geocaching.android.model.GeoCache;
import su.geocaching.android.model.GeoCacheType;
import su.geocaching.android.ui.map.GoogleMapWrapper;
import su.geocaching.android.ui.map.ViewPortChangeListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SelectGoogleMapWrapper extends GoogleMapWrapper implements ISelectMapWrapper, ClusterManager.OnClusterItemClickListener<GeoCache> {
    private ClusterManager<GeoCache> mClusterManager;
    private Set<Integer> cache = new HashSet<Integer>();

    public SelectGoogleMapWrapper(Context context, final GoogleMap mMap) {
        super(context, mMap);
        mClusterManager = new ClusterManager<GeoCache>(context, mMap);
        mClusterManager.setRenderer(new GeoCacheRenderer());

        mClusterManager.setOnClusterItemClickListener(this);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
    }

    @Override
    protected void onCameraChange(CameraPosition cameraPosition, ViewPortChangeListener listener) {
        super.onCameraChange(cameraPosition, listener);
        mClusterManager.onCameraChange(cameraPosition);
    }

    private class GeoCacheRenderer extends DefaultClusterRenderer<GeoCache> {
        public GeoCacheRenderer() {
            super(context, googleMap, mClusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(GeoCache geoCache, MarkerOptions markerOptions) {
            markerOptions.icon(BitmapDescriptorFactory
                    .fromResource(Controller.getInstance().getResourceManager().getMarkerResId(geoCache.getType(), geoCache.getStatus())))
                    .title(geoCache.getName());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<GeoCache> cluster, MarkerOptions markerOptions) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(Controller.getInstance().getResourceManager().getMarkerResId(GeoCacheType.GROUP, null)));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            return cluster.getSize() > 1;
        }
    }

    @Override
    public void updateGeoCacheMarkers(List<GeoCache> geoCacheList) {
        final Iterator<GeoCache> iterator = geoCacheList.iterator();
        while (iterator.hasNext()) {
            final GeoCache next = iterator.next();
            if (cache.contains(next.getId())) {
                iterator.remove();
            } else {
                cache.add(next.getId());
            }
        }
        mClusterManager.addItems(geoCacheList);
    }

    @Override
    public boolean onClusterItemClick(GeoCache item) {
        NavigationManager.startInfoActivity(context, item);
        return true;
    }
}
