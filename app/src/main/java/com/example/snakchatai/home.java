package com.example.snakchatai;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.snakchatai.adapter.HomeUserAdapter;
import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class home extends Fragment {

    RecyclerView recyclerView;
    HomeUserAdapter adapter;

    public home() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.chat_recycler_view);

        // STEP 1 & 2: Yahan humne normal LinearLayoutManager ki jagah
        // apna naya SafeLinearLayoutManager use kiya hai.
        recyclerView.setLayoutManager(new SafeLinearLayoutManager(getContext()));

        // Firestore query
        Query query = FirebaseUtil.allUserCollectionReference()
                .whereNotEqualTo("userId", FirebaseUtil.currentUserId());

        FirestoreRecyclerOptions<UserModel> options =
                new FirestoreRecyclerOptions.Builder<UserModel>()
                        .setQuery(query, UserModel.class)
                        .build();

        adapter = new HomeUserAdapter(options, getContext());
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    // --- STEP 1: Safe Manager Class ---
    // Ye class crash ko catch karti hai jab hum back aate hain
    class SafeLinearLayoutManager extends LinearLayoutManager {
        public SafeLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Log.e("STC_DEBUG", "Inconsistency detected and handled safely!");
            }
        }
    }
}