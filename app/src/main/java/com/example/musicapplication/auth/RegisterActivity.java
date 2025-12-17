package com.example.musicapplication.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicapplication.R;
import com.example.musicapplication.data.repository.AuthRepository;
import com.example.musicapplication.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {
    
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etDisplayName;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etDisplayName = findViewById(R.id.et_display_name);
        btnRegister = findViewById(R.id.btn_register_action);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);

        btnRegister.setOnClickListener(v -> handleRegister());
        
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String displayName = etDisplayName.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (displayName.isEmpty()) {
            etDisplayName.setError("Vui lòng nhập tên hiển thị");
            etDisplayName.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        authRepository.register(email, password, displayName, new AuthRepository.OnAuthResultListener() {
            @Override
            public void onSuccess(AuthRepository.FirebaseUserWrapper user) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(Exception error) {
                setLoading(false);
                String errorMessage = "Đăng ký thất bại";
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("email")) {
                        errorMessage = "Email đã được sử dụng";
                    } else if (error.getMessage().contains("weak")) {
                        errorMessage = "Mật khẩu quá yếu";
                    } else {
                        errorMessage = error.getMessage();
                    }
                }
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
        etConfirmPassword.setEnabled(!loading);
        etDisplayName.setEnabled(!loading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

