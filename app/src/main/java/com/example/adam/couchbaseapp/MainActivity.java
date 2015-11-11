package com.example.adam.couchbaseapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toolbar;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    public final static String DATABASE_NAME = "mydb";

    private Manager mCouchbaseManager;
    private Database mCouchbaseDatabase;
    private ListView mTodoList;
    private ArrayList<Document> mTodoArray;
    private TodoAdapter mAdapter;
    private FloatingActionButton mFab;

    public enum Sort {
        Ascending, Descending
    }

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        mTodoList = (ListView) findViewById(R.id.todo_list);
        mTodoArray = new ArrayList<Document>();
        mAdapter = new TodoAdapter(this, mTodoArray);
        mTodoList.setAdapter(mAdapter);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTodo();
            }
        });
//        mTodoList.setOnTouchListener(new ShowHideOnScroll(mFab));

        setupDatabase();
        addLongPressDelete(mTodoList);
        addTapHandler(mTodoList);

        if (mCouchbaseDatabase != null) {
            createViews();
            listenForDatabaseChanges();
            populateEntries();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mFab.setOnClickListener(null);
    }

    // Setup
    private void setupDatabase() {
        try {
            mCouchbaseManager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            mCouchbaseDatabase = mCouchbaseManager.getDatabase(DATABASE_NAME);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Damn it. Database creation failed.", e);
            return;
        }
    }

    private void populateEntries() {
        try {
            QueryEnumerator queryResult = activeQuery().run();
            for (Iterator<QueryRow> it = queryResult; it.hasNext(); ) {
                QueryRow row = it.next();
                addEntry(row.getDocument());
                mAdapter.notifyDataSetChanged();
            }
        } catch (Exception e ) {
            Log.e(LOG_TAG, "All documents query failed");
        }
    }

    // Queries
    private void createViews() {
        com.couchbase.lite.View allTodos = mCouchbaseDatabase.getView(TodoAdapter.QUERY_ALL_TODOS);
        allTodos.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.get(TodoAdapter.DOC_TYPE).equals(TodoAdapter.TODO_TYPE)) {
                    List<Object> key = new ArrayList<>();
                    key.add(document.get(TodoAdapter.TODO_CREATED));
                    emitter.emit(key, document);
                }
            }
        }, "0.2");
    }

    private Query allTodos() {
        return mCouchbaseDatabase.createAllDocumentsQuery();
    }

    public Query dateSorted(Sort sort) {
        Query query = mCouchbaseDatabase.getView(TodoAdapter.QUERY_ALL_TODOS).createQuery();
        query.setDescending(sort.equals(Sort.Descending));

        return query;
    }

    public Query activeQuery() {
        return dateSorted(Sort.Ascending);
    }

    // Listeners
    private void listenForDatabaseChanges() {
        mCouchbaseDatabase.addChangeListener(new Database.ChangeListener() {
            public void changed(Database.ChangeEvent event) {
                for (int i = 0; i < event.getChanges().size(); i++) {
                    Document retrievedDocument = mCouchbaseDatabase.getDocument(event.getChanges().get(i).getDocumentId());
                    if (retrievedDocument.isDeleted()) {
                        removeEntry(retrievedDocument);
                    } else {
                        addEntry(retrievedDocument);
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // Data
    // TODO: Move to time helper
    public String currentTime() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        return formatter.format(Calendar.getInstance().getTime());
    }

    // TODO: Update to receive broadcast from database change
    public void removeEntry(Document document) {
        int documentIndex = mAdapter.getIndexOf(document.getId());
        if (documentIndex > -1) {
            mTodoArray.remove(documentIndex);
        }
    }

    // TODO: Update to receive broadcast from database change
    public void addEntry(Document document) {
        // NOTE: Gee, it'd be great if a Set was available here
        if (mTodoArray.contains(document)) return;
        mTodoArray.add(document);
    }

    // TODO: Move into model
    public void createTodo(String title) {
        Map<String, Object> todoData = new HashMap<>();

        todoData.put(TodoAdapter.DOC_TYPE, TodoAdapter.TODO_TYPE);
        todoData.put(TodoAdapter.TODO_TITLE, title);
        todoData.put(TodoAdapter.TODO_CREATED, currentTime());
        todoData.put(TodoAdapter.TODO_ORDER, -1);
        todoData.put(TodoAdapter.TODO_DONE, false);
        Log.e(LOG_TAG, "Create todo: " + todoData.toString());

        Document document = mCouchbaseDatabase.createDocument();
        try {
            document.putProperties(todoData);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot write document to database");
        }
    }

    public void toggleDone(Document document) {
        Boolean isDone = (Boolean) document.getProperty(TodoAdapter.TODO_DONE);
        Log.e(LOG_TAG, isDone.toString());

        Map<String, Object> properties = new HashMap<>();
        properties.putAll(document.getProperties());
        properties.put(TodoAdapter.TODO_DONE, !isDone);
        try {
            document.putProperties(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gestures
    private void addLongPressDelete(final ListView list) {
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Permanently delete todo?");

                final Document listItemDocument = (Document) list.getItemAtPosition(position);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            listItemDocument.delete();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error deleting", e);
                        }
                    }
                });
                builder.setNegativeButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(true);
                alert.show();
                return true;
            }
        });
    }

    private void addTapHandler(final ListView list) {
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Document listItemDocument = (Document) list.getItemAtPosition(position);
                toggleDone(listItemDocument);
            }
        });
    }

    // Actions
    public void addTodo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New TODO item");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createTodo(input.getText().toString());
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    // Options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.e(LOG_TAG, "Settings tapped");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
