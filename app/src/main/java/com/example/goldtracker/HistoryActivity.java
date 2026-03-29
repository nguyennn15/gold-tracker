package com.example.goldtracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> list;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listHistory);
        list = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("HISTORY", MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            list.add(entry.getValue().toString());
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
    }
}