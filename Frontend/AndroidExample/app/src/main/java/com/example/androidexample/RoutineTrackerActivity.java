package com.example.androidexample;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;

public class RoutineTrackerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_tracker);

        // Force light mode for colors to show correctly
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EditText routineTitle = findViewById(R.id.routineTitle);
        EditText routineDescription = findViewById(R.id.routineDescription);
        EditText routineStartDate = findViewById(R.id.routineStartDate);
        Spinner spinnerLabel = findViewById(R.id.spinnerLabel);
        Button btnSave = findViewById(R.id.btnSave);

        // Spinner
        String[] labels = {"Work", "Personal", "School", "Finance", "Social"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        spinnerLabel.setAdapter(adapter);

        btnSave.setOnClickListener(v -> {
            String title = routineTitle.getText().toString();
            String description = routineDescription.getText().toString();
            String startDate = routineStartDate.getText().toString();
            String label = spinnerLabel.getSelectedItem().toString();
        });

        // Date picker
        routineStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> routineStartDate.setText(
                            String.format("%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }
}
