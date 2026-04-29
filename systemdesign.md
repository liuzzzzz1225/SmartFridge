# 第四章 系统设计

## 4.1 系统架构设计

本系统采用典型的Android应用分层架构，遵循关注点分离原则，将系统划分为表现层、业务逻辑层、数据持久层和外部服务层四个主要层次。系统的整体架构如图4-1所示。

### 4.1.1 表现层设计

表现层负责用户界面的展示和用户交互的处理，主要包含以下组件：

1. 主界面（MainActivity）
```java
public class MainActivity extends AppCompatActivity {
    private EditText searchEditText;
    private Button addButton;
    private RecyclerView foodRecyclerView;
    private FoodAdapter foodAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupDatabase();
        setupListeners();
    }
}
```

2. 图像处理界面（ImageProcessActivity）
```java
public class ImageProcessActivity extends AppCompatActivity {
    private ImageView previewImageView;
    private Button detectButton;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);
        initViews();
        setupImageProcessing();
    }
}
```

### 4.1.2 业务逻辑层设计

业务逻辑层实现了系统的核心功能，主要包括：

1. 食材管理逻辑
```java
public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private List<Food> foodList;
    private FoodDatabase foodDatabase;
    
    public void addFood(Food food) {
        foodDatabase.addFood(food);
        updateFoodList();
    }
    
    public void removeItem(int position) {
        Food food = foodList.get(position);
        foodDatabase.deleteFood(food.getId());
        foodList.remove(position);
        notifyItemRemoved(position);
    }
}
```

2. 图像识别逻辑
```java
public class BaiduAiHelper {
    private static final String API_KEY = "xxx";
    private static final String SECRET_KEY = "xxx";
    
    public static void detectVegetable(Bitmap bitmap, final ApiCallback callback) {
        if (accessToken == null) {
            getAccessToken(new ApiCallback() {
                @Override
                public void onSuccess(String token) {
                    performDetection(bitmap, callback);
                }
            });
        } else {
            performDetection(bitmap, callback);
        }
    }
}
```

### 4.1.3 数据持久层设计

数据持久层负责数据的存储和访问，采用SQLite数据库实现：

```java
public class FoodDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FoodDB";
    private static final int DATABASE_VERSION = 1;
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "expiry_date TEXT, " +
                "quantity INTEGER)";
        db.execSQL(createTable);
    }
}
```

### 4.1.4 外部服务层设计

外部服务层主要负责与百度AI平台的交互：

```java
public class BaiduAiHelper {
    private static final String VEGETABLE_DETECT_URL = 
        "https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient";
    
    private static void performDetection(Bitmap bitmap, final ApiCallback callback) {
        String base64Image = convertBitmapToBase64(bitmap);
        RequestBody requestBody = new FormBody.Builder()
                .add("image", base64Image)
                .build();
        // 发送请求到百度AI服务器
    }
}
```

## 4.2 数据库设计

### 4.2.1 数据库表结构设计

系统使用SQLite数据库存储食材信息，主要包含以下表：

1. 食材表（foods）
```sql
CREATE TABLE foods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    expiry_date TEXT NOT NULL,
    quantity INTEGER NOT NULL
);
```

### 4.2.2 数据访问接口设计

系统实现了完整的CRUD操作接口：

```java
public class FoodDatabase extends SQLiteOpenHelper {
    // 添加食材
    public void addFood(Food food) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", food.getName());
        values.put("expiry_date", food.getExpiryDate());
        values.put("quantity", food.getQuantity());
        db.insert(TABLE_NAME, null, values);
        db.close();
    }
    
    // 查询食材
    public List<Food> getAllFood() {
        List<Food> foodList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + 
                           " ORDER BY expiry_date ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // 处理查询结果
        return foodList;
    }
}
```

## 4.3 用户界面设计

### 4.3.1 主界面设计

主界面采用Material Design设计规范，主要包含以下元素：

1. 搜索栏
```xml
<EditText
    android:id="@+id/searchEditText"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="搜索食材"/>
```

2. 食材列表
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/foodRecyclerView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

### 4.3.2 交互设计

系统实现了多种现代化的交互方式：

1. 左滑删除
```java
ItemTouchHelper.SimpleCallback swipeCallback = 
    new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                         @NonNull RecyclerView.ViewHolder viewHolder,
                         @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                         int direction) {
        int position = viewHolder.getAdapterPosition();
        adapter.removeItem(position);
    }
};
```

## 4.4 安全设计

### 4.4.1 数据安全

系统实现了多层数据安全保护机制：

1. SQL注入防护
```java
public List<Food> searchFood(String query) {
    SQLiteDatabase db = this.getReadableDatabase();
    String[] selectionArgs = new String[]{"%" + query + "%"};
    Cursor cursor = db.query(TABLE_NAME, null,
            "name LIKE ?", selectionArgs,
            null, null, null);
    // 处理查询结果
}
```

### 4.4.2 权限管理

系统实现了完整的权限管理机制：

```java
private void checkPermissions() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }
}
```

## 4.5 性能优化设计

### 4.5.1 数据加载优化

系统采用异步加载机制处理数据：

```java
private void loadFoodData() {
    new AsyncTask<Void, Void, List<Food>>() {
        @Override
        protected List<Food> doInBackground(Void... voids) {
            return foodDatabase.getAllFood();
        }

        @Override
        protected void onPostExecute(List<Food> foods) {
            foodAdapter.updateData(foods);
        }
    }.execute();
}
```

### 4.5.2 图像处理优化

系统实现了图像压缩和缓存机制：

```java
private Bitmap compressImage(Bitmap original) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    original.compress(Bitmap.CompressFormat.JPEG, 80, out);
    byte[] bytes = out.toByteArray();
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
}
```

## 4.6 系统测试设计

### 4.6.1 单元测试

系统实现了关键功能的单元测试：

```java
@Test
public void testAddFood() {
    Food food = new Food("测试食材", "2024-12-31", 1);
    foodDatabase.addFood(food);
    List<Food> foods = foodDatabase.getAllFood();
    assertFalse(foods.isEmpty());
    assertEquals("测试食材", foods.get(0).getName());
}
```

### 4.6.2 集成测试

系统实现了主要功能流程的集成测试：

```java
@Test
public void testImageRecognitionFlow() {
    // 模拟图像识别流程
    Bitmap testImage = BitmapFactory.decodeResource(
        getResources(), R.drawable.test_food);
    BaiduAiHelper.detectVegetable(testImage, new ApiCallback() {
        @Override
        public void onSuccess(String result) {
            assertNotNull(result);
            // 验证识别结果
        }
    });
}
```
```

这个系统设计文档详细描述了系统的各个层面，包括架构设计、数据库设计、用户界面设计、安全设计、性能优化和测试设计等方面。每个部分都基于实际的代码实现，并采用了学术化的语言进行描述。如果您需要对某个部分进行修改或补充，请告诉我。