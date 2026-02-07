package com.example.snakchatai.repository;

import android.content.Context;

import com.example.snakchatai.remote.FirebaseClient;
import com.example.snakchatai.utils.DataModel;
import com.example.snakchatai.utils.DataModelType;
import com.example.snakchatai.utils.ErrorCallBack;
import com.example.snakchatai.utils.NewEventCallBack;
import com.example.snakchatai.utils.SuccessCallBack;
import com.example.snakchatai.webrtc.MyPeerConnectionObserver;
import com.example.snakchatai.webrtc.WebRTCClient;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

public class MainRepository implements WebRTCClient.Listener {

    // 🔹 Singleton
    private static MainRepository instance;

    public static MainRepository getInstance() {
        if (instance == null) {
            instance = new MainRepository();
        }
        return instance;
    }

    // 🔹 fields
    private final Gson gson = new Gson();
    private FirebaseClient firebaseClient;   // ❗ NOT final
    private WebRTCClient webRTCClient;

    private String currentUsername;
    private String target;

    private SurfaceViewRenderer remoteView;

    public Listener listener;

    // 🔹 private constructor
    private MainRepository() {}

    // 🔹 LOGIN (FIRST CALL ALWAYS)
    public void login(String username, Context context, SuccessCallBack callBack) {

        currentUsername = username;
        firebaseClient = new FirebaseClient(username);

        webRTCClient = new WebRTCClient(
                context,
                new MyPeerConnectionObserver() {

                    @Override
                    public void onAddStream(MediaStream mediaStream) {
                        if (remoteView != null && !mediaStream.videoTracks.isEmpty()) {
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
                        if (newState == PeerConnection.PeerConnectionState.CONNECTED && listener != null) {
                            listener.webrtcConnected();
                        }

                        if (newState == PeerConnection.PeerConnectionState.CLOSED
                                || newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                            if (listener != null) listener.webrtcClosed();
                        }
                    }
                },
                username
        );

        webRTCClient.listener = this;
        callBack.onSuccess();
    }

    // 🔹 UI init
    public void initLocalView(SurfaceViewRenderer view) {
        webRTCClient.initLocalSurfaceView(view);
    }

    public void initRemoteView(SurfaceViewRenderer view) {
        remoteView = view;
        webRTCClient.initRemoteSurfaceView(view);
    }

    // 🔹 Call flow
    public void sendCallRequest(String target, ErrorCallBack errorCallBack) {
        firebaseClient.sendMessageToOtherUser(
                new DataModel(target, currentUsername, null, DataModelType.StartCall),
                errorCallBack
        );
    }

    public void startCall(String target) {
        this.target = target;
        webRTCClient.call(target);
    }

    public void endCall() {
        webRTCClient.closeConnection();
    }

    // 🔹 Media controls
    public void switchCamera() {
        webRTCClient.switchCamera();
    }

    public void toggleAudio(boolean mute) {
        webRTCClient.toggleAudio(mute);
    }

    public void toggleVideo(boolean mute) {
        webRTCClient.toggleVideo(mute);
    }

    // 🔹 Firebase signaling listener
    public void subscribeForLatestEvent(NewEventCallBack callBack) {

        firebaseClient.observeIncomingLatestEvent(model -> {

            switch (model.getType()) {

                case Offer:
                    target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(
                            new SessionDescription(SessionDescription.Type.OFFER, model.getData())
                    );
                    webRTCClient.answer(target);
                    break;

                case Answer:
                    target = model.getSender();
                    webRTCClient.onRemoteSessionReceived(
                            new SessionDescription(SessionDescription.Type.ANSWER, model.getData())
                    );
                    break;

                case IceCandidate:
                    IceCandidate candidate =
                            gson.fromJson(model.getData(), IceCandidate.class);
                    webRTCClient.addIceCandidate(candidate);
                    break;

                case StartCall:
                    target = model.getSender();
                    callBack.onNewEventReceived(model);
                    break;
            }
        });
    }

    // 🔹 Send signaling data
    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        firebaseClient.sendMessageToOtherUser(model, () -> {});
    }

    // 🔹 UI callbacks
    public interface Listener {
        void webrtcConnected();
        void webrtcClosed();
    }
}
