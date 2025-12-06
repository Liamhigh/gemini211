package org.verumomnis.utils

import android.content.Context
import java.io.File

object FileUtils {

    fun createCaseFolder(context: Context, caseId: String, caseName: String) {
        val dir = File(context.filesDir, "cases/$caseId")
        dir.mkdirs()
        File(dir, "metadata.txt").writeText("Case Name: $caseName")
    }

    fun saveReport(context: Context, caseId: String, text: String) {
        val reportFile = File(context.filesDir, "cases/$caseId/contradictions.txt")
        reportFile.writeText(text)
    }

    fun loadReport(context: Context, caseId: String): String {
        val file = File(context.filesDir, "cases/$caseId/contradictions.txt")
        return if (file.exists()) file.readText() else "No report found."
    }
}
