package com.example.snakchatai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class splash_screen extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);


            Intent nextIntent;
            if (isLoggedIn) {
                nextIntent = new Intent(splash_screen.this, MainActivity.class);
            } else {
                nextIntent = new Intent(splash_screen.this, login_screen.class);
            }

            startActivity(nextIntent);
            finish();
        }, SPLASH_DELAY);

    }
}
