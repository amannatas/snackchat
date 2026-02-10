package com.example.snakchatai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.snakchatai.utils.FirebaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class splash_screen extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(() -> {

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // User is logged in
                getFCMTokenAndProceed();
            } else {
                // User is not logged in
                startActivity(new Intent(splash_screen.this, login_screen.class));
                finish();
            }

        }, SPLASH_DELAY);
    }

    private void getFCMTokenAndProceed() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken", token);
            }
            startActivity(new Intent(splash_screen.this, MainActivity.class));
            finish();
        });
    }
}
