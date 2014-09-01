package su.geocaching.android.ui.selectmap;

import android.os.AsyncTask;
import su.geocaching.android.controller.apimanager.DownloadGeoCachesTask;
import su.geocaching.android.controller.apimanager.GeoRect;
import su.geocaching.android.controller.managers.LogManager;
import su.geocaching.android.model.GeoCache;

import java.util.List;

/**
 * Keeps the state of current select map
 */
public class SelectMapViewModel {
    private static final String TAG = SelectMapViewModel.class.getCanonicalName();

    private SelectMapActivity activity;

    private DownloadGeoCachesTask downloadTask = null;

    public void beginUpdateGeocacheOverlay(GeoRect viewPort) {
        LogManager.d(TAG, "Update rectangle %s", viewPort);
        cancelDownloadTask();
        downloadTask = new DownloadGeoCachesTask(this);
        downloadTask.execute(viewPort);
        onShowDownloadingInfo();
    }

    public void geocacheListDownloaded(List<GeoCache> geoCacheList) {
        onHideDownloadingInfo();
        if (geoCacheList == null || geoCacheList.size() == 0) {
            return;
        }
        activity.updateGeoCacheMarkers(geoCacheList);
    }

    private synchronized void cancelDownloadTask() {
        if (downloadTask != null && !downloadTask.isCancelled()) {
            // don't interrupt if already running
            downloadTask.cancel(false);
        }
    }

    private synchronized void onHideDownloadingInfo() {
        if (activity != null) {
            activity.hideDownloadingInfo();
        }
    }

    private synchronized void onShowDownloadingInfo() {
        if (activity != null) {
            activity.showDownloadingInfo();
        }
    }

    public synchronized void registerActivity(SelectMapActivity activity) {
        if (this.activity != null) {
            LogManager.e(TAG, "Attempt to register activity while activity is not null");
        }
        this.activity = activity;
        // display [grouped] caches
        if (downloadTask != null && !downloadTask.isCancelled() && (downloadTask.getStatus() != AsyncTask.Status.FINISHED)) {
            onShowDownloadingInfo();
        }
    }

    public synchronized void unregisterActivity(SelectMapActivity activity) {
        if (this.activity == null) {
            LogManager.e(TAG, "Attempt to unregister activity while activity is null");
        }
        this.activity = null;
        cancelDownloadTask();
    }
}