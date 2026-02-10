package com.example.snakchatai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakchatai.adapter.CallLogAdapter;
import com.example.snakchatai.model.CallLogModel;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class CallLogFragment extends Fragment {

    RecyclerView recyclerView;
    CallLogAdapter adapter;

    public CallLogFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_log, container, false);
        recyclerView = view.findViewById(R.id.call_log_recycler_view);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        Query query = FirebaseUtil.allUserCollectionReference().document(FirebaseUtil.currentUserId()).collection("calls").orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<CallLogModel> options = new FirestoreRecyclerOptions.Builder<CallLogModel>()
                .setQuery(query, CallLogModel.class).build();

        adapter = new CallLogAdapter(options, requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
