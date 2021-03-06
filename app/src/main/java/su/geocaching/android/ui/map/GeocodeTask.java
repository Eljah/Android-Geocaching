package su.geocaching.android.ui.map;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import su.geocaching.android.controller.managers.LogManager;
import su.geocaching.android.ui.R;
import su.geocaching.android.ui.selectmap.SelectMapActivity;

import java.io.IOException;
import java.util.List;

public class GeocodeTask extends AsyncTask<String, Void, List<Address>> {

    private static final String TAG = GeocodeTask.class.getCanonicalName();

    private SelectMapActivity activity;

    public GeocodeTask(SelectMapActivity context) {
        this.activity = context;
    }

    @Override
    protected void onPreExecute() {
        LogManager.d(TAG, "onPreExecute");
    }

    @Override
    protected List<Address> doInBackground(String... query) {
        try {
            List<Address> addresses = new Geocoder(activity).getFromLocationName(query[0], 1);
            return addresses;
        } catch (IOException e) {
            LogManager.e(TAG, "Failed to connect to geocoder service", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Address> result) {
        LogManager.d(TAG, "onPostExecute");
        if (result == null || result.isEmpty()) {
            Toast toast = Toast.makeText(activity, R.string.select_map_nothing_found, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP| Gravity.CENTER_HORIZONTAL, 0, 100);
            toast.show();
            return;
        }
        Address firstAddress = result.get(0);
        activity.animateTo(new LatLng(firstAddress.getLatitude(), firstAddress.getLongitude()));
    }
}
