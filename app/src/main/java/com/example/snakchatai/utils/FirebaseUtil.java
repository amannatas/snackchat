package com.example.snakchatai.utils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {

    // --- FIX: Firestore instance ka method add kiya ---
    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://snakchat-ai-default-rtdb.asia-southeast1.firebasedatabase.app"
    );

    public static String currentUserId(){
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn(){
        return currentUserId() != null;
    }

    public static DocumentReference currentUserDetails(){
        return getFirestore().collection("users").document(currentUserId());
    }

    public static CollectionReference allUserCollectionReference(){
        return getFirestore().collection("users");
    }

    public static DocumentReference getChatroomReference(String chatroomId){
        return getFirestore().collection("chatrooms").document(chatroomId);
    }

    public static CollectionReference getChatroomMessageReference(String chatroomId){
        return getChatroomReference(chatroomId).collection("chats");
    }

    public static String getChatroomId(String userId1,String userId2){
        if(userId1.hashCode()<userId2.hashCode()){
            return userId1+"_"+userId2;
        }else{
            return userId2+"_"+userId1;
        }
    }

    public static CollectionReference allChatroomCollectionReference(){
        return getFirestore().collection("chatrooms");
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(FirebaseUtil.currentUserId())){
            return allUserCollectionReference().document(userIds.get(1));
        }else{
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    public static String timestampToString(Timestamp timestamp){
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }

    public static void logout(){
        FirebaseAuth.getInstance().signOut();
    }

    public static StorageReference  getCurrentProfilePicStorageRef(){
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(FirebaseUtil.currentUserId());
    }

    public static StorageReference  getOtherProfilePicStorageRef(String otherUserId){
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(otherUserId);
    }

    public static StorageReference getChatroomImageStorageRef(String chatroomId) {
        return FirebaseStorage.getInstance().getReference().child("chatroom_images").child(chatroomId);
    }

    public static CollectionReference getCallLogCollectionReference(String userId) {
        return allUserCollectionReference().document(userId).collection("calls");
    }

    public static void getCurrentUsername(
            OnCompleteListener<DocumentSnapshot> listener
    ) {
        currentUserDetails().get().addOnCompleteListener(listener);
    }
}