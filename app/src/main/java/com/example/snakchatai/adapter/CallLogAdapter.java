package com.example.snakchatai.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.snakchatai.R;
import com.example.snakchatai.model.CallLogModel;
import com.example.snakchatai.model.UserModel;
import com.example.snakchatai.utils.AndroidUtil;
import com.example.snakchatai.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class CallLogAdapter extends FirestoreRecyclerAdapter<CallLogModel, CallLogAdapter.CallLogViewHolder> {

    private Context context;

    public CallLogAdapter(@NonNull FirestoreRecyclerOptions<CallLogModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull CallLogViewHolder holder, int position, @NonNull CallLogModel model) {

        FirebaseUtil.allUserCollectionReference().document(model.getUserId()).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        UserModel otherUser = task.getResult().toObject(UserModel.class);
                        if (otherUser != null) {
                            holder.username.setText(otherUser.getUsername());
                            FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                    });
                        }
                    }
                });

        if (model.isOutgoing()) {
            holder.callTypeIcon.setImageResource(R.drawable.ic_call_made);
        } else {
            holder.callTypeIcon.setImageResource(R.drawable.ic_call_received);
        }
    }

    @NonNull
    @Override
    public CallLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_log_recycler_row, parent, false);
        return new CallLogViewHolder(view);
    }

    static class CallLogViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView profilePic;
        ImageView callTypeIcon;

        public CallLogViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_name_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            callTypeIcon = itemView.findViewById(R.id.call_type_icon);
        }
    }
}
