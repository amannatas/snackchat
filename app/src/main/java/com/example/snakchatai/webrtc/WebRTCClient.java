package com.example.snakchatai.webrtc;

import android.content.Context;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.DataModelType;
import com.google.gson.Gson;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class WebRTCClient {

    private final Context context;
    private final String username;
    private final Gson gson = new Gson();

    private final EglBase eglBase = EglBase.create();
    private final EglBase.Context eglContext = eglBase.getEglBaseContext();

    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;

    private CameraVideoCapturer videoCapturer;
    private SurfaceTextureHelper textureHelper;

    private VideoSource videoSource;
    private AudioSource audioSource;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private final List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    public Listener listener;

    // ----------------------------------------------------

    public WebRTCClient(Context context,
                        PeerConnection.Observer observer,
                        String username) {

        this.context = context.getApplicationContext();
        this.username = username;

        initFactory();
        factory = createFactory();

        // ✅ TURN + STUN (DO NOT REMOVE)
        iceServers.add(
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                        .setUsername("83eebabf8b4cce9d5dbcb649")
                        .setPassword("2D7JvfkOQtBdYW3R")
                        .createIceServer()
        );

        peerConnection = createPeerConnection(observer);

        videoSource = factory.createVideoSource(false);
        audioSource = factory.createAudioSource(new MediaConstraints());
    }

    // ----------------------------------------------------
    // FACTORY
    // ----------------------------------------------------

    private void initFactory() {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
        );
    }

    private PeerConnectionFactory createFactory() {
        return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(
                        new DefaultVideoEncoderFactory(eglContext, true, true)
                )
                .setVideoDecoderFactory(
                        new DefaultVideoDecoderFactory(eglContext)
                )
                .createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer) {
        PeerConnection.RTCConfiguration config =
                new PeerConnection.RTCConfiguration(iceServers);

        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        config.continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;

        return factory.createPeerConnection(config, observer);
    }

    // ----------------------------------------------------
    // RENDERERS
    // ----------------------------------------------------

    private void initRenderer(SurfaceViewRenderer view, boolean mirror) {
        view.setMirror(mirror);
        view.setEnableHardwareScaler(true);
        view.init(eglContext, null);
    }

    public void initLocalSurfaceView(SurfaceViewRenderer view) {
        initRenderer(view, true);
        startLocalMedia(view);
    }

    public void initRemoteSurfaceView(SurfaceViewRenderer view) {
        initRenderer(view, false);
    }

    // ----------------------------------------------------
    // LOCAL MEDIA
    // ----------------------------------------------------

    private void startLocalMedia(SurfaceViewRenderer localView) {

        textureHelper =
                SurfaceTextureHelper.create("CameraThread", eglContext);

        videoCapturer = createCameraCapturer();
        videoCapturer.initialize(
                textureHelper,
                context,
                videoSource.getCapturerObserver()
        );

        try {
            videoCapturer.startCapture(640, 480, 30);
        } catch (Exception e) {
            e.printStackTrace();
        }

        localVideoTrack =
                factory.createVideoTrack("LOCAL_VIDEO", videoSource);
        localVideoTrack.addSink(localView);

        localAudioTrack =
                factory.createAudioTrack("LOCAL_AUDIO", audioSource);

        List<String> streamIds = new ArrayList<>();
        streamIds.add("stream");

        peerConnection.addTrack(localVideoTrack, streamIds);
        peerConnection.addTrack(localAudioTrack, streamIds);
    }

    private CameraVideoCapturer createCameraCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);

        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) {
                return enumerator.createCapturer(device, null);
            }
        }
        throw new RuntimeException("No front camera found");
    }

    // ----------------------------------------------------
    // CALL FLOW
    // ----------------------------------------------------

    public void call(String target) {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        );
        constraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
        );

        peerConnection.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);

                sendSignal(
                        target,
                        sdp.description,
                        DataModelType.Offer
                );
            }
        }, constraints);
    }

    public void answer(String target) {
        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);

                sendSignal(
                        target,
                        sdp.description,
                        DataModelType.Answer
                );
            }
        }, new MediaConstraints());
    }

    public void onRemoteSessionReceived(SessionDescription sdp) {
        peerConnection.setRemoteDescription(new MySdpObserver(), sdp);
    }

    // ----------------------------------------------------
    // ICE
    // ----------------------------------------------------

    public void addIceCandidate(IceCandidate candidate) {
        if (peerConnection != null) {
            peerConnection.addIceCandidate(candidate);
        }
    }

    public void sendIceCandidate(IceCandidate candidate, String target) {
        if (listener != null) {
            listener.onTransferDataToOtherPeer(
                    new DataModel(
                            target,
                            username,
                            gson.toJson(candidate),
                            DataModelType.IceCandidate
                    )
            );
        }
    }

    // ----------------------------------------------------
    // CONTROLS
    // ----------------------------------------------------

    public void switchCamera() {
        if (videoCapturer != null) {
            videoCapturer.switchCamera(null);
        }
    }

    public void toggleVideo(boolean muted) {
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(!muted);
        }
    }

    public void toggleAudio(boolean muted) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!muted);
        }
    }

    // ----------------------------------------------------
    // CLEANUP (CRITICAL)
    // ----------------------------------------------------

    public void closeConnection() {
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            }
            if (textureHelper != null) textureHelper.dispose();
            if (peerConnection != null) peerConnection.close();
            eglBase.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------

    private void sendSignal(String target, String data, DataModelType type) {
        if (listener != null) {
            listener.onTransferDataToOtherPeer(
                    new DataModel(target, username, data, type)
            );
        }
    }

    public interface Listener {
        void onTransferDataToOtherPeer(DataModel model);
    }
}
