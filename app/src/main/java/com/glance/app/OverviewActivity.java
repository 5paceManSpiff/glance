package com.glance.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class OverviewActivity extends Activity {

    public static final String TAG = "Overview";

    private ListView listView;
    private SharedPreferences prefs;
    private OverviewAdapter adapter;
    private TextView name;
    private ImageButton refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);

        prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);

        setupViews();

        new GetFriendsRequest(false).execute();
    }

    @Override
    public void onBackPressed() {
        logout();
        return;
    }

    private void setupViews() {
        listView = (ListView) findViewById(R.id.overview_list);
        name = (TextView) findViewById(R.id.ab_name);
        refresh = (ImageButton) findViewById(R.id.ab_refresh);

        name.setText(prefs.getString(Constants.PREF_USERNAME, ""));

        adapter = new OverviewAdapter(this, R.layout.overview_row);
        listView.setAdapter(adapter);
    }

    public void backListener(View v) {
        logout();
    }

    private void logout() {
        Toast.makeText(getApplicationContext(), "logging out", Toast.LENGTH_SHORT).show();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_LOGGED_IN, false);
        editor.commit();
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
    }

    public void refreshListener(View v) {
        new GetFriendsRequest().execute();
    }

    public class Overview {

        public static final int NORMAL = 0;
        public static final int ADD = 1;
        public static final int REFRESHING = 2;

        public int type;
        public String sender;
        public int messageCount;

        public Overview(String s, int m) {
            sender = s;
            messageCount = m;
            type = NORMAL;
        }

        public Overview(int t) {
            sender = "";
            messageCount = 0;
            type = t;
        }
    }

    private class OverviewAdapter extends ArrayAdapter<Overview> {

        private Context context;
        private int resource;

        public OverviewAdapter(Context c, int r) {
            super(c, r);
            context = c;
            resource = r;

            add(new Overview(Overview.ADD));
            listView.setOnItemClickListener(new ClickCallback());
        }

        public void notifyDataSetChanged(int t) {
            super.notifyDataSetChanged();
            insert(new Overview(t), 0);
        }

        private class ClickCallback implements AdapterView.OnItemClickListener {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

                    alert.setTitle("Add Friend");

                    final EditText input = new EditText(getContext());
                    input.setHint("username");
                    alert.setView(input);

                    alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new AddFriendRequest(input.getText().toString()).execute();
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    alert.show();
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                convertView = inflater.inflate(resource, parent, false);
            }

            TextView sender = (TextView) convertView.findViewById(R.id.overview_row_sender);
            TextView count = (TextView) convertView.findViewById(R.id.overview_row_count);
            TextView msg = (TextView) convertView.findViewById(R.id.overview_row_msg);
            sender.setText(getItem(position).sender);
            count.setText(String.valueOf(getItem(position).messageCount));

            switch (getItem(position).type) {
                case Overview.ADD:
                case Overview.REFRESHING:
                    sender.setVisibility(View.INVISIBLE);
                    count.setVisibility(View.INVISIBLE);
                    msg.setVisibility(View.VISIBLE);
                    msg.setText((getItem(position).type == Overview.ADD) ? "+" : "refreshing");
                    break;
                case Overview.NORMAL:
                    sender.setVisibility(View.VISIBLE);
                    count.setVisibility(View.VISIBLE);
                    msg.setVisibility(View.INVISIBLE);
                    break;
            }

            count.setVisibility(View.INVISIBLE);
            // TODO fix message count data

            return convertView;
        }
    }

    private class GetFriendsRequest extends AsyncTask<Void, Void, HttpResponse> {

        private boolean display;

        public GetFriendsRequest() {
            display = true;
        }

        public GetFriendsRequest(boolean d) {
            display = d;
        }

        @Override
        protected void onPreExecute() {
            refresh.setClickable(false);
            adapter.clear();
            if (display) adapter.notifyDataSetChanged(Overview.REFRESHING);
            else adapter.notifyDataSetChanged();
        }

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            String name = prefs.getString(Constants.PREF_USERNAME, "");
            String pass = prefs.getString(Constants.PREF_PASSWORD, "");

            try {
                StringEntity input = new StringEntity("{\"username\":\"" + name + "\", \"password\":\"" + pass + "\"}");
                input.setContentType("application/json");
                HttpResponse response = Request.make("http://aaronlandis.io:8000/getfriends", input);
                return response;
            } catch (IOException e) {
                // TODO input io exception
            }

            return null;
        }

        @Override
        protected void onPostExecute(HttpResponse response) {
            int code;
            if (response == null) {
                Toast.makeText(getApplicationContext(), "could not connect", Toast.LENGTH_SHORT).show();
                refresh.setClickable(true);
                return;
            } else {
                code = response.getStatusLine().getStatusCode();
            }

            switch (code) {
                case 200:
                    String raw = Request.readResponse(response);
                    try {
                        adapter.clear();
                        JSONArray array = new JSONArray(raw);
                        for (int i = 0; i < array.length(); i++) {
                            String friend = array.getString(i);
                            adapter.add(new Overview(friend, 0));
                            // TODO implement accurate message count
                        }
                        adapter.notifyDataSetChanged(Overview.ADD);
                        Log.i(TAG, "updated friends list");
                    } catch (Exception e) {
                        // TODO manage json exception
                        Log.e(TAG, e.getMessage());
                    }
                    break;
                case 400:
                    Log.i(TAG, "error getting friends");
                    break;
            }

            refresh.setClickable(true);
        }
    }

    private class AddFriendRequest extends AsyncTask<Void, Void, HttpResponse> {

        private String friend;

        public AddFriendRequest(String s) {
            friend = s;
        }

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            String name = prefs.getString(Constants.PREF_USERNAME, "");
            String pass = prefs.getString(Constants.PREF_PASSWORD, "");

            try {
                StringEntity input = new StringEntity("{\"username\":\"" + name + "\", \"password\":\"" + pass + "\", \"friend\":\"" + friend + "\"}");
                input.setContentType("application/json");
                HttpResponse response = Request.make("http://aaronlandis.io:8000/addfriend", input);
                return response;
            } catch (IOException e) {
                // TODO input io exception
            }

            return null;
        }

        @Override
        protected void onPostExecute(HttpResponse response) {
            int code;
            if (response == null) {
                Toast.makeText(getApplicationContext(), "could not connect", Toast.LENGTH_SHORT).show();
                return;
            } else {
                code = response.getStatusLine().getStatusCode();
            }

            switch (code) {
                case 200:
                    Log.i(TAG, "friend added successfully");
                    new GetFriendsRequest().execute();
                    break;
                case 400:
                    Toast.makeText(getApplicationContext(), "error adding friend", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "friend add failed");
                    break;
            }
        }
    }
}
