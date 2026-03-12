# 多API管理功能实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 将单一API配置升级为支持多API管理的列表界面，用户可以添加、编辑、删除多个API配置并设置默认API。

**架构:** 新建api包存放API管理相关组件，使用SharedPreferences+Gson存储API配置列表，通过单例ApiConfigManager管理所有API操作，保持向后兼容性。

**技术栈:** Kotlin, AndroidX, SharedPreferences, Gson, RecyclerView

---

## 文件结构

### 新增文件
```
app/src/main/java/io/legado/app/ui/book/read/api/
├── ApiConfig.kt                    # API配置数据类
├── ApiConfigManager.kt             # API配置管理器单例
├── ApiManageActivity.kt            # API管理界面
├── ApiManageAdapter.kt             # RecyclerView适配器
└── ApiEditDialog.kt                # 添加/编辑API对话框

app/src/main/res/layout/
├── activity_api_manage.xml         # API管理界面布局
└── item_api_config.xml             # API列表项布局
```

### 修改文件
```
app/src/main/java/io/legado/app/ui/book/read/
├── ApiConfigDialog.kt              # 修改为启动ApiManageActivity
└── ReviewBottomSheet.kt            # 使用ApiConfigManager获取API配置

app/src/main/res/values/
├── strings.xml                     # 新增英文字符串资源
└── values-zh/strings.xml           # 新增中文字符串资源
```

---

## Chunk 1: 基础数据结构和字符串资源

### Task 1: 创建ApiConfig数据类

**文件:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/api/ApiConfig.kt`

- [ ] **步骤 1: 创建ApiConfig数据类**

```kotlin
package io.legado.app.ui.book.read.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * API配置数据类
 * @param id 唯一标识
 * @param name 显示名称
 * @param apiKey API密钥
 * @param apiUrl API地址
 * @param apiModel 模型名称
 * @param isDefault 是否为默认API
 */
@Parcelize
data class ApiConfig(
    val id: String,
    val name: String,
    val apiKey: String,
    val apiUrl: String,
    val apiModel: String,
    val isDefault: Boolean = false
) : Parcelable
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiConfig.kt
git commit -m "feat: add ApiConfig data class"
```

### Task 2: 添加字符串资源

**文件:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

- [ ] **步骤 1: 添加英文字符串资源**

在 `app/src/main/res/values/strings.xml` 中约第362行（api_model之后）添加：

```xml
<string name="api_manage">API Management</string>
<string name="add_api_config">Add API</string>
<string name="edit_api_config">Edit API</string>
<string name="api_name">API Name</string>
<string name="default_api">Default</string>
<string name="set_as_default">Set as Default</string>
<string name="confirm_delete">Confirm Delete</string>
<string name="confirm_delete_msg">Delete this API configuration?</string>
<string name="empty_api_list">No API configured yet</string>
<string name="empty_api_list_hint">Click the add button above to add an API configuration</string>
<string name="api_name_required">API name is required</string>
<string name="api_url_required">API URL is required</string>
<string name="api_model_required">Model name is required</string>
<string name="save_success">Saved successfully</string>
<string name="delete_success">Deleted successfully</string>
<string name="set_default_success">Set as default successfully</string>
```

- [ ] **步骤 2: 添加中文字符串资源**

在 `app/src/main/res/values-zh/strings.xml` 中约第360行（api_model之后）添加：

```xml
<string name="api_manage">API管理</string>
<string name="add_api_config">添加API</string>
<string name="edit_api_config">编辑API</string>
<string name="api_name">API名称</string>
<string name="default_api">默认</string>
<string name="set_as_default">设为默认</string>
<string name="confirm_delete">确认删除</string>
<string name="confirm_delete_msg">确定要删除此API配置吗？</string>
<string name="empty_api_list">暂无API配置</string>
<string name="empty_api_list_hint">点击上方添加按钮添加API配置</string>
<string name="api_name_required">请输入API名称</string>
<string name="api_url_required">请输入API地址</string>
<string name="api_model_required">请输入模型名称</string>
<string name="save_success">保存成功</string>
<string name="delete_success">删除成功</string>
<string name="set_default_success">已设为默认</string>
```

- [ ] **步骤 3: 提交**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat: add string resources for API management"
```

