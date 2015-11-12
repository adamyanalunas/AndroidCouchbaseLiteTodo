package com.example.adam.couchbaseapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.couchbase.lite.Query;
import com.example.adam.couchbaseapp.models.Todo;

import java.util.ArrayList;

/**
 * Created by adam on 11/10/15.
 */
public class TodoAdapter extends BaseAdapter {

    private final static String SHOW_ALL_KEY = "showAll";

    private Context mContext;
    private SharedPreferences mPreferences;
    private Boolean mShowAll;
    private ArrayList<Todo> mTodoList;

    public TodoAdapter(Context context, ArrayList<Todo> todoList) {
        mContext = context;
        mTodoList = todoList;

        // TODO: Move out to custom perference manager to remove dependency
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mShowAll = mPreferences.getBoolean(SHOW_ALL_KEY, true);
    }

    public int getCount() {
        return mTodoList.size();
    }

    public Object getItem(int position) {
        return mTodoList.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public int getIndexOf(String id) {
        for(int i = 0; i < mTodoList.size(); i++) {
            if(mTodoList.get(i).getDocID().equals(id)) {
                return i;
            }
        }

        return -1;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView todoTitle = null;
        Todo currentTodo;

        try {
            if(convertView == null) {
                todoTitle = new TextView(mContext);
                todoTitle.setPadding(10, 25, 10, 25);
            } else {
                todoTitle = (TextView) convertView;
            }

            // TODO: Move all of this view config to a custom view that listens & responds to data changes
            currentTodo = mTodoList.get(position);
            todoTitle.setText(currentTodo.getTitle());

            int textColor = currentTodo.isDone() ? currentTodo.getDisabledColor() : currentTodo.getEnabledColor();
            todoTitle.setTextColor(textColor);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return todoTitle;
    }
    // Properties
    public Query getActiveQuery() {
        CouchDatabase couchDatabase = CouchDatabase.getInstance();
        return mShowAll ? couchDatabase.dateSorted(CouchDatabase.Sort.Ascending) : couchDatabase.dateSortedActive(CouchDatabase.Sort.Ascending);
    }

    public Boolean getAreAllVisible() {
        return mShowAll;
    }

    public void setAreAllVisible(Boolean allVisible) {
        mShowAll = allVisible;
        mPreferences.edit().putBoolean(SHOW_ALL_KEY, allVisible).apply();
    }
}
