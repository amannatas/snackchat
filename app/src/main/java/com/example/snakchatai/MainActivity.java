package com.example.snakchatai;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
DrawerLayout drawerLayout;
NavigationView navigationView;
Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navview);
        toolbar = findViewById(R.id.toolbar);
        
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.OpenDrawer, R.string.CloseDrawer);
        drawerLayout.addDrawerListener(toggle);

        toggle.syncState();
        // ✅ ADD BACK HANDLER AFTER drawerLayout IS INITIALIZED
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id ==R.id.optNots){
                    Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.password_layout);
                    dialog.setCancelable(false);
                    EditText MPIN = dialog.findViewById(R.id.MPIN);
                    Button btnenter = dialog.findViewById(R.id.btnenter);

                    btnenter.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String password = MPIN.getText().toString();
                            if (password.equals("7410")) {
                                Intent imain = new Intent(MainActivity.this, fake_chat.class);
                                startActivity(imain);
                                dialog.dismiss();
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "Worng password", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    dialog.show();

                }else if (id == R.id.logout){
                    logout();

                } else if (id == R.id.setting) {
                    Toast.makeText(MainActivity.this, "No internet", Toast.LENGTH_SHORT).show();

                } else if (id == R.id.account) {
                    Toast.makeText(MainActivity.this, "No internet", Toast.LENGTH_SHORT).show();

                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", false).apply();

        Intent intent = new Intent(MainActivity.this, splash_screen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
