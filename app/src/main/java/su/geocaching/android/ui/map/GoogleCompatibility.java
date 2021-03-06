package su.geocaching.android.ui.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import su.geocaching.android.controller.managers.NavigationManager;

public class GoogleCompatibility {

    private final static String gmsAppId = "com.google.android.gms";

    public static boolean checkGoogleMapsDependencies(final Activity activity) {
        return
            checkApplicationIsAvailable(activity, gmsAppId, "Сервисы Google Play")
            && checkGooglePlayServicesAvailable(activity);
    }

    private static boolean checkGooglePlayServicesAvailable (final Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if(resultCode != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 69);
            if (dialog != null)
                dialog.show();
            else
                NavigationManager.startAndroidMarketActivity(activity, gmsAppId);
            return false;
        }

        return true;
    }

    private static boolean checkApplicationIsAvailable(final Context context, final String appId, final String appName) {
        if(!isApplicationInstalled(context, appId)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Для корректной работы приложения тербуется установить " + appName);
            builder.setCancelable(false);
            builder.setPositiveButton("Установить", getApplicationListener(context, appId));
            AlertDialog dialog = builder.create();
            dialog.show();
            return false;
        }
        return true;
    }

    private static boolean isApplicationInstalled(final Context context, final String appId)
    {
        try
        {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(appId, 0);
            return true;
        }
        catch(PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    private static DialogInterface.OnClickListener getApplicationListener(final Context context, final String appId)
    {
        return new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                NavigationManager.startAndroidMarketActivity(context, appId);
            }
        };
    }
}
