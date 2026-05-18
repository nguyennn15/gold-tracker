package com.example.goldtracker;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChartActivity extends AppCompatActivity {

    BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        barChart = findViewById(R.id.barChart);

        SharedPreferences prefs = getSharedPreferences("HISTORY", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        HashMap<String, Integer> countMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String historyItem = entry.getValue().toString();  // ví dụ: "SJC 9999 | 5 lượng | 123,456,789 VND"

            if (historyItem.contains("|")) {
                String goldType = historyItem.split("\\|")[0].trim();
                countMap.put(goldType, countMap.getOrDefault(goldType, 0) + 1);
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());  // tên loại vàng
            index++;
        }

        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 2)); // SJC
            entries.add(new BarEntry(1, 4)); // DOJI
            entries.add(new BarEntry(2, 1)); // PNJ
            labels.add("SJC 9999");
            labels.add("DOJI");
            labels.add("PNJ");
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lần quy đổi theo loại vàng");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(12f);

        int textColor;
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            textColor = Color.WHITE;
        } else {
            textColor = Color.BLACK;
        }

        dataSet.setValueTextColor(textColor);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(30);
        xAxis.setTextColor(textColor);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(textColor);
        barChart.getAxisRight().setEnabled(false);

        barChart.getLegend().setTextColor(textColor);

        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1500);
        barChart.invalidate();
    }
}