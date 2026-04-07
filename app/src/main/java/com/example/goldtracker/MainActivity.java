package com.example.goldtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;

public class MainActivity extends AppCompatActivity {
    private TableLayout tableDashboard;
    private TextView txtResult, txtCurrentPrice;
    private EditText edtQuantity;
    private Button btnConvert;
    private Spinner spinnerGoldType;

    private double currentGoldPriceVND = 0;

    private String[] goldNames = {
            "SJC 9999", "SJC Ring", "DOJI Hanoi", "DOJI HCM", "DOJI Jewelry", "VN Gold SJC", "PNJ Hanoi",
            "PNJ 24K", "Bao Tin SJC", "Bao Tin 9999", "Viettin SJC"
    };

    private String[] goldCodes = {
            "SJL1L10", "SJ9999", "DOHNL", "DOHCML", "DOJINHTV", "VNGSJC", "PQHNVM", "PQHN24NTT", "BTSJC", "BT9999NTT", "VIETTINMSJC"
    };

    private String currentGoldCode = "SJL1L10";
    private Button btnHistory;
    private Button btnChart;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tableDashboard = findViewById(R.id.tableDashboard);
        txtResult = findViewById(R.id.txtResult);
        edtQuantity = findViewById(R.id.edtQuantity);
        btnConvert = findViewById(R.id.btnConvert);
        spinnerGoldType = findViewById(R.id.spinnerGoldType);
        txtCurrentPrice = findViewById(R.id.txtCurrentPrice);
        btnHistory = findViewById(R.id.btnHistory);
        btnChart = findViewById(R.id.btnChart);

        //Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, goldNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoldType.setAdapter(adapter);

        //sự kiện khi chọn 1 loại vàng mới
        spinnerGoldType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Lấy mã code vị trí được chọn
                currentGoldCode = goldCodes[position];