---

## Chunk 2: ApiConfigManager管理器

### Task 3: 创建ApiConfigManager单例

**文件:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/api/ApiConfigManager.kt`

- [ ] **步骤 1: 创建ApiConfigManager单例类**

```kotlin
package io.legado.app.ui.book.read.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import java.util.UUID

/**
 * API配置管理器
 * 单例模式，负责API配置的增删改查和持久化存储
 */
object ApiConfigManager {

    private const val PREF_API_CONFIGS = "api_configs"
    private const val PREF_API_KEY = "api_key"
    private const val PREF_API_URL = "api_url"
    private const val PREF_API_MODEL = "api_model"

    private val gson = Gson()
    private val listType = object : TypeToken<List<ApiConfig>>() {}.type

    private var cachedConfigs: MutableList<ApiConfig>? = null
    private lateinit var prefs: SharedPreferences

    /**
     * 初始化管理器
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("api_config", Context.MODE_PRIVATE)
        // 迁移旧配置
        migrateOldConfig(context)
    }

    /**
     * 迁移旧的单一API配置到新的多API配置系统
     */
    private fun migrateOldConfig(context: Context) {
        val configs = getApiConfigs()
        if (configs.isNotEmpty()) return // 已经有配置，无需迁移

        val oldApiKey = context.getPrefString(PREF_API_KEY, null)
        if (oldApiKey != null) {
            val migratedConfig = ApiConfig(
                id = UUID.randomUUID().toString(),
                name = "默认API",
                apiKey = oldApiKey,
                apiUrl = context.getPrefString(PREF_API_URL, "") ?: "",
                apiModel = context.getPrefString(PREF_API_MODEL, "qwen3.5-flash") ?: "qwen3.5-flash",
                isDefault = true
            )
            addApiConfig(migratedConfig)
        }
    }

    /**
     * 获取所有API配置
     */
    fun getApiConfigs(): List<ApiConfig> {
        if (cachedConfigs == null) {
            val json = prefs.getString(PREF_API_CONFIGS, null)
            cachedConfigs = if (json.isNullOrEmpty()) {
                mutableListOf()
            } else {
                gson.fromJson(json, listType).toMutableList()
            }
        }
        return cachedConfigs ?: emptyList()
    }

    /**
     * 获取默认API配置
     */
    fun getDefaultApi(): ApiConfig? {
        return getApiConfigs().firstOrNull { it.isDefault }
    }

    /**
     * 添加API配置
     */
    fun addApiConfig(config: ApiConfig) {
        val configs = getApiConfigs().toMutableList()
        // 如果设置为默认，清除其他默认标记
        if (config.isDefault) {
            configs.forEach { it.copy(isDefault = false) }
        }
        configs.add(config)
        saveConfigs(configs)
    }

    /**
     * 更新API配置
     */
    fun updateApiConfig(config: ApiConfig) {
        val configs = getApiConfigs().toMutableList()
        val index = configs.indexOfFirst { it.id == config.id }
        if (index != -1) {
            // 如果设置为默认，清除其他默认标记
            if (config.isDefault) {
                configs.forEach { it.copy(isDefault = false) }
            }
            configs[index] = config
            saveConfigs(configs)
        }
    }

    /**
     * 删除API配置
     */
    fun deleteApiConfig(id: String) {
        val configs = getApiConfigs().toMutableList()
        val removed = configs.removeIf { it.id == id }
        if (removed) {
            // 如果删除的是默认API，设置第一个为默认
            if (configs.isNotEmpty() && configs.none { it.isDefault }) {
                configs[0] = configs[0].copy(isDefault = true)
            }
            saveConfigs(configs)
        }
    }

