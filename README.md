# 食材管理 (Food Manager)

一个基于 Android 的食材管理应用，支持手动录入和相机拍照识别食材，帮助用户追踪食材保质期，减少食物浪费。

## 功能

- **食材录入** — 手动输入食材名称、保质期、数量
- **相机识别** — 拍照后调用百度 AI 图像识别 API 自动识别食材种类
- **实时搜索** — 按食材名称即时过滤列表
- **左滑删除** — 左滑快速移除已消耗或过期的食材
- **下拉刷新** — 刷新食材列表
- **保质期预警** — 根据保质期自动标记：过期（红色）、临期（黄色）、正常（灰色）

## 技术栈

| 层面 | 技术 |
|------|------|
| 语言 | Java 11 |
| 构建 | Gradle (Kotlin DSL) |
| 最低 SDK | Android 16 (Android 4.1) |
| 目标 SDK | 34 |
| UI | RecyclerView + Material Design |
| 数据库 | SQLite (SQLiteOpenHelper) |
| 网络 | OkHttp 4 |
| JSON | Gson |
| 图像识别 | 百度 AI 图像分类 API |

## 项目结构

```
app/src/main/java/com/example/myapplication/
├── MainActivity.java          # 主界面：食材列表、搜索、左滑删除
├── ImageProcessActivity.java  # 图像处理：拍照、调用百度AI识别、保存
├── BaiduAiHelper.java         # 百度 AI API 封装（鉴权 + 识别）
├── FoodDatabase.java          # SQLite 数据库 CRUD 操作
├── FoodAdapter.java           # RecyclerView 适配器
├── Food.java                  # 食材数据模型
└── DetectionResult.java       # 识别结果模型
```

## 快速开始

### 1. 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11+
- Android SDK 34

### 2. 配置百度 AI API

在百度 AI 开放平台注册并获取 API Key 和 Secret Key，然后编辑：

```
app/src/main/assets/settings.json
```

填入你的密钥：

```json
{
    "baidu_ai": {
        "api_key": "你的API_KEY",
        "secret_key": "你的SECRET_KEY"
    }
}
```

> 使用的是百度 AI 的**食材识别**接口（`/image-classify/v1/classify/ingredient`）。

### 3. 构建运行

```bash
# 调试构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行单元测试
./gradlew test

# 运行 Android 仪器测试
./gradlew connectedAndroidTest
```

或直接用 Android Studio 打开项目，点击 Run 即可。

## 权限说明

| 权限 | 用途 |
|------|------|
| CAMERA | 拍照识别食材 |
| INTERNET | 调用百度 AI API |
| ACCESS_NETWORK_STATE | 检测网络状态 |

## License

MIT
