package com.example.snakchatai.webrtc;

import android.content.Context;
import android.util.Log;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.DataModelType;
import com.google.gson.Gson;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class WebRTCClient {

    private static final String TAG = "WebRTCClient";
    private final Context context;
    private final String username;
    private final Gson gson = new Gson();

    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;

    private CameraVideoCapturer videoCapturer;
    private SurfaceTextureHelper textureHelper;

    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    // 🚀 Fix: Jab tak RemoteDescription set nahi hoti, candidates yahan rukenge
    private final List<IceCandidate> iceCandidateBuffer = new ArrayList<>();
    public Listener listener;

    public WebRTCClient(Context context, PeerConnection.Observer observer, String username) {
        this.context = context.getApplicationContext();
        this.username = username;
        initWebRTC(observer);
    }

    private void initWebRTC(PeerConnection.Observer observer) {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
        );

        eglBase = EglBase.create();
        EglBase.Context eglContext = eglBase.getEglBaseContext();

        factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglContext, true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglContext))
                .createPeerConnectionFactory();

        // 🛠️ Multi-Port TURN Setup (Auto-Switching Logic)
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        // 1. Google STUN (Direct path dhoondne ke liye)
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        // 2. OpenRelay Port 443 (Sabse zyada firewall bypass karta hai)
        iceServers.add(PeerConnection.IceServer.builder("turns:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());

        // 3. OpenRelay Port 80 (Alternative agar 443 fail ho)
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(iceServers);

        // ✅ Unified Plan: Modern standard for Audio/Video tracks
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;


        peerConnection = factory.createPeerConnection(config, observer);

        videoSource = factory.createVideoSource(false);
        audioSource = factory.createAudioSource(new MediaConstraints());
    }

    public void initLocalSurfaceView(SurfaceViewRenderer view) {
        view.init(eglBase.getEglBaseContext(), null);
        view.setMirror(true);
        view.setEnableHardwareScaler(true);
        startLocalMedia(view);
    }

    public void initRemoteSurfaceView(SurfaceViewRenderer view) {
        view.init(eglBase.getEglBaseContext(), null);
        view.setMirror(false);
        view.setEnableHardwareScaler(true);
    }

    private void startLocalMedia(SurfaceViewRenderer localView) {
        textureHelper = SurfaceTextureHelper.create("CameraThread", eglBase.getEglBaseContext());
        videoCapturer = createCameraCapturer();
        if (videoCapturer != null) {
            videoCapturer.initialize(textureHelper, context, videoSource.getCapturerObserver());
            try {
                videoCapturer.startCapture(640, 480, 30);
            } catch (Exception e) {
                Log.e(TAG, "Camera Start Error: " + e.getMessage());
            }
        }

        localVideoTrack = factory.createVideoTrack("LOCAL_VIDEO_TRACK", videoSource);
        localVideoTrack.addSink(localView);
        localAudioTrack = factory.createAudioTrack("LOCAL_AUDIO_TRACK", audioSource);

        List<String> streamIds = new ArrayList<>();
        streamIds.add("main_stream");

        if (peerConnection != null) {
            peerConnection.addTrack(localVideoTrack, streamIds);
            peerConnection.addTrack(localAudioTrack, streamIds);
        }
    }

    private CameraVideoCapturer createCameraCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) return enumerator.createCapturer(device, null);
        }
        return null;
    }

    public void call(String target) {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);
                sendSignal(target, sdp.description, DataModelType.Offer);
            }
        }, constraints);
    }

    public void answer(String target) {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);
                sendSignal(target, sdp.description, DataModelType.Answer);
            }
        }, constraints);
    }

    public void onRemoteSessionReceived(SessionDescription sdp) {
        if (peerConnection != null) {
            peerConnection.setRemoteDescription(new MySdpObserver() {
                @Override
                public void onSetSuccess() {
                    super.onSetSuccess();
                    // ✅ Remote set hote hi saare buffered candidates release karo
                    Log.d(TAG, "Remote Description Set! Releasing candidates...");
                    synchronized (iceCandidateBuffer) {
                        for (IceCandidate candidate : iceCandidateBuffer) {
                            peerConnection.addIceCandidate(candidate);
                        }
                        iceCandidateBuffer.clear();
                    }
                }
            }, sdp);
        }
    }

    public void addIceCandidate(IceCandidate candidate) {
        if (peerConnection != null) {
            if (peerConnection.getRemoteDescription() != null) {
                peerConnection.addIceCandidate(candidate);
            } else {
                // 🚀 Buffer candidate if remote description is missing
                synchronized (iceCandidateBuffer) {
                    iceCandidateBuffer.add(candidate);
                }
            }
        }
    }

    public void sendIceCandidate(IceCandidate candidate, String target) {
        sendSignal(target, gson.toJson(candidate), DataModelType.IceCandidate);
    }

    private void sendSignal(String target, String data, DataModelType type) {
        if (listener != null) {
            listener.onTransferDataToOtherPeer(new DataModel(target, username, data, type));
        }
    }

    public void switchCamera() { if (videoCapturer != null) videoCapturer.switchCamera(null); }
    public void toggleAudio(boolean isMuted) { if (localAudioTrack != null) localAudioTrack.setEnabled(!isMuted); }
    public void toggleVideo(boolean isMuted) { if (localVideoTrack != null) localVideoTrack.setEnabled(!isMuted); }

    public void closeConnection() {
        try {
            if (videoCapturer != null) { videoCapturer.stopCapture(); videoCapturer.dispose(); }
            if (textureHelper != null) { textureHelper.dispose(); }
            if (peerConnection != null) { peerConnection.close(); peerConnection.dispose(); }
            if (factory != null) factory.dispose();
            if (eglBase != null) eglBase.release();
        } catch (Exception e) {
            Log.e(TAG, "Cleanup error: " + e.getMessage());
        }
    }

    public interface Listener {
        void onTransferDataToOtherPeer(DataModel model);
    }
}