package com.leonardo.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.leonardo.myapplication.ui.feed.FeedActivity;
import com.leonardo.myapplication.ui.livecheck.LiveCheckActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnTask1;
    private Button btnTask2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnTask1 = findViewById(R.id.btn_task1);
        btnTask2 = findViewById(R.id.btn_task2);

        btnTask1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FeedActivity.class));
            }
        });

        btnTask2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LiveCheckActivity.class));
            }
        });
    }
}