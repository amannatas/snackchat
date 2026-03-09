package com.example.snakchatai.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.ErrorCallBack;
import com.example.snakchatai.utils.NewEventCallBack;
import com.google.firebase.database.*;
import com.google.gson.Gson;

public class FirebaseClient {

    private static final String TAG = "FirebaseClient";
    private final Gson gson = new Gson();
    private final DatabaseReference dbRef =
            FirebaseDatabase.getInstance().getReference("webrtc_calls");

    private final String currentUsername;
    private static final String SIGNALS_FIELD = "incoming_signals";

    private ChildEventListener childEventListener;

    public FirebaseClient(String currentUsername) {
        this.currentUsername = currentUsername;

        // 🚀 PRO-FIX 1: App launch par purana kachra saaf karo (No Ghost Calls)
        dbRef.child(currentUsername).child(SIGNALS_FIELD).removeValue();

        // 🚀 PRO-FIX 2: Presence & Crash Handling
        dbRef.child(currentUsername).child("online").setValue(true);
        dbRef.child(currentUsername).child("online").onDisconnect().setValue(false);
        // Agar net tute ya app crash ho, toh pending signals auto-delete kar do
        dbRef.child(currentUsername).child(SIGNALS_FIELD).onDisconnect().removeValue();
    }

    public void sendMessageToOtherUser(DataModel dataModel, ErrorCallBack errorCallBack) {
        Log.d(TAG, "Sending signal: " + dataModel.getType() + " to " + dataModel.getTarget());

        dbRef.child(dataModel.getTarget())
                .child(SIGNALS_FIELD)
                .push()
                .setValue(gson.toJson(dataModel))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Signal " + dataModel.getType() + " sent successfully!"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send signal: " + e.getMessage());
                    if (errorCallBack != null) errorCallBack.onError();
                });
    }

    public void observeIncomingLatestEvent(NewEventCallBack callBack) {
        // Purana listener clear karo agar multi-time call ho jaye
        clearListeners();

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    String json = snapshot.getValue(String.class);
                    if (json != null) {
                        DataModel dataModel = gson.fromJson(json, DataModel.class);
                        Log.d(TAG, "Received signal: " + dataModel.getType() + " from " + dataModel.getSender());

                        // UI ya Repository ko data bhejo
                        callBack.onNewEventReceived(dataModel);

                        // 🚀 PRO-FIX 3: Process hote hi securely Firebase se delete karo
                        snapshot.getRef().removeValue().addOnFailureListener(e ->
                                Log.e(TAG, "Failed to delete processed signal: " + e.getMessage())
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing incoming signal", e);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase DB listener cancelled: " + error.getMessage());
            }
        };

        dbRef.child(currentUsername)
                .child(SIGNALS_FIELD)
                .addChildEventListener(childEventListener);
    }

    public void clearListeners() {
        if (childEventListener != null) {
            dbRef.child(currentUsername)
                    .child(SIGNALS_FIELD)
                    .removeEventListener(childEventListener);
            childEventListener = null;
            Log.d(TAG, "Signaling listener cleared.");
        }
    }
}