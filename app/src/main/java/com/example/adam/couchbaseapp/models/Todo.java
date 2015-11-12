package com.example.adam.couchbaseapp.models;

import android.graphics.Color;
import android.util.Log;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by adam on 11/11/15.
 */
public class Todo {

    // TODO: Find a better, more global home for this one
    public final static String DOC_TYPE = "type";
    public final static String TYPE = "todo";
    public final static String TITLE = "title";
    public final static String CREATED = "created";
    public final static String DUE = "due";
    public final static String ORDER = "order";
    public final static String DONE = "done";

    private final static String LOG_TAG = "Todo";

    private Document mDocument;
    private String mTitle;
    private Date mCreated;
    private int mOrder;
    private Boolean mDone;
    private Date mDue;

    public Todo(Database database, String title) {
        this(database, title, -1);
    }

    public Todo(Database database, String title, int order) {
        Map<String, Object> todoData = new HashMap<>();

        mTitle = title;
        mCreated = Calendar.getInstance().getTime();
        mOrder = order;
        mDone = false;
        mDue = null;

        mDocument = database.createDocument();
        try {
            mDocument.putProperties(todoData);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot write document to database");
        }
    }

    public Todo(Document doc) {
        mDocument = doc;

        Map<String, Object> properties = new HashMap<>();
        properties.putAll(mDocument.getProperties());

        mTitle = (String) properties.get(Todo.TITLE);
        mCreated = dateFromString((String) properties.get(Todo.CREATED));
        mOrder = (int) properties.get(Todo.ORDER);
        mDone = (Boolean) properties.get(Todo.DONE);
        mDue = dateFromString((String) properties.get(Todo.DUE));
    }

    public void save() {
        Map<String, Object> properties = new HashMap<>();
        properties.putAll(mDocument.getProperties());

        properties.put(Todo.DOC_TYPE, Todo.TYPE);
        properties.put(Todo.TITLE, mTitle);
        properties.put(Todo.CREATED, timeStamp(mCreated));
        properties.put(Todo.ORDER, mOrder);
        properties.put(Todo.DONE, mDone);
        properties.put(Todo.DUE, timeStamp(mDue));

        try {
            mDocument.putProperties(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        try {
            mDocument.delete();
        } catch (Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Todo)) return false;

        Todo other = (Todo) o;

        return getDocID().equals(other.getDocID());
    }

    // Properties
    public Boolean isDone() {
        return mDone;
    }

    public String getDocID() { return mDocument.getId(); }
    public String getTitle() { return mTitle; }
    public Date getCreated() { return mCreated; }
    public int getOrder() { return mOrder; }
    public static int getEnabledColor() { return Color.DKGRAY; }
    public static int getDisabledColor() { return  Color.LTGRAY; }

    public void toggleDone() {
        mDone = !isDone();
        // TODO: Don't force save here, let app decide when to save
        save();
    }

    // Helpers
    // TODO: Move to time helper
    public String timeStamp(Date date) {
        if (date == null) return null;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        return formatter.format(date);
    }

    public Date dateFromString(String date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
