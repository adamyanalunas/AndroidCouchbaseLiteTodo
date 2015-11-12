package com.example.adam.couchbaseapp;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.adam.couchbaseapp.models.Todo;

import java.util.ArrayList;

/**
 * Created by adam on 11/10/15.
 */
public class TodoAdapter extends BaseAdapter {

    public final static String QUERY_ALL_TODOS = "todos";

    private Context mContext;
    private ArrayList<Todo> mTodoList;

    public TodoAdapter(Context context, ArrayList<Todo> todoList) {
        mContext = context;
        mTodoList = todoList;
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
}
