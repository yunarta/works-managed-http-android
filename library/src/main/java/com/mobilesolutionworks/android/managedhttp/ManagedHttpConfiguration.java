package com.mobilesolutionworks.android.managedhttp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by yunarta on 17/5/15.
 */
class ManagedHttpConfiguration {

    protected static ManagedHttpConfiguration INSTANCE;

    public static ManagedHttpConfiguration configure(Context context) {
        if (INSTANCE == null) {
            Context applicationContext = context.getApplicationContext();

            Bundle metaData;
            try {
                ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                metaData = ai.metaData;
            } catch (PackageManager.NameNotFoundException e) {
                metaData = new Bundle();
            }

            if (!metaData.containsKey("works.managedhttp.service.get") ||
                    !metaData.containsKey("works.managedhttp.service.class") ||
//                    !metaData.containsKey("works.managedhttp.service.clear") ||
                    !metaData.containsKey("works.managedhttp.authority")) {
                throw new IllegalStateException("works.managedhttp.service OR works.managedhttp.authority not configured properly");
            }

            INSTANCE = new ManagedHttpConfiguration(
                    metaData.getString("works.managedhttp.service.get"),
                    "" /*metaData.getString("works.managedhttp.service.clear")*/,
                    new Uri.Builder().scheme("content").authority(metaData.getString("works.managedhttp.authority")).build(),
                    metaData.getString("works.managedhttp.service.class")
            );
        }

        return INSTANCE;
    }

    public final Uri authority;

    public final String action;

    public final String clearCookie;

    public Class className;

    protected ManagedHttpConfiguration(String action, String clearCookie, Uri authority, String className) {
        this.action = action;
        this.clearCookie = clearCookie;
        this.authority = authority;
        try {
            this.className = Class.forName(className);
        } catch (ClassNotFoundException e) {
            this.className = null;
        }
    }


}
