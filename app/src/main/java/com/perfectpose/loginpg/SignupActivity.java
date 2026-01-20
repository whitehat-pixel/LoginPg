package com.perfectpose.loginpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    EditText etUser, etPass;
    Button btnSignup;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnSignup = findViewById(R.id.btnSignup);
        auth = FirebaseAuth.getInstance();

        btnSignup.setOnClickListener(v -> {
            auth.createUserWithEmailAndPassword(
                    etUser.getText().toString(),
                    etPass.getText().toString()
            ).addOnSuccessListener(r -> {
                startActivity(new Intent(this, ExerciseActivity.class));
                finish();
            });
        });
    }
}
