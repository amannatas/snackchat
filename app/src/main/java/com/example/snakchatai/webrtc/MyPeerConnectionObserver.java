package com.example.snakchatai.webrtc;

import android.util.Log;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

public class MyPeerConnectionObserver implements PeerConnection.Observer {

    private final SurfaceViewRenderer remoteView;
    private static final String TAG = "WebRTC_Observer";

    public MyPeerConnectionObserver(SurfaceViewRenderer remoteView) {
        this.remoteView = remoteView;
        Log.d(TAG, "Observer initialized with remoteView: " + (remoteView != null));
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
        Log.d(TAG, "onIceConnectionChange: " + state);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {}

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
        Log.d(TAG, "onIceGatheringChange: " + state);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        // Handled in MainRepository
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        // 🚀 FIX: Yahan se addSink hata diya hai taaki crash na ho.
        // Ye kaam ab safely MainRepository (Main Thread) mein ho raha hai.
        Log.d(TAG, "onAddTrack triggered in Observer");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        // Deprecated method, usually onAddTrack is used in Unified Plan
        Log.d(TAG, "onAddStream triggered");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream: Stream removed");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {}

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded");
    }
}