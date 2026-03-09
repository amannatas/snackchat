package com.example.snakchatai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.snakchatai.utils.AESUtils;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

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
    private ImageButton sendMessageBtn, backBtn, videoCallBtn, attachFileBtn, menuBtn;
    private TextView otherUsername;
    private RecyclerView recyclerView;
    private RelativeLayout toolbar;

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
        otherUsername.setSingleLine(true);
        otherUsername.setEllipsize(android.text.TextUtils.TruncateAt.END);

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
        toolbar = findViewById(R.id.toolbar);
        menuBtn = findViewById(R.id.menu_btn);
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

        if(menuBtn != null) {
            menuBtn.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
                popup.getMenu().add("Clear All Chat");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Clear All Chat")) {
                        showClearChatDialog();
                    }
                    return true;
                });
                popup.show();
            });
        }
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Kya aap saari chat delete karna chahte hain?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Toast.makeText(this, "Clearing chat...", Toast.LENGTH_SHORT).show();
                    recursiveDelete(FirebaseUtil.getChatroomMessageReference(chatroomId).limit(400));
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void recursiveDelete(Query query) {
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                updateChatroomAfterClear();
                return;
            }
            WriteBatch batch = FirebaseUtil.getFirestore().batch();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                batch.delete(doc.getReference());
            }
            batch.commit().addOnSuccessListener(unused -> recursiveDelete(query));
        });
    }

    private void updateChatroomAfterClear() {
        FirebaseUtil.getChatroomReference(chatroomId)
                .update("lastMessage", "Chat cleared", "lastMessageTimestamp", Timestamp.now())
                .addOnSuccessListener(unused -> runOnUiThread(() -> {
                    Toast.makeText(chat_screen.this, "Chat Cleared!", Toast.LENGTH_SHORT).show();
                    if (adapter != null) adapter.notifyDataSetChanged();
                }));
    }

    @Override
    public void onSelectionModeChanged(boolean active, int count) {
        if (active) {
            if (toolbar != null) toolbar.setVisibility(View.GONE);
            if (actionMode == null) {
                actionMode = startActionMode(new ActionMode.Callback() {
                    @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        getMenuInflater().inflate(R.menu.delete_chat_menu, menu);
                        return true;
                    }
                    @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        if (item.getItemId() == R.id.delete_chat) {
                            List<DocumentSnapshot> selected = adapter.getSelectedItems();
                            for (DocumentSnapshot s : selected) s.getReference().delete();
                            mode.finish();
                            return true;
                        }
                        return false;
                    }
                    @Override public void onDestroyActionMode(ActionMode mode) {
                        actionMode = null;
                        adapter.clearSelection();
                        if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
                    }
                    @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
                });
            }
            if (actionMode != null) actionMode.setTitle(String.valueOf(count));
        } else if (actionMode != null) {
            actionMode.finish();
        }
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
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId).orderBy("timestamp", Query.Direction.DESCENDING).limit(messageLimit);
        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>().setQuery(query, ChatMessageModel.class).build();
        if (adapter != null) {
            adapter.updateOptions(options);
            recyclerView.postDelayed(() -> isLoadingMore = false, 1500);
        }
    }

    private void sendMessage(String msg, String type) {
        if (chatroomModel == null) return;

        // 🔒 Encryption logic
        String finalMsg = type.equals("TEXT") ? AESUtils.encrypt(msg) : msg;

        chatroomModel.setLastMessage(type.equals("IMAGE") ? "Image" : msg);
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSeen(false);

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(new ChatMessageModel(finalMsg, FirebaseUtil.currentUserId(), Timestamp.now(), type, false))
                .addOnSuccessListener(doc -> {
                    if (type.equals("TEXT")) messageInput.setText("");
                });

        sendChatNotification(type.equals("TEXT") ? msg : "Sent an image");
    }

    private void uploadImageToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] bytes = byteBuffer.toByteArray();
            inputStream.close();

            RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "img_" + System.currentTimeMillis() + ".jpg", fileBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET).build();

            Request request = new Request.Builder().url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload").post(requestBody).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {}
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws java.io.IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String url = new JSONObject(response.body().string()).getString("secure_url");
                            runOnUiThread(() -> sendMessage(url, "IMAGE"));
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    response.close();
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void markMessageAsSeen() {
        FirebaseUtil.getChatroomMessageReference(chatroomId).whereEqualTo("senderId", otherUser.getUserId()).whereEqualTo("seen", false).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) doc.getReference().update("seen", true);
                });
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
            Request request = new Request.Builder().url("https://notification-server-18zv.onrender.com/send-chat-notification").post(body).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {}
                @Override public void onResponse(Call call, Response response) throws java.io.IOException { response.close(); }
            });
        } catch (Exception e) { Log.e("ChatNotification", "JSON Error", e); }
    }

    private void getOrCreateChatroom() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnSuccessListener(doc -> {
            chatroomModel = doc.toObject(ChatroomModel.class);
            if (chatroomModel == null) {
                chatroomModel = new ChatroomModel(chatroomId, Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()), Timestamp.now(), "", "");
                FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
            }
            markMessageAsSeen();
        });
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); markMessageAsSeen(); }
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
            jsonBody.put("senderName", "SnakeChat User");
            jsonBody.put("message", "Incoming Video Call...");
            jsonBody.put("type", "VIDEO_CALL");
            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url("https://notification-server-18zv.onrender.com/send-chat-notification").post(body).build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {}
                @Override public void onResponse(Call call, Response response) throws java.io.IOException { response.close(); }
            });
        } catch (Exception e) { Log.e("CallNotification", "Error: " + e.getMessage()); }
    }
}