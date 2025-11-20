package io.legado.app.help

import org.json.JSONObject

data class ReviewThread(
    val root: JSONObject,
    var replies: MutableList<JSONObject> = mutableListOf(),
    var repliesLoaded: Boolean = false   // 是否已加载子评论
)
