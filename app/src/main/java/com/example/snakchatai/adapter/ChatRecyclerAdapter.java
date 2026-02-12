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
import java.util.List;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    private final Context context;
    private boolean isSelectionMode = false;
    private final List<Integer> selectedPositions = new ArrayList<>();
    private final SelectionListener selectionListener;

    public interface SelectionListener {
        void onSelectionModeChanged(boolean isSelectionMode, int selectedItems);
    }

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, SelectionListener selectionListener) {
        super(options);
        this.context = context;
        this.selectionListener = selectionListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);

            if (model.getMessageType() != null && model.getMessageType().equals("IMAGE")) {
                holder.rightChatImageView.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setVisibility(View.GONE);
                Glide.with(context).load(model.getMessage()).into(holder.rightChatImageView);
            } else {
                holder.rightChatImageView.setVisibility(View.GONE);
                holder.rightChatTextview.setVisibility(View.VISIBLE);
                holder.rightChatTextview.setText(model.getMessage());
            }
        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            if (model.getMessageType() != null && model.getMessageType().equals("IMAGE")) {
                holder.leftChatImageView.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setVisibility(View.GONE);
                Glide.with(context).load(model.getMessage()).into(holder.leftChatImageView);
            } else {
                holder.leftChatImageView.setVisibility(View.GONE);
                holder.leftChatTextview.setVisibility(View.VISIBLE);
                holder.leftChatTextview.setText(model.getMessage());
            }
        }

        holder.itemView.setSelected(selectedPositions.contains(position));

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(holder.getAdapterPosition());
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(holder.getAdapterPosition());
            }
        });
    }

    private void toggleSelection(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);

        if (selectedPositions.isEmpty()) {
            isSelectionMode = false;
        }
        selectionListener.onSelectionModeChanged(isSelectionMode, selectedPositions.size());
    }

    public void clearSelection() {
        if (!isSelectionMode) {
            return;
        }
        isSelectionMode = false;
        List<Integer> positionsToUpdate = new ArrayList<>(selectedPositions);
        selectedPositions.clear();
        for (int position : positionsToUpdate) {
            if (position < getItemCount()) {
                notifyItemChanged(position);
            }
        }
        selectionListener.onSelectionModeChanged(false, 0);
    }

    public List<DocumentSnapshot> getSelectedItems() {
        List<DocumentSnapshot> selectedItems = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < getItemCount()) {
                selectedItems.add(getSnapshots().getSnapshot(position));
            }
        }
        return selectedItems;
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    static class ChatModelViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;
        ImageView leftChatImageView, rightChatImageView;

        public ChatModelViewHolder(@NonNull View itemView) {
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
