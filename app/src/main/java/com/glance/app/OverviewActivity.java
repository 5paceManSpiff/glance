package com.glance.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class OverviewActivity extends Activity {

    private ImageButton back;
    private ListView listView;
    private ArrayList<Overview> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);

        data = new ArrayList<Overview>();

        setupViews();
    }

    private void setupViews() {
        back = (ImageButton) findViewById(R.id.ab_back);
        listView = (ListView) findViewById(R.id.overview_list);

        OverviewAdapter adapter = new OverviewAdapter(this, R.layout.overview_row);
        listView.setAdapter(adapter);

        for (int i = 0; i < 25; i++) {
            data.add(i, new Overview("Joe Shmoe", i));
        }

        adapter.addAll(data);
    }

    public void backListener(View v) {
        Toast.makeText(getApplicationContext(), "logging out", Toast.LENGTH_SHORT).show();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_LOGGED_IN, false);
        editor.commit();
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    public class Overview {
        public String sender;
        public int messageCount;

        public Overview(String s, int m) {
            sender = s;
            messageCount = m;
        }
    }

    private class OverviewAdapter extends ArrayAdapter<Overview> {

        private Context context;
        private int resource;

        public OverviewAdapter(Context c, int r) {
            super(c, r);
            context = c;
            resource = r;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                convertView = inflater.inflate(resource, parent, false);
            }

            TextView sender = (TextView) convertView.findViewById(R.id.overview_row_sender);
            TextView count = (TextView) convertView.findViewById(R.id.overview_row_count);

            sender.setText(data.get(position).sender);
            count.setText(String.valueOf(data.get(position).messageCount));

            return convertView;
        }
    }
}
