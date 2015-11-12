package com.example.adam.couchbaseapp;

import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.android.AndroidContext;
import com.example.adam.couchbaseapp.models.Todo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by adam on 11/10/15.
 */
public class CouchDatabase {
    public final static String DATABASE_NAME = "mydb";
    public final static String LOG_TAG = "CouchDatabase";
    public final static String QUERY_ACTIVE_TODOS = "QUERY_ACTIVE_TODOS";
    public final static String QUERY_ALL_TODOS = "todos";

    private Manager mCouchbaseManager;
    private Database mCouchbaseDatabase;
    private static CouchDatabase ourInstance = new CouchDatabase();

    public static CouchDatabase getInstance() { return ourInstance; }
    public Database getDatabase() { return mCouchbaseDatabase; }

    public enum Sort {
        Ascending, Descending
    }

    private CouchDatabase() {
        try {
            mCouchbaseManager = new Manager(new AndroidContext(MyApplication.getContext()), Manager.DEFAULT_OPTIONS);
            mCouchbaseDatabase = mCouchbaseManager.getDatabase(DATABASE_NAME);

            if (mCouchbaseDatabase != null) {
                createViews();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Damn it. Database creation failed.", e);
        }
    }

    private void createViews() {
        com.couchbase.lite.View allTodos = mCouchbaseDatabase.getView(QUERY_ALL_TODOS);
        allTodos.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get(Todo.DOC_TYPE).equals(Todo.TYPE)) {
                    List<Object> key = new ArrayList<>();
                    key.add(document.get(Todo.CREATED));
                    emitter.emit(key, document);
                }
            }
        }, "0.2");

        com.couchbase.lite.View activeTodos = mCouchbaseDatabase.getView(QUERY_ACTIVE_TODOS);
        activeTodos.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get(Todo.DOC_TYPE).equals(Todo.TYPE) && (Boolean) document.get(Todo.DONE) == false) {
                    List<Object> key = new ArrayList<>();
                    key.add(document.get(Todo.CREATED));
                    emitter.emit(key, document);
                }
            }
        }, "0.1");
    }

    private Query allTodos() {
        return mCouchbaseDatabase.createAllDocumentsQuery();
    }

    public Query dateSorted(Sort sort) {
        Query query = mCouchbaseDatabase.getView(QUERY_ALL_TODOS).createQuery();
        query.setDescending(sort.equals(Sort.Descending));

        return query;
    }

    public Query dateSortedActive(Sort sort) {
        Query query = mCouchbaseDatabase.getView(QUERY_ACTIVE_TODOS).createQuery();
        query.setDescending(sort.equals(Sort.Descending));

        return query;
    }
}
