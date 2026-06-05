package io.legado.app.help

import android.util.Log
import io.legado.app.help.QdCat
import kotlin.collections.get

object Simicatalog {
    val idmap = mutableMapOf<Int, String>()

    private var lastLocalIndex: Int? = null
    private var lastQdIndex: Int? = null

    data class Candidate(
        val sim: Double,
        val cid: String,
        val title: String,
        val index: Int
    )

    fun get_qdchapter_id(title: String, index: Int): String? {
        if (title == "emptyTextChapter") return ""

        idmap[index]?.let { return it }

        val qdcat = QdCat.getQdcatalog()
        if (qdcat.length() == 0) {
            Log.w("BESTMATCH", "起点目录为空")
            return null
        }

        val cleanedTitle = removeChapterTitle(keepAfterFirstSpace(title))
        Log.d("BESTMATCH", "$title $cleanedTitle")

        var bestSim = -1.0
        var bestCid: String? = null
        var bestTitle = ""
        var bestQdIndex = -1

        val highSimCandidates = mutableListOf<Candidate>()

        for (i in 0 until qdcat.length()) {
            val item = qdcat.getJSONObject(i)

            var t1 = item.getString("text")
            t1 = removeChapterTitle(keepAfterFirstSpace(t1))

            if (t1.isBlank()) continue

            val sim = jaroWinklerSimilarity(t1, cleanedTitle)
            val cid = item.getString("href")

            if (sim > bestSim) {
                bestSim = sim
                bestCid = cid
                bestTitle = t1
                bestQdIndex = i
            }

            if (sim > 0.999) {
                highSimCandidates.add(
                    Candidate(
                        sim = sim,
                        cid = cid,
                        title = t1,
                        index = i
                    )
                )
            }
        }

        if (highSimCandidates.isNotEmpty()) {
            val bestCandidate = chooseBestCandidateByLastOffset(
                candidates = highSimCandidates,
                localIndex = index
            )

            if (bestCandidate != null) {
                bestSim = bestCandidate.sim
                bestCid = bestCandidate.cid
                bestTitle = bestCandidate.title
                bestQdIndex = bestCandidate.index
            }
        }

        Log.d(
            "BESTMATCH",
            "Local='$cleanedTitle' localIndex=$index → Qidian='$bestTitle' qdIndex=$bestQdIndex sim=$bestSim"
        )

        if (!bestCid.isNullOrBlank()) {
            idmap[index] = bestCid

            if (bestQdIndex >= 0) {
                lastLocalIndex = index
                lastQdIndex = bestQdIndex
            }
        }

        return bestCid
    }

    private fun chooseBestCandidateByLastOffset(
        candidates: List<Candidate>,
        localIndex: Int
    ): Candidate? {
        if (candidates.isEmpty()) return null

        val lastLocal = lastLocalIndex
        val lastQd = lastQdIndex

        return if (lastLocal != null && lastQd != null) {
            val offset = lastQd - lastLocal
            val predictedQdIndex = localIndex + offset

            Log.d(
                "BESTMATCH",
                "使用历史偏移匹配: lastLocal=$lastLocal lastQd=$lastQd offset=$offset predictedQdIndex=$predictedQdIndex"
            )

            candidates.minByOrNull {
                kotlin.math.abs(it.index - predictedQdIndex)
            }
        } else {
            Log.d("BESTMATCH", "无历史偏移，使用原始 index 接近逻辑")

            candidates.minByOrNull {
                kotlin.math.abs(it.index - localIndex)
            }
        }
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
        val bracketRegex = Regex("[（(【\\[][^）)】\\]]*[）)】\\]]")

        val chapterRegex = Regex(
            "^\\s*(第[\\u4e00-\\u9fa5\\d]+(章|回|卷)?|[\\u4e00-\\u9fa5\\d]+)(?=\\s+[^\\d\\s])\\s*"
        )

        return input
            .trim()
            .replace(bracketRegex, "")
            .replace(chapterRegex, "")
            .trim()
    }
}