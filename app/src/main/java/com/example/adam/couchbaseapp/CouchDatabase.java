package com.example.adam.couchbaseapp;

import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

/**
 * Created by adam on 11/10/15.
 */
public class CouchDatabase {
    public final static String DATABASE_NAME = "mydb";
    public final static String LOG_TAG = "CouchDatabase";

    private Manager mCouchbaseManager;
    private Database mCouchbaseDatabase;
    private static CouchDatabase ourInstance = new CouchDatabase();

    public static CouchDatabase getInstance() { return ourInstance; }
    public Database getDatabase() { return mCouchbaseDatabase; }

    private CouchDatabase() {
        try {
            mCouchbaseManager = new Manager(new AndroidContext(MyApplication.getContext()), Manager.DEFAULT_OPTIONS);
            mCouchbaseDatabase = mCouchbaseManager.getDatabase(DATABASE_NAME);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Damn it. Database creation failed.", e);
        }
    }
}
