package io.legado.app.ui.book.read.api

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        binding.tvTitle.text = if (config == null) {
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
        binding.btnSave.setOnClickListener {
            saveConfig()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
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
