package com.fiture.close.game.server.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * <pre>
 *  author : juneYang
 *  time   : 2020/12/04 6:43 PM
 *  desc   :Apk工具类
 *  version: 1.0
 * </pre>
 */
public class ApkUtils {
    private static final String TAG = "ApkUtils";

    /**
     * 获取已安装apk的PackageInfo
     */
    public static PackageInfo getInstalledApkPackageInfo(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);

        for (PackageInfo packageinfo : apps) {
            String thisName = packageinfo.packageName;
            if (thisName.equals(packageName)) {
                return packageinfo;
            }
        }

        return null;
    }

    /**
     * 判断apk是否已安装
     */
    public static boolean isInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        boolean installed = false;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return installed;
    }

    /**
     * 获取已安装Apk文件的源Apk文件
     */
    public static String getSourceApkPath(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return appInfo.sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 安装Apk
     *
     * @param context
     * @param apkPath
     */
    public static void installApk(Context context, String apkPath) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + apkPath), "application/vnd.android.package-archive");

        context.startActivity(intent);
    }

    /**
     * 判断程序是否已在运行
     */
    public static boolean isTopActivity(Context context, String packageName) {
        Log.d(TAG, "**********************top packageName:" + packageName);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);

        if (tasksInfo.size() > 0) {
            Log.d(TAG, "*********************curr packageName:" + tasksInfo.get(0).topActivity.getPackageName());
            // 应用程序位于堆栈的顶层
            return packageName.equals(tasksInfo.get(0).topActivity.getPackageName());
        }
        return false;
    }

}
