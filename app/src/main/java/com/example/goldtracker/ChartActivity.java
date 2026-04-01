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

        // Đọc lịch sử và thống kê số lần quy đổi theo loại vàng
        SharedPreferences prefs = getSharedPreferences("HISTORY", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        // Map để đếm số lần theo loại vàng
        HashMap<String, Integer> countMap = new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String historyItem = entry.getValue().toString();  // ví dụ: "SJC 9999 | 5 lượng | 123,456,789 VND"

            // Lấy tên loại vàng (phần trước dấu "|")
            if (historyItem.contains("|")) {
                String goldType = historyItem.split("\\|")[0].trim();
                countMap.put(goldType, countMap.getOrDefault(goldType, 0) + 1);
            }
        }

        // Chuẩn bị dữ liệu cho biểu đồ
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());  // tên loại vàng
            index++;
        }

        // Nếu chưa có dữ liệu → dùng demo
        if (entries.isEmpty()) {
            entries.add(new BarEntry(0, 2)); // SJC
            entries.add(new BarEntry(1, 4)); // DOJI
            entries.add(new BarEntry(2, 1)); // PNJ
            labels.add("SJC 9999");
            labels.add("DOJI");
            labels.add("PNJ");
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lần quy đổi theo loại vàng");
        dataSet.setColor(Color.parseColor("#4CAF50"));  // màu xanh đẹp
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);  // độ rộng cột

        barChart.setData(barData);

        // Cấu hình trục X (tên loại vàng)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(30);  // xoay nhãn nếu dài

        // Cấu hình trục Y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(1f);  // bước nhảy là số nguyên
        barChart.getAxisRight().setEnabled(false);

        // Tùy chỉnh khác
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1500);  // animation đẹp
        barChart.invalidate();    // refresh chart
    }
}