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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakchatai.adapter.HomeUserAdapter;
import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.utils.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.Query;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class fake_chat extends AppCompatActivity {

    BottomNavigationView bnview;
    RecyclerView recyclerView;
    HomeUserAdapter adapter;

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

        // ---------- RecyclerView ----------
        recyclerView = findViewById(R.id.chat_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query query = FirebaseUtil.allUserCollectionReference()
                .whereNotEqualTo("userId", FirebaseUtil.currentUserId());

        FirestoreRecyclerOptions<UserModel> options =
                new FirestoreRecyclerOptions.Builder<UserModel>()
                        .setQuery(query, UserModel.class)
                        .build();

        adapter = new HomeUserAdapter(options, this);
        recyclerView.setAdapter(adapter);

        // ---------- Bottom Navigation ----------
        bnview = findViewById(R.id.bottombar);

        bnview.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int id = item.getItemId();

                if (id == R.id.home) {
                    // already on home
                    return true;

                } else if (id == R.id.call) {
                    startActivity(new Intent(fake_chat.this, call.class));
                    return true;

                } else if (id == R.id.icon) {
                    startActivity(new Intent(fake_chat.this, icon_change.class));
                    return true;
                }

                return false;
            }
        });
    }

    // ---------- Firestore lifecycle ----------
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}
