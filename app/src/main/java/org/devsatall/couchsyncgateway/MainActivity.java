package org.devsatall.couchsyncgateway;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;

import java.net.URI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Database database;
    ResultSet result;
    Endpoint targetEndpoint;
    Query query;
    Button btnGuardar;
    EditText edtName, edtPriority;
    MutableDocument mutableDoc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnGuardar = (Button) findViewById(R.id.btnGuardar);
        edtName = (EditText) findViewById(R.id.edtNameTarea);
        edtPriority = (EditText) findViewById(R.id.edtPriority);
        btnGuardar.setOnClickListener(this);
        Context context = getApplicationContext();
        DatabaseConfiguration config = new DatabaseConfiguration(context);
        try {
            database = new Database(BuildConfig.DB, config);
            Log.i("error", database.getName());
            Toast.makeText(this, String.valueOf(database.getCount()),
                    Toast.LENGTH_LONG).show();
        }catch(CouchbaseLiteException e){
            e.printStackTrace();
        }

        // Create a query to fetch documents of type SDK.
        try {
            query = QueryBuilder.select(SelectResult.all()).from(DataSource.database(database));
        }catch(Exception e){
            e.printStackTrace();
        }
        try {
            result = query.execute();
        }catch (Exception e){

        }
        Log.i("test", "Number of rows ::  " + result.allResults().size());
    }

    @Override
    public void onClick(View view) {
        // Create a new document (i.e. a record) in the database.
        mutableDoc = new MutableDocument()
                .setString("name", edtName.getText().toString())
                .setString("priority", edtPriority.getText().toString());

        // Save it to the database.
        try {
            database.save(mutableDoc);
            Toast.makeText(MainActivity.this, "Datos guardados!",Toast.LENGTH_LONG).show();
            sync();
            clearData();
        }catch(CouchbaseLiteException e){
            e.printStackTrace();
        }
    }

    public void sync(){
        try {
            // Create replicators to push and pull changes to and from the cloud.
            targetEndpoint = new URLEndpoint(new URI(BuildConfig.ENDPOINT));
        }catch (Exception e){
            e.printStackTrace();
        }
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(database, targetEndpoint);
        replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

        // Add authentication.
        replConfig.setAuthenticator(new BasicAuthenticator(BuildConfig.USER,
                BuildConfig.PASSWORD));

        // Create replicator (be sure to hold a reference somewhere that will prevent the Replicator from being GCed)
        Replicator replicator = new Replicator(replConfig);

        // Listen to replicator change events.
        replicator.addChangeListener(new ReplicatorChangeListener() {
            @Override
            public void changed(@NonNull ReplicatorChange change) {
                if (change.getStatus().getError() != null) {
                    Log.i("test", "Error code ::  " + change.getStatus().getError().getMessage());
                }
            }
        });

        // Start replication.
        replicator.start();
    }

    public void clearData(){
        edtName.setText("");
        edtPriority.setText("");
    }
}
