package com.example.snakchatai.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder,
                                    int position,
                                    @NonNull UserModel model) {

        holder.username.setText(model.getUsername());
        holder.phone.setText(model.getPhone());

        FirebaseUtil.getOtherProfilePicStorageRef(model.getUserId())
                .getDownloadUrl()
                .addOnSuccessListener(uri ->
                        AndroidUtil.setProfilePic(context, uri, holder.profilePic)
                );

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, chat_screen.class);
            AndroidUtil.passUserModelAsIntent(intent, model);
            context.startActivity(intent); // ✅ NO FLAGS
        });
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.search_user_recycler_raw, parent, false);
        return new UserViewHolder(view);
    }

    static class UserViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
