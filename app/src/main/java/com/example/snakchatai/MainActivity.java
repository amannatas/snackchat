package com.example.snakchatai;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;

import android.util.Log;
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

import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.repository.MainRepository;
import com.example.snakchatai.utils.FirebaseUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
DrawerLayout drawerLayout;
NavigationView navigationView;
Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        FirebaseUtil.getCurrentUsername(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                // login state broken
                startActivity(new Intent(this, login_screen.class));
                finish();
                return;
            }

            UserModel user = task.getResult().toObject(UserModel.class);

            if (user == null || user.getUsername() == null) {
                // username missing = NO CALL
                startActivity(new Intent(this, LoginUsernameActivity.class));
                finish();
                return;
            }

            // 🔥 NOW AND ONLY NOW WebRTC login
            MainRepository.getInstance().login(
                    user.getUsername(),
                    this,
                    () -> {
                        Log.d("WEBRTC", "WebRTC login success");
                    }
            );
        });



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
                    FirebaseAuth.getInstance().signOut();

                    SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isLoggedIn", false);
                    editor.apply();
                    Intent iout = new Intent(MainActivity.this, splash_screen.class);
                    startActivity(iout);
                    finish();

                } else if (id == R.id.setting) {
                    Toast.makeText(MainActivity.this, "No internet", Toast.LENGTH_SHORT).show();

                } else if (id == R.id.account) {
                    Toast.makeText(MainActivity.this, "No internet", Toast.LENGTH_SHORT).show();

                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        getFCMToken();
    }
    void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String token = task.getResult();
                Log.i("my token",token);

            }
        });
    }


}
