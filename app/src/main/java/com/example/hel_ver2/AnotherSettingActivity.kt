package com.example.hel_ver2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AnotherSettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_another_setting)

        val backToMainButton: Button = findViewById(R.id.button_back_to_main)
        backToMainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val goToLocationPageButton: Button = findViewById(R.id.button_go_to_location_page)
        goToLocationPageButton.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }

        val goToTextActivityButton: Button = findViewById(R.id.button_go_to_text_activity)
        goToTextActivityButton.setOnClickListener {
            val intent = Intent(this, TextActivity::class.java)
            startActivity(intent)
        }
    }
}
