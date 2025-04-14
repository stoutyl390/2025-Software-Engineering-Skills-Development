package com.example.myapplication;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.utils.PostgreSqlUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterActivity extends AppCompatActivity {  // 类名修改

    private EditText unametxt, pswtxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);  // 布局文件引用修改

        initializeViews();
        setupRegistrationButton();
    }

    private void initializeViews() {
        unametxt = findViewById(R.id.username);
        pswtxt = findViewById(R.id.password);
    }

    private void setupRegistrationButton() {
        Button btn = findViewById(R.id.btn);
        btn.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String uname = unametxt.getText().toString().trim();
        String psw = pswtxt.getText().toString().trim();

        if (uname.isEmpty() || psw.isEmpty()) {
            showToast("用户名和密码不能为空");
            return;
        }

        if (psw.length() < 6) {
            showToast("密码至少需要6位");
            return;
        }

        new RegistrationTask().execute(uname, psw);
    }

    private class RegistrationTask extends AsyncTask<String, Void, Integer> {
        private static final int SUCCESS = 0;
        private static final int USER_EXISTS = 1;
        private static final int ERROR = 2;

        @Override
        protected Integer doInBackground(String... credentials) {
            String uname = credentials[0];
            String psw = credentials[1];

            try (Connection conn = PostgreSqlUtil.openConnection()) {
                if (isUserExists(conn, uname)) {
                    return USER_EXISTS;
                }

                insertNewUser(conn, uname, psw);
                return SUCCESS;
            } catch (SQLException e) {
                e.printStackTrace();
                return ERROR;
            }
        }

        private boolean isUserExists(Connection conn, String uname) throws SQLException {
            String sql = "SELECT uname FROM userInfo WHERE uname = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uname);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        }

        private void insertNewUser(Connection conn, String uname, String psw) throws SQLException {
            String sql = "INSERT INTO userInfo(uname, psw) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uname);
                stmt.setString(2, psw);
                stmt.executeUpdate();
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            handleRegistrationResult(resultCode);
        }
    }

    private void handleRegistrationResult(int resultCode) {
        switch (resultCode) {
            case RegistrationTask.SUCCESS:
                navigateToLogin();
                break;
            case RegistrationTask.USER_EXISTS:
                showToast("用户名已存在");
                break;
            case RegistrationTask.ERROR:
                showToast("注册失败，请检查网络");
                break;
        }
    }

    private void navigateToLogin() {
        showToast("注册成功");
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}