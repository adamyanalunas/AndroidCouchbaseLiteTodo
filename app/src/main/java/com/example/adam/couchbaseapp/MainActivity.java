package com.example.adam.couchbaseapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toolbar;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.example.adam.couchbaseapp.models.Todo;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";

    private TodoAdapter mAdapter;
    private CouchDatabase mCouchbase = CouchDatabase.getInstance();
    private Database mDatabase = CouchDatabase.getInstance().getDatabase();
    private FloatingActionButton mFab;
    private ListView mTodoList;
    private ArrayList<Todo> mTodoArray;

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

        addLongPressDelete(mTodoList);
        addTapHandler(mTodoList);

        if (mDatabase != null) {
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
    private void populateEntries() {
        try {
            QueryEnumerator queryResult = mAdapter.getActiveQuery().run();
            for (Iterator<QueryRow> it = queryResult; it.hasNext(); ) {
                QueryRow row = it.next();
                addEntry(row.getDocument());
                mAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Listeners
    // TODO: Receive broadcast to remove CBL dependency
    private void listenForDatabaseChanges() {
        mDatabase.addChangeListener(new Database.ChangeListener() {
            public void changed(Database.ChangeEvent event) {
                for (int i = 0; i < event.getChanges().size(); i++) {
                    Document retrievedDocument = mDatabase.getDocument(event.getChanges().get(i).getDocumentId());
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
        return new Todo(mDatabase, title);
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.toggle_visible) {
            Boolean hideCompleted = mAdapter.getAreAllVisible();
            int titleID = hideCompleted ? R.string.show_all_todos : R.string.hide_completed_todos;
            String title = getResources().getString(titleID);
            item.setTitle(title);

            mAdapter.setAreAllVisible(!hideCompleted);

            mTodoArray.clear();
            populateEntries();
            mAdapter.notifyDataSetChanged();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
