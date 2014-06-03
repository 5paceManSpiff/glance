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
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class LoginActivity extends Activity {

    public static final String TAG = "Login";

    public static final String LOGIN_WAIT = "...";
    public static final String LOGIN_GO = "login";

    private SharedPreferences prefs;
    private EditText username;
    private EditText password;
    private Button login;

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
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.PREF_USERNAME, username.getText().toString());
        editor.putString(Constants.PREF_PASSWORD, hash(password.getText().toString(), Constants.SALT));
        editor.commit();
        new LoginRequest().execute();
    }

    private String hash(String s, String salt) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            String fin = s + salt;
            digest.update(fin.getBytes());
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

    private class LoginRequest extends AsyncTask<Void, Void, HttpResponse> {

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            String name = prefs.getString(Constants.PREF_USERNAME, "");
            String pass = prefs.getString(Constants.PREF_PASSWORD, "");

            try {
                StringEntity input = new StringEntity("{\"username\":\"" + name + "\", \"password\":\"" + pass + "\"}");
                input.setContentType("application/json");
                HttpResponse response = Request.make("http://aaronlandis.io:8000/login", input);
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
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(Constants.PREF_LOGGED_IN, true);
                    editor.commit();
                    Intent i = new Intent(getApplicationContext(), OverviewActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    overridePendingTransition(0, android.R.anim.fade_in);
                    Log.i(TAG, "success");
                    break;
                case 400:
                    Toast.makeText(getApplicationContext(), "login failed", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "failed");
                    break;
            }
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
