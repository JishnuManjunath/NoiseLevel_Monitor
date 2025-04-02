package com.example.noise_level_monitor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.Path;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int UPDATE_INTERVAL_MS = 250;
    private static final float SMOOTHING_FACTOR = 0.3f;

    private SurfaceView audioVisualizer;
    private AudioRecord audioRecord;
    private TextView noiseLevelText;
    private TextView studySuitabilityText;
    private boolean isRecording = false;
    private Handler handler = new Handler();
    private double lastDbValue = 0;
    private long lastUpdateTime = 0;
    private Button backButton, viewHistoryButton;
    private NoiseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize database
        database = NoiseDatabase.getInstance(this);

        // Initialize UI components
        audioVisualizer = findViewById(R.id.audio_visualizer);
        noiseLevelText = findViewById(R.id.noise_level_text);
        studySuitabilityText = findViewById(R.id.study_suitability_text);
        backButton = findViewById(R.id.back);
        viewHistoryButton = findViewById(R.id.view_history_button);

        // Setup button listeners
        findViewById(R.id.start_recording_button).setOnClickListener(v -> startRecordingWithPermissionCheck());
        findViewById(R.id.stop_recording_button).setOnClickListener(v -> stopRecording());

        // Back button setup
        backButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Login Page", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, main.class);
            startActivity(intent);
            finish();
        });

        // View history button setup
        viewHistoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoiseHistoryActivity.class);
            startActivity(intent);
        });

        // Start recording as soon as the app is opened if permissions are granted
        startRecordingWithPermissionCheck();
    }

    private void startRecordingWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            initAudioRecorderAndStart();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void initAudioRecorderAndStart() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            startRecording();
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission to record audio was denied", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void startRecording() {
        isRecording = true;
        audioRecord.startRecording();
        findViewById(R.id.start_recording_button).setVisibility(View.GONE);
        findViewById(R.id.stop_recording_button).setVisibility(View.VISIBLE);
        lastUpdateTime = 0;
        lastDbValue = 0;

        final short[] buffer = new short[1024]; // Make buffer final
        new Thread(() -> {
            while (isRecording) {
                audioRecord.read(buffer, 0, buffer.length);
                updateVisualizer(buffer);
                updateNoiseLevel(buffer);
            }
        }).start();
    }

    private void updateVisualizer(short[] buffer) {
        handler.post(() -> {
            Canvas canvas = audioVisualizer.getHolder().lockCanvas();
            if (canvas != null) {
                try {
                    int width = audioVisualizer.getWidth();
                    int height = audioVisualizer.getHeight();
                    float centerY = height / 2f;//basesline 
                    float scaleFactor = height / 65536f;//16 bit to pixels

                    canvas.drawColor(Color.BLACK);

                    Path wavePath = new Path();
                    wavePath.moveTo(0, centerY);

                    float[] smoothedPoints = new float[buffer.length];
                    for (int i = 0; i < buffer.length; i++) {
                        float currentY = centerY + (buffer[i] * scaleFactor);
                        if (i == 0) {
                            smoothedPoints[i] = currentY;
                        } else {
                            smoothedPoints[i] = smoothedPoints[i - 1] +
                                    (currentY - smoothedPoints[i - 1]) * 0.2f;
                        }
                    }

                    for (int i = 0; i < buffer.length - 1; i++) {
                        float x = (i * width) / (float) buffer.length;
                        if (i == 0) {
                            wavePath.moveTo(x, smoothedPoints[i]);
                        } else {
                            float nextX = ((i + 1) * width) / (float) buffer.length;
                            float midX = (x + nextX) / 2;
                            float midY = (smoothedPoints[i] + smoothedPoints[i + 1]) / 2;
                            wavePath.quadTo(x, smoothedPoints[i], midX, midY);
                        }
                    }

                    Paint wavePaint = new Paint();
                    wavePaint.setStyle(Paint.Style.STROKE);
                    wavePaint.setStrokeWidth(2f);
                    wavePaint.setAntiAlias(true);
                    wavePaint.setColor(Color.parseColor("#2196F3"));
                    wavePaint.setAlpha(200);

                    canvas.drawPath(wavePath, wavePaint);
                } finally {
                    audioVisualizer.getHolder().unlockCanvasAndPost(canvas);
                }
            }
        });
    }

    private void updateNoiseLevel(short[] buffer) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return;
        }//min interval 

        double sum = 0;
        for (short sample : buffer) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / buffer.length);

        double calculatedDb = 20 * Math.log10(Math.max(rms, 1));
        calculatedDb = Math.max(0, Math.min(calculatedDb, 120));

        if (lastDbValue == 0) {
            lastDbValue = calculatedDb;
        } else {
            calculatedDb = (calculatedDb * SMOOTHING_FACTOR) + (lastDbValue * (1 - SMOOTHING_FACTOR));
        }
        lastDbValue = calculatedDb;

        lastUpdateTime = currentTime;

        final double finalDb = calculatedDb;
        final String dbText = String.format("Sound Level: %.1f dB", finalDb);

        // Determine noise level category
        final String category;
        final int categoryColor;
        if (finalDb < 30) {
            category = "Very Quiet";
            categoryColor = Color.BLACK;
        } else if (finalDb < 45) {
            category = "Quiet";
            categoryColor = Color.BLACK; // Darker green
        } else if (finalDb < 55) {
            category = "Moderate";
            categoryColor = Color.BLACK;
        } else if (finalDb < 65) {
            category = "Loud";
            categoryColor = Color.BLACK; // Orange
        } else {
            category = "Extremely Loud";
            categoryColor = Color.BLACK; // Dark red
        }

        final String categoryText = String.format("%s\nCategory: %s", dbText, category);

        // Determine study suitability based on categories
        final String studySuitabilityMessage;
        if (finalDb == 50) {
            studySuitabilityMessage = "";
        } else if (finalDb > 65) {
            studySuitabilityMessage = "Not Suitable";
        } else {
            studySuitabilityMessage = "";
        }

        handler.post(() -> {
            noiseLevelText.setText(categoryText);
            noiseLevelText.setTextColor(categoryColor);
            studySuitabilityText.setText(studySuitabilityMessage);
        });

        if (database != null) {
            new Thread(() -> {
                DataAddonHistory measurement = new DataAddonHistory(
                        0, // ID will be auto-generated by SQLite
                        finalDb,
                        System.currentTimeMillis(),
                        category + " - " + studySuitabilityMessage
                );

                database.insert(measurement);

                int currentCount = database.getTotalMeasurementsCount();
                if (currentCount > 100) {
                    database.trimOldMeasurements();
                }
            }).start();
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                audioRecord = null;
            }
        }
        findViewById(R.id.start_recording_button).setVisibility(View.VISIBLE);
        findViewById(R.id.stop_recording_button).setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAudioRecorderAndStart();
            } else {
                Toast.makeText(this, "Permission to record audio is required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
