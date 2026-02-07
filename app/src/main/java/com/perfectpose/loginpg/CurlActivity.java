package com.perfectpose.loginpg;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.tasks.vision.pose.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurlActivity extends AppCompatActivity {

    PreviewView previewView;
    TextView tvCount, tvPosture;
    Button btnReset;

    int curlCount = 0;
    boolean armUp = false;

    PoseLandmarker poseLandmarker;
    ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curl);

        previewView = findViewById(R.id.previewView);
        tvCount = findViewById(R.id.tvCount);
        tvPosture = findViewById(R.id.tvPosture);
        btnReset = findViewById(R.id.btnReset);

        cameraExecutor = Executors.newSingleThreadExecutor();

        setupPose();
        startCamera();

        btnReset.setOnClickListener(v -> {
            curlCount = 0;
            tvCount.setText("Curls: 0");
        });
    }

    private void setupPose() {
        PoseLandmarkerOptions options =
                PoseLandmarkerOptions.builder()
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setResultListener((result, input) -> runOnUiThread(() -> processPose(result)))
                        .build();

        poseLandmarker = PoseLandmarker.createFromOptions(this, options);
    }

    private void processPose(PoseLandmarkerResult result) {
        if (result.landmarks().isEmpty()) return;

        // Right arm landmarks
        LandmarkProto.NormalizedLandmark shoulder =
                result.landmarks().get(0).getLandmark(12);
        LandmarkProto.NormalizedLandmark elbow =
                result.landmarks().get(0).getLandmark(14);
        LandmarkProto.NormalizedLandmark wrist =
                result.landmarks().get(0).getLandmark(16);

        double angle = calculateAngle(shoulder, elbow, wrist);

        // Curl logic
        if (angle < 50) {
            if (!armUp) {
                armUp = true;
            }
            tvPosture.setVisibility(TextView.GONE);
        }
        else if (angle > 160) {
            if (armUp) {
                curlCount++;
                tvCount.setText("Curls: " + curlCount);
                armUp = false;
            }
            tvPosture.setVisibility(TextView.GONE);
        }
        else {
            // Incorrect posture zone
            tvPosture.setVisibility(TextView.VISIBLE);
        }
    }

    private double calculateAngle(
            LandmarkProto.NormalizedLandmark a,
            LandmarkProto.NormalizedLandmark b,
            LandmarkProto.NormalizedLandmark c) {

        double abX = a.getX() - b.getX();
        double abY = a.getY() - b.getY();
        double cbX = c.getX() - b.getX();
        double cbY = c.getY() - b.getY();

        double dot = (abX * cbX + abY * cbY);
        double magAB = Math.sqrt(abX * abX + abY * abY);
        double magCB = Math.sqrt(cbX * cbX + cbY * cbY);

        return Math.toDegrees(Math.acos(dot / (magAB * magCB)));
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    // MediaPipe frame input handled internally
                    imageProxy.close();
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, analysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
