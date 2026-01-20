package com.perfectpose.loginpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvSignup;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);

        auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> {
            auth.signInWithEmailAndPassword(
                    etEmail.getText().toString(),
                    etPassword.getText().toString()
            ).addOnSuccessListener(r -> {
                startActivity(new Intent(this, ExerciseActivity.class));
                finish();
            });
        });

        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class))
        );
    }
}
