package com.example.snakchatai.repository;

import android.content.Context;

import com.example.snakchatai.model.CallLogModel;
import com.example.snakchatai.remote.FirebaseClient;
import com.example.snakchatai.utils.*;
import com.example.snakchatai.webrtc.MyPeerConnectionObserver;
import com.example.snakchatai.webrtc.WebRTCClient;
import com.google.firebase.Timestamp;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SurfaceViewRenderer;

public class MainRepository implements WebRTCClient.Listener {

    private static MainRepository instance;

    public static synchronized MainRepository getInstance() {
        if (instance == null) {
            instance = new MainRepository();
        }
        return instance;
    }

    private final Gson gson = new Gson();

    private FirebaseClient firebaseClient;
    private WebRTCClient webRTCClient;

    private String currentUsername;
    private String target;

    private SurfaceViewRenderer remoteView;

    public Listener listener;

    private MainRepository() {}

    // 🔹 LOGIN
    public void login(String username, Context context, SuccessCallBack callBack) {

        currentUsername = username;
        firebaseClient = new FirebaseClient(username);

        createWebRTCClient(context);

        if (callBack != null) callBack.onSuccess();
    }

    private void createWebRTCClient(Context context) {

        webRTCClient = new WebRTCClient(
                context,
                new MyPeerConnectionObserver() {

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                        if (remoteView != null
                                && mediaStream != null
                                && !mediaStream.videoTracks.isEmpty()) {
                            mediaStream.videoTracks.get(0).addSink(remoteView);
                        }
                    }

                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate) {
                        if (target != null) {
                            webRTCClient.sendIceCandidate(iceCandidate, target);
                        }
                    }

                    @Override
                    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                        if (listener == null) return;

                        if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                            listener.webrtcConnected();
                        }

                        if (newState == PeerConnection.PeerConnectionState.CLOSED
                                || newState == PeerConnection.PeerConnectionState.DISCONNECTED
                                || newState == PeerConnection.PeerConnectionState.FAILED) {
                            listener.webrtcClosed();
                        }
                    }
                },
                currentUsername
        );

        webRTCClient.listener = this;
    }

    // 🔹 UI
    public void initLocalView(SurfaceViewRenderer view) {
        if (webRTCClient != null) {
            webRTCClient.initLocalSurfaceView(view);
        }
    }

    public void initRemoteView(SurfaceViewRenderer view) {
        remoteView = view;
        if (webRTCClient != null) {
            webRTCClient.initRemoteSurfaceView(view);
        }
    }

    public void sendCallRequest(String targetUserId, Runnable onError) {
        if (firebaseClient == null || currentUsername == null) {
            if (onError != null) {
                onError.run();
            }
            return;
        }

        this.target = targetUserId;

        DataModel dataModel = new DataModel(
                currentUsername,
                targetUserId,
                null,
                DataModelType.StartCall
        );

        firebaseClient.sendMessageToOtherUser(dataModel, () -> {
            if (onError != null) {
                onError.run();
            }
        });
    }

    // 🔹 Start WebRTC Call
    public void startCall(String target) {

        if (webRTCClient == null || target == null || target.isEmpty()) return;

        this.target = target;

        webRTCClient.call(target);

        FirebaseUtil.getCallLogCollectionReference(currentUsername)
                .add(new CallLogModel(target, "", Timestamp.now(), false));
    }

    // 🔹 End Call
    public void endCall() {

        try {

            if (firebaseClient != null) {
                firebaseClient.clearListeners();
            }

            if (remoteView != null) {
                remoteView.clearImage();
                remoteView.release();
                remoteView = null;
            }

            if (webRTCClient != null) {
                webRTCClient.closeConnection();
                webRTCClient = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        target = null;
    }

    // 🔹 Reject
    public void rejectCall() {

        if (target == null || firebaseClient == null) return;

        firebaseClient.sendMessageToOtherUser(
                new DataModel(
                        currentUsername,
                        target,
                        null,
                        DataModelType.RejectCall
                ),
                () -> {}
        );

        endCall();
    }

    // 🔹 Signaling bridge
    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        if (firebaseClient != null) {
            firebaseClient.sendMessageToOtherUser(model, () -> {});
        }
    }

    // 🔹 Observe events
    public void subscribeForLatestEvent(NewEventCallBack callBack) {
        if (firebaseClient != null) {
            firebaseClient.observeIncomingLatestEvent(callBack);
        }
    }

    public interface Listener {
        void webrtcConnected();
        void webrtcClosed();
    }
    public void switchCamera() {
        if (webRTCClient != null) webRTCClient.switchCamera();
    }

    public void toggleAudio(boolean isMuted) {
        if (webRTCClient != null) webRTCClient.toggleAudio(isMuted);
    }

    public void toggleVideo(boolean isMuted) {
        if (webRTCClient != null) webRTCClient.toggleVideo(isMuted);
    }

}
