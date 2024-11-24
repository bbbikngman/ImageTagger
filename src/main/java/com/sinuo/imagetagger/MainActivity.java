package com.sinuo.imagetagger;


import static com.sinuo.imagetagger.GPTService.sendImageToGPT;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.sinuo.imagetagger.utils.ContextUtils;
import com.sinuo.imagetagger.utils.DrawingUtils;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ImageView imageDisplay;
    private Button sendButton;
    private ImageView cameraIcon;
    private ImageView galleryIcon;
    private Uri photoURI;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    // 创建一个单线程的ExecutorService
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 及以上使用新权限
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    },REQUEST_CODE_PERMISSIONS
            );
        } else {
            // Android 13 以下使用旧权限
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_PERMISSIONS
            );
        }



    }




    // 在活动的 onCreate 方法中调用:
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //设置context内容为这里
        ContextUtils.initialize(this);
        // 初始化权限检查
        checkAndRequestPermissions();

        // 初始化视图
        initViews();

        // 设置监听器
        setListeners();
    }



    // 处理权限请求结果:
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限被拒绝：" + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    
    // 用于处理相机返回的结果
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    displayImage(photoURI);
                    sendButton.setEnabled(true);
                } else {
                    Toast.makeText(this, "相机拍照失败", Toast.LENGTH_SHORT).show();
                }
            });

    // 用于处理相册返回的结果
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    displayImage(selectedImage);
                    sendButton.setEnabled(true);
                } else {
                    Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show();
                }
            });


    // 跳转到设置界面
    public void openSettingsActivity(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void initViews() {
        imageDisplay = findViewById(R.id.image_display);
        sendButton = findViewById(R.id.button_send);
        cameraIcon = findViewById(R.id.icon_camera);
        galleryIcon = findViewById(R.id.icon_gallery);

        // 默认情况下，发送按钮不可用
        sendButton.setEnabled(false);
    }

    private void setListeners() {
        cameraIcon.setOnClickListener(v -> openCamera());
        galleryIcon.setOnClickListener(v -> openGallery());
        sendButton.setOnClickListener(v -> {
            // 禁用发送按钮，防止重复点击
            sendButton.setEnabled(false);

            // 获取当前显示的图片作为Bitmap
            imageDisplay.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(imageDisplay.getDrawingCache());
            imageDisplay.setDrawingCacheEnabled(false);

            // 创建显示设置的Runnable
            Runnable showSettings = () -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            };

            // 使用ExecutorService执行网络请求
            executorService.execute(() -> {
                GPTService.sendImageToGPT(bitmap, showSettings, new GPTService.ResponseCallback() {
                    @Override
                    public void onSuccess(String content, List<TaggedObject> taggedObjects) {
                        Bitmap taggedBitmap  = DrawingUtils.drawTagsOnBitmap( bitmap, taggedObjects);
                        runOnUiThread(() -> {
                            imageDisplay.setImageBitmap(taggedBitmap);
                            sendButton.setEnabled(true);
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> sendButton.setEnabled(true));
                    }
                });
            });
        });
    }

    private void openCamera() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("无法创建图片存储目录");
        }

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir     /* directory */
        );
    }

    private void displayImage(Uri imageUri) {
        if (imageUri != null) {
            try {
                imageDisplay.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUri)
                        .error(R.drawable.error) // 添加错误占位图
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                Toast.makeText(MainActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }


                        })
                        .into(imageDisplay);
            } catch (Exception e) {
                Toast.makeText(this, "图片显示失败", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭ExecutorService
        executorService.shutdown();
        try {
            // 等待未完成的任务完成
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
