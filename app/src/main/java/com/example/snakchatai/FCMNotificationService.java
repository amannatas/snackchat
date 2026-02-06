package com.example.snakchatai;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.snakchatai.utils.FirebaseUtil;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d("FCM", "TOKEN: " + token);

        if(FirebaseUtil.isLoggedIn()){
            FirebaseUtil.currentUserDetails().update("fcmToken", token);
        }
    }
}
