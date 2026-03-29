package com.example.goldtracker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;

import java.util.ArrayList;

public class ChartActivity extends AppCompatActivity {

    BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        barChart = findViewById(R.id.barChart);

        ArrayList<BarEntry> entries = new ArrayList<>();

        // Demo dữ liệu (có thể lấy từ HISTORY)
        entries.add(new BarEntry(1, 2)); // SJC
        entries.add(new BarEntry(2, 4)); // DOJI
        entries.add(new BarEntry(3, 1)); // PNJ

        BarDataSet dataSet = new BarDataSet(entries, "Số lần quy đổi");
        BarData data = new BarData(dataSet);

        barChart.setData(data);
        barChart.invalidate();
    }
}