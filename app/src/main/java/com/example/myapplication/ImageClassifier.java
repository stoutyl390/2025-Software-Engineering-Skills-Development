package com.example.myapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ImageClassifier extends AppCompatActivity {

    private static final String API_URL = "http://115.29.227.21:5000/api/classify";
    private static final String TEST_IMAGES_DIR = Environment.getExternalStorageDirectory().getPath() + "/test_images";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ImageClassificationTask(this).execute();
    }

    private static class ImageClassificationTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        private final OkHttpClient client;
        private final ObjectMapper mapper;

        public ImageClassificationTask(Context context) {
            this.context = context;
            this.client = new OkHttpClient();
            this.mapper = new ObjectMapper();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 1. 读取测试目录下的图片文件
                File imageDir = new File(TEST_IMAGES_DIR);
                File[] imageFiles = imageDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".png") || lowerName.endsWith(".jpg")
                            || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp");
                });

                if (imageFiles == null || imageFiles.length == 0) {
                    return "Error: No images found in " + TEST_IMAGES_DIR;
                }

                // 2. 构建多部分请求体
                MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);

                MediaType imageMediaType = MediaType.parse("image/*");

                // 3. 添加所有图片文件
                for (File file : imageFiles) {
                    RequestBody fileBody = RequestBody.create(file, imageMediaType);
                    builder.addFormDataPart("images", file.getName(), fileBody);
                }

                // 4. 构建并执行请求
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(builder.build())
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return "HTTP error: " + response.code();
                    }
                    return response.body() != null ? response.body().string() : "Empty response";
                }

            } catch (IOException e) {
                return "Request failed: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (result.startsWith("Error:") || result.startsWith("HTTP error:") || result.startsWith("Request failed:")) {
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show();
                    return;
                }

                JsonNode jsonResult = mapper.readTree(result);

                StringBuilder sb = new StringBuilder();
                sb.append("Request ID: ").append(jsonResult.get("request_id").asText()).append("\n")
                        .append("Total Images: ").append(jsonResult.get("image_count").asInt()).append("\n")
                        .append("Success: ").append(jsonResult.get("success_count").asInt()).append("\n\n")
                        .append("Details:\n");

                for (JsonNode item : jsonResult.get("results")) {
                    if (item.has("error")) {
                        sb.append("❌ ").append(item.get("error").asText()).append("\n");
                    } else {
                        sb.append(String.format(Locale.getDefault(),
                                "✅ %s: class = %s, confidence = %.4f%n",
                                item.get("filename").asText(),
                                item.get("class").asText(),
                                item.get("confidence").asDouble()));
                    }
                }

                // 在Logcat输出完整结果
                System.out.println("API Response:\n" + sb);

                // 显示简洁的Toast通知
                Toast.makeText(context,
                        "分类完成，成功处理 " + jsonResult.get("success_count").asInt() + "/" +
                                jsonResult.get("image_count").asInt() + " 张图片",
                        Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                Toast.makeText(context, "解析响应失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                System.err.println("JSON parsing failed: " + e.getMessage());
            }
        }
    }
}