package com.perfectpose.loginpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ExerciseActivity extends AppCompatActivity {

    Button btnSquat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        btnSquat = findViewById(R.id.btnSquat);

        btnSquat.setOnClickListener(v -> {
            Intent intent = new Intent(this, PoseActivity.class);
            intent.putExtra("exercise_type", "SQUAT");
            startActivity(intent);
        });
    }
}
