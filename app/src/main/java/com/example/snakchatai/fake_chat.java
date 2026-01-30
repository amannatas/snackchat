package com.example.snakchatai;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class fake_chat extends AppCompatActivity {
BottomNavigationView bnview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fake_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent ihome = new Intent(fake_chat.this, chat_screen.class);
        Intent icall = new Intent(fake_chat.this, call.class);
        Intent iicon = new Intent(fake_chat.this, icon_change.class);
        bnview = findViewById(R.id.bottombar);
        bnview.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem Item) {
                int id = Item.getItemId();
                if(id == R.id.home){
                    startActivity(ihome);
                    finish();

                }else if (id == R.id.call){
                    startActivity(icall);
                }else if(id == R.id.icon){
                    startActivity(iicon);
                }
                return false;
            }
        });
    }
}