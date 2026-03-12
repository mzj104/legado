# 多API管理功能设计文档

**日期:** 2026-03-12
**作者:** Claude & 用户协作设计

## 1. 概述

将现有的单一API配置（`ApiConfigDialog`）升级为支持多API管理的列表界面。用户可以添加、编辑、删除多个API配置，并设置其中一个为默认API。

## 2. 用户需求

- 当前"添加API"菜单只能配置一个API
- 需要支持多个API配置的管理
- 使用时自动使用默认API，无需每次选择

## 3. 架构设计

### 3.1 新增组件

| 组件 | 职责 | 依赖 |
|------|------|------|
| `ApiConfig` | 数据类，封装单个API配置 | 无 |
| `ApiConfigManager` | 单例，管理API配置的存储、检索、增删改查 | SharedPreferences, Gson |
| `ApiManageActivity` | API管理列表界面 | ApiConfigManager, ApiManageAdapter, ApiEditDialog |
| `ApiManageAdapter` | RecyclerView适配器，绑定API配置数据 | ApiConfig |
| `ApiEditDialog` | 添加/编辑API的对话框 | ApiConfig |

### 3.2 修改现有组件

| 组件 | 修改内容 |
|------|----------|
| `ApiConfigDialog` | 替换为启动`ApiManageActivity` |
| `ReviewBottomSheet` | 使用`ApiConfigManager`获取默认API配置 |

## 4. 数据模型

```kotlin
data class ApiConfig(
    val id: String,           // 唯一标识 (UUID)
    val name: String,         // 显示名称
    val apiKey: String,       // API密钥（可为空）
    val apiUrl: String,       // API地址
    val apiModel: String,     // 模型名称
    val isDefault: Boolean    // 是否默认
)
```

## 5. 数据存储

使用SharedPreferences存储API列表，通过Gson进行JSON序列化：

```kotlin
private const val PREF_API_CONFIGS = "api_configs"

// 存储格式
[
  {
    "id": "uuid-1",
    "name": "通义千问",
    "apiKey": "sk-xxx",
    "apiUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
    "apiModel": "qwen-plus",
    "isDefault": true
  },
  {
    "id": "uuid-2",
    "name": "OpenAI",
    "apiKey": "sk-yyy",
    "apiUrl": "https://api.openai.com/v1",
    "apiModel": "gpt-4o",
    "isDefault": false
  }
]
```

## 6. UI设计

### 6.1 API管理界面

```
┌─────────────────────────────────┐
│ ← API管理          [+添加]       │  标题栏
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 通义千问        [默认★]     │ │  列表项
│ │ qwen-plus                   │ │
│ │              [编辑] [删除]   │ │
│ └─────────────────────────────┘ │
│ ┌─────────────────────────────┐ │
│ │ OpenAI                      │ │
│ │ gpt-4o                      │ │
│ │     [设为默认] [编辑] [删除] │ │
│ └─────────────────────────────┘ │
│ ...                             │
└─────────────────────────────────┘
```

### 6.2 编辑对话框

字段：
- API名称（新增，必填）
- API密钥（可为空）
- API地址（必填）
- 模型名称（必填）

## 7. 交互流程

### 7.1 打开管理界面
```
阅读界面菜单 → "添加API" → ApiManageActivity
```

### 7.2 添加API
```
[+添加] → ApiEditDialog（空表单）→ 填写 → 保存 → 刷新列表
```

### 7.3 编辑API
```
列表项点击 → ApiEditDialog（预填充）→ 修改 → 保存 → 刷新列表
```

### 7.4 设置默认
```
[设为默认] → 清除其他默认标记 → 设置当前为默认 → 保存 → 刷新列表
```

### 7.5 删除API
```
[删除] → 确认对话框 → 确认 → 删除 → 刷新列表
```

### 7.6 使用API
```
自动匹配功能 → ApiConfigManager.getDefaultApi() → 获取默认API配置
```

## 8. 输入验证

| 字段 | 必填 | 说明 |
|------|------|------|
| API名称 | 是 | 用于识别不同的API配置 |
| API密钥 | 否 | 某些API可能不需要密钥 |
| API地址 | 是 | API的完整URL |
| 模型名称 | 是 | 要使用的模型 |

错误提示使用Toast显示。

## 9. 向后兼容

### 9.1 迁移逻辑

在`ApiConfigManager`初始化时检查旧配置：

```kotlin
private fun migrateOldConfig(context: Context) {
    val oldApiKey = context.getPrefString("api_key", null)
    if (oldApiKey != null && apiConfigs.isEmpty()) {
        val migratedConfig = ApiConfig(
            id = UUID.randomUUID().toString(),
            name = "默认API",
            apiKey = oldApiKey,
            apiUrl = context.getPrefString("api_url", "") ?: "",
            apiModel = context.getPrefString("api_model", "qwen3.5-flash") ?: "qwen3.5-flash",
            isDefault = true
        )
        addApiConfig(migratedConfig)
    }
}
```

### 9.2 兼容方法

`ApiConfigDialog`保留静态方法以兼容现有调用：

```kotlin
fun getApiKey(context: Context): String {
    return ApiConfigManager.getInstance(context).getDefaultApi()?.apiKey ?: ""
}

fun getApiUrl(context: Context): String {
    return ApiConfigManager.getInstance(context).getDefaultApi()?.apiUrl ?: ""
}

fun getApiModel(context: Context): String {
    return ApiConfigManager.getInstance(context).getDefaultApi()?.apiModel ?: "qwen3.5-flash"
}
```

## 10. 文件结构

### 10.1 新增文件

```
app/src/main/java/io/legado/app/ui/book/read/api/
├── ApiConfig.kt
├── ApiConfigManager.kt
├── ApiManageActivity.kt
├── ApiManageAdapter.kt
└── ApiEditDialog.kt
```

### 10.2 修改文件

```
app/src/main/java/io/legado/app/ui/book/read/
├── ApiConfigDialog.kt
└── ReviewBottomSheet.kt

app/src/main/res/layout/
├── activity_api_manage.xml
└── item_api_config.xml

app/src/main/res/values/strings.xml
app/src/main/res/values-zh/strings.xml
```

## 11. 空状态处理

- 列表为空时显示提示："暂无API配置，点击上方添加按钮添加API配置"
- 获取默认API时返回null，调用方处理

## 12. 测试要点

1. 添加、编辑、删除API配置
2. 设置默认API功能
3. 空列表状态显示
4. 向后兼容：旧配置正确迁移
5. 输入验证：各字段必填/可选规则
6. 使用时正确获取默认API
