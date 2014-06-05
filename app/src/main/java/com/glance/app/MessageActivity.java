package com.glance.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;


public class MessageActivity extends Activity {

    public static final String TAG = "Message";

    private TextView name;
    private SharedPreferences prefs;
    private ListView listView;
    private MessageAdapter adapter;
    private String friend;
    private LinearLayout btns;
    private Button btn;
    private Button send;
    private ArrayList<String> messages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message);

        messages = new ArrayList<String>();
        messages.add("hi");
        messages.add("brb");
        messages.add("gtg");
        messages.add("lol");
        messages.add("haha");
        messages.add("xd");
        messages.add("jk");
        messages.add("thx");
        messages.add("cos");

        prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        friend = getIntent().getExtras().getString("friend");

        setupViews();

        new GetMessagesRequest().execute();
    }

    public void backListener(View v) {
        Intent i = new Intent(this, OverviewActivity.class);
        startActivity(i);
    }

    public void refreshListener(View v) {
        new GetMessagesRequest().execute();
    }

    private void setupViews() {
        name = (TextView) findViewById(R.id.ab_name);
        listView = (ListView) findViewById(R.id.message_list);
        btns = (LinearLayout) findViewById(R.id.message_btns);
        send = (Button) findViewById(R.id.message_send);

        name.setText(friend);

        adapter = new MessageAdapter(this, R.layout.message_row);
        listView.setAdapter(adapter);

        for (final String msg : messages) {
            btn = (Button) btns.inflate(this, R.layout.message_btn, null);
            btn.setText(msg);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    send.setText(send.getText().toString() + msg + " ");
                }
            });
            btns.addView(btn);
        }

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!send.getText().toString().isEmpty()) new SendMessageRequest(send.getText().toString()).execute();
            }
        });
    }

    public class Message {

        public static final int ME = 0;
        public static final int YOU = 1;

        public String msg;
        public int sender;

        public Message(String m, int s) {
            msg = m;
            sender = s;
        }
    }

    private class MessageAdapter extends ArrayAdapter<Message> {

        private Context context;
        private int resource;

        public MessageAdapter(Context c, int r) {
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

            TextView msg = (TextView) convertView.findViewById(R.id.message_row_msg);
            msg.setText(getItem(position).msg);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) msg.getLayoutParams();

            if (getItem(position).sender == Message.ME) {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                msg.setBackgroundColor(getResources().getColor(R.color.color_2));
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                msg.setBackgroundColor(getResources().getColor(R.color.color_4));
            }

            msg.setLayoutParams(params);

            return convertView;
        }
    }

    private class GetMessagesRequest extends AsyncTask<Void, Void, HttpResponse> {

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            String name = prefs.getString(Constants.PREF_USERNAME, "");
            String pass = prefs.getString(Constants.PREF_PASSWORD, "");

            try {
                StringEntity input = new StringEntity("{\"username\":\"" + name + "\", \"password\":\"" + pass + "\", \"friend\":\"" + friend + "\"}");
                input.setContentType("application/json");
                HttpResponse response = Request.make("http://aaronlandis.io:8000/getmessages", input);
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
                    String raw = Request.readResponse(response);
                    try {
                        adapter.clear();
                        JSONArray array = new JSONArray(raw);
                        for (int i = 0; i < array.length(); i++) {
                            String sender = array.getJSONObject(i).getString("sender");
                            String message = array.getJSONObject(i).getString("message");
                            int who;
                            if (sender.equals(friend)) {who = Message.YOU;}
                            else {who = Message.ME;}
                            adapter.add(new Message(message, who));
                        }
                        adapter.notifyDataSetChanged();
                        Log.i(TAG, "updated messages for " + friend);
                    } catch (Exception e) {
                        // TODO manage json exception
                        Log.e(TAG, e.getMessage());
                    }
                    break;
                case 400:
                    Log.i(TAG, "error getting messages");
                    break;
            }
        }
    }

    private class SendMessageRequest extends AsyncTask<Void, Void, HttpResponse> {

        private String message;

        public SendMessageRequest(String m) {message = m;}

        @Override
        protected void onPreExecute() {
            send.setClickable(false);
            send.setText("...sending...");
        }

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            String name = prefs.getString(Constants.PREF_USERNAME, "");
            String pass = prefs.getString(Constants.PREF_PASSWORD, "");

            try {
                String messageFinal = name + ":" + message;
                StringEntity input = new StringEntity("{\"username\":\"" + name + "\", \"password\":\"" + pass + "\", \"message\":\"" + messageFinal + "\", \"friend\":\"" + friend + "\"}");
                input.setContentType("application/json");
                HttpResponse response = Request.make("http://aaronlandis.io:8000/sendmessage", input);
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
                send.setClickable(true);
                return;
            } else {
                code = response.getStatusLine().getStatusCode();
            }

            switch (code) {
                case 200:
                    Log.i(TAG, "message sent");
                    new GetMessagesRequest().execute();
                    break;
                case 400:
                    Log.i(TAG, "error sending message");
                    break;
            }

            send.setClickable(true);
            send.setText("");
        }
    }
}
