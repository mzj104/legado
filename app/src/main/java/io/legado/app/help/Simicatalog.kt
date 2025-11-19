package io.legado.app.help

import android.util.Log
import io.legado.app.help.QdCat
import kotlin.collections.get

object Simicatalog {
    val idmap = mutableMapOf<Int, String>()

    fun get_qdchapter_id(title: String, index: Int): String? {
        if (title == "emptyTextChapter") return ""
        // ---------- 1. 命中缓存 ----------
        idmap[index]?.let { return it }

        val qdcat = QdCat.getQdcatalog()
        if (qdcat.length() == 0) {
            Log.w("BESTMATCH", "起点目录为空")
            return null
        }

        val cleanedTitle = removeChapterTitle(keepAfterFirstSpace(title))
        Log.d("BESTMATCH",title+" "+cleanedTitle)
        var bestSim = -1.0
        var bestCid: String? = null
        var bestTitle = ""

        // ---------- 2. 遍历起点目录 ----------
        for (i in 0 until qdcat.length()) {
            val item = qdcat.getJSONObject(i)

            var t1 = item.getString("text")
            t1 = removeChapterTitle(keepAfterFirstSpace(t1))

            if (t1.isBlank()) continue

            val sim = jaroWinklerSimilarity(t1, cleanedTitle)

            if (sim > bestSim) {
                bestSim = sim
                bestCid = item.getString("href")
                bestTitle = t1

                if (sim > 0.999) break
            }
        }

        Log.d("BESTMATCH", "Local='$cleanedTitle' → Qidian='$bestTitle' sim=$bestSim")

        // ---------- 3. 避免写入 null / 空 ----------
        if (bestCid != null && bestCid!!.isNotBlank()) {
            idmap[index] = bestCid!!
        }

        return bestCid
    }

    fun jaroWinklerSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0

        val mtp = IntArray(3)
        val max = maxOf(s1.length, s2.length)
        val matchWindow = max / 2 - 1

        val matches1 = BooleanArray(s1.length)
        val matches2 = BooleanArray(s2.length)

        for (i in s1.indices) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(i + matchWindow + 1, s2.length)
            for (j in start until end) {
                if (!matches2[j] && s1[i] == s2[j]) {
                    matches1[i] = true
                    matches2[j] = true
                    mtp[0]++
                    break
                }
            }
        }

        if (mtp[0] == 0) return 0.0

        var k = 0
        for (i in s1.indices) {
            if (matches1[i]) {
                while (!matches2[k]) k++
                if (s1[i] != s2[k]) mtp[1]++
                k++
            }
        }

        mtp[1] /= 2

        var prefix = 0
        for (i in 0 until minOf(4, minOf(s1.length, s2.length))) {
            if (s1[i] == s2[i]) prefix++ else break
        }
        mtp[2] = prefix

        val jaro = (
                mtp[0] / s1.length.toDouble() +
                        mtp[0] / s2.length.toDouble() +
                        (mtp[0] - mtp[1]) / mtp[0].toDouble()
                ) / 3

        return jaro + (mtp[2] * 0.1 * (1 - jaro))
    }

    fun keepAfterFirstSpace(text: String): String {
        val idx = text.indexOf(' ')
        return if (idx != -1 && idx < text.length - 1) {
            text.substring(idx + 1)
        } else {
            text
        }
    }

    fun removeChapterTitle(input: String): String {
        val regex = Regex(
            "^\\s*(第[\\u4e00-\\u9fa5\\d]+(章|回|卷)?|[\\u4e00-\\u9fa5\\d]+)(?=\\s+[^\\d\\s])\\s*"
        )
        return input.trim().replace(regex, "")
    }
}