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
