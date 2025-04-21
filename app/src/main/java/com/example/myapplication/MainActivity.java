package com.example.myapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // 请求相机权限的请求码
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    // 请求存储权限的请求码
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    // 要访问的 API 地址
    private static final String API_URL = "http://115.29.227.21:5000/api/classify";

    // 用于显示图片预览的 ImageView
    private ImageView imgPreview;
    // 当前拍摄图片的路径
    private String currentPhotoPath;
    // OkHttpClient 实例，用于发送网络请求
    private OkHttpClient httpClient;

    // 用于处理拍照结果的 ActivityResultLauncher
    private ActivityResultLauncher<Intent> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取布局文件中的拍照按钮
        Button btnCapture = findViewById(R.id.btn_capture);
        imgPreview = findViewById(R.id.img_preview);
        // 初始化 OkHttpClient
        httpClient = new OkHttpClient();

        // 初始化 takePictureLauncher
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                // 使用 Lambda 表达式处理拍照结果
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 将拍摄的图片添加到系统相册
                        galleryAddPic();
                        // 显示图片预览
                        setPic();
                        // 显示是否上传图片的对话框
                        showUploadDialog();
                    }
                }
        );

        btnCapture.setOnClickListener(v -> {
            // 检查相机权限和存储权限是否都已授予
            if (checkCameraPermission() && checkStoragePermission()) {
                // 权限已授予，启动拍照意图
                dispatchTakePictureIntent();
            } else {
                // 权限未授予，请求权限
                requestPermissions();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpClient != null) {
            // 在活动销毁时关闭 OkHttpClient 的执行器服务
            httpClient.dispatcher().executorService().shutdown();
        }
    }

    // 检查相机权限是否已授予
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 检查存储权限是否已授予
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    // 请求相机和存储权限
    private void requestPermissions() {
        boolean needCamera = !checkCameraPermission();
        boolean needStorage = !checkStoragePermission();

        if (needCamera && needStorage) {
            // 同时请求相机和存储权限
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION
            );
        } else if (needCamera) {
            // 仅请求相机权限
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else if (needStorage) {
            // 仅请求存储权限
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == REQUEST_CAMERA_PERMISSION || requestCode == REQUEST_STORAGE_PERMISSION)
                && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 权限请求成功，启动拍照意图
            dispatchTakePictureIntent();
        } else {
            // 权限请求失败，显示提示信息
            Toast.makeText(this, "需要权限才能拍照和保存图片", Toast.LENGTH_SHORT).show();
        }
    }

    // 抑制 lint 检查（此处查询权限不需要额外声明，可能被误报）
    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 创建存储图片的文件
            File photoFile = createImageFile();
                // 通过 FileProvider 获取文件的 Uri
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.myapplication.fileprovider",
                        photoFile);
                // 将图片的 Uri 设置为拍照意图的输出
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                // 使用 takePictureLauncher 启动拍照活动
                takePictureLauncher.launch(takePictureIntent);
    }

    private File createImageFile() {
        // 生成时间戳作为文件名的一部分
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // 获取外部文件存储目录下的 Pictures 目录
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            // 创建临时文件
            File image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
            // 保存文件的绝对路径
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 因为已经使用了 registerForActivityResult，这里的 onActivityResult 可以为空或者删除
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        // 发送广播通知系统相册更新
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // 从文件路径解码出 Bitmap 并显示在 ImageView 上
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        imgPreview.setImageBitmap(bitmap);
    }

    private void showUploadDialog() {
        new AlertDialog.Builder(this)
                .setTitle("上传图片")
                .setMessage("是否要上传这张图片到服务器?")
                .setPositiveButton("上传", (dialog, which) -> uploadImage())
                .setNegativeButton("取消", null)
                .show();
    }

    private void uploadImage() {
        if (currentPhotoPath == null) {
            // 当前没有可上传的图片，显示提示信息
            Toast.makeText(this, "没有可上传的图片", Toast.LENGTH_SHORT).show();
            return;
        }
        // 在新线程中执行上传操作，避免阻塞主线程
        new Thread(() -> {
            try {
                File imageFile = new File(currentPhotoPath);

                // 使用新的方式创建 RequestBody
                MediaType mediaType = MediaType.parse("image/jpeg");
                RequestBody fileBody = RequestBody.create(imageFile, mediaType);
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("images", imageFile.getName(), fileBody)
                        .build();

                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(requestBody)
                        .build();
                Response response = httpClient.newCall(request).execute();
                System.out.println("444444");
                // 确保响应体不为空，避免空指针异常
                final String responseBody = Objects.requireNonNull(response.body()).string();

                // 在主线程中更新 UI，显示上传结果
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "上传成功: " + responseBody, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "上传失败: " + responseBody, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (IOException e) {
                // 上传过程中发生错误，在主线程中显示错误提示
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "上传错误: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
