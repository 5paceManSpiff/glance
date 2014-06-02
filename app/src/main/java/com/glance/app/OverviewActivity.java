package com.glance.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;


public class OverviewActivity extends Activity {

    public ImageButton back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);

        setupViews();
    }

    private void setupViews() {
        back = (ImageButton) findViewById(R.id.ab_back);
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
}
