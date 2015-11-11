package com.example.adam.couchbaseapp;

/**
 * Created by adam on 11/10/15.
 */
public class CouchDatabase {
    private static CouchDatabase ourInstance = new CouchDatabase();

    public static CouchDatabase getInstance() {
        return ourInstance;
    }

    private CouchDatabase() {
    }
}
