package com.example.musicapplication.main;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.musicapplication.R;

public class PrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView tvTitle = toolbar.findViewById(R.id.tv_title);
        tvTitle.setText("Privacy Policy");
        ImageButton btnBack = toolbar.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        // Bạn có thể load nội dung động từ server ở đây nếu cần
        // TextView tvContent = findViewById(R.id.tv_privacy_content);
        // tvContent.setText(...);
    }
}