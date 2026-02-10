package com.example.snakchatai;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
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

import java.util.Arrays;
import java.util.UUID;

public class chat_screen extends AppCompatActivity
        implements ChatRecyclerAdapter.SelectionListener {

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;

    ChatRecyclerAdapter adapter;

    EditText messageInput;
    ImageButton sendMessageBtn, backBtn, videoCallBtn, attachFileBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;

    ActivityResultLauncher<Intent> imagePickerLauncher;
    ActionMode actionMode;

    boolean isLoadingMore = false;
    long messageLimit = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        imagePickerLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK &&
                                    result.getData() != null &&
                                    result.getData().getData() != null) {
                                sendImage(result.getData().getData());
                            }
                        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            otherUser = getIntent().getParcelableExtra("user", UserModel.class);
        } else {
            otherUser = getIntent().getParcelableExtra("user");
        }

        if (otherUser == null) {
            finish();
            return;
        }

        chatroomId = FirebaseUtil.getChatroomId(
                FirebaseUtil.currentUserId(),
                otherUser.getUserId()
        );

        bindViews();
        setupClicks();

        otherUsername.setText(otherUser.getUsername());

        getOrCreateChatroom();
        setupRecycler();
    }

    private void bindViews() {
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        videoCallBtn = findViewById(R.id.video_call_btn);
        attachFileBtn = findViewById(R.id.attach_file_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);
    }

    private void setupClicks() {

        backBtn.setOnClickListener(v -> finish());

        sendMessageBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) sendMessage(msg, "TEXT");
        });

        attachFileBtn.setOnClickListener(v ->
                com.github.dhaval2404.imagepicker.ImagePicker.with(this)
                        .crop()
                        .compress(1024)
                        .maxResultSize(1080, 1080)
                        .createIntent(intent -> {
                            imagePickerLauncher.launch(intent);
                            return null;
                        })
        );

        videoCallBtn.setOnClickListener(v -> {
            call_fragment callFragment = new call_fragment();
            Bundle args = new Bundle();
            args.putString("targetUserId", otherUser.getUserId());
            callFragment.setArguments(args);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, callFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    private void setupRecycler() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(messageLimit);

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(query, ChatMessageModel.class)
                        .build();

        adapter = new ChatRecyclerAdapter(options, this, this);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(true);

        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!rv.canScrollVertically(-1) && !isLoadingMore) {
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

        adapter.updateOptions(options);
        recyclerView.postDelayed(() -> isLoadingMore = false, 1000);
    }

    private void sendMessage(String msg, String type) {
        if (chatroomModel == null) return;

        chatroomModel.setLastMessage(type.equals("IMAGE") ? "Image" : msg);
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessageTimestamp(Timestamp.now());

        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        FirebaseUtil.getChatroomMessageReference(chatroomId)
                .add(new ChatMessageModel(
                        msg,
                        FirebaseUtil.currentUserId(),
                        Timestamp.now(),
                        type
                ));

        messageInput.setText("");
    }

    private void sendImage(Uri uri) {
        String name = UUID.randomUUID().toString();
        FirebaseUtil.getChatroomImageStorageRef(chatroomId)
                .child(name)
                .putFile(uri)
                .addOnSuccessListener(t ->
                        t.getStorage().getDownloadUrl()
                                .addOnSuccessListener(u ->
                                        sendMessage(u.toString(), "IMAGE")
                                ));
    }

    private void getOrCreateChatroom() {
        FirebaseUtil.getChatroomReference(chatroomId)
                .get()
                .addOnSuccessListener(doc -> {
                    chatroomModel = doc.toObject(ChatroomModel.class);
                    if (chatroomModel == null) {
                        chatroomModel = new ChatroomModel(
                                chatroomId,
                                Arrays.asList(
                                        FirebaseUtil.currentUserId(),
                                        otherUser.getUserId()
                                ),
                                Timestamp.now(),
                                ""
                        );
                        FirebaseUtil.getChatroomReference(chatroomId)
                                .set(chatroomModel);
                    }
                });
    }

    @Override
    public void onSelectionModeChanged(boolean active, int count) {
        if (active) {
            if (actionMode == null) {
                actionMode = startActionMode(new ActionMode.Callback() {
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        getMenuInflater().inflate(R.menu.delete_chat_menu, menu);
                        return true;
                    }

                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        if (item.getItemId() == R.id.delete_chat) {
                            for (DocumentSnapshot s : adapter.getSelectedItems())
                                s.getReference().delete();
                            mode.finish();
                            return true;
                        }
                        return false;
                    }

                    public void onDestroyActionMode(ActionMode mode) {
                        actionMode = null;
                        adapter.clearSelection();
                    }

                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }
                });
            }
            actionMode.setTitle(String.valueOf(count));
        } else if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.stopListening();
        if (actionMode != null) actionMode.finish();
    }
}
