
package com.flipcam;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flipcam.cameramanager.CameraXManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private PreviewView previewView;
    private CameraXManager cameraXManager;
    private boolean isRecording = false;

    private ImageButton recordButton;
    private ImageButton switchCameraButton;
    private ImageButton switchModeButton;
    private TextView recordingTimer;

    private Handler timerHandler;
    private long startTime = 0L;

    private boolean isVideoMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        recordButton = findViewById(R.id.recordButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);
        switchModeButton = findViewById(R.id.switchModeButton);
        recordingTimer = findViewById(R.id.recordingTimer);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        recordButton.setOnClickListener(v -> {
            if (isVideoMode) {
                toggleRecording();
            } else {
                cameraXManager.takePicture();
            }
        });

        switchCameraButton.setOnClickListener(v -> cameraXManager.switchCamera());

        switchModeButton.setOnClickListener(v -> {
            isVideoMode = !isVideoMode;
            updateUIMode();
        });

        timerHandler = new Handler(Looper.getMainLooper());
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        if (isRecording) {
            cameraXManager.startRecording();
            startTimer();
            recordButton.setImageResource(android.R.drawable.ic_media_stop);
        } else {
            cameraXManager.stopRecording();
            stopTimer();
            recordButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void updateUIMode() {
        if (isVideoMode) {
            switchModeButton.setImageResource(R.drawable.ic_photo_camera);
            recordButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            switchModeButton.setImageResource(R.drawable.ic_videocam);
            recordButton.setImageResource(R.drawable.ic_photo_camera);
        }
    }

    private void startCamera() {
        cameraXManager = new CameraXManager(this, this, previewView.getSurfaceProvider());
        cameraXManager.startCamera();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        recordingTimer.setVisibility(View.VISIBLE);
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        recordingTimer.setVisibility(View.GONE);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            recordingTimer.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraXManager != null) {
            cameraXManager.release();
        }
    }
}
