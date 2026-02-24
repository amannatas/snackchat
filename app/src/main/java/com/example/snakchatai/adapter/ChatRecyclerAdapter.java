package com.example.snakchatai.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.snakchatai.R;
import com.example.snakchatai.model.ChatMessageModel;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChatRecyclerAdapter
        extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    private final Context context;
    private final SelectionListener selectionListener;
    private final Set<String> selectedIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface SelectionListener {
        void onSelectionModeChanged(boolean active, int count);
    }

    public ChatRecyclerAdapter(
            @NonNull FirestoreRecyclerOptions<ChatMessageModel> options,
            Context context,
            SelectionListener selectionListener
    ) {
        super(options);
        this.context = context;
        this.selectionListener = selectionListener;
        setHasStableIds(false);
    }

    @Override
    protected void onBindViewHolder(
            @NonNull ChatModelViewHolder holder,
            int position,
            @NonNull ChatMessageModel model
    ) {
        try {
            int safePosition = holder.getBindingAdapterPosition();
            if (safePosition == RecyclerView.NO_POSITION || safePosition >= getSnapshots().size()) {
                return;
            }

            boolean isMe = model.getSenderId() != null &&
                    model.getSenderId().equals(FirebaseUtil.currentUserId());

            holder.leftChatLayout.setVisibility(isMe ? View.GONE : View.VISIBLE);
            holder.rightChatLayout.setVisibility(isMe ? View.VISIBLE : View.GONE);

            holder.leftChatImageView.setVisibility(View.GONE);
            holder.rightChatImageView.setVisibility(View.GONE);
            holder.leftChatTextview.setVisibility(View.GONE);
            holder.rightChatTextview.setVisibility(View.GONE);

            // --- TIME LOGIC ---
            if (model.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeString = sdf.format(model.getTimestamp().toDate());
                holder.leftTime.setText(timeString);
                holder.rightTime.setText(timeString);
            }

            // --- SEEN STATUS LOGIC (Only for me) ---
            if (isMe) {
                holder.seenStatusIcon.setVisibility(View.VISIBLE);
                if (model.isSeen()) {
                    // Yahan aap apna blue tick icon set karein
                    holder.seenStatusIcon.setImageResource(R.drawable.ic_seen_blue);
                } else {
                    // Yahan aap apna grey tick icon set karein
                    holder.seenStatusIcon.setImageResource(R.drawable.ic_delivered_grey);
                }
            } else {
                holder.seenStatusIcon.setVisibility(View.GONE);
            }

            // --- MESSAGE CONTENT LOGIC ---
            if ("IMAGE".equals(model.getMessageType())) {
                if (isMe) {
                    holder.rightChatImageView.setVisibility(View.VISIBLE);
                    Glide.with(context).load(model.getMessage()).into(holder.rightChatImageView);
                } else {
                    holder.leftChatImageView.setVisibility(View.VISIBLE);
                    Glide.with(context).load(model.getMessage()).into(holder.leftChatImageView);
                }
            } else {
                if (isMe) {
                    holder.rightChatTextview.setVisibility(View.VISIBLE);
                    holder.rightChatTextview.setText(model.getMessage());
                } else {
                    holder.leftChatTextview.setVisibility(View.VISIBLE);
                    holder.leftChatTextview.setText(model.getMessage());
                }
            }

            // --- SELECTION LOGIC ---
            DocumentSnapshot snapshot = getSnapshots().getSnapshot(safePosition);
            String docId = snapshot.getId();
            holder.itemView.setActivated(selectedIds.contains(docId));

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < getSnapshots().size()) {
                    toggleSelection(pos);
                }
                return true;
            });

            holder.itemView.setOnClickListener(v -> {
                if (!isSelectionMode) return;
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < getSnapshots().size()) {
                    toggleSelection(pos);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSelection(int position) {
        try {
            if (position == RecyclerView.NO_POSITION || position >= getSnapshots().size()) return;
            DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
            String docId = snapshot.getId();

            if (selectedIds.contains(docId)) {
                selectedIds.remove(docId);
            } else {
                selectedIds.add(docId);
            }

            isSelectionMode = !selectedIds.isEmpty();
            notifyItemChanged(position);

            if (selectionListener != null) {
                selectionListener.onSelectionModeChanged(isSelectionMode, selectedIds.size());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void clearSelection() {
        if (selectedIds.isEmpty()) return;
        selectedIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (selectionListener != null) selectionListener.onSelectionModeChanged(false, 0);
    }

    public List<DocumentSnapshot> getSelectedItems() {
        List<DocumentSnapshot> list = new ArrayList<>();
        try {
            for (int i = 0; i < getSnapshots().size(); i++) {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(i);
                if (selectedIds.contains(snapshot.getId())) list.add(snapshot);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (!selectedIds.isEmpty()) {
            selectedIds.clear();
            isSelectionMode = false;
            if (selectionListener != null) selectionListener.onSelectionModeChanged(false, 0);
        }
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    static class ChatModelViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview, leftTime, rightTime;
        ImageView leftChatImageView, rightChatImageView, seenStatusIcon;

        ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftChatImageView = itemView.findViewById(R.id.left_chat_imageview);
            rightChatImageView = itemView.findViewById(R.id.right_chat_imageview);

            // Naye IDs jo XML mein add karni hain:
            leftTime = itemView.findViewById(R.id.left_time_text);
            rightTime = itemView.findViewById(R.id.right_time_text);
            seenStatusIcon = itemView.findViewById(R.id.seen_status_icon);
        }
    }
}