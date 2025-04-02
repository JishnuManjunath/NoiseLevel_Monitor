package com.example.noise_level_monitor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
//parse str,dynamic list,access ele,location
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoiseHistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private TextView loudCountView;
    private TextView veryLoudCountView;
    private NoiseDatabase database;
    private Button backButton;
    private Handler refreshHandler;
    private static final long REFRESH_INTERVAL = 5000; // 5 seconds
    private boolean isActivityActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noise_history);

        // Initialize views
        historyListView = findViewById(R.id.noise_history_list);
        loudCountView = findViewById(R.id.loud_count);
        veryLoudCountView = findViewById(R.id.very_loud_count);
        backButton = findViewById(R.id.button);
        refreshHandler = new Handler(Looper.getMainLooper());
        database = NoiseDatabase.getInstance(this);

        // Set up back button action
        backButton.setOnClickListener(v -> {
            Toast.makeText(this, "Launching Noise Detection Page", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        startPeriodicRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
        startPeriodicRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
        stopPeriodicRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPeriodicRefresh();
    }

    private void startPeriodicRefresh() {
        stopPeriodicRefresh(); // Ensure no duplicate callbacks
        fetchNoiseHistory();
        refreshHandler.postDelayed(this::periodicRefreshTask, REFRESH_INTERVAL);//referesh after some interval
    }

    private void periodicRefreshTask() {
        if (isActivityActive) {
            fetchNoiseHistory();
            refreshHandler.postDelayed(this::periodicRefreshTask, REFRESH_INTERVAL);
        }
    }

    private void stopPeriodicRefresh() {
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void fetchNoiseHistory() {
        if (database == null) return;

        new Thread(() -> {
            List<DataAddonHistory> allMeasurements = database.getLatest100Measurements();
            if (allMeasurements == null) allMeasurements = new ArrayList<>();

            int loudCount = 0;
            int veryLoudCount = 0;
            List<DataAddonHistory> loudMeasurements = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());

            for (DataAddonHistory measurement : allMeasurements) {
                if (measurement.getNoiseLevel() > 50) {
                    loudMeasurements.add(measurement);
                    if (measurement.getNoiseLevel() > 70) {
                        veryLoudCount++;
                    } else {
                        loudCount++;
                    }
                }
            }

            if (!isActivityActive) return;

            // Update UI on the main thread
            int finalLoudCount = loudCount;
            int finalVeryLoudCount = veryLoudCount;
            runOnUiThread(() -> updateUI(finalLoudCount, finalVeryLoudCount, loudMeasurements));
        }).start();
    }

    private void updateUI(int loudCount, int veryLoudCount, List<DataAddonHistory> loudMeasurements) {
        loudCountView.setText(String.format("Loud Count (50-70 dB): %d", loudCount));
        veryLoudCountView.setText(String.format("Very Loud Count (>70 dB): %d", veryLoudCount));

        if (loudMeasurements.isEmpty()) {
            Toast.makeText(this, "No loud measurements found", Toast.LENGTH_SHORT).show();
        }

        // Use the custom adapter
        NoiseHistoryAdapter adapter = new NoiseHistoryAdapter(this, loudMeasurements);
        historyListView.setAdapter(adapter);
    }
}
