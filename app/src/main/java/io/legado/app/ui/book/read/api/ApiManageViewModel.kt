package io.legado.app.ui.book.read.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
