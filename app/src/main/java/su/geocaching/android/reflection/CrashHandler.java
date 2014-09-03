package su.geocaching.android.reflection;

import com.crashlytics.android.Crashlytics;

import su.geocaching.android.GeocachingApplication;
import su.geocaching.android.log.Logger;

public class CrashHandler {
    public static void init(GeocachingApplication application) {

        Crashlytics.start(application);

        // Flushing logs to disk
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    Logger.t("UNHANDLED", ex);
                    Logger.dropOnCrash();
                } catch (Throwable t) {

                }
                originalHandler.uncaughtException(thread, ex);
            }
        });
    }

    public static void logHandledException(Throwable e) {
        Crashlytics.logException(e);
    }

    public static void setUid(int uid, int dcId, String key) {
        Crashlytics.setInt("dc_id", dcId);
        Crashlytics.setString("auth_key_id", key);
        Crashlytics.setUserIdentifier("" + uid);
    }

    public static void removeUid() {
        Crashlytics.setInt("dc_id", 0);
        Crashlytics.setString("auth_key_id", "");
        Crashlytics.setUserIdentifier("" + 0);
    }
}
