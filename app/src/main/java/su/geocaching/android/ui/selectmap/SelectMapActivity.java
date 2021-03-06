package su.geocaching.android.ui.selectmap;

import android.app.Dialog;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;
import su.geocaching.android.controller.Controller;
import su.geocaching.android.controller.apimanager.GeoRect;
import su.geocaching.android.controller.managers.*;
import su.geocaching.android.model.GeoCache;
import su.geocaching.android.model.MapInfo;
import su.geocaching.android.ui.R;
import su.geocaching.android.ui.map.GeocodeTask;
import su.geocaching.android.ui.map.ScaleView;
import su.geocaching.android.ui.map.ViewPortChangeListener;
import su.geocaching.android.ui.preferences.MapPreferenceActivity;

import java.util.List;

public class SelectMapActivity extends SlidingActivity implements IConnectionAware, ILocationAware {
    private static final String TAG = SelectMapActivity.class.getCanonicalName();
    private static final String SELECT_ACTIVITY_FOLDER = "/SelectActivity";
    private static final int ENABLE_CONNECTION_DIALOG_ID = 0;

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private ISelectMapWrapper mapWrapper;

    private LowPowerUserLocationManager locationManager;
    private ConnectionManager connectionManager;
    private ProgressBar progressCircle;
    private TextView connectionInfoTextView;
    private TextView downloadingInfoTextView;
    private TextView groupingInfoTextView;
    private MenuItem searchMenuItem;
    private ScaleView scaleView;

    private Toast tooManyCachesToast;
    private Toast statusNullLastLocationToast;

    private SelectMapViewModel selectMapViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.d(TAG, "onCreate");

        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidthRes(R.dimen.shadow_width);
        sm.setShadowDrawable(R.drawable.shadow);
        sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        sm.setFadeDegree(0.35f);
        sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setBehindContentView(R.layout.map_sliding_menu);


        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.select_map_activity);
