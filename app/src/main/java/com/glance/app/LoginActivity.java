package com.glance.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity {

    public static final String TAG = "Login";

    public static final String LOGIN_WAIT = "...";
    public static final String LOGIN_GO = "login";

    public SharedPreferences prefs;
    public EditText username;
    public EditText password;
    public Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_LOGGED_IN, false)) {
            Intent i = new Intent(getApplicationContext(), OverviewActivity.class);
            startActivity(i);
        }

        setupViews();
    }

    private void setupViews() {
        username = (EditText) findViewById(R.id.login_username);
        password = (EditText) findViewById(R.id.login_password);
        login = (Button) findViewById(R.id.login_button);

        String name = prefs.getString(Constants.PREF_USERNAME, "");
        username.setText(name);
        if (!name.isEmpty()) {
            password.requestFocus();
        }

        login.setClickable(false);
        login.setText(LOGIN_WAIT);

        username.addTextChangedListener(new LoginTextWatcher());
        password.addTextChangedListener(new LoginTextWatcher());
    }

    public void loginListener(View v) {
        new TestRequest().execute();
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.PREF_LOGGED_IN, true);
        editor.putString(Constants.PREF_USERNAME, username.getText().toString());
        editor.putString(Constants.PREF_PASSWORD, hash(password.getText().toString()));
        editor.commit();
        Intent i = new Intent(getApplicationContext(), OverviewActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(0, android.R.anim.fade_in);
    }

    private String hash(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (byte aMessageDigest : messageDigest) {
                hexString.append(Integer.toHexString(0xFF & aMessageDigest));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.i(TAG, e.getMessage());
        }

        return null;
    }

    private class TestRequest extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://aaronlandis.io:8000");

            try {
                List<NameValuePair> pairs = new ArrayList<NameValuePair>(1);
                pairs.add(new BasicNameValuePair("username", "testname"));
                post.setEntity(new UrlEncodedFormEntity(pairs));

                HttpResponse response = client.execute(post);
            } catch (ClientProtocolException e) {
                // TODO login protocol exception
            } catch (IOException e) {
                // TODO login io exception
            }

            return null;
        }
    }

    private class LoginTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (username.getText().length() == 0 && password.getText().length() == 0) {
                login.setClickable(false);
                login.setText(LOGIN_WAIT);
            } else {
                login.setClickable(true);
                login.setText(LOGIN_GO);
            }
        }
    }
}
