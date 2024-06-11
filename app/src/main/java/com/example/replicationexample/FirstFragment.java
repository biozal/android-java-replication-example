package com.example.replicationexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.couchbase.lite.*;

import android.util.Log;

import com.example.replicationexample.databinding.FragmentFirstBinding;

import java.net.URI;
import java.net.URISyntaxException;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private Database database;
    private Collection defaultCollection;
    private Replicator replicator;
    private ListenerToken token;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            //WARNING
            //realistically this should be all done on a background thread, not on the UI thread
            //if you pull down a lot of documents, expect the UI to hang while it processes them


            //get a database config and set the path to the app's files directory in a place you can
            //write to in Android
            DatabaseConfiguration config = new DatabaseConfiguration();
            config.setDirectory(getContext().getFilesDir().getCanonicalPath());

            //create a database or open an existing database
            database = new Database("mydb", config);

            //get the default collection and scope
            defaultCollection = database.getCollection("_default", "_default");
            Log.d("::ReplicationExample::", "Initialized a database");

            binding.buttonFirst.setOnClickListener(v -> {
                try {
                    ReplicatorConfiguration replConfig = new ReplicatorConfiguration(
                            new URLEndpoint(new URI("wss://xxxxxxxxxx.apps.cloud.couchbase.com:4984/travel")))  //replace URL with your endpoint from App Services - Connection Settings tab
                            .addCollection(defaultCollection, new CollectionConfiguration())
                            .setType(ReplicatorType.PUSH_AND_PULL)
                            .setContinuous(true)
                            .setAuthenticator(new BasicAuthenticator("xxxxx", "xxxxx".toCharArray())); //replace username and password with your username and password created in App Services
                    replicator = new Replicator(replConfig);
                    token = replicator.addChangeListener(new ReplicatorChangeListener() {
                        @Override
                        public void changed(ReplicatorChange change) {
                            binding.textviewFirst.append( String.format("Replicator state :: ActivityLevel: %s Progress Completed: %d Progress Total: %d\n\n",
                                    change.getStatus().getActivityLevel(),
                                    change.getStatus().getProgress().getCompleted(),
                                    change.getStatus().getProgress().getTotal()));
                            binding.textviewFirst.append(String.format("Document Count: %d\n", defaultCollection.getCount()));
                        }
                    });
                    replicator.start();

                } catch (URISyntaxException e) {
                    Log.e("::ReplicationExample:: URI Syntax Exception", String.format("Error: %s Stack Trace: %s", e.getMessage(), e.getStackTrace().toString()));
                }
            });
        } catch (CouchbaseLiteException e) {
            Log.e("::ReplicationExample:: Coucbase Lite Exception", String.format("Error: %s Stack Trace: %s", e.getMessage(), e.getStackTrace().toString()));
        } catch (Exception e) {
            Log.e("::ReplicationExample::", String.format("Error: %s Stack Trace: %s", e.getMessage(), e.getStackTrace().toString()));
        }
    }

    @Override
    public void onDestroyView() {

        try {
            //realistically this should be managed at the app level not view level, but need
            //to clean up resources
            token.remove();
            replicator.stop();
            database.close();
        } catch (CouchbaseLiteException e) {
            Log.e("::ReplicationExample:: Coucbase Lite Exception", String.format("Error: %s Stack Trace: %s", e.getMessage(), e.getStackTrace().toString()));
        }

        super.onDestroyView();
        binding = null;
    }

}