package com.perfectpose.loginpg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private static final int SQUAT_GOAL = 100;
    ProgressBar progressSquat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnSquat = findViewById(R.id.btnSquat);
        progressSquat = findViewById(R.id.progressSquat);

        if (btnSquat != null) {
            btnSquat.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, SquatActivity.class))
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProgress();
    }

    private void loadProgress() {
        SharedPreferences prefs =
                getSharedPreferences("fitness_data", MODE_PRIVATE);

        int squats = prefs.getInt("squat_count", 0);

        if (progressSquat != null) {
            progressSquat.setMax(SQUAT_GOAL);
            progressSquat.setProgress(Math.min(squats, SQUAT_GOAL));
        }
    }
}
