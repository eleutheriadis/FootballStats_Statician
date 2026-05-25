package com.epo.footballstats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.epo.footballstats.R;

/**
 * Κεντρική οθόνη: επιλογή ρόλου.
 *   • Υπεύθυνος Στατιστικής → R1 + R2
 *   • Φίλαθλος             → R3 + R4
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStatistician = findViewById(R.id.btnStatistician);
        Button btnFan          = findViewById(R.id.btnFan);

        btnStatistician.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectMatchActivity.class);
            startActivity(intent);
        });

        btnFan.setOnClickListener(v -> {
            Intent intent = new Intent(this, FanSelectMatchActivity.class);
            startActivity(intent);
        });
    }
}