    /**
     * 设置默认API
     */
    fun setDefaultApi(id: String) {
        val configs = getApiConfigs().toMutableList()
        configs.forEachIndexed { index, apiConfig ->
            configs[index] = apiConfig.copy(isDefault = apiConfig.id == id)
        }
        saveConfigs(configs)
    }

    /**
     * 保存配置列表
     */
    private fun saveConfigs(configs: List<ApiConfig>) {
        cachedConfigs = configs.toMutableList()
        val json = gson.toJson(configs)
        prefs.edit().putString(PREF_API_CONFIGS, json).apply()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedConfigs = null
    }

    // ========== 向后兼容方法 ==========

    /**
     * 获取API密钥（兼容旧代码）
     */
    fun getApiKey(context: Context): String {
        init(context)
        return getDefaultApi()?.apiKey ?: ""
    }

    /**
     * 获取API地址（兼容旧代码）
     */
    fun getApiUrl(context: Context): String {
        init(context)
        return getDefaultApi()?.apiUrl ?: ""
    }

    /**
     * 获取API模型（兼容旧代码）
     */
    fun getApiModel(context: Context): String {
        init(context)
        return getDefaultApi()?.apiModel ?: "qwen3.5-flash"
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiConfigManager.kt
git commit -m "feat: add ApiConfigManager singleton"
```

---

## Chunk 3: UI布局文件

### Task 4: 创建API管理界面布局

**文件:**
- Create: `app/src/main/res/layout/activity_api_manage.xml`

- [ ] **步骤 1: 创建API管理界面布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <io.legado.app.ui.widget.TitleBar
        android:id="@+id/title_bar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:title="@string/api_manage"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:padding="8dp"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/title_bar"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        tools:listitem="@layout/item_api_config" />

    <TextView
        android:id="@+id/empty_hint"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/empty_api_list_hint"
        android:textSize="14sp"
        android:textColor="@color/secondaryText"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/res/layout/activity_api_manage.xml
git commit -m "feat: add API management activity layout"
```

### Task 5: 创建API列表项布局

**文件:**
- Create: `app/src/main/res/layout/item_api_config.xml`

- [ ] **步骤 1: 创建API列表项布局**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="12dp">

        <TextView
            android:id="@+id/tv_name"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textColor="@color/primaryText"
            android:textStyle="bold"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/tv_default"
            tools:text="通义千问" />

        <TextView
            android:id="@+id/tv_default"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/default_api"
            android:textSize="12sp"
            android:textColor="@color/accent"
            android:background="@drawable/bg_badge"
            android:paddingStart="8dp"
            android:paddingEnd="8dp"
            android:paddingTop="2dp"
            android:paddingBottom="2dp"
            android:visibility="gone"
            app:layout_constraintTop_toTopOf="@id/tv_name"
            app:layout_constraintEnd_toEndOf="parent"
            tools:visibility="visible" />

        <TextView
            android:id="@+id/tv_model"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="14sp"
            android:textColor="@color/secondaryText"
            android:maxLines="1"
            android:ellipsize="end"
            app:layout_constraintTop_toBottomOf="@id/tv_name"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/btn_edit"
            tools:text="qwen-plus" />

        <ImageButton
            android:id="@+id/btn_set_default"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginTop="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_star_border"
            android:contentDescription="@string/set_as_default"
            android:visibility="gone"
            app:layout_constraintTop_toBottomOf="@id/tv_model"
            app:layout_constraintEnd_toStartOf="@id/btn_edit"
            tools:visibility="visible" />

        <ImageButton
            android:id="@+id/btn_edit"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginTop="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_edit"
            android:contentDescription="@string/edit_api_config"
            app:layout_constraintTop_toBottomOf="@id/tv_model"
            app:layout_constraintEnd_toStartOf="@id/btn_delete" />

        <ImageButton
            android:id="@+id/btn_delete"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:layout_marginTop="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_delete"
            android:contentDescription="@string/confirm_delete"
            app:layout_constraintTop_toBottomOf="@id/tv_model"
            app:layout_constraintEnd_toEndOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</androidx.cardview.widget.CardView>
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/res/layout/item_api_config.xml
git commit -m "feat: add API config item layout"
```

### Task 6: 创建徽章背景drawable

**文件:**
- Create: `app/src/main/res/drawable/bg_badge.xml`

- [ ] **步骤 1: 创建徽章背景**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/bg_accent" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/res/drawable/bg_badge.xml
git commit -m "feat: add badge background drawable"
```

---

## Chunk 4: RecyclerView适配器

### Task 7: 创建ApiManageAdapter

**文件:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/api/ApiManageAdapter.kt`

- [ ] **步骤 1: 创建Adapter类**

```kotlin
package io.legado.app.ui.book.read.api

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.databinding.ItemApiConfigBinding

/**
 * API配置列表适配器
 */
class ApiManageAdapter(
    private val callBack: CallBack
) : ListAdapter<ApiConfig, ApiManageAdapter.ViewHolder>(DiffCallback) {

    interface CallBack {
        fun onEdit(config: ApiConfig)
        fun onDelete(config: ApiConfig)
        fun onSetDefault(config: ApiConfig)
    }

    private object DiffCallback : DiffUtil.ItemCallback<ApiConfig>() {
        override fun areItemsTheSame(oldItem: ApiConfig, newItem: ApiConfig): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ApiConfig, newItem: ApiConfig): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(private val binding: ItemApiConfigBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ApiConfig) {
            binding.apply {
                tvName.text = item.name
                tvModel.text = item.apiModel

                // 默认标记
                if (item.isDefault) {
                    tvDefault.visibility = android.view.View.VISIBLE
                    btnSetDefault.visibility = android.view.View.GONE
                } else {
                    tvDefault.visibility = android.view.View.GONE
                    btnSetDefault.visibility = android.view.View.VISIBLE
                }

                // 点击整个卡片进行编辑
                root.setOnClickListener {
                    callBack.onEdit(item)
                }

                // 设置默认
                btnSetDefault.setOnClickListener {
                    callBack.onSetDefault(item)
                }

                // 编辑
                btnEdit.setOnClickListener {
                    callBack.onEdit(item)
                }

                // 删除
                btnDelete.setOnClickListener {
                    callBack.onDelete(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApiConfigBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiManageAdapter.kt
git commit -m "feat: add ApiManageAdapter"
```

---

## Chunk 5: 编辑对话框

### Task 8: 创建ApiEditDialog

**文件:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/api/ApiEditDialog.kt`

- [ ] **步骤 1: 创建编辑对话框**

```kotlin
package io.legado.app.ui.book.read.api

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.R
import io.legado.app.databinding.DialogApiEditBinding
import io.legado.app.utils.toastOnUi

/**
 * API配置编辑对话框
 */
class ApiEditDialog : BottomSheetDialogFragment() {

    private var _binding: DialogApiEditBinding? = null
    private val binding get() = _binding!!

    private var config: ApiConfig? = null
    private var onSaveCallback: ((ApiConfig) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogApiEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置标题
        binding.titleBar.title = if (config == null) {
            getString(R.string.add_api_config)
        } else {
            getString(R.string.edit_api_config)
        }

        // 填充现有数据
        config?.let {
            binding.etName.setText(it.name)
            binding.etApiKey.setText(it.apiKey)
            binding.etApiUrl.setText(it.apiUrl)
            binding.etApiModel.setText(it.apiModel)
        }

        // 保存按钮
        binding.titleBar.setTitleMenu(R.string.action_save) {
            saveConfig()
            true
        }

        // 实时验证
        setupValidation()
    }

    private fun setupValidation() {
        binding.etName.doAfterTextChanged { validateInput() }
        binding.etApiUrl.doAfterTextChanged { validateInput() }
        binding.etApiModel.doAfterTextChanged { validateInput() }
    }

    private fun validateInput(): Boolean {
        val name = binding.etName.text?.toString()?.trim()
        val url = binding.etApiUrl.text?.toString()?.trim()
        val model = binding.etApiModel.text?.toString()?.trim()

        var isValid = true

        if (name.isNullOrEmpty()) {
            binding.tilName.error = getString(R.string.api_name_required)
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (url.isNullOrEmpty()) {
            binding.tilApiUrl.error = getString(R.string.api_url_required)
            isValid = false
        } else {
            binding.tilApiUrl.error = null
        }

        if (model.isNullOrEmpty()) {
            binding.tilApiModel.error = getString(R.string.api_model_required)
            isValid = false
        } else {
            binding.tilApiModel.error = null
        }

        return isValid
    }

    private fun saveConfig() {
        if (!validateInput()) {
            return
        }

        val name = binding.etName.text?.toString()?.trim() ?: return
        val apiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        val apiUrl = binding.etApiUrl.text?.toString()?.trim() ?: return
        val apiModel = binding.etApiModel.text?.toString()?.trim() ?: return

        val newConfig = ApiConfig(
            id = config?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            apiKey = apiKey,
            apiUrl = apiUrl,
            apiModel = apiModel,
            isDefault = config?.isDefault ?: false
        )

        onSaveCallback?.invoke(newConfig)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ApiEditDialog"

        fun show(
            context: Context,
            config: ApiConfig? = null,
            onSave: (ApiConfig) -> Unit
        ) {
            val dialog = ApiEditDialog()
            dialog.config = config
            dialog.onSaveCallback = onSave
            dialog.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, TAG)
        }
    }
}
```

- [ ] **步骤 2: 创建对话框布局**

创建 `app/src/main/res/layout/dialog_api_edit.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <io.legado.app.ui.widget.TitleBar
        android:id="@+id/title_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:title="@string/add_api_config" />

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/til_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="@string/api_name">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxLines="1" />

    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/til_api_key"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="@string/api_key"
        app:endIconMode="password_toggle">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_api_key"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxLines="1"
            android:inputType="textPassword" />

    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/til_api_url"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="@string/api_url">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_api_url"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxLines="1"
            android:inputType="textUri" />

    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/til_api_model"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="@string/api_model">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_api_model"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxLines="1" />

    </com.google.android.material.textfield.TextInputLayout>

</LinearLayout>
```

- [ ] **步骤 3: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiEditDialog.kt
git add app/src/main/res/layout/dialog_api_edit.xml
git commit -m "feat: add API edit dialog"
```

---

## Chunk 6: API管理Activity

### Task 9: 创建ApiManageActivity

**文件:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/api/ApiManageActivity.kt`

- [ ] **步骤 1: 创建Activity类**

```kotlin
package io.legado.app.ui.book.read.api

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityApiManageBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.applyTint
import io.legado.app.utils toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch

/**
 * API管理界面
 */
class ApiManageActivity : VMBaseActivity<ActivityApiManageBinding, ApiManageViewModel>(),
    ApiManageAdapter.CallBack {

    override val binding by viewBinding(ActivityApiManageBinding::inflate)
    override val viewModel by viewModels<ApiManageViewModel>()

    private val adapter by lazy { ApiManageAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.api_manage, menu)
        menu.findItem(R.id.menu_add).applyTint(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> addNewApi()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.titleBar.setTitle(R.string.api_manage)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.apiConfigs.collect { configs ->
                adapter.submitList(configs)

                // 空状态处理
                if (configs.isEmpty()) {
                    binding.emptyHint.visibility = android.view.View.VISIBLE
                    binding.recyclerView.visibility = android.view.View.GONE
                } else {
                    binding.emptyHint.visibility = android.view.View.GONE
                    binding.recyclerView.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    private fun addNewApi() {
        ApiEditDialog.show(this) { config ->
            viewModel.addApiConfig(config)
            toastOnUi(R.string.save_success)
        }
    }

    override fun onEdit(config: ApiConfig) {
        ApiEditDialog.show(this, config) { updatedConfig ->
            viewModel.updateApiConfig(updatedConfig)
            toastOnUi(R.string.save_success)
        }
    }

    override fun onDelete(config: ApiConfig) {
        alert(R.string.confirm_delete, R.string.confirm_delete_msg) {
            positiveButton(R.string.delete) {
                viewModel.deleteApiConfig(config.id)
                toastOnUi(R.string.delete_success)
            }
           NegativeButton()
        }
    }

    override fun onSetDefault(config: ApiConfig) {
        viewModel.setDefaultApi(config.id)
        toastOnUi(R.string.set_default_success)
    }

    override fun onDestroy() {
        super.onDestroy()
        ApiConfigManager.clearCache()
    }
}
```

- [ ] **步骤 2: 创建ViewModel**

创建 `app/src/main/java/io/legado/app/ui/book/read/api/ApiManageViewModel.kt`:

```kotlin
package io.legado.app.ui.book.read.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.ui.book.read.api.ApiConfig
import io.legado.app.ui.book.read.api.ApiConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * API管理ViewModel
 */
class ApiManageViewModel : ViewModel() {

    private val _apiConfigs = MutableStateFlow<List<ApiConfig>>(emptyList())
    val apiConfigs: StateFlow<List<ApiConfig>> = _apiConfigs.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            val configs = ApiConfigManager.getApiConfigs()
            _apiConfigs.value = configs
        }
    }

    fun addApiConfig(config: ApiConfig) {
        viewModelScope.launch {
            ApiConfigManager.addApiConfig(config)
            loadConfigs()
        }
    }

    fun updateApiConfig(config: ApiConfig) {
        viewModelScope.launch {
            ApiConfigManager.updateApiConfig(config)
            loadConfigs()
        }
    }

    fun deleteApiConfig(id: String) {
        viewModelScope.launch {
            ApiConfigManager.deleteApiConfig(id)
            loadConfigs()
        }
    }

    fun setDefaultApi(id: String) {
        viewModelScope.launch {
            ApiConfigManager.setDefaultApi(id)
            loadConfigs()
        }
    }
}
```

- [ ] **步骤 3: 创建菜单资源**

创建 `app/src/main/res/menu/api_manage.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/menu_add"
        android:title="@string/add_api_config"
        android:icon="@drawable/ic_add"
        app:showAsAction="ifRoom" />

</menu>
```

- [ ] **步骤 4: 在AndroidManifest.xml中注册Activity**

在 `app/src/main/AndroidManifest.xml` 中添加：

```xml
<activity
    android:name=".ui.book.read.api.ApiManageActivity"
    android:label="@string/api_manage"
    android:theme="@style/Theme.AppCompat.Light.DarkActionBar" />
```

- [ ] **步骤 5: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiManageActivity.kt
git add app/src/main/java/io/legado/app/ui/book/read/api/ApiManageViewModel.kt
git add app/src/main/res/menu/api_manage.xml
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add ApiManageActivity with ViewModel"
```

---

## Chunk 7: 修改现有组件

### Task 10: 修改ApiConfigDialog启动Activity

**文件:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ApiConfigDialog.kt`

- [ ] **步骤 1: 修改ApiConfigDialog为启动Activity**

将整个文件替换为：

```kotlin
package io.legado.app.ui.book.read

import android.content.Context
import io.legado.app.ui.book.read.api.ApiConfigManager
import io.legado.app.utils.startActivity

/**
 * API配置入口
 * 现在启动API管理Activity
 */
object ApiConfigDialog {

    /**
     * 显示API管理界面
     */
    fun show(context: Context) {
        // 初始化管理器
        ApiConfigManager.init(context)
        // 启动API管理Activity
        context.startActivity<ApiManageActivity>()
    }

    // ========== 向后兼容方法 ==========

    /**
     * 获取API密钥（兼容旧代码）
     */
    @JvmStatic
    fun getApiKey(context: Context): String {
        return ApiConfigManager.getApiKey(context)
    }

    /**
     * 获取API地址（兼容旧代码）
     */
    @JvmStatic
    fun getApiUrl(context: Context): String {
        return ApiConfigManager.getApiUrl(context)
    }

    /**
     * 获取API模型（兼容旧代码）
     */
    @JvmStatic
    fun getApiModel(context: Context): String {
        return ApiConfigManager.getApiModel(context)
    }
}
```

添加导入语句：

```kotlin
import io.legado.app.ui.book.read.api.ApiManageActivity
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/ApiConfigDialog.kt
git commit -m "refactor: change ApiConfigDialog to launch ApiManageActivity"
```

### Task 11: 修改ReviewBottomSheet使用ApiConfigManager

**文件:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReviewBottomSheet.kt`

- [ ] **步骤 1: 修改API配置获取方式**

在 `ReviewBottomSheet.kt` 中，找到获取API配置的代码（约第496-498行），修改为：

```kotlin
// 从ApiConfigManager获取API配置
val apiKey = ApiConfigManager.getApiKey(requireContext())
val apiUrl = ApiConfigManager.getApiUrl(requireContext())
val apiModel = ApiConfigManager.getApiModel(requireContext())
```

确保添加导入：

```kotlin
import io.legado.app.ui.book.read.api.ApiConfigManager
```

- [ ] **步骤 2: 提交**

```bash
git add app/src/main/java/io/legado/app/ui/book/read/ReviewBottomSheet.kt
git commit -m "refactor: use ApiConfigManager in ReviewBottomSheet"
```

---

## Chunk 8: 测试和验证

### Task 12: 功能测试

- [ ] **步骤 1: 测试空状态**

1. 清除应用数据或全新安装
2. 点击"添加API"菜单
3. 验证：显示API管理界面，显示"暂无API配置"提示

- [ ] **步骤 2: 测试添加API**

1. 点击"添加"按钮
2. 填写：名称="通义千问", API密钥="sk-test", API地址="https://api.example.com", 模型="qwen-plus"
3. 点击保存
4. 验证：列表显示新添加的API

- [ ] **步骤 3: 测试编辑API**

1. 点击列表中的API项
2. 修改名称为"通义千问2"
3. 点击保存
4. 验证：列表中名称已更新

- [ ] **步骤 4: 测试设置默认**

1. 添加第二个API
2. 点击第二个API的"设为默认"按钮
3. 验证：第一个API失去默认标记，第二个API显示"默认"标签

- [ ] **步骤 5: 测试删除API**

1. 点击非默认API的"删除"按钮
2. 确认删除
3. 验证：该API从列表中移除

- [ ] **步骤 6: 测试输入验证**

1. 点击"添加"按钮
2. 不填写任何内容，点击保存
3. 验证：显示"请输入API名称"等错误提示
4. 只填写名称和模型，不填写API地址
5. 验证：显示"请输入API地址"错误

- [ ] **步骤 7: 测试向后兼容**

1. 在旧版本应用中配置API
2. 升级到新版本
3. 验证：旧配置自动迁移为默认API

- [ ] **步骤 8: 测试API使用**

1. 配置默认API
2. 使用自动匹配功能
3. 验证：使用默认API配置

### Task 13: 提交最终代码

- [ ] **步骤 1: 检查代码**

```bash
git status
git diff
```

- [ ] **步骤 2: 最终提交**

```bash
git add .
git commit -m "feat: complete multi-API management feature"
```

---

## 实现注意事项

1. **图标资源**: 确保项目中存在以下图标，或使用现有图标替代：
   - `ic_add` - 添加按钮
   - `ic_edit` - 编辑按钮
   - `ic_delete` - 删除按钮
   - `ic_star_border` - 设为默认按钮

2. **颜色资源**: 确保以下颜色已定义：
   - `@color/accent` - 强调色（用于默认标记）
   - `@color/bg_accent` - 强调色背景（用于徽章）
   - `@color/primaryText` - 主要文字颜色
   - `@color/secondaryText` - 次要文字颜色
   - `@color/background` - 背景色

3. **ViewBinding**: 确保项目已启用ViewBinding

4. **Gson**: 确保项目已添加Gson依赖

5. **测试**: 每个任务完成后建议运行应用进行验证
