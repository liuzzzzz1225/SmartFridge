# 食材管理系统数据字典

## 1. 数据库表结构

### 1.1 食材表（foods）

| 字段名 | 数据类型 | 长度 | 是否可空 | 默认值 | 说明 |
|--------|----------|------|----------|--------|------|
| id | INTEGER | - | 否 | AUTOINCREMENT | 主键，自增 |
| name | TEXT | - | 否 | - | 食材名称 |
| expiry_date | TEXT | - | 否 | - | 保质期，格式：YYYY-MM-DD |
| quantity | INTEGER | - | 否 | 0 | 食材数量 |

## 2. 数据模型类

### 2.1 Food类

```java
public class Food {
    private int id;          // 食材ID
    private String name;     // 食材名称
    private String expiryDate; // 保质期
    private int quantity;    // 数量
}
```

## 3. 接口参数

### 3.1 百度AI接口参数

#### 3.1.1 获取访问令牌
- 接口URL：https://aip.baidubce.com/oauth/2.0/token
- 请求方式：POST
- 请求参数：
  | 参数名 | 类型 | 是否必填 | 说明 |
  |--------|------|----------|------|
  | grant_type | String | 是 | 固定值：client_credentials |
  | client_id | String | 是 | API Key |
  | client_secret | String | 是 | Secret Key |

#### 3.1.2 图像识别接口
- 接口URL：https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient
- 请求方式：POST
- 请求参数：
  | 参数名 | 类型 | 是否必填 | 说明 |
  |--------|------|----------|------|
  | access_token | String | 是 | 访问令牌 |
  | image | String | 是 | Base64编码的图像数据 |

## 4. 常量定义

### 4.1 系统常量
| 常量名 | 值 | 说明 |
|--------|-----|------|
| REQUEST_IMAGE_CAPTURE | 1 | 相机拍照请求码 |
| PERMISSION_REQUEST_CODE | 200 | 权限请求码 |
| DATABASE_NAME | "FoodDB" | 数据库名称 |
| DATABASE_VERSION | 1 | 数据库版本号 |
| TABLE_NAME | "foods" | 食材表名 |

### 4.2 API常量
| 常量名 | 值 | 说明 |
|--------|-----|------|
| API_KEY | "YOUR_API_KEY" | 百度AI API Key |
| SECRET_KEY | "YOUR_SECRET_KEY" | 百度AI Secret Key |
| TOKEN_URL | "https://aip.baidubce.com/oauth/2.0/token" | 获取令牌URL |
| VEGETABLE_DETECT_URL | "https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient" | 图像识别URL |

## 5. 权限定义

### 5.1 Android权限
| 权限名 | 说明 |
|--------|------|
| CAMERA | 相机权限，用于拍照功能 |
| READ_EXTERNAL_STORAGE | 读取外部存储权限，用于读取图片 |
| WRITE_EXTERNAL_STORAGE | 写入外部存储权限，用于保存图片 |
| INTERNET | 网络权限，用于API调用 |

## 6. 错误码定义

### 6.1 系统错误码
| 错误码 | 说明 |
|--------|------|
| 1001 | 数据库操作失败 |
| 1002 | 图片处理失败 |
| 1003 | 网络请求失败 |
| 1004 | API调用失败 |

## 7. 数据格式

### 7.1 日期格式
- 输入格式：YYYY-MM-DD
- 显示格式：YYYY年MM月DD日

### 7.2 图片格式
- 支持格式：JPEG、PNG
- 最大大小：10MB
- 压缩质量：100%

### 7.3 JSON格式
```json
{
    "id": 1,
    "name": "食材名称",
    "expiry_date": "2024-03-20",
    "quantity": 1
}
```

## 8. 数据验证规则

### 8.1 食材名称
- 长度：1-50个字符
- 字符类型：中文、英文、数字
- 不允许：特殊字符

### 8.2 保质期
- 格式：YYYY-MM-DD
- 范围：当前日期至未来一年

### 8.3 数量
- 类型：正整数
- 范围：1-9999 