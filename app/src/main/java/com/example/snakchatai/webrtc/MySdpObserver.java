package com.example.snakchatai.webrtc;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class MySdpObserver implements SdpObserver {

    private static final String TAG = "WebRTC_SDP";

    @Override
    public void onCreateSuccess(SessionDescription sdp) {
        Log.d(TAG, "onCreateSuccess: " + sdp.type);
    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, "onSetSuccess");
    }

    @Override
    public void onCreateFailure(String error) {
        Log.e(TAG, "onCreateFailure: " + error);
    }

    @Override
    public void onSetFailure(String error) {
        Log.e(TAG, "onSetFailure: " + error);
    }
}
