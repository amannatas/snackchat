package com.example.snakchatai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class login_screen extends AppCompatActivity {
EditText phonenum;
Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);

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

            String phone = "+91" + input;

            Intent intent = new Intent(login_screen.this, LoginOtpActivity.class);
            intent.putExtra("phone", phone);

            startActivity(intent);   // ✅ YAHI FIX HAI
        });




    }
}