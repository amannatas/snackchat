package com.example.snakchatai.webrtc;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.VideoTrack;

public class MyPeerConnectionObserver implements PeerConnection.Observer {

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        // Optional: debugging
        // Log.d("WebRTC", "Signaling: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
        // CRITICAL: connection drop handling
        // Log.d("WebRTC", "ICE state: " + state);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        // Not mandatory
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
        // Log.d("WebRTC", "ICE gathering: " + state);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        // ❗ MUST be overridden by MainRepository
        // Forwarded from anonymous class there
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
    }

    /**
     * ⚠️ LEGACY (PLAN-B) – still keep for safety
     */
    @Override
    public void onAddStream(MediaStream mediaStream) {
        // Backup support (some devices still trigger this)
        if (!mediaStream.videoTracks.isEmpty()) {
            VideoTrack videoTrack = mediaStream.videoTracks.get(0);
            videoTrack.setEnabled(true);
        }
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        // Not used (no chat over RTC)
    }

    @Override
    public void onRenegotiationNeeded() {
        // DO NOT auto renegotiate
        // This breaks calls if mishandled
    }

    /**
     * ✅ MODERN (UNIFIED PLAN) – MOST IMPORTANT
     */
    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        if (receiver.track() instanceof VideoTrack) {
            VideoTrack track = (VideoTrack) receiver.track();
            track.setEnabled(true);
        }
    }
}
