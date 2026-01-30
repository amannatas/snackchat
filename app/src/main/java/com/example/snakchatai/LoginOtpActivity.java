package com.example.snakchatai;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginOtpActivity extends AppCompatActivity {

    private String phoneNumber;
    private String verificationCode;

    private final long OTP_TIMEOUT_SECONDS = 60L;

    private EditText otpInput;
    private Button nextBtn;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_otp);

        otpInput = findViewById(R.id.login_otp);
        nextBtn = findViewById(R.id.login_next_btn);
        progressBar = findViewById(R.id.login_progress_bar);

        mAuth = FirebaseAuth.getInstance();

        Bundle bundle = getIntent().getExtras();
        if (bundle == null || !bundle.containsKey("phone")) {
            finish();
            return;
        }

        phoneNumber = bundle.getString("phone");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            finish();
            return;
        }

        sendOtp(false);

        nextBtn.setOnClickListener(v -> {
            String enteredOtp = otpInput.getText().toString().trim();

            if (enteredOtp.length() != 6) {
                Toast.makeText(this, "wrong otp", Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneAuthCredential credential =
                    PhoneAuthProvider.getCredential(verificationCode, enteredOtp);
            signIn(credential);
        });
    }

    private void sendOtp(boolean isResend) {
        setInProgress(true);

        PhoneAuthOptions.Builder builder =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                if (mAuth.getCurrentUser() == null) {
                                    signIn(credential);
                                }
                                setInProgress(false);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(LoginOtpActivity.this, "otp not sent", Toast.LENGTH_SHORT).show();
                                setInProgress(false);
                            }

                            @Override
                            public void onCodeSent(@NonNull String code,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                verificationCode = code;
                                Toast.makeText(LoginOtpActivity.this, "otp sent", Toast.LENGTH_SHORT).show();
                                setInProgress(false);
                            }
                        });

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void signIn(PhoneAuthCredential credential) {
        setInProgress(true);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setInProgress(false);

                        if (task.isSuccessful()) {
                            // yahi pe login successful hua hai, toh yahan daal
                            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.apply();
                            // Firestore instance
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            // User data map
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("phone", phoneNumber);
                            userData.put("lastLogin", System.currentTimeMillis());

                            // Firestore me user document save kar (user UID ko document id bana ke)
                            db.collection("users")
                                    .document(mAuth.getCurrentUser().getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Data saved successfully, ab next screen pe ja
                                        Intent intent = new Intent(LoginOtpActivity.this,LoginUsernameActivity.class);
                                        intent.putExtra("phone", phoneNumber);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Agar data save fail hua to bhi next screen ja sakta hai, but toast dikha do
                                        Toast.makeText(LoginOtpActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginOtpActivity.this, MainActivity.class);
                                        intent.putExtra("phone", phoneNumber);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    });
                        } else {
                            Toast.makeText(LoginOtpActivity.this, "OTP verification failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setInProgress(boolean inProgress) {
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        nextBtn.setVisibility(inProgress ? View.GONE : View.VISIBLE);
    }
}
