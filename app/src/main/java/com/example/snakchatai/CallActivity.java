package com.example.snakchatai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.snakchatai.databinding.ActivityCallBinding;
import com.example.snakchatai.repository.MainRepository;
import com.example.snakchatai.utils.DataModelType;
import com.example.snakchatai.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.webrtc.RendererCommon;

public class CallActivity extends AppCompatActivity implements MainRepository.Listener {

    private ActivityCallBinding views;
    private MainRepository mainRepository;

    private boolean isCameraMuted = false;
    private boolean isMicrophoneMuted = false;

    private static final int PERMISSION_REQUEST_CODE = 101;

    private String targetUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        if (getIntent() != null && getIntent().hasExtra("targetUserId")) {
            targetUserId = getIntent().getStringExtra("targetUserId");
        }

        if (hasPermissions()) {
            onPermissionGranted();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                onPermissionGranted();
            } else {
                Toast.makeText(
                        this,
                        "Camera & Microphone permission required",
                        Toast.LENGTH_LONG
                ).show();
                onBackPressed();
            }
        }
    }

    private void onPermissionGranted() {
        mainRepository = MainRepository.getInstance();
        mainRepository.listener = this;

        mainRepository.login(
                FirebaseUtil.currentUserId(),
                this,
                this::initUI
        );
    }

    private void initUI() {
        views.localView.setMirror(true);
        views.localView.setZOrderMediaOverlay(true);
        views.remoteView.setZOrderMediaOverlay(true);

        mainRepository.initLocalView(views.localView);
        mainRepository.initRemoteView(views.remoteView);

        if (targetUserId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(targetUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            if (doc != null && doc.exists()) {
                                String otherUsername = doc.getString("username");

                                if (otherUsername == null || otherUsername.isEmpty()) {
                                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                views.incomingNameTV.setText("Calling " + otherUsername);
                                mainRepository.sendCallRequest(targetUserId, () ->
                                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                                );

                            } else {
                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to fetch user", Toast.LENGTH_SHORT).show();
                        }
                    });
        }


        mainRepository.subscribeForLatestEvent(data -> {
            if (data.getType() == DataModelType.StartCall) {
                runOnUiThread(() -> {
                    views.incomingNameTV.setText(data.getSender() + " is calling you");
                    views.incomingCallLayout.setVisibility(View.VISIBLE);

                    views.acceptButton.setOnClickListener(v -> {
                        mainRepository.startCall(data.getSender());
                        views.incomingCallLayout.setVisibility(View.GONE);
                    });

                    views.rejectButton.setOnClickListener(v -> {
                        views.incomingCallLayout.setVisibility(View.GONE);
                        mainRepository.rejectCall();
                    });
                });
            }
        });

        views.switchCameraButton.setOnClickListener(v ->
                mainRepository.switchCamera()
        );

        views.micButton.setOnClickListener(v -> {
            mainRepository.toggleAudio(isMicrophoneMuted);
            isMicrophoneMuted = !isMicrophoneMuted;
        });

        views.videoButton.setOnClickListener(v -> {
            mainRepository.toggleVideo(isCameraMuted);
            isCameraMuted = !isCameraMuted;
        });

        views.endCallButton.setOnClickListener(v -> {
            mainRepository.endCall();
        });
    }

    @Override
    public void webrtcConnected() {
        runOnUiThread(() -> {
            views.incomingCallLayout.setVisibility(View.GONE);

            views.callLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void webrtcClosed() {
        runOnUiThread(this::finish);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainRepository.endCall();
    }
}
