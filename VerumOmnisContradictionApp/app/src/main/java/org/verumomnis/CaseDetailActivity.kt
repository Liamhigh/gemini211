package org.verumomnis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.verumomnis.databinding.ActivityCaseDetailBinding
import org.verumomnis.engine.ContradictionEngine
import org.verumomnis.utils.FileUtils
import org.verumomnis.utils.OcrUtils

class CaseDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCaseDetailBinding
    private val allEvidence = StringBuilder()
    private var caseId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        caseId = intent.getStringExtra("caseId") ?: ""

        binding.btnAddText.setOnClickListener {
            val text = binding.editTextNote.text.toString()
            if (text.isNotEmpty()) {
                allEvidence.append(text).append("\n")
                binding.editTextNote.text.clear()
            }
        }

        binding.btnAddImage.setOnClickListener {
            OcrUtils.pickImage(this) { extractedText ->
                allEvidence.append(extractedText).append("\n")
            }
        }

        binding.btnGenerateReport.setOnClickListener {
            lifecycleScope.launch {
                val engine = ContradictionEngine()
                engine.ingest(allEvidence.toString())
                val results = engine.analyze()
                val report = engine.buildReport(results)
                FileUtils.saveReport(this@CaseDetailActivity, caseId, report)
                val intent = Intent(this@CaseDetailActivity, ReportViewerActivity::class.java)
                intent.putExtra("caseId", caseId)
                startActivity(intent)
            }
        }
    }
}
