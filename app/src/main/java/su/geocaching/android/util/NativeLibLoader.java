package su.geocaching.android.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import su.geocaching.android.log.Logger;

/**
 * @author Yuri Denison
 * @since 03.09.2014
 */
public class NativeLibLoader {
    private static final String TAG = "NativeLibLoader";


    public static void initNativeLibs(Context context) {
        String packageName = context.getPackageName();
        String dataPath = context.getFilesDir().getAbsolutePath();
        int packageVersion = 0;
        try {
            packageVersion = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        File destLocalFile = new File(dataPath + "/libtimg_" + packageVersion + ".so");

        if (Build.VERSION.SDK_INT >= 9) {
            String apkPath = context.getApplicationInfo().sourceDir;
            String nativeDir = context.getApplicationInfo().nativeLibraryDir;
            File destFile = new File(nativeDir + "/libtimg.so");

            if (destLocalFile.exists() || destFile.exists()) {
                if (destFile.exists()) {
                    Logger.d(TAG, "Library installed");
                    System.loadLibrary("timg");
                } else {
                    Logger.d(TAG, "Local library installed");
                    System.load(destLocalFile.getAbsolutePath());
                }
                return;
            } else {
                Logger.w(TAG, "Library NOT installed");
            }

            // Try to self-recover
            String folder;
            if (Build.CPU_ABI.equalsIgnoreCase("armeabi-v7a")) {
                // ARM7 recover
                folder = "armeabi-v7a";
            } else if (Build.CPU_ABI.equalsIgnoreCase("armeabi")) {
                folder = "armeabi";
            } else if (Build.CPU_ABI.equalsIgnoreCase("x86")) {
                folder = "x86";
            } else if (Build.CPU_ABI.equalsIgnoreCase("mips")) {
                folder = "mips";
            } else {
                Logger.w(TAG, "Unknown arch: " + Build.CPU_ABI);
                return;
            }

            Logger.w(TAG, "Selected recover arch: " + folder);

            ZipFile zipFile = null;
            InputStream stream = null;
            try {
                zipFile = new ZipFile(apkPath);
                ZipEntry entry = zipFile.getEntry("lib/" + folder + "/libtimg.so");
                if (entry == null) {
                    throw new Exception("Unable to find file in apk:" + "lib/" + folder + "/libtimg.so");
                }
                stream = zipFile.getInputStream(entry);
                IOUtils.copy(stream, destLocalFile);
                System.load(destLocalFile.getAbsolutePath());
                return;
            } catch (Exception e) {
                Logger.t(TAG, e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Try load anyway
//        System.loadLibrary("timg");
    }
}
