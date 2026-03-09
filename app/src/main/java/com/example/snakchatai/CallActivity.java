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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.snakchatai.databinding.ActivityCallBinding;
import com.example.snakchatai.repository.MainRepository;
import com.example.snakchatai.utils.FirebaseUtil;
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

        if (getIntent() != null) {
            targetUserId = getIntent().getStringExtra("targetUserId");
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
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted();
        } else {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void onPermissionGranted() {
        mainRepository = MainRepository.getInstance();
        mainRepository.listener = this;

        // Z-Order: Local view ko remote ke upar dikhane ke liye
        views.localView.setZOrderMediaOverlay(true);

        mainRepository.login(FirebaseUtil.currentUserId(), this, () -> {
            runOnUiThread(this::initUI);
        });
    }

    private void initUI() {
        mainRepository.initLocalView(views.localView);
        mainRepository.initRemoteView(views.remoteView);

        // Signalling observer setup
        mainRepository.subscribeForLatestEvent(data -> {
            runOnUiThread(() -> {
                switch (data.getType()) {
                    case StartCall:
                        views.incomingNameTV.setText(data.getSender() + " is calling...");
                        views.incomingCallLayout.setVisibility(View.VISIBLE);
                        views.callLayout.setVisibility(View.GONE);
                        break;
                    case EndCall:
                    case RejectCall:
                        Toast.makeText(this, "Call Terminated", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
            });
        });

        if (getIntent().getBooleanExtra("isIncoming", false)) {
            views.incomingCallLayout.setVisibility(View.VISIBLE);
            views.callLayout.setVisibility(View.GONE);
        } else {
            startCallProcess();
        }

        setupButtons();
    }

    private void startCallProcess() {
        views.incomingCallLayout.setVisibility(View.GONE);
        views.callLayout.setVisibility(View.VISIBLE);

        if (targetUserId != null) {
            mainRepository.sendCallRequest(targetUserId, () -> {
                mainRepository.startCall(targetUserId);
            });
        }
    }

    private void setupButtons() {
        // Accept Button
        views.acceptButton.setOnClickListener(v -> {
            views.incomingCallLayout.setVisibility(View.GONE);
            views.callLayout.setVisibility(View.VISIBLE);
            // Repository automatically handles 'Answer' when it sees 'Offer' in the stream
        });

        // Reject/End Buttons
        views.rejectButton.setOnClickListener(v -> {
            mainRepository.rejectCall();
            finish();
        });

        views.endCallButton.setOnClickListener(v -> showEndCallDialog());

        // Controls
        views.switchCameraButton.setOnClickListener(v -> mainRepository.switchCamera());

        views.micButton.setOnClickListener(v -> {
            isMicrophoneMuted = !isMicrophoneMuted;
            mainRepository.toggleAudio(isMicrophoneMuted);
            views.micButton.setImageResource(isMicrophoneMuted ?
                    R.drawable.ic_baseline_mic_off_24 : R.drawable.ic_baseline_mic_24);
        });

        views.videoButton.setOnClickListener(v -> {
            isCameraMuted = !isCameraMuted;
            mainRepository.toggleVideo(isCameraMuted);
            views.videoButton.setImageResource(isCameraMuted ?
                    R.drawable.ic_baseline_videocam_off_24 : R.drawable.ic_baseline_videocam_24);
        });
    }

    private void showEndCallDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Call")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> {
                    endCallSafely();
                    finish();
                })
                .setNegativeButton("No", null)
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
        runOnUiThread(() -> Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void webrtcClosed() {
        runOnUiThread(this::finish);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        endCallSafely();
    }
}