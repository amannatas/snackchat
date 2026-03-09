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

    // Final variable ensures it must be initialized in constructor
    private final SurfaceViewRenderer remoteView;
    private static final String TAG = "WebRTC_Observer";

    // Constructor: Agar ye call hoga, toh remoteView initialize ho hi jayega
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
        // Handle logic in MainRepository
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        if (receiver.track() instanceof VideoTrack) {
            VideoTrack track = (VideoTrack) receiver.track();
            Log.d(TAG, "onAddTrack: Remote Video Track received!");

            // Check for null before adding sink to avoid crashes
            if (remoteView != null) {
                track.setEnabled(true);
                track.addSink(remoteView);
                Log.d(TAG, "onAddTrack: Success - Track added to Sink");
            } else {
                Log.e(TAG, "onAddTrack: Error - remoteView is NULL!");
            }
        }
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        if (!mediaStream.videoTracks.isEmpty() && remoteView != null) {
            VideoTrack track = mediaStream.videoTracks.get(0);
            track.setEnabled(true);
            track.addSink(remoteView);
        }
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