                // Gọi lại API với mã vàng mới
                new GetGoldPriceTask().execute(currentGoldCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Tính năng sáng tạo: Auto refresh giá vàng
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new GetGoldPriceTask().execute(currentGoldCode);
                handler.postDelayed(this, 30000);
            }
        }, 30000);

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = edtQuantity.getText().toString();

                if (input.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Vui lòng nhập số lượng cần quy đổi!", Toast.LENGTH_SHORT).show();
                }
                else if (currentGoldPriceVND <= 0) {
                    Toast.makeText(MainActivity.this, "Đang tải giá vàng, vui lòng đợi giây lát!", Toast.LENGTH_SHORT).show();
                }
                else {
                    // Tính toán
                    calculateTotal();

                    // Lưu lịch sử
                    SharedPreferences prefs = getSharedPreferences("HISTORY", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    String historyItem =
                            goldNames[spinnerGoldType.getSelectedItemPosition()] + " | "
                                    + edtQuantity.getText().toString() + " lượng | "
                                    + txtResult.getText().toString();

                    editor.putString(String.valueOf(System.currentTimeMillis()), historyItem);
                    editor.apply();

                    // Thông báo
                    Toast.makeText(MainActivity.this, "Đã quy đổi thành công!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Tính toán khi nhập số lượng
        edtQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        btnChart.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChartActivity.class));
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetector = new ShakeDetector();
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void calculateTotal() {
        String input = edtQuantity.getText().toString();
        if (!input.isEmpty() && currentGoldPriceVND > 0) {
            try {
                double quantity = Double.parseDouble(input);
                double totalVND = quantity * currentGoldPriceVND;
                txtResult.setText(String.format("%,.0f VND", totalVND));
            } catch (Exception e) {
                txtResult.setText("0 VND");
            }
        } else {
            txtResult.setText("0 VND");
        }
    }

    private class GetGoldPriceTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = "https://www.vang.today/api/prices";
            StringBuilder result = new StringBuilder();

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                return "Lỗi: " + e.getMessage();
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                JSONObject jsonObject = new JSONObject(s);

                if (!jsonObject.has("prices")) {
                    tableDashboard.removeAllViews();
                    TextView errorText = new TextView(MainActivity.this);
                    errorText.setText("Không tìm thấy dữ liệu 'prices'.");
                    tableDashboard.addView(errorText);
                    return;
                }
                JSONObject pricesObj = jsonObject.getJSONObject("prices");

                tableDashboard.removeAllViews();

                //HEADER
                TableRow headerRow = new TableRow(MainActivity.this);
                headerRow.setBackgroundColor(Color.parseColor("#E0E0E0"));
                headerRow.setPadding(0, 10, 0, 10);

                String[] headers = {"LOẠI VÀNG", "GIÁ MUA", "GIÁ BÁN"};
                for (String h : headers) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText(h);
                    tv.setTypeface(null, Typeface.BOLD);
                    tv.setGravity(Gravity.CENTER);
                    headerRow.addView(tv);
                }
                tableDashboard.addView(headerRow);

                //Row du lieu
                Iterator<String> keys = pricesObj.keys();
                while (keys.hasNext()) {
                    String typeCode = keys.next();
                    JSONObject item = pricesObj.getJSONObject(typeCode);

                    if (typeCode.equals("XAUUSD")) continue;

                    String name = item.getString("name");
                    double buy = item.getDouble("buy");
                    double sell = item.getDouble("sell");

                    String strBuy = String.format("%,.0f", buy);
                    String strSell = String.format("%,.0f", sell);

                    // Tạo 1 hàng mới
                    TableRow row = new TableRow(MainActivity.this);
                    row.setPadding(0, 15, 0, 15);

                    // Cột 1: Loại vàng
                    TextView tvName = new TextView(MainActivity.this);
                    tvName.setText(name);
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setGravity(Gravity.CENTER);
                    row.addView(tvName);

                    // Cột 2: Giá mua
                    TextView tvBuy = new TextView(MainActivity.this);
                    tvBuy.setText(strBuy);
                    tvBuy.setTextColor(Color.parseColor("#2E7D32"));
                    tvBuy.setGravity(Gravity.CENTER);
                    row.addView(tvBuy);

                    // Cột 3: Giá bán
                    TextView tvSell = new TextView(MainActivity.this);
                    tvSell.setText(strSell);
                    tvSell.setTextColor(Color.parseColor("#B71C1C")); // Đỏ đậm
                    tvSell.setGravity(Gravity.CENTER);
                    row.addView(tvSell);

                    tableDashboard.addView(row);

                    View line = new View(MainActivity.this);
                    line.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
                    line.setBackgroundColor(Color.parseColor("#CCCCCC"));
                    tableDashboard.addView(line);

                    // Lấy giá để quy đổi tiền nếu khớp Spinner
                    if (typeCode.equals(currentGoldCode)) {
                        currentGoldPriceVND = sell;
                    }
                }
                txtCurrentPrice.setText("Giá vàng: " + String.format("%,.0f", currentGoldPriceVND) + " VND / Lượng");
                // Tính năng sáng tạo: Hiệu ứng khi đổi giá
                txtCurrentPrice.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
                        .withEndAction(() -> txtCurrentPrice.animate().scaleX(1).scaleY(1).setDuration(200));
                // Tính năng sáng tạo: Cảnh báo giá tăng/giảm
                double oldPrice = currentGoldPriceVND;

                if (currentGoldPriceVND > oldPrice) {
                    txtCurrentPrice.setTextColor(Color.GREEN);
                } else if (currentGoldPriceVND < oldPrice) {
                    txtCurrentPrice.setTextColor(Color.RED);
                } else {
                    txtCurrentPrice.setTextColor(Color.BLACK);
                }

                calculateTotal();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    private class ShakeDetector implements SensorEventListener {
        private static final float SHAKE_THRESHOLD = 12.0f; // ngưỡng lắc
        private long lastShakeTime = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float acceleration = (float) Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > 1000) { // chống lắc liên tục
                    lastShakeTime = currentTime;
                    new GetGoldPriceTask().execute(currentGoldCode);
                    vibrateDevice();
                    Toast.makeText(MainActivity.this, "Đang cập nhật giá vàng...", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(200);
    }

}