package com.example.snakchatai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

// Naye Imports jo OTP fix ke liye zaroori hain
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class login_screen extends AppCompatActivity {
    EditText phonenum;
    Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);

        // --- FIREBASE APP CHECK INITIALIZATION (Real OTP Fix) ---
        // Ye code Google ko batata hai ki aapki app genuine hai
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
        // -------------------------------------------------------

        phonenum = findViewById(R.id.phonenum);
        login = findViewById(R.id.loginButton);

        login.setOnClickListener(v -> {
            String input = phonenum.getText().toString().trim();

            if (input.isEmpty()) {
                phonenum.setError("Phone number required");
                return;
            }

            if (!input.matches("\\d{10}")) {
                phonenum.setError("Enter valid 10-digit number");
                return;
            }

            // Indian format (+91) automatic add ho raha hai
            String phone = "+91" + input;

            Intent intent = new Intent(login_screen.this, LoginOtpActivity.class);
            intent.putExtra("phone", phone);
            startActivity(intent);
        });
    }
}