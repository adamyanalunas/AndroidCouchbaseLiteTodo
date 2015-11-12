package com.example.adam.couchbaseapp;

import android.app.Application;
import android.content.Context;

/**
 * Created by adam on 11/12/15.
 */
public class MyApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
