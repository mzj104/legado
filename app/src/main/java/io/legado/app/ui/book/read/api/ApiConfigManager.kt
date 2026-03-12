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
                gson.fromJson<List<ApiConfig>>(json, listType).toMutableList()
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
            configs.forEachIndexed { index, it ->
                configs[index] = it.copy(isDefault = false)
            }
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
                configs.forEachIndexed { index, it ->
                    configs[index] = it.copy(isDefault = false)
                }
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
