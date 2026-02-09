package com.example.snakchatai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.snakchatai.databinding.FragmentCallBinding;
import com.example.snakchatai.repository.MainRepository;
import com.example.snakchatai.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class call_fragment extends Fragment implements MainRepository.Listener {

    private FragmentCallBinding views;
    private MainRepository mainRepository;

    private boolean isCameraMuted = false;
    private boolean isMicrophoneMuted = false;

    private static final int PERMISSION_REQUEST_CODE = 101;

    private String targetUserId = "OTHER_USER_ID_HERE"; // <-- Yeh id daal de, jiska username chahiye

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        views = FragmentCallBinding.inflate(inflater, container, false);
        return views.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
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
                        requireContext(),
                        "Camera & Microphone permission required",
                        Toast.LENGTH_LONG
                ).show();
                requireActivity().onBackPressed();
            }
        }
    }

    // 🔥 SINGLE ENTRY POINT AFTER PERMISSION
    private void onPermissionGranted() {
        mainRepository = MainRepository.getInstance();
        mainRepository.listener = this;

        mainRepository.login(
                FirebaseUtil.currentUserId(),
                requireContext(),
                this::initUI // WebRTC init sirf yahin se
        );
    }

    private void initUI() {

        // Pehle Firebase se other user ka username fetch kar le
        FirebaseFirestore.getInstance()
                .collection("users") // <-- apne collection ka naam yahan daal
                .document(targetUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc != null && doc.exists()) {
                            String otherUsername = doc.getString("username"); // <-- apne field ka naam yahan daal

                            if (otherUsername == null || otherUsername.isEmpty()) {
                                Toast.makeText(requireContext(), "Username not found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Display the username
                            views.incomingNameTV.setText("Calling " + otherUsername);

                            // Use targetUserId to send the call request
                            views.callBtn.setOnClickListener(v -> {
                                mainRepository.sendCallRequest(targetUserId, () ->
                                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                                );
                            });

                        } else {
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch user", Toast.LENGTH_SHORT).show();
                    }
                });

        // Camera & views init
        mainRepository.initLocalView(views.localView);
        mainRepository.initRemoteView(views.remoteView);

        mainRepository.subscribeForLatestEvent(data -> {
            if (data.getType() == com.example.snakchatai.utils.DataModelType.StartCall) {
                requireActivity().runOnUiThread(() -> {
                    views.incomingNameTV.setText(
                            data.getSender() + " is calling you"
                    );
                    views.incomingCallLayout.setVisibility(View.VISIBLE);

                    views.acceptButton.setOnClickListener(v -> {
                        mainRepository.startCall(data.getSender());
                        views.incomingCallLayout.setVisibility(View.GONE);
                    });

                    views.rejectButton.setOnClickListener(v ->
                            views.incomingCallLayout.setVisibility(View.GONE)
                    );
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
            requireActivity()
                    .getSupportFragmentManager()
                    .popBackStack();
        });
    }

    @Override
    public void webrtcConnected() {
        requireActivity().runOnUiThread(() -> {
            views.incomingCallLayout.setVisibility(View.GONE);
            views.whoToCallLayout.setVisibility(View.GONE);
            views.callLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void webrtcClosed() {
        requireActivity().runOnUiThread(() ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        views = null;
    }
}
