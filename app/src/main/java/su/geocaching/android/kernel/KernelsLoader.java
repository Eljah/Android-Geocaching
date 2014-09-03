package su.geocaching.android.kernel;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.CopyOnWriteArrayList;

import su.geocaching.android.log.Logger;

/**
 * @author Yuri Denison
 * @since 03.09.2014
 */
public class KernelsLoader {
    private static final String TAG = "KernelsLoader";

    public static interface KernelsLoadingListener {
        public void onKernelsLoaded();
    }

    private CopyOnWriteArrayList<KernelsLoadingListener> listeners = new CopyOnWriteArrayList<KernelsLoadingListener>();
    private Handler handler = new Handler(Looper.getMainLooper());

    private Thread loaderThread;
    private volatile boolean isLoaded = false;

    public void addListener(KernelsLoadingListener loadingListener) {
        if (listeners.contains(loadingListener)) {
            return;
        }
        listeners.add(loadingListener);
    }

    public void removeListener(KernelsLoadingListener listener) {
        listeners.remove(listener);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    private void notifyLoaded() {
        Logger.d(TAG, "Loaded");
        isLoaded = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (KernelsLoadingListener loadingListener : listeners) {
                    loadingListener.onKernelsLoaded();
                }
            }
        });
    }

    private void ensureLoaded(ApplicationKernel kernel) {
        // nothing
    }

    public boolean stagedLoad(final ApplicationKernel kernel) {
        long initStart = SystemClock.uptimeMillis();

        // None of this objects starts doing something in background until someone ack them about this
        // At this stage nothing might request for data from storage kernel
//        kernel.initTechKernel(); // Technical information about environment. Might be loaded first.
//        kernel.initAuthKernel(); // Authentication kernel. Might be loaded before other kernels.
//
//        kernel.initSettingsKernel(); // User app settings
//        kernel.initFileKernel(); // Uploading/Downloading files
//        kernel.initSearchKernel(); // Searching in app
//
//        kernel.initApiKernel(); // Initializing api kernel
//        kernel.initStorageKernel(); // Database kernel
//
//        Logger.d(TAG, "Kernels created in " + (SystemClock.uptimeMillis() - initStart) + " ms");
//
//        kernel.runKernels();

        ensureLoaded(kernel);

        Logger.d(TAG, "Kernels loaded in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        notifyLoaded();
        return true;
    }
}

