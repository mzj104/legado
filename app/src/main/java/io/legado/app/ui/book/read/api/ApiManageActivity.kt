package io.legado.app.ui.book.read.api

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
import io.legado.app.utils.toastOnUi
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
            negativeButton()
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
