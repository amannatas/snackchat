package com.example.snakchatai.webrtc;

import android.content.Context;

import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.DataModelType;
import com.google.gson.Gson;

import org.webrtc.*;

import java.util.ArrayList;
import java.util.List;

public class WebRTCClient {

    private final Context context;
    private final String username;
    private final Gson gson = new Gson();

    private EglBase eglBase;
    private EglBase.Context eglContext;

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

        eglBase = EglBase.create();
        eglContext = eglBase.getEglBaseContext();

        factory = createFactory();

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
                sendSignal(target, sdp.description, DataModelType.Offer);
            }
        }, constraints);
    }

    public void answer(String target) {
        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);
                sendSignal(target, sdp.description, DataModelType.Answer);
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

    // ----------------------------------------------------
    // CLEANUP (FIXED PROPERLY)
    // ----------------------------------------------------

    public void closeConnection() {

        try {

            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
                videoCapturer = null;
            }

            if (localVideoTrack != null) {
                localVideoTrack.dispose();
                localVideoTrack = null;
            }

            if (localAudioTrack != null) {
                localAudioTrack.dispose();
                localAudioTrack = null;
            }

            if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
            }

            if (audioSource != null) {
                audioSource.dispose();
                audioSource = null;
            }

            if (textureHelper != null) {
                textureHelper.dispose();
                textureHelper = null;
            }

            if (peerConnection != null) {
                peerConnection.close();
                peerConnection.dispose();
                peerConnection = null;
            }

            if (factory != null) {
                factory.dispose();
                factory = null;
            }

            if (eglBase != null) {
                eglBase.release();
                eglBase = null;
            }

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
    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            CameraVideoCapturer cameraCapturer =
                    (CameraVideoCapturer) videoCapturer;
            cameraCapturer.switchCamera(null);
        }
    }

    public void toggleAudio(boolean isMuted) {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!isMuted);
        }
    }

    public void toggleVideo(boolean isMuted) {
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(!isMuted);
        }
    }

}