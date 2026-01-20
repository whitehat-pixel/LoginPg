package com.perfectpose.loginpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPushup = findViewById(R.id.btnPushup);
        Button btnSquat = findViewById(R.id.btnSquat);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnPushup.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PoseActivity.class);
            intent.putExtra("MODE", "PUSHUP");
            startActivity(intent);
        });

        btnSquat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PoseActivity.class);
            intent.putExtra("MODE", "SQUAT");
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> finish());
    }
}
