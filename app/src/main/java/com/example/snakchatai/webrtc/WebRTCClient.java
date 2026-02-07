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
    private final EglBase.Context eglBaseContext = eglBase.getEglBaseContext();

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;

    private CameraVideoCapturer videoCapturer;
    private VideoSource localVideoSource;
    private AudioSource localAudioSource;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private final List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    public Listener listener;

    // ----------------------------------------------------

    public WebRTCClient(Context context,
                        PeerConnection.Observer observer,
                        String username) {

        this.context = context;
        this.username = username;

        initPeerConnectionFactory();
        peerConnectionFactory = createPeerConnectionFactory();

        iceServers.add(
                PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
                        .setUsername("83eebabf8b4cce9d5dbcb649")
                        .setPassword("2D7JvfkOQtBdYW3R")
                        .createIceServer()
        );

        peerConnection = createPeerConnection(observer);

        localVideoSource = peerConnectionFactory.createVideoSource(false);
        localAudioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
    }

    // ----------------------------------------------------
    // INIT SECTION
    // ----------------------------------------------------

    private void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions options =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(options);
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(
                        new DefaultVideoEncoderFactory(
                                eglBaseContext, true, true
                        )
                )
                .setVideoDecoderFactory(
                        new DefaultVideoDecoderFactory(eglBaseContext)
                )
                .createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer) {
        PeerConnection.RTCConfiguration config =
                new PeerConnection.RTCConfiguration(iceServers);

        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        return peerConnectionFactory.createPeerConnection(config, observer);
    }

    // ----------------------------------------------------
    // SURFACE VIEW
    // ----------------------------------------------------

    private void initRenderer(SurfaceViewRenderer view) {
        view.setEnableHardwareScaler(true);
        view.setMirror(true);
        view.init(eglBaseContext, null);
    }

    public void initLocalSurfaceView(SurfaceViewRenderer view) {
        initRenderer(view);
        startLocalVideo(view);
    }

    public void initRemoteSurfaceView(SurfaceViewRenderer view) {
        initRenderer(view);
    }

    // ----------------------------------------------------
    // LOCAL MEDIA
    // ----------------------------------------------------

    private void startLocalVideo(SurfaceViewRenderer view) {

        SurfaceTextureHelper helper =
                SurfaceTextureHelper.create("CameraThread", eglBaseContext);

        videoCapturer = getVideoCapturer();
        videoCapturer.initialize(
                helper,
                context,
                localVideoSource.getCapturerObserver()
        );

        videoCapturer.startCapture(480, 360, 15);

        localVideoTrack =
                peerConnectionFactory.createVideoTrack(
                        "local_video",
                        localVideoSource
                );

        localVideoTrack.addSink(view);

        localAudioTrack =
                peerConnectionFactory.createAudioTrack(
                        "local_audio",
                        localAudioSource
                );

        List<String> streamIds = new ArrayList<>();
        streamIds.add("stream");

        peerConnection.addTrack(localVideoTrack, streamIds);
        peerConnection.addTrack(localAudioTrack, streamIds);
    }

    private CameraVideoCapturer getVideoCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);

        for (String device : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(device)) {
                return enumerator.createCapturer(device, null);
            }
        }

        throw new RuntimeException("Front camera not found");
    }

    // ----------------------------------------------------
    // CALL FLOW
    // ----------------------------------------------------

    public void call(String target) {
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
        );

        peerConnection.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);

                if (listener != null) {
                    listener.onTransferDataToOtherPeer(
                            new DataModel(
                                    target,
                                    username,
                                    sdp.description,
                                    DataModelType.Offer
                            )
                    );
                }
            }
        }, constraints);
    }

    public void answer(String target) {
        peerConnection.createAnswer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(new MySdpObserver(), sdp);

                if (listener != null) {
                    listener.onTransferDataToOtherPeer(
                            new DataModel(
                                    target,
                                    username,
                                    sdp.description,
                                    DataModelType.Answer
                            )
                    );
                }
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
        peerConnection.addIceCandidate(candidate);
    }

    public void sendIceCandidate(IceCandidate candidate, String target) {
        addIceCandidate(candidate);

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
    // CLEANUP
    // ----------------------------------------------------

    public void closeConnection() {
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            }
            if (peerConnection != null) {
                peerConnection.close();
            }
            eglBase.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------

    public interface Listener {
        void onTransferDataToOtherPeer(DataModel model);
    }
}
