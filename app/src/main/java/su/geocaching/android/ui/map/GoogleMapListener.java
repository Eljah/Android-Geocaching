package su.geocaching.android.ui.map;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Yuri Denison
 * @since 01/09/14
 */
public class GoogleMapListener implements GoogleMap.OnCameraChangeListener {
    private List<GoogleMap.OnCameraChangeListener> listeners = new LinkedList<GoogleMap.OnCameraChangeListener>();

    public void addListener(GoogleMap.OnCameraChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        for (GoogleMap.OnCameraChangeListener listener : listeners) {
            listener.onCameraChange(cameraPosition);
        }
    }
}
