package com.penn.jba;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;

import org.lasque.tusdk.core.TuSdk;

/**
 * Created by penn on 09/04/2017.
 */

public class PPApplication extends Application {
    private static Context appContext;

    @Override public void onCreate() {
        super.onCreate();
        appContext = this;

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        TuSdk.init(this.getApplicationContext(), "14da989436547ce3-00-bxlqq1");
        LeakCanary.install(this);
        // Normal app init code...
    }

    public static Context getContext(){
        return appContext;
    }
}