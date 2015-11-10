package com.example.adam.couchbaseapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    public final static String DATABASE_NAME = "mydb";

    private Manager couchbaseManager;
    private Database couchDatabase;
    private ListView todoList;
    private ArrayList<Document> todoArray;
    private TodoAdapter adapter;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.todoList = (ListView) findViewById(R.id.todo_list);
        this.todoArray = new ArrayList<Document>();
        this. adapter = new TodoAdapter(this, this.todoArray);
        this.todoList.setAdapter(this.adapter);

        this.fab = (FloatingActionButton) findViewById(R.id.fab);
        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTodo();
            }
        });
//        this.todoList.setOnTouchListener(new ShowHideOnScroll(this.fab));

        try {
            this.couchbaseManager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
            this.couchDatabase = this.couchbaseManager.getDatabase(DATABASE_NAME);
            Query allDocumentsQuery = couchDatabase.createAllDocumentsQuery();
            QueryEnumerator queryResult = allDocumentsQuery.run();
            for (Iterator<QueryRow> it = queryResult; it.hasNext();) {
                QueryRow row = it.next();
                todoArray.add(row.getDocument());
                adapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Damn it.", e);
            return;
        }

        if (this.couchDatabase != null) {
            this.couchDatabase.addChangeListener(new Database.ChangeListener() {
                public void changed(Database.ChangeEvent event) {
                    for(int i=0; i < event.getChanges().size(); i++) {
                        Document retrievedDocument = couchDatabase.getDocument(event.getChanges().get(i).getDocumentId());
                        if (retrievedDocument.isDeleted()) {
                            int documentIndex = adapter.getIndexOf(retrievedDocument.getId());
                            if (documentIndex > -1) {
                                todoArray.remove(documentIndex);
                            }
                        } else {
                            todoArray.add(retrievedDocument);
                        }

                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        this.todoList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Document listItemDocument = (Document) todoList.getItemAtPosition(position);
//                final int listItemIndex = position;
//                final CharSequence[] items = {"Delete Item"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Permanently delete todo?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            try {
                                listItemDocument.delete();
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error deleting", e);
                            }
                        }
                    }
                });
                builder.setNegativeButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
//                builder.setItems(items, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (which == 0) {
//                            try {
//                                listItemDocument.delete();
//                            } catch (Exception e) {
//                                Log.e(LOG_TAG, "Error deleting", e);
//                            }
//                        }
//                    }
//                });
                AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(true);
                alert.show();
                return true;
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.fab.setOnClickListener(null);
    }

    public void addTodo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New TODO item");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, Object> doContent = new HashMap<String, Object>();
                doContent.put(TodoAdapter.TODO_TITLE, input.getText().toString());
                Document document = couchDatabase.createDocument();
                try {
                    document.putProperties(doContent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Cannot write document to database");
                }
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


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
