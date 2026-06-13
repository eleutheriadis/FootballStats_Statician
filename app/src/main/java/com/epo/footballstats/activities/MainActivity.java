package com.epo.footballstats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.epo.footballstats.R;

/**
 * Κεντρική οθόνη — Εφαρμογή Υπευθύνου Στατιστικής.
 *
 * Διαθέτει μόνο έναν ρόλο (στατιστικός) → ροή R1 + R2.
 * (Ο ρόλος "Φίλαθλος" βρίσκεται σε ξεχωριστή εφαρμογή.)
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStatistician = findViewById(R.id.btnStatistician);
        btnStatistician.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectMatchActivity.class);
            startActivity(intent);
        });
    }
}