//        getSupportActionBar().setHomeButtonEnabled(true);

        scaleView = (ScaleView)findViewById(R.id.scaleView);

        progressCircle = (ProgressBar) findViewById(R.id.progressCircle);

        connectionInfoTextView = (TextView) findViewById(R.id.connectionInfoTextView);
        groupingInfoTextView = (TextView) findViewById(R.id.groupingInfoTextView);
        downloadingInfoTextView = (TextView) findViewById(R.id.downloadingInfoTextView);

        setUpMapIfNeeded();

        locationManager = Controller.getInstance().getLowPowerLocationManager();
        connectionManager = Controller.getInstance().getConnectionManager();

        selectMapViewModel = Controller.getInstance().getSelectMapViewModel();
        Controller.getInstance().getGoogleAnalyticsManager().trackActivityLaunch(SELECT_ACTIVITY_FOLDER);
    }

    private void updateMapInfoFromSettings() {
        MapInfo lastMapInfo = Controller.getInstance().getPreferencesManager().getLastSelectMapInfo();
        mapWrapper.restoreMapState(lastMapInfo);
    }

    private void saveMapInfoToSettings() {
        MapInfo mapInfo = mapWrapper.getMapState();
        Controller.getInstance().getPreferencesManager().setLastSelectMapInfo(mapInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogManager.d(TAG, "onResume");

        scaleView.setVisibility(Controller.getInstance().getPreferencesManager().isMapScaleVisible() ? View.VISIBLE : View.GONE);
        setUpMapIfNeeded();

        // update mapView setting in case they were changed in preferences
        mapWrapper.updateMapLayer();
//        mapWrapper.setZoomControlsEnabled(Controller.getInstance().getPreferencesManager().isZoomButtonsVisible());
        // add subscriber to connection manager
        connectionManager.addSubscriber(this);
        // ask to enable if disabled
        if (!connectionManager.isActiveNetworkConnected()) {
            showDialog(ENABLE_CONNECTION_DIALOG_ID);
        }
        locationManager.addSubscriber(this);
        // set user location
        updateLocationOverlay(locationManager.getLastKnownLocation());
        // update mapView center and zoom level
        updateMapInfoFromSettings();
        // register activity against view model
        selectMapViewModel.registerActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogManager.d(TAG, "onPause");
        // unsubscribe  form location and connection manager
        locationManager.removeSubscriber(this);
        connectionManager.removeSubscriber(this);
        saveMapInfoToSettings();
        // don't keep reference to this activity in view model
        selectMapViewModel.unregisterActivity(this);
    }

    /**
     * Creating menu object
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.select_map_menu, menu);

        if (Geocoder.isPresent()) {
            //Create the search view
            SearchView searchView = createSearchView();

            searchMenuItem = menu.add(R.string.menu_search);
            searchMenuItem.setIcon(R.drawable.ic_menu_search);
            searchMenuItem.setActionView(searchView);
            searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        }

        return true;
    }

    private SearchView createSearchView() {
        SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint(this.getString(R.string.select_map_search_query_hint));

        final SelectMapActivity activity = this;
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) searchMenuItem.collapseActionView();
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                new GeocodeTask(activity).execute(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return searchView;
    }

    public void animateTo(LatLng point) {
        mapWrapper.animateToGeoPoint(point);
    }

    @Override
    public boolean onSearchRequested() {
        if (searchMenuItem != null) {
            searchMenuItem.expandActionView();
            return true;
        }
        return false;
    }

    /**
     * Called when menu element selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavigationManager.startDashboardActivity(this);
                return true;
            case R.id.menu_mylocation:
                onMyLocationClick();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, MapPreferenceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateLocationOverlay(Location location) {
        LogManager.d(TAG, "updateLocationOverlay");
        if (location != null) {
            mapWrapper.updateLocationMarker(location);
        }
    }

    public synchronized void updateGeoCacheMarkers(List<GeoCache> geoCachesList) {
        LogManager.d(TAG, "geoCachesList updated; size: %d", geoCachesList.size());
        mapWrapper.updateGeoCacheMarkers(geoCachesList);
    }

    public void hideDownloadingInfo() {
        downloadingInfoTextView.setVisibility(View.GONE);
        updateProgressCircleVisibility();
    }

    public void showDownloadingInfo() {
        downloadingInfoTextView.setVisibility(View.VISIBLE);
        updateProgressCircleVisibility();
    }

    private void updateProgressCircleVisibility() {
        if (downloadingInfoTextView.getVisibility() == View.VISIBLE || groupingInfoTextView.getVisibility() == View.VISIBLE) {
            //setSupportProgressBarIndeterminateVisibility(true);
            progressCircle.setVisibility(View.VISIBLE);
        } else {
            //setSupportProgressBarIndeterminateVisibility(false);
            progressCircle.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConnectionLost() {
        connectionInfoTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionFound() {
        // TODO: make text shorter and use INVISIBLE instead of GONE
        connectionInfoTextView.setVisibility(View.GONE);
    }

    protected Dialog onCreateDialog(int id) {
        return (id == ENABLE_CONNECTION_DIALOG_ID) ? new EnableConnectionDialog(this) : null;
    }

    private void onMyLocationClick() {
        final Location lastLocation = locationManager.getLastKnownLocation();
        if (lastLocation != null) {
            mapWrapper.animateToLocation(lastLocation);
        } else {
            if (statusNullLastLocationToast == null) {
                statusNullLastLocationToast = Toast.makeText(getBaseContext(), getString(R.string.status_null_last_location), Toast.LENGTH_SHORT);
            }
            statusNullLastLocationToast.show();
        }
    }

    @Override
    public void updateLocation(Location location) {
        updateLocationOverlay(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView
     * MapView}) will show a prompt for the user to install/update the Google Play services APK on
     * their device.
     * <p/>
     * A user can return to this Activity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the Activity may not have been
     * completely destroyed during this process (it is likely that it would only be stopped or
     * paused), {@link #onCreate(Bundle)} may not be called again so we should call this method in
     * {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            mMap = mapFragment.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            } else {
                Toast.makeText(this,  this.getString(R.string.error_map_creation), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    SupportMapFragment mapFragment;

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mapWrapper = new SelectGoogleMapWrapper(this, mMap);

        mapWrapper.setViewPortChangeListener(new ViewPortChangeListener() {
            @Override
            public void onViewPortChanged(GeoRect viewPort) {
                View mapView = mapFragment.getView();
                if (mapView != null) {
                    scaleView.updateMapViewPort(viewPort);
                    selectMapViewModel.beginUpdateGeocacheOverlay(viewPort);
                } else {
                    LogManager.e(TAG, "mapView is Null");
                }
            }
        });

        mapWrapper.setupMyLocationLayer();
    }
}