package com.example.snakchatai;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class chat_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_screen);
        View root = findViewById(R.id.main);
        View bottomLayout = findViewById(R.id.bottom_layout);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Bottom layout ko nav bar ke upar push karo
            bottomLayout.setPadding(
                    bottomLayout.getPaddingLeft(),
                    bottomLayout.getPaddingTop(),
                    bottomLayout.getPaddingRight(),
                    systemBars.bottom
            );

            return insets;
        });


    }
}