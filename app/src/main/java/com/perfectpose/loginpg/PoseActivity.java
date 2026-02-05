package com.perfectpose.loginpg;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoseActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;

    PreviewView previewView;
    TextView txtReps;

    PoseLandmarker poseLandmarker;
    ExecutorService cameraExecutor;

    boolean isSquatDown = false;
    int sessionSquats = 0;
    long frameTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose);

        previewView = findViewById(R.id.previewView);
        txtReps = findViewById(R.id.txtReps);

        cameraExecutor = Executors.newSingleThreadExecutor();
        setupPoseLandmarker();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            startCamera();
        }
    }

    private void setupPoseLandmarker() {
        PoseLandmarker.PoseLandmarkerOptions options =
                PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(
                                BaseOptions.builder()
                                        .setModelAssetPath("pose_landmarker_lite.task")
                                        .build()
                        )
                        .setRunningMode(RunningMode.LIVE_STREAM)
                        .setResultListener(this::onPoseResult)
                        .build();

        poseLandmarker = PoseLandmarker.createFromOptions(this, options);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(480, 640))
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

            } catch (Exception e) {
                Log.e("CAMERA", "Camera error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void analyzeFrame(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null ||
                imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = yuvToBitmap(imageProxy);
        imageProxy.close();

        MPImage mpImage =
                new BitmapImageBuilder(bitmap).build();

        poseLandmarker.detectAsync(mpImage, frameTime++);
    }

    private Bitmap yuvToBitmap(ImageProxy imageProxy) {
        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.getWidth(),
                imageProxy.getHeight(),
                null
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0,
                        imageProxy.getWidth(),
                        imageProxy.getHeight()),
                90,
                out
        );

        byte[] imageBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(
                imageBytes, 0, imageBytes.length);
    }

    private void onPoseResult(PoseLandmarkerResult result, MPImage image) {
        if (result.landmarks().isEmpty()) return;

        List<NormalizedLandmark> lm = result.landmarks().get(0);

        NormalizedLandmark hip = lm.get(23);
        NormalizedLandmark knee = lm.get(25);
        NormalizedLandmark ankle = lm.get(27);

        double kneeAngle = calculateAngle(hip, knee, ankle);

        if (kneeAngle < 100 && !isSquatDown) {
            isSquatDown = true;
        }

        if (kneeAngle > 160 && isSquatDown) {
            isSquatDown = false;
            sessionSquats++;
            saveSquat();

            runOnUiThread(() ->
                    txtReps.setText("Squats: " + sessionSquats)
            );
        }
    }

    private void saveSquat() {
        SharedPreferences prefs =
                getSharedPreferences("fitness_data", MODE_PRIVATE);

        int total = prefs.getInt("squat_count", 0);
        prefs.edit().putInt("squat_count", total + 1).apply();
    }

    private double calculateAngle(
            NormalizedLandmark a,
            NormalizedLandmark b,
            NormalizedLandmark c) {

        double abX = a.x() - b.x();
        double abY = a.y() - b.y();
        double cbX = c.x() - b.x();
        double cbY = c.y() - b.y();

        double dot = abX * cbX + abY * cbY;
        double magAB = Math.sqrt(abX * abX + abY * abY);
        double magCB = Math.sqrt(cbX * cbX + cbY * cbY);

        return Math.toDegrees(Math.acos(dot / (magAB * magCB)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poseLandmarker != null) poseLandmarker.close();
        cameraExecutor.shutdown();
    }
}
