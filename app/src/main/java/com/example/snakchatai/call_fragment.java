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
import com.example.snakchatai.utils.DataModelType;

public class call_fragment extends Fragment implements MainRepository.Listener {

    private FragmentCallBinding views;
    private MainRepository mainRepository;

    private boolean isCameraMuted = false;
    private boolean isMicrophoneMuted = false;

    private static final int PERMISSION_REQUEST_CODE = 101;

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
        String username = "user123"; // real username yahan pass kar

        mainRepository = MainRepository.getInstance();
        mainRepository.listener = this;

        mainRepository.login(
                username,
                requireContext(),
                this::initUI // ⚠️ WebRTC init sirf yahin se
        );
    }

    private void initUI() {

        views.callBtn.setOnClickListener(v -> {
            String target = views.targetUserNameEt.getText().toString().trim();
            if (target.isEmpty()) {
                Toast.makeText(requireContext(), "Enter username", Toast.LENGTH_SHORT).show();
                return;
            }
            mainRepository.sendCallRequest(target, () ->
                    Toast.makeText(requireContext(),
                            "User not found",
                            Toast.LENGTH_SHORT).show()
            );
        });

        // 🔥 Camera init AB safe hai
        mainRepository.initLocalView(views.localView);
        mainRepository.initRemoteView(views.remoteView);

        mainRepository.subscribeForLatestEvent(data -> {
            if (data.getType() == DataModelType.StartCall) {
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
