package org.verumomnis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.verumomnis.databinding.ActivityReportViewerBinding
import org.verumomnis.utils.FileUtils

class ReportViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val caseId = intent.getStringExtra("caseId") ?: ""
        val report = FileUtils.loadReport(this, caseId)
        binding.textReport.text = report
    }
}
