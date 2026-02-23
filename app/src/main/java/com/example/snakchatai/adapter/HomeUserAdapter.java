package com.example.snakchatai.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakchatai.R;
import com.example.snakchatai.chat_screen;
import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.utils.AndroidUtil;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class HomeUserAdapter
        extends FirestoreRecyclerAdapter<UserModel, HomeUserAdapter.UserViewHolder> {

    private final Context context;

    public HomeUserAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options,
                           Context context) {
        super(options);
        this.context = context;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        try {
            if (position >= 0 && position < getSnapshots().size()) {
                return getSnapshots().getSnapshot(position).getId().hashCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RecyclerView.NO_ID;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder,
                                    int position,
                                    @NonNull UserModel model) {
        try {
            // Safe position check
            int safePosition = holder.getBindingAdapterPosition();
            if (safePosition == RecyclerView.NO_POSITION) {
                return;
            }

            // Check bounds
            if (safePosition >= getSnapshots().size()) {
                return;
            }

            holder.username.setText(model.getUsername());
            holder.phone.setText(model.getPhone());

            // Handle profile picture asynchronously
            FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId())
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        // Double check position hasn't changed
                        if (holder.getBindingAdapterPosition() == safePosition) {
                            AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Double check position hasn't changed
                        if (holder.getBindingAdapterPosition() == safePosition) {
                            holder.profilePic.setImageResource(R.drawable.baseline_person_24);
                        }
                    });

            // Click listener
            holder.itemView.setOnClickListener(v -> {
                int clickPosition = holder.getBindingAdapterPosition();
                if (clickPosition != RecyclerView.NO_POSITION &&
                        clickPosition < getSnapshots().size()) {

                    Intent intent = new Intent(context, chat_screen.class);
                    Bundle args = new Bundle();
                    args.putParcelable("user", model);
                    intent.putExtras(args);
                    context.startActivity(intent);
                }
            });

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_user_recycler_raw, parent, false);
        return new UserViewHolder(view);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username, phone;
        ImageView profilePic;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_name_text);
            phone = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}