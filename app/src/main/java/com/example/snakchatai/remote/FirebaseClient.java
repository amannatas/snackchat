package com.example.snakchatai.remote;

import androidx.annotation.NonNull;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.ErrorCallBack;
import com.example.snakchatai.utils.NewEventCallBack;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.util.Objects;

public class FirebaseClient {

    private final Gson gson = new Gson();
    private final DatabaseReference dbRef =
            FirebaseDatabase.getInstance().getReference("webrtc_calls");

    private final String currentUsername;
    private static final String LATEST_EVENT_FIELD = "latest_event";

    private ValueEventListener eventListener;

    public FirebaseClient(String currentUsername) {
        this.currentUsername = currentUsername;
        dbRef.child(currentUsername).child("online").setValue(true);
    }

    public void sendMessageToOtherUser(
            DataModel dataModel,
            ErrorCallBack errorCallBack
    ) {
        dbRef.child(dataModel.getTarget())
                .child(LATEST_EVENT_FIELD)
                .setValue(gson.toJson(dataModel))
                .addOnFailureListener(e -> {
                    if (errorCallBack != null) errorCallBack.onError();
                });
    }

    public void observeIncomingLatestEvent(NewEventCallBack callBack) {

        eventListener = new ValueEventListener() {
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
        };

        dbRef.child(currentUsername)
                .child(LATEST_EVENT_FIELD)
                .addValueEventListener(eventListener);
    }

    // 🔥 IMPORTANT – call this in endCall or onDestroy
    public void clearListeners() {
        if (eventListener != null) {
            dbRef.child(currentUsername)
                    .child(LATEST_EVENT_FIELD)
                    .removeEventListener(eventListener);
            eventListener = null;
        }
    }
}