package com.example.snakchatai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakchatai.adapter.ChatRecyclerAdapter;
import com.example.snakchatai.model.ChatMessageModel;
import com.example.snakchatai.model.ChatroomModel;
import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class chat_screen extends AppCompatActivity implements ChatRecyclerAdapter.SelectionListener {

    private UserModel otherUser;
    private String chatroomId;
    private ChatroomModel chatroomModel;
    private ChatRecyclerAdapter adapter;

    private EditText messageInput;
    private ImageButton sendMessageBtn, backBtn, videoCallBtn, attachFileBtn;
    private TextView otherUsername;
    private RecyclerView recyclerView;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActionMode actionMode;

    private boolean isLoadingMore = false;
    private long messageLimit = 30;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final String CLOUD_NAME = "dbolenrtr";
    private final String UPLOAD_PRESET = "chat_images";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        initImagePicker();

        otherUser = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? getIntent().getParcelableExtra("user", UserModel.class)
                : getIntent().getParcelableExtra("user");

        if (otherUser == null) {
            finish();
            return;
        }

        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        bindViews();
        setupClicks();

        otherUsername.setText(otherUser.getUsername());

        getOrCreateChatroom();
        setupRecycler();
    }

    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        uploadImageToCloudinary(result.getData().getData());
                    }
                });
    }

    private void bindViews() {
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        videoCallBtn = findViewById(R.id.video_call_btn);
        attachFileBtn = findViewById(R.id.attach_file_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
    }

    private void setupClicks() {
        backBtn.setOnClickListener(v -> finish());
        sendMessageBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) sendMessage(msg, "TEXT");
        });
        attachFileBtn.setOnClickListener(v ->
                com.github.dhaval2404.imagepicker.ImagePicker.with(this)
                        .crop().compress(1024).maxResultSize(1080, 1080)
                        .createIntent(intent -> {
                            imagePickerLauncher.launch(intent);
                            return null;
                        })
        );
        videoCallBtn.setOnClickListener(v -> {
            sendCallNotification();
            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("targetUserId", otherUser.getUserId());
            startActivity(intent);
        });
    }

    private void setupRecycler() {
        SafeLinearLayoutManager lm = new SafeLinearLayoutManager(this);
        lm.setReverseLayout(true);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);

        recyclerView.setItemAnimator(null);

        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(messageLimit);

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(query, ChatMessageModel.class)
                        .build();

        adapter = new ChatRecyclerAdapter(options, this, this);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (positionStart == 0 && itemCount == 1 && !isLoadingMore) {
                    recyclerView.scrollToPosition(0);
                }
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(-1) && !isLoadingMore && dy < 0) {
                    loadMoreMessages();
                }
            }
        });
    }

    private void loadMoreMessages() {
        isLoadingMore = true;
        messageLimit += 30;

        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(messageLimit);

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(query, ChatMessageModel.class)
                        .build();

        recyclerView.post(() -> {
            if (adapter != null) {
                adapter.updateOptions(options);
                recyclerView.postDelayed(() -> isLoadingMore = false, 1500);
            }
        });
    }

    private void sendMessage(String msg, String type) {
        if (chatroomModel == null) return;

        // Update Chatroom state
        chatroomModel.setLastMessage(type.equals("IMAGE") ? "Image" : msg);
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSeen(false); // New message is unseen by other

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        // Add message to sub-collection
        // Seen is false by default when sending
        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(new ChatMessageModel(msg, FirebaseUtil.currentUserId(), Timestamp.now(), type, false))
                .addOnSuccessListener(doc -> { if (type.equals("TEXT")) messageInput.setText(""); });

        sendChatNotification(msg);
    }

    private void markMessageAsSeen() {
        // Mark all messages sent by OTHER user as seen
        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .whereEqualTo("senderId", otherUser.getUserId())
                .whereEqualTo("seen", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("seen", true);
                    }
                });

        // Also update the chatroom main seen status if I was the receiver
        if (chatroomModel != null && !chatroomModel.getLastMessageSenderId().equals(FirebaseUtil.currentUserId())) {
            FirebaseUtil.getChatroomReference(chatroomId).update("lastMessageSeen", true);
        }
    }

    private void sendChatNotification(String message) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("targetUserId", otherUser.getUserId());
            jsonBody.put("senderId", FirebaseUtil.currentUserId());
            jsonBody.put("message", message);

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://notification-server-18zv.onrender.com/send-chat-notification")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    // Agar network error ho ya server offline ho
                    Log.e("ChatNotification", "Failed to send: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        // Agar server ne 200 OK diya
                        Log.d("ChatNotification", "Success! Response: " + response.body().string());
                    } else {
                        // Agar server ne error diya (404, 500, etc.)
                        Log.e("ChatNotification", "Server Error: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e("ChatNotification", "JSON Error", e);
        }
    }

    private void uploadImageToCloudinary(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[is.available()];
            is.read(bytes); is.close();
            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/*"));
            MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "chat_image.jpg", fileBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET).build();
            Request request = new Request.Builder().url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload").post(requestBody).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {}
                @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject obj = new JSONObject(response.body().string());
                            String imageUrl = obj.getString("secure_url");
                            runOnUiThread(() -> sendMessage(imageUrl, "IMAGE"));
                        } catch (Exception e) { Log.e("Cloudinary", "Parse error", e); }
                    }
                }
            });
        } catch (Exception e) { Log.e("Cloudinary", "Upload error", e); }
    }

    private void getOrCreateChatroom() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnSuccessListener(doc -> {
            chatroomModel = doc.toObject(ChatroomModel.class);
            if (chatroomModel == null) {
                // FIXED CONSTRUCTOR: Added 5th argument (empty message) to match your ChatroomModel
                chatroomModel = new ChatroomModel(chatroomId, Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()), Timestamp.now(), "", "");
                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
            }
            markMessageAsSeen(); // Mark seen once chatroom is loaded
        });
    }

    @Override
    public void onSelectionModeChanged(boolean active, int count) {
        if (active) {
            if (actionMode == null) {
                actionMode = startActionMode(new ActionMode.Callback() {
                    @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) { getMenuInflater().inflate(R.menu.delete_chat_menu, menu); return true; }
                    @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        if (item.getItemId() == R.id.delete_chat) {
                            for (DocumentSnapshot s : adapter.getSelectedItems()) s.getReference().delete();
                            mode.finish(); return true;
                        } return false;
                    }
                    @Override public void onDestroyActionMode(ActionMode mode) { actionMode = null; adapter.clearSelection(); }
                    @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
                });
            }
            actionMode.setTitle(String.valueOf(count));
        } else if (actionMode != null) { actionMode.finish(); }
    }

    @Override protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
        markMessageAsSeen(); // Mark seen when user returns to screen
    }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
    @Override protected void onDestroy() { super.onDestroy(); if (actionMode != null) actionMode.finish(); }

    class SafeLinearLayoutManager extends LinearLayoutManager {
        public SafeLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); }
            catch (IndexOutOfBoundsException e) { Log.e("STC_DEBUG", "Caught RecyclerView Inconsistency!"); }
        }
    }
    private void sendCallNotification() {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("targetUserId", otherUser.getUserId());
            jsonBody.put("senderId", FirebaseUtil.currentUserId());
            // Agar aapke paas current user ka naam hai toh yahan dynamic kar dein
            jsonBody.put("senderName", "Aman (SnakeChat)");
            jsonBody.put("message", "Incoming Video Call..."); // Ye line add karein
            jsonBody.put("type", "VIDEO_CALL");

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://notification-server-18zv.onrender.com/send-chat-notification") // Server ka endpoint
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    Log.e("CallNotification", "Failed to send call notification: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    Log.d("CallNotification", "Call notification sent success!");
                }
            });
        } catch (Exception e) {
            Log.e("CallNotification", "Error: " + e.getMessage());
        }
    }
}