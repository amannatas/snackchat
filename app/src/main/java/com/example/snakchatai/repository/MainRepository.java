package com.example.snakchatai.repository;

import android.content.Context;

import com.example.snakchatai.model.CallLogModel;
import com.example.snakchatai.remote.FirebaseClient;
import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.DataModelType;
import com.example.snakchatai.utils.ErrorCallBack;
import com.example.snakchatai.utils.NewEventCallBack;
import com.example.snakchatai.utils.SuccessCallBack;
import com.example.snakchatai.utils.FirebaseUtil;
import com.example.snakchatai.webrtc.MyPeerConnectionObserver;
import com.example.snakchatai.webrtc.WebRTCClient;
import com.google.firebase.Timestamp;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

public class MainRepository implements WebRTCClient.Listener {

    // 🔹 Singleton
    private static MainRepository instance;

    public static synchronized MainRepository getInstance() {
        if (instance == null) {
            instance = new MainRepository();
        }
        return instance;
    }

    // 🔹 fields
    private final Gson gson = new Gson();
    private FirebaseClient firebaseClient;
    private WebRTCClient webRTCClient;

    private String currentUsername;
    private String target;

    private SurfaceViewRenderer remoteView;

    public Listener listener;

    private MainRepository() {}

    // 🔹 LOGIN (MUST BE CALLED FIRST)
    public void login(String username, Context context, SuccessCallBack callBack) {
        try {
            currentUsername = username;
            firebaseClient = new FirebaseClient(username);

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
                            if (target != null && webRTCClient != null) {
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
                    username
            );

            webRTCClient.listener = this;

            if (callBack != null) callBack.onSuccess();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 UI init
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

    // 🔹 Call flow
    public void sendCallRequest(String target, ErrorCallBack errorCallBack) {
        if (firebaseClient == null || currentUsername == null) return;

        firebaseClient.sendMessageToOtherUser(
                new DataModel(target, currentUsername, null, DataModelType.StartCall),
                errorCallBack
        );

        // Log the outgoing call
        FirebaseUtil.getCallLogCollectionReference(currentUsername)
                .add(new CallLogModel(target, "", Timestamp.now(), true));
    }

    public void startCall(String target) {
        if (webRTCClient == null || target == null || target.isEmpty()) return;
        this.target = target;
        webRTCClient.call(target);

        // Log the incoming call
        FirebaseUtil.getCallLogCollectionReference(currentUsername)
                .add(new CallLogModel(target, "", Timestamp.now(), false));
    }

    public void endCall() {
        if (webRTCClient != null) {
            webRTCClient.closeConnection();
        }
        target = null;
    }

    // 🔹 Media controls
    public void switchCamera() {
        if (webRTCClient != null) webRTCClient.switchCamera();
    }

    public void toggleAudio(boolean mute) {
        if (webRTCClient != null) webRTCClient.toggleAudio(mute);
    }

    public void toggleVideo(boolean mute) {
        if (webRTCClient != null) webRTCClient.toggleVideo(mute);
    }

    // 🔹 Firebase signaling listener
    public void subscribeForLatestEvent(NewEventCallBack callBack) {
        if (firebaseClient == null) return;

        firebaseClient.observeIncomingLatestEvent(model -> {
            if (model == null) return;

            switch (model.getType()) {

                case Offer:
                    target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(
                            new SessionDescription(
                                    SessionDescription.Type.OFFER,
                                    model.getData()
                            )
                    );
                    webRTCClient.answer(target);
                    break;

                case Answer:
                    target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(
                            new SessionDescription(
                                    SessionDescription.Type.ANSWER,
                                    model.getData()
                            )
                    );
                    break;

                case IceCandidate:
                    IceCandidate candidate =
                            gson.fromJson(model.getData(), IceCandidate.class);
                    webRTCClient.addIceCandidate(candidate);
                    break;

                case StartCall:
                    target = model.getSender();
                    if (callBack != null) callBack.onNewEventReceived(model);
                    break;

                case RejectCall:
                    endCall();
                    if (listener != null) listener.webrtcClosed();
                    break;
            }
        });
    }

    // 🔹 Reject outgoing/incoming call
    public void rejectCall() {
        if (target == null || firebaseClient == null) return;

        firebaseClient.sendMessageToOtherUser(
                new DataModel(
                        target,
                        currentUsername,
                        null,
                        DataModelType.RejectCall
                ),
                () -> {}
        );

        endCall();
    }

    // 🔹 Send signaling data
    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        if (firebaseClient != null) {
            firebaseClient.sendMessageToOtherUser(model, () -> {});
        }
    }

    // 🔹 UI callbacks
    public interface Listener {
        void webrtcConnected();
        void webrtcClosed();
    }
}
