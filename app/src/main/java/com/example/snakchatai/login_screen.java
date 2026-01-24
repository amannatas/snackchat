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
EditText phonenum, password;
Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_screen);

        phonenum = findViewById(R.id.phonenum);
        password = findViewById(R.id.password);
        login = findViewById(R.id.loginButton);


        Intent imain = new Intent(login_screen.this, MainActivity.class);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phone = phonenum.getText().toString().trim();
                String pass = password.getText().toString().trim();

                if (phone.equals("9639810546") && pass.equals("7410")) {
                    startActivity(imain);
                } else {
                    phonenum.setError("Wrong Phone Number or Password");
                }
            }
        });


    }
}