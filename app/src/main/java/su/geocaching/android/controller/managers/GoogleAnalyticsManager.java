package su.geocaching.android.controller.managers;

import android.content.Context;

import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import su.geocaching.android.ui.BuildConfig;
import su.geocaching.android.ui.R;

public class GoogleAnalyticsManager {
    private final Context context;

    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    synchronized Tracker getTracker() {
        if (!mTrackers.containsKey(TrackerName.GLOBAL_TRACKER)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            analytics.setDryRun(BuildConfig.DEBUG);

            Tracker t = analytics.newTracker(R.xml.global_analytics_tracker);
            mTrackers.put(TrackerName.GLOBAL_TRACKER, t);
        }
        return mTrackers.get(TrackerName.GLOBAL_TRACKER);
    }

    public GoogleAnalyticsManager(Context context) {
        this.context = context;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler instanceof ExceptionReporter) {
            ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
            exceptionReporter.setExceptionParser(new AnalyticsExceptionParser());
        }
    }

    public void trackActivityLaunch(String activityName) {
        final Tracker tracker = getTracker();
        tracker.setAppName(activityName);
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public void trackExternalActivityLaunch(String activityName) {
        trackActivityLaunch("/external" + activityName);
    }

    public void trackCaughtException(String tag, Throwable ex) {
//        final Tracker tracker = getTracker();
//        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public void trackError(String tag, String message) {
//        EasyTracker.getTracker().sendException(String.format("Error in class %s : %s", tag, message), false);
    }

    private class AnalyticsExceptionParser implements ExceptionParser {

        public String getDescription(String thread, Throwable throwable) {
            return String.format("Thread: %s, Exception: %s", thread, getStackTrace(throwable));
        }

        private String getStackTrace( Throwable ex) {
            final StringWriter stackTrace = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stackTrace);
            ex.printStackTrace(printWriter);
            return stackTrace.toString();
        }
    }
}
