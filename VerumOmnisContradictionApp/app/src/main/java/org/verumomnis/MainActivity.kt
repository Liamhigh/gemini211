package org.verumomnis

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.verumomnis.databinding.ActivityMainBinding
import org.verumomnis.utils.FileUtils
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateCase.setOnClickListener {
            val caseName = binding.editCaseName.text.toString()
            if (caseName.isNotEmpty()) {
                val caseId = UUID.randomUUID().toString()
                FileUtils.createCaseFolder(this, caseId, caseName)
                val intent = Intent(this, CaseDetailActivity::class.java)
                intent.putExtra("caseId", caseId)
                startActivity(intent)
            }
        }
    }
}
