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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRecyclerAdapter
        extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    private final Context context;
    private final SelectionListener selectionListener;

    // 🔥 stable selection (documentId based)
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
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getSnapshots().getSnapshot(position).getId().hashCode();
    }

    @Override
    protected void onBindViewHolder(
            @NonNull ChatModelViewHolder holder,
            int position,
            @NonNull ChatMessageModel model
    ) {

        // ---------- MESSAGE UI ----------
        boolean isMe = model.getSenderId().equals(FirebaseUtil.currentUserId());

        holder.leftChatLayout.setVisibility(isMe ? View.GONE : View.VISIBLE);
        holder.rightChatLayout.setVisibility(isMe ? View.VISIBLE : View.GONE);

        if ("IMAGE".equals(model.getMessageType())) {
            if (isMe) {
                holder.rightChatImageView.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setVisibility(View.GONE);
                Glide.with(context).load(model.getMessage()).into(holder.rightChatImageView);
            } else {
                holder.leftChatImageView.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setVisibility(View.GONE);
                Glide.with(context).load(model.getMessage()).into(holder.leftChatImageView);
            }
        } else {
            if (isMe) {
                holder.rightChatImageView.setVisibility(View.GONE);
                holder.rightChatTextview.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setText(model.getMessage());
            } else {
                holder.leftChatImageView.setVisibility(View.GONE);
                holder.leftChatTextview.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setText(model.getMessage());
            }
        }

        // ---------- SELECTION STATE (🔥 CRITICAL LINE) ----------
        String docId = getSnapshots().getSnapshot(position).getId();
        holder.itemView.setActivated(selectedIds.contains(docId));

        // ---------- LONG CLICK ----------
        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(holder.getBindingAdapterPosition());
            return true;
        });

        // ---------- NORMAL CLICK ----------
        holder.itemView.setOnClickListener(v -> {
            if (!isSelectionMode) return;
            toggleSelection(holder.getBindingAdapterPosition());
        });
    }

    // ---------- SELECTION CORE ----------
    private void toggleSelection(int position) {
        if (position == RecyclerView.NO_POSITION) return;

        String docId = getSnapshots().getSnapshot(position).getId();

        if (selectedIds.contains(docId)) {
            selectedIds.remove(docId);
        } else {
            selectedIds.add(docId);
        }

        isSelectionMode = !selectedIds.isEmpty();
        notifyItemChanged(position);
        selectionListener.onSelectionModeChanged(isSelectionMode, selectedIds.size());
    }

    public void clearSelection() {
        if (selectedIds.isEmpty()) return;

        selectedIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        selectionListener.onSelectionModeChanged(false, 0);
    }

    public List<DocumentSnapshot> getSelectedItems() {
        List<DocumentSnapshot> list = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            DocumentSnapshot s = getSnapshots().getSnapshot(i);
            if (selectedIds.contains(s.getId())) {
                list.add(s);
            }
        }
        return list;
    }

    // ---------- VIEW HOLDER ----------
    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    static class ChatModelViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;
        ImageView leftChatImageView, rightChatImageView;

        ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            leftChatImageView = itemView.findViewById(R.id.left_chat_imageview);
            rightChatImageView = itemView.findViewById(R.id.right_chat_imageview);
        }
    }
}
