package io.legado.app.help

import android.util.Log

object CommentManager {

    // chapterIndex → (paragraphIndex → count)
    private val commentCache =
        mutableMapOf<Int, MutableMap<Int, Int>>()

    fun putChapterComments(
        chapterIndex: Int,
        data: Map<String, Int>
    ) {
        val map = mutableMapOf<Int, Int>()

        data.forEach { (k, v) ->
            // Key 可能是 "chapter_731075014_para_12"
            val parts = k.split("_para_")
            if (parts.size == 2) {
                val p = parts[1].toIntOrNull()?.plus(1)
                if (p != null) {
                    map[p] = v
                }
            }
        }

        commentCache[chapterIndex] = map

        Log.d("SAVECOMMENT", "保存评论：chapter=$chapterIndex → $map")
    }

    fun getCommentCountForParagraph(
        chapterIndex: Int,
        paragraphIndex: Int
    ): Int {
        return commentCache[chapterIndex]?.get(paragraphIndex) ?: 0
    }
}
