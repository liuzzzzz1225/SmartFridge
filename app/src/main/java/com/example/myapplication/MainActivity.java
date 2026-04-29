package com.example.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "MainActivity";
    
    private EditText searchEditText;
    private Button addButton;
    private RecyclerView foodRecyclerView;
    private Uri photoURI;
    private FoodDatabase foodDatabase;
    private BroadcastReceiver updateReceiver;
    private ImageButton refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Starting onCreate");
            setContentView(R.layout.activity_main);

            initViews();
            setupDatabase();
            setupListeners();
            checkPermissions();
            
            // 注册广播接收器
            updateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("com.example.myapplication.UPDATE_FOOD_LIST".equals(intent.getAction())) {
                        updateFoodList();
                    }
                }
            };
            registerReceiver(updateReceiver, new IntentFilter("com.example.myapplication.UPDATE_FOOD_LIST"));
            
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "应用初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (updateReceiver != null) {
            unregisterReceiver(updateReceiver);
        }
    }

    private void initViews() {
        try {
            Log.d(TAG, "Initializing views");
            searchEditText = findViewById(R.id.searchEditText);
            addButton = findViewById(R.id.addButton);
            refreshButton = findViewById(R.id.refreshButton);
            foodRecyclerView = findViewById(R.id.foodRecyclerView);
            foodRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // 添加文本变化监听器，实现实时搜索
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // 当文本改变时执行搜索
                    String query = s.toString();
                    searchFood(query);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            // 保留回车键搜索功能
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchEditText.getText().toString();
                    searchFood(query);
                    return true;
                }
                return false;
            });

            // 添加刷新按钮点击监听
            refreshButton.setOnClickListener(v -> {
                // 执行旋转动画
                refreshButton.animate()
                        .rotationBy(360f)
                        .setDuration(1000)
                        .start();
                // 刷新数据
                updateFoodList();
            });

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: ", e);
            throw e;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 当活动恢复时刷新列表
        updateFoodList();
    }

    private void setupDatabase() {
        try {
            Log.d(TAG, "Setting up database");
            foodDatabase = new FoodDatabase(this);
            updateFoodList();
            Log.d(TAG, "Database setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupDatabase: ", e);
            throw e;
        }
    }

    private void setupListeners() {
        addButton.setOnClickListener(v -> showAddOptionsDialog());

        // 修改为左滑删除
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(ContextCompat.getColor(MainActivity.this, R.color.delete_background));
            private final Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FoodAdapter adapter = (FoodAdapter) foodRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.removeItem(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                  int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                
                // 修改背景位置（左滑）
                background.setBounds(itemView.getRight() + ((int) dX),
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom());
                background.draw(c);

                // 修改图标位置（左滑）
                if (deleteIcon != null) {
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconRight = itemView.getRight() - iconMargin;
                    int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(foodRecyclerView);
    }

    private void showAddOptionsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_options, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 设置手动输入按钮点击事件
        dialogView.findViewById(R.id.btnManualInput).setOnClickListener(v -> {
            dialog.dismiss();
            showManualInputDialog();
        });
        
        // 设置相机按钮点击事件
        dialogView.findViewById(R.id.btnOpenCamera).setOnClickListener(v -> {
            dialog.dismiss();
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            } else {
                requestCameraPermission();
            }
        });
        
        dialog.show();
    }

    private void showManualInputDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_input, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setTitle("请输入食材信息")
                .setPositiveButton("保存", (dialog, which) -> {
                    EditText nameEdit = dialogView.findViewById(R.id.editTextName);
                    EditText expiryEdit = dialogView.findViewById(R.id.editTextExpiry);
                    EditText quantityEdit = dialogView.findViewById(R.id.editTextQuantity);
                    
                    String name = nameEdit.getText().toString();
                    String expiryDays = expiryEdit.getText().toString();
                    String quantityStr = quantityEdit.getText().toString();
                    
                    // 允许空值，设置默认值
                    if (name.isEmpty()) name = "未命名";
                    int days = expiryDays.isEmpty() ? 7 : Integer.parseInt(expiryDays);
                    int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);
                    
                    // 计算保质期截止日期
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, days);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String expiryDate = sdf.format(calendar.getTime());
                    
                    Food food = new Food(name, expiryDate, quantity);
                    foodDatabase.addFood(food);
                    updateFoodList();
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null);
        
        builder.create().show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d("Camera", "Starting camera intent");
        
        // 先检查是否有相机应用
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d("Camera", "Photo file created: " + photoFile.getAbsolutePath());
            } catch (IOException ex) {
                Log.e("Camera", "Error creating photo file", ex);
                ex.printStackTrace();
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.myapplication.fileprovider",
                        photoFile);
                Log.d("Camera", "PhotoURI: " + photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (Exception e) {
                    Log.e("Camera", "Error starting camera", e);
                    Toast.makeText(this, "启动相机失败", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "设备没有相机", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // 启动图像处理活动
            Intent processIntent = new Intent(this, ImageProcessActivity.class);
            processIntent.putExtra("photoUri", photoURI.toString());
            startActivity(processIntent);
        }
    }

    private void searchFood(String query) {
        new Thread(() -> {
            final List<Food> foodList;
            if (query.isEmpty()) {
                // 如果搜索框为空，显示所有记录
                foodList = foodDatabase.getAllFood();
            } else {
                // 否则执行搜索
                foodList = foodDatabase.searchFood(query);
            }
            runOnUiThread(() -> {
                FoodAdapter adapter = new FoodAdapter(foodList, foodDatabase);
                foodRecyclerView.setAdapter(adapter);
                
                // 如果找到了结果，滚动到第一个匹配项
                if (!foodList.isEmpty()) {
                    foodRecyclerView.smoothScrollToPosition(0);
                }
            });
        }).start();
    }

    private void updateFoodList() {
        try {
            Log.d(TAG, "Updating food list");
            new Thread(() -> {
                try {
                    final List<Food> foodList = foodDatabase.getAllFood();
                    runOnUiThread(() -> {
                        try {
                            FoodAdapter adapter = new FoodAdapter(foodList, foodDatabase);
                            foodRecyclerView.setAdapter(adapter);
                            Log.d(TAG, "Food list updated successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting adapter: ", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error getting food list: ", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in updateFoodList: ", e);
        }
    }

    private void checkPermissions() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }
}