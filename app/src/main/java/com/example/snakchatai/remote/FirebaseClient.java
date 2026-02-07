package com.example.snakchatai.remote;

import androidx.annotation.NonNull;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.ErrorCallBack;
import com.example.snakchatai.utils.NewEventCallBack;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.Objects;

public class FirebaseClient {

    private final Gson gson = new Gson();
    private final DatabaseReference dbRef =
            FirebaseDatabase.getInstance().getReference("webrtc_calls");

    private final String currentUsername;
    private static final String LATEST_EVENT_FIELD = "latest_event";

    // 🔹 Username constructor (LoginUsernameActivity se aayega)
    public FirebaseClient(String currentUsername) {
        this.currentUsername = currentUsername;

        // ensure user node exists
        dbRef.child(currentUsername).child("online").setValue(true);
    }

    // 🔹 Dusre user ko signaling message bhejna
    public void sendMessageToOtherUser(
            DataModel dataModel,
            ErrorCallBack errorCallBack
    ) {
        dbRef.child(dataModel.getTarget())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        dbRef.child(dataModel.getTarget())
                                .child(LATEST_EVENT_FIELD)
                                .setValue(gson.toJson(dataModel));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        errorCallBack.onError();
                    }
                });
    }

    // 🔹 Apne liye aane wale offer / answer / ICE sunna
    public void observeIncomingLatestEvent(NewEventCallBack callBack) {
        dbRef.child(currentUsername)
                .child(LATEST_EVENT_FIELD)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (snapshot.getValue() == null) return;

                            String json = Objects.requireNonNull(
                                    snapshot.getValue()).toString();

                            DataModel dataModel =
                                    gson.fromJson(json, DataModel.class);

                            callBack.onNewEventReceived(dataModel);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
}
