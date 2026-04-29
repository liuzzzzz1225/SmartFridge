package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.view.View;
import android.widget.ArrayAdapter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import androidx.core.content.FileProvider;

public class ImageProcessActivity extends AppCompatActivity {
    private static final String TAG = "ImageProcessActivity";
    private ImageView processedImageView;
    private EditText nameEditText;
    private EditText expiryDateEditText;
    private EditText quantityEditText;
    private Button saveButton;
    private FoodDatabase foodDatabase;
    private Uri photoUri;
    private Handler mainHandler;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String jsonResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_image_process);
            
            // 设置标题栏
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false); // 不显示返回箭头
                getSupportActionBar().setTitle("My Application"); // 设置标题
            }

            mainHandler = new Handler(Looper.getMainLooper());
            initViews();
            setupDatabase();
            processImage();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            showToast("初始化失败: " + e.getMessage());
            finish();
        }
    }

    private void initViews() {
        try {
            processedImageView = findViewById(R.id.processedImageView);
            nameEditText = findViewById(R.id.nameEditText);
            expiryDateEditText = findViewById(R.id.expiryDateEditText);
            quantityEditText = findViewById(R.id.quantityEditText);
            saveButton = findViewById(R.id.saveButton);
            Button saveAndAddButton = findViewById(R.id.saveAndAddButton);

            String uriString = getIntent().getStringExtra("photoUri");
            if (uriString == null) {
                throw new IllegalArgumentException("No photo URI provided");
            }
            photoUri = Uri.parse(uriString);
            Log.d(TAG, "Photo URI: " + photoUri);

            // 设置默认值和点击监听
            expiryDateEditText.setHint("保质期（天，默认7天）");
            quantityEditText.setHint("数量（默认3）");
            
            // 设置焦点变化监听
            expiryDateEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    expiryDateEditText.setText("");  // 获得焦点时清空文本
                } else if (expiryDateEditText.getText().toString().isEmpty()) {
                    expiryDateEditText.setHint("保质期（天，默认7天）");  // 失去焦点且为空时恢复提示
                }
            });

            quantityEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    quantityEditText.setText("");  // 获得焦点时清空文本
                } else if (quantityEditText.getText().toString().isEmpty()) {
                    quantityEditText.setHint("数量（默认3）");  // 失去焦点且为空时恢复提示
                }
            });

            saveButton.setOnClickListener(v -> saveFood());
            saveAndAddButton.setOnClickListener(v -> saveAndAddAnother());

            ImageButton backButton = findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: ", e);
            showToast("初始化视图失败");
            finish();
        }
    }

    private void setupDatabase() {
        try {
            foodDatabase = new FoodDatabase(this);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up database: ", e);
            showToast("据库初始化失败");
            finish();
        }
    }

    private void processImage() {
        try {
            Log.d(TAG, "Starting image processing");
            // 获取拍摄的图片
            Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
            if (photo == null) {
                throw new IllegalStateException("Failed to load image");
            }
            processedImageView.setImageBitmap(photo);

            // 首先获取access token
            BaiduAiHelper.getAccessToken(new BaiduAiHelper.ApiCallback() {
                @Override
                public void onSuccess(String token) {
                    Log.d(TAG, "Got access token successfully");
                    // 获取token成功后进行识别
                    BaiduAiHelper.detectVegetable(photo, new BaiduAiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            Log.d(TAG, "Detection result: " + result);
                            handleDetectionResult(result);
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Detection failed: " + error);
                            showToast("识别失败: " + error);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to get token: " + error);
                    showToast("获取token失败: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: ", e);
            showToast("图片处理失败: " + e.getMessage());
        }
    }

    private void handleDetectionResult(String jsonResult) {
        try {
            this.jsonResult = jsonResult;
            
            Log.d(TAG, "Parsing detection result");
            JsonObject result = new Gson().fromJson(jsonResult, JsonObject.class);
            JsonArray resultArray = result.getAsJsonArray("result");
            if (resultArray != null && resultArray.size() > 0) {
                // 过滤掉非果蔬食材
                List<String> validItems = new ArrayList<>();
                for (int i = 0; i < resultArray.size(); i++) {
                    JsonObject item = resultArray.get(i).getAsJsonObject();
                    String name = item.get("name").getAsString();
                    // 如果不包含"非果蔬"，则添加到列表
                    if (!name.contains("非果蔬")) {
                        validItems.add(name);
                    }
                }
                
                if (!validItems.isEmpty()) {
                    mainHandler.post(() -> showDetectionResultDialog(validItems));
                } else {
                    showToast("未能识别出有效的食材");
                }
            } else {
                Log.w(TAG, "No detection results found");
                showToast("未能识别出食材");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing result: ", e);
            showToast("解析结果失败: " + e.getMessage());
        }
    }

    private void showDetectionResultDialog(List<String> validItems) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_detection_result, null);
        ListView listView = dialogView.findViewById(R.id.listView);
        Button manualButton = dialogView.findViewById(R.id.manualButton);
        Button retakeButton = dialogView.findViewById(R.id.retakeButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, validItems);
        listView.setAdapter(adapter);

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("选择食材")
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = validItems.get(position);
            nameEditText.setText(selectedName);
            dialog.dismiss();
        });

        manualButton.setOnClickListener(v -> {
            nameEditText.setText("");
            nameEditText.requestFocus();
            dialog.dismiss();
        });

        retakeButton.setOnClickListener(v -> {
            dialog.dismiss();
            // 启动相机
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showToast("创建图片文件失败");
                return;
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "com.example.myapplication.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        });

        dialog.show();
    }

    private void saveFood() {
        try {
            String name = nameEditText.getText().toString();
            String expiryDays = expiryDateEditText.getText().toString();
            String quantityStr = quantityEditText.getText().toString();

            if (name.isEmpty()) {
                showToast("请输入食材名称");
                return;
            }

            // 使用默认值
            int days = expiryDays.isEmpty() ? 7 : Integer.parseInt(expiryDays);
            int quantity = quantityStr.isEmpty() ? 3 : Integer.parseInt(quantityStr);  // 默认数量改为3

            // 计算保质期截止日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, days);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String expiryDate = sdf.format(calendar.getTime());

            Food food = new Food(name, expiryDate, quantity);
            foodDatabase.addFood(food);

            showToast("保存成功");
            // 返回主界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing numbers: ", e);
            showToast("请输入有效的数字");
        } catch (Exception e) {
            Log.e(TAG, "Error saving food: ", e);
            showToast("保存失败: " + e.getMessage());
        }
    }

    private void saveAndAddAnother() {
        try {
            String name = nameEditText.getText().toString();
            String expiryDays = expiryDateEditText.getText().toString();
            String quantityStr = quantityEditText.getText().toString();

            if (name.isEmpty()) {
                showToast("请输入食材名称");
                return;
            }

            // 使用默认值
            int days = expiryDays.isEmpty() ? 7 : Integer.parseInt(expiryDays);
            int quantity = quantityStr.isEmpty() ? 3 : Integer.parseInt(quantityStr);  // 默认数量改为3

            // 计算保质期截止日期
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, days);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String expiryDate = sdf.format(calendar.getTime());

            Food food = new Food(name, expiryDate, quantity);
            foodDatabase.addFood(food);
            showToast("保存成功");

            // 发送广播通知MainActivity更新列表
            Intent updateIntent = new Intent("com.example.myapplication.UPDATE_FOOD_LIST");
            sendBroadcast(updateIntent);

            // 清空输入框
            nameEditText.setText("");
            expiryDateEditText.setText("7");
            quantityEditText.setText("1");

            // 显示选择对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_detection_result, null);
            ListView listView = dialogView.findViewById(R.id.listView);
            Button manualButton = dialogView.findViewById(R.id.manualButton);
            Button retakeButton = dialogView.findViewById(R.id.retakeButton);

            // 使用之前识别的结果
            JsonObject result = new Gson().fromJson(jsonResult, JsonObject.class);
            JsonArray resultArray = result.getAsJsonArray("result");
            List<String> validItems = new ArrayList<>();
            if (resultArray != null) {
                for (int i = 0; i < resultArray.size(); i++) {
                    JsonObject item = resultArray.get(i).getAsJsonObject();
                    String itemName = item.get("name").getAsString();
                    if (!itemName.contains("非果蔬")) {
                        validItems.add(itemName);
                    }
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, validItems);
            listView.setAdapter(adapter);

            AlertDialog dialog = builder.setView(dialogView)
                    .setTitle("选择食材")
                    .create();

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = validItems.get(position);
                nameEditText.setText(selectedName);
                dialog.dismiss();
            });

            manualButton.setOnClickListener(v -> {
                nameEditText.setText("");
                nameEditText.requestFocus();
                dialog.dismiss();
            });

            retakeButton.setOnClickListener(v -> {
                dialog.dismiss();
                // 启动相机
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    showToast("创建图片文件失败");
                    return;
                }
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this,
                            "com.example.myapplication.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error in saveAndAddAnother: ", e);
            showToast("保存失败: " + e.getMessage());
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            processImage();  // 重新处理新拍摄的图片
        }
    }

    private void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(ImageProcessActivity.this, message, Toast.LENGTH_SHORT).show());
    }
} 