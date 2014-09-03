package su.geocaching.android;

import android.app.Application;

import su.geocaching.android.kernel.ApplicationKernel;
import su.geocaching.android.kernel.KernelsLoader;
import su.geocaching.android.reflection.CrashHandler;

/**
 * @author Grigory Kalabin. grigory.kalabin@gmail.com
 * @since April 2011
 */
public class GeocachingApplication extends Application {
    private ApplicationKernel kernel;
    private KernelsLoader kernelsLoader;

    @Override
    public void onCreate() {
        if (kernel != null) {
            super.onCreate();
            return;
        }

//        NativeLibLoader.initNativeLibs(this);

        CrashHandler.init(this);
        kernel = new ApplicationKernel(this);
        super.onCreate();

        kernelsLoader = new KernelsLoader();
        kernelsLoader.stagedLoad(kernel);
    }
}
