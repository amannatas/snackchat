package com.example.snakchatai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import com.example.snakchatai.databinding.ActivityCallBinding;
import com.example.snakchatai.repository.MainRepository;
import com.example.snakchatai.utils.DataModelType;
import com.example.snakchatai.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CallActivity extends AppCompatActivity implements MainRepository.Listener {

    private ActivityCallBinding views;
    private MainRepository mainRepository;

    private boolean isCameraMuted = false;
    private boolean isMicrophoneMuted = false;

    private static final int PERMISSION_REQUEST_CODE = 101;

    private String targetUserId;
    private boolean isCallEnded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());

        if (getIntent() != null && getIntent().hasExtra("targetUserId")) {
            targetUserId = getIntent().getStringExtra("targetUserId");
        }

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "Invalid call request", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showEndCallDialog();
            }
        });

        if (hasPermissions()) {
            onPermissionGranted();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                Toast.makeText(this, "Camera & Microphone permission required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void onPermissionGranted() {
        mainRepository = MainRepository.getInstance();
        mainRepository.listener = this;

        mainRepository.login(FirebaseUtil.currentUserId(), this, this::initUI);
    }

    private void initUI() {

        views.localView.setMirror(true);
        views.localView.setZOrderMediaOverlay(true);

        mainRepository.initLocalView(views.localView);

        views.callLayout.setVisibility(View.VISIBLE);
        views.incomingCallLayout.setVisibility(View.GONE);

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

                            mainRepository.sendCallRequest(
                                    targetUserId,
                                    () -> Toast.makeText(this, "User not available", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });

        mainRepository.subscribeForLatestEvent(data -> {
            if (data.getType() == DataModelType.StartCall) {
                runOnUiThread(() -> {
                    views.incomingNameTV.setText(data.getSender() + " is calling you");
                    views.incomingCallLayout.setVisibility(View.VISIBLE);
                });
            }
        });

        views.switchCameraButton.setOnClickListener(v -> mainRepository.switchCamera());

        views.micButton.setOnClickListener(v -> {
            mainRepository.toggleAudio(isMicrophoneMuted);
            isMicrophoneMuted = !isMicrophoneMuted;
        });

        views.videoButton.setOnClickListener(v -> {
            mainRepository.toggleVideo(isCameraMuted);
            isCameraMuted = !isCameraMuted;
        });

        views.endCallButton.setOnClickListener(v -> showEndCallDialog());
    }

    private void showEndCallDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Call")
                .setMessage("Are you sure you want to end this call?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> {
                    endCallSafely();
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void endCallSafely() {
        if (!isCallEnded && mainRepository != null) {
            isCallEnded = true;
            mainRepository.endCall();
        }
    }

    @Override
    public void webrtcConnected() {
        runOnUiThread(() -> {
            views.incomingCallLayout.setVisibility(View.GONE);
            views.callLayout.setVisibility(View.VISIBLE);
            mainRepository.initRemoteView(views.remoteView);
        });
    }

    @Override
    public void webrtcClosed() {
        runOnUiThread(() -> {
            if (!isFinishing()) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (views != null) {
            if (views.localView != null) {
                views.localView.release();
            }

            if (views.remoteView != null) {
                views.remoteView.release();
            }
        }

        if (mainRepository != null) {
            mainRepository.listener = null;
        }

        endCallSafely();
    }
}