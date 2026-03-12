package io.legado.app.ui.book.read

import android.content.Context
import io.legado.app.ui.book.read.api.ApiConfigManager
import io.legado.app.ui.book.read.api.ApiManageActivity
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
