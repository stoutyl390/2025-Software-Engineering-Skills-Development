package com.example.myapplication;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.utils.PostgreSqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {
    //分别定义用户名和密码
    private EditText unametxt, pswtxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login); // 更新后的布局文件

        // 初始化UI组件
        unametxt = findViewById(R.id.username);
        pswtxt = findViewById(R.id.password);
        Button btn = findViewById(R.id.btn);
        TextView zhuce = findViewById(R.id.zhuce);

        // 登录按钮点击事件
        btn.setOnClickListener(v -> attemptLogin());

        // 注册文本点击事件(点集注册按钮后会进入到注册页面）
        zhuce.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String username = unametxt.getText().toString().trim();
        String password = pswtxt.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        new DatabaseLoginTask().execute(username, password);
    }

    private class DatabaseLoginTask extends AsyncTask<String, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected Boolean doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            try (Connection conn = PostgreSqlUtil.openConnection()) {
                String sql = "SELECT id FROM users WHERE username = ? AND password = crypt(?, password)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);

                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                errorMessage = e.getMessage();
                Log.e("DB_ERROR", "Database error: " + errorMessage);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                String message = !errorMessage.isEmpty() ?
                        "连接数据库失败: " + errorMessage : "用户名或密码错误";
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }
    }
}