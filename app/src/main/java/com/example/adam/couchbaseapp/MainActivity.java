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
import com.example.adam.couchbaseapp.models.Todo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    public final static String DATABASE_NAME = "mydb";

    private Manager mCouchbaseManager;
    private Database mCouchbaseDatabase;
    private ListView mTodoList;
    private ArrayList<Todo> mTodoArray;
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
        mTodoArray = new ArrayList<>();
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
            e.printStackTrace();
        }
    }

    // Queries
    private void createViews() {
        com.couchbase.lite.View allTodos = mCouchbaseDatabase.getView(TodoAdapter.QUERY_ALL_TODOS);
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
    // TODO: Update to receive broadcast from database change
    public void removeEntry(Document document) {
        int documentIndex = mAdapter.getIndexOf(document.getId());
        if (documentIndex > -1) {
            mTodoArray.remove(documentIndex);
        }
    }

    // TODO: Update to receive broadcast from database change
    public void addEntry(Document doc) {
        Todo todo = new Todo(doc);
        // NOTE: Gee, it'd be great if a Set was available here
        if (mTodoArray.contains(todo)) return;
        mTodoArray.add(todo);
    }

    public Todo createTodo(String title) {
        return new Todo(mCouchbaseDatabase, title);
    }

    // Gestures
    private void addLongPressDelete(final ListView list) {
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Permanently delete todo?");

                final Todo todo = (Todo) list.getItemAtPosition(position);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        todo.delete();
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
                final Todo todo = (Todo) list.getItemAtPosition(position);
                todo.toggleDone();
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
                createTodo(input.getText().toString()).save();
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
