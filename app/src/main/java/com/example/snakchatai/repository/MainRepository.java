package com.example.snakchatai.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.snakchatai.remote.FirebaseClient;
import com.example.snakchatai.utils.*;
import com.example.snakchatai.webrtc.MyPeerConnectionObserver;
import com.example.snakchatai.webrtc.WebRTCClient;
import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class MainRepository implements WebRTCClient.Listener {

    private static final String TAG = "MainRepository";
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
    private SurfaceViewRenderer localView; // ✅ Added to persist local view
    private Context context;
    private final List<IceCandidate> iceCandidateBuffer = new ArrayList<>();

    public Listener listener;

    private MainRepository() {}

    public void login(String username, Context context, SuccessCallBack callBack) {
        this.currentUsername = username;
        this.context = context.getApplicationContext();
        this.firebaseClient = new FirebaseClient(username);
        if (callBack != null) callBack.onSuccess();
    }

    // ✅ Modified: Persists the view and inits if client exists
    public void initLocalView(SurfaceViewRenderer view) {
        this.localView = view;
        if (webRTCClient != null) {
            webRTCClient.initLocalSurfaceView(view);
        }
    }

    public void initRemoteView(SurfaceViewRenderer view) {
        this.remoteView = view;
        if (webRTCClient == null && context != null) {
            initWebRTC(context);
        } else if (webRTCClient != null) {
            webRTCClient.initRemoteSurfaceView(view);
        }
    }

    private void initWebRTC(Context context) {
        webRTCClient = new WebRTCClient(context, new MyPeerConnectionObserver(remoteView) {
            @Override
            public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
                super.onAddTrack(receiver, mediaStreams);
                if (receiver.track() instanceof VideoTrack && remoteView != null) {
                    VideoTrack track = (VideoTrack) receiver.track();
                    new Handler(Looper.getMainLooper()).post(() -> track.addSink(remoteView));
                }
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                if (target != null) {
                    webRTCClient.sendIceCandidate(iceCandidate, target);
                } else {
                    iceCandidateBuffer.add(iceCandidate);
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                if (listener != null) {
                    if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                        listener.webrtcConnected();
                    } else if (newState == PeerConnection.PeerConnectionState.FAILED ||
                            newState == PeerConnection.PeerConnectionState.CLOSED ||
                            newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                        listener.webrtcClosed();
                    }
                }
            }
        }, currentUsername);

        webRTCClient.listener = this;

        // ✅ IMPORTANT: If localView was set before WebRTC was ready, init it now
        if (localView != null) {
            webRTCClient.initLocalSurfaceView(localView);
        }
    }

    public void sendCallRequest(String targetUserId, Runnable onDone) {
        this.target = targetUserId;
        DataModel dataModel = new DataModel(targetUserId, currentUsername, null, DataModelType.StartCall);
        firebaseClient.sendMessageToOtherUser(dataModel, () -> {
            if (onDone != null) onDone.run();
        }, () -> Log.e(TAG, "Failed to send call request"));
    }

    public void startCall(String targetId) {
        this.target = targetId;
        if (webRTCClient != null) {
            webRTCClient.call(targetId);
            sendBufferedCandidates();
        }
    }

    public void answerCall(String targetId) {
        this.target = targetId;
        if (webRTCClient != null) {
            webRTCClient.answer(targetId);
            sendBufferedCandidates();
        }
    }

    private void sendBufferedCandidates() {
        if (target != null && !iceCandidateBuffer.isEmpty() && webRTCClient != null) {
            for (IceCandidate candidate : iceCandidateBuffer) {
                webRTCClient.sendIceCandidate(candidate, target);
            }
            iceCandidateBuffer.clear();
        }
    }

    public void onRemoteSessionReceived(SessionDescription sdp) {
        if (webRTCClient != null) webRTCClient.onRemoteSessionReceived(sdp);
    }

    public void addIceCandidate(IceCandidate candidate) {
        if (webRTCClient != null) webRTCClient.addIceCandidate(candidate);
    }

    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        if (firebaseClient != null) {
            firebaseClient.sendMessageToOtherUser(model, () -> {}, () -> Log.e(TAG, "Transfer Error"));
        }
    }

    public void subscribeForLatestEvent(NewEventCallBack callBack) {
        if (firebaseClient != null) {
            firebaseClient.observeIncomingLatestEvent(data -> {
                try {
                    switch (data.getType()) {
                        case StartCall:
                            this.target = data.getSender();
                            callBack.onNewEventReceived(data);
                            break;
                        case Offer:
                            this.target = data.getSender();
                            if (webRTCClient != null) {
                                webRTCClient.onRemoteSessionReceived(new SessionDescription(SessionDescription.Type.OFFER, data.getData()));
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    webRTCClient.answer(data.getSender());
                                }, 500);
                            }
                            break;
                        case Answer:
                            this.target = data.getSender();
                            if (webRTCClient != null) {
                                webRTCClient.onRemoteSessionReceived(new SessionDescription(SessionDescription.Type.ANSWER, data.getData()));
                            }
                            break;
                        case IceCandidate:
                            IceCandidate candidate = gson.fromJson(data.getData(), IceCandidate.class);
                            if (webRTCClient != null) webRTCClient.addIceCandidate(candidate);
                            break;
                        case EndCall:
                        case RejectCall:
                            if (listener != null) listener.webrtcClosed();
                            callBack.onNewEventReceived(data);
                            endCall();
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Signaling Error: " + e.getMessage());
                }
            });
        }
    }

    public void endCall() {
        if (webRTCClient != null) webRTCClient.closeConnection();
        target = null;
        iceCandidateBuffer.clear();
        // Option: localView = null; // Agar activity destroy ho rahi ho
    }

    public void rejectCall() {
        if (target != null) {
            DataModel model = new DataModel(target, currentUsername, null, DataModelType.RejectCall);
            firebaseClient.sendMessageToOtherUser(model, () -> {}, () -> Log.e(TAG, "Reject Error"));
        }
        endCall();
    }

    public void switchCamera() { if (webRTCClient != null) webRTCClient.switchCamera(); }
    public void toggleAudio(boolean isMuted) { if (webRTCClient != null) webRTCClient.toggleAudio(isMuted); }
    public void toggleVideo(boolean isMuted) { if (webRTCClient != null) webRTCClient.toggleVideo(isMuted); }

    public interface Listener {
        void webrtcConnected();
        void webrtcClosed();
    }
}