package com.example.noise_level_monitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coverpage);

        Button startAppButton = findViewById(R.id.start_app_button);
        startAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show a Toast message before launching MainActivity
                Toast.makeText(main.this,
                        "Launching Sound Estimator page",
                        Toast.LENGTH_SHORT).show();

                // Intent to launch MainActivity
                Intent intent = new Intent(main.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the cover activity
            }
        });
    }
}