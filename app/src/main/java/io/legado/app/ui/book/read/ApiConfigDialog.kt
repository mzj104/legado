package io.legado.app.ui.book.read

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.legado.app.R
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString

object ApiConfigDialog {

    const val PREF_API_KEY = "api_key"
    const val PREF_API_URL = "api_url"
    const val PREF_API_MODEL = "api_model"

    fun getApiKey(context: Context): String {
        return context.getPrefString(PREF_API_KEY, "") ?: ""
    }

    fun getApiUrl(context: Context): String {
        return context.getPrefString(PREF_API_URL, "") ?: ""
    }

    fun getApiModel(context: Context): String {
        return context.getPrefString(PREF_API_MODEL, "qwen3.5-flash") ?: "qwen3.5-flash"
    }

    fun show(context: Context) {
        val layout = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_api_config, null)

        val etApiKey = layout.findViewById<EditText>(R.id.et_api_key)
        val etApiUrl = layout.findViewById<EditText>(R.id.et_api_url)
        val etApiModel = layout.findViewById<EditText>(R.id.et_api_model)

        etApiKey.setText(getApiKey(context))
        etApiUrl.setText(getApiUrl(context))
        etApiModel.setText(getApiModel(context))

        AlertDialog.Builder(context)
            .setTitle(R.string.api_config)
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val apiKey = etApiKey.text?.toString()?.trim() ?: ""
                val apiUrl = etApiUrl.text?.toString()?.trim() ?: ""
                val apiModel = etApiModel.text?.toString()?.trim() ?: ""

                when {
                    apiKey.isEmpty() -> {
                        Toast.makeText(context, "请填入api key", Toast.LENGTH_SHORT).show()
                    }
                    apiModel.isEmpty() -> {
                        Toast.makeText(context, "请设置模型名", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        context.putPrefString(PREF_API_KEY, apiKey)
                        context.putPrefString(PREF_API_URL, apiUrl)
                        context.putPrefString(PREF_API_MODEL, apiModel)
                        Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
