package io.legado.app.help

import android.util.Log

/**
 * 段落评论信息
 */
data class ParagraphCommentInfo(
    val count: Int,           // 评论数量
    val isHotSegment: Boolean  // 是否为热评段落
)

object CommentManager {

    // chapterIndex → (paragraphIndex → ParagraphCommentInfo)
    private val commentCache =
        mutableMapOf<Int, MutableMap<Int, ParagraphCommentInfo>>()

    /**
     * 保存章节评论数据（旧接口，保持向后兼容）
     */
    fun putChapterComments(
        chapterIndex: Int,
        data: Map<String, Int>
    ) {
        val map = mutableMapOf<Int, ParagraphCommentInfo>()

        data.forEach { (k, v) ->
            // Key 可能是 "chapter_731075014_para_12"
            val parts = k.split("_para_")
            if (parts.size == 2) {
                val p = parts[1].toIntOrNull()?.plus(1)
                if (p != null) {
                    map[p] = ParagraphCommentInfo(v, false)
                }
            }
        }

        commentCache[chapterIndex] = map

        Log.d("SAVECOMMENT", "保存评论：chapter=$chapterIndex → $map")
    }

    /**
     * 保存章节评论数据（包含热评信息）
     */
    fun putChapterCommentsWithHotSegment(
        chapterIndex: Int,
        data: Map<String, Int>,
        hotSegments: Set<Int>  // 热评段落的 segmentId 集合
    ) {
        val map = mutableMapOf<Int, ParagraphCommentInfo>()

        data.forEach { (k, v) ->
            // Key 可能是 "chapter_731075014_para_12"
            val parts = k.split("_para_")
            if (parts.size == 2) {
                val p = parts[1].toIntOrNull()?.plus(1)
                if (p != null) {
                    val isHot = hotSegments.contains(p - 1)  // segmentId 从 0 开始，paragraphIndex 从 1 开始
                    map[p] = ParagraphCommentInfo(v, isHot)
                }
            }
        }

        commentCache[chapterIndex] = map

        Log.d("SAVECOMMENT", "保存评论（含热评）：chapter=$chapterIndex, 热评段落数=${hotSegments.size} → $map")
    }

    fun getCommentCountForParagraph(
        chapterIndex: Int,
        paragraphIndex: Int
    ): Int {
        return commentCache[chapterIndex]?.get(paragraphIndex)?.count ?: 0
    }

    /**
     * 获取段落评论信息（包含热评状态）
     */
    fun getCommentInfoForParagraph(
        chapterIndex: Int,
        paragraphIndex: Int
    ): ParagraphCommentInfo {
        return commentCache[chapterIndex]?.get(paragraphIndex) ?: ParagraphCommentInfo(0, false)
    }

    /**
     * 判断段落是否为热评
     */
    fun isHotSegment(
        chapterIndex: Int,
        paragraphIndex: Int
    ): Boolean {
        return commentCache[chapterIndex]?.get(paragraphIndex)?.isHotSegment ?: false
    }
}
