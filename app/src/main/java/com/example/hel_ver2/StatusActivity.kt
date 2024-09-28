//package com.example.hel_ver2
//
//import android.os.Bundle
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//
//class StatusActivity : AppCompatActivity() {
//
//    private lateinit var textViewSensorData: TextView
//
//    private lateinit var imageViewLedHRa: ImageView
//    private lateinit var imageViewLedHRb: ImageView
//    private lateinit var imageViewLedSWa: ImageView
//    private lateinit var imageViewLedSWb: ImageView
//    private lateinit var imageViewLedSWc: ImageView
//    private lateinit var imageViewLedSWd: ImageView
//    private lateinit var imageViewLedSWe: ImageView
//    private lateinit var imageViewLedSWf: ImageView
//    private lateinit var imageViewNewLed: ImageView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_status)
//
//        textViewSensorData = findViewById(R.id.textViewSensorData)
//
//        imageViewLedHRa = findViewById(R.id.imageViewLedHRa)
//        imageViewLedHRb = findViewById(R.id.imageViewLedHRb)
//        imageViewLedSWa = findViewById(R.id.imageViewLedSWa)
//        imageViewLedSWb = findViewById(R.id.imageViewLedSWb)
//        imageViewLedSWc = findViewById(R.id.imageViewLedSWc)
//        imageViewLedSWd = findViewById(R.id.imageViewLedSWd)
//        imageViewLedSWe = findViewById(R.id.imageViewLedSWe)
//        imageViewLedSWf = findViewById(R.id.imageViewLedSWf)
//        imageViewNewLed = findViewById(R.id.imageViewNewLed)
//
//        // SettingsActivity로부터 수신된 데이터를 가져옴
//        val receivedData = intent.getStringExtra("received_data")
//
//        // 데이터를 화면에 표시
//        if (receivedData != null) {
//            displaySensorData(receivedData)
//        } else {
//            textViewSensorData.text = "수신된 데이터가 없습니다."
//        }
//    }
//
//    private fun displaySensorData(data: String) {
//        if (data.length >= 16) {  // 16 바이트 길이를 확인
//            val HRa = data.substring(0, 2).toIntOrNull() ?: 0
//            val HRb = data.substring(2, 4).toIntOrNull() ?: 0
//            val SWa = data.substring(4, 6).toIntOrNull() ?: 0
//            val SWb = data.substring(6, 8).toIntOrNull() ?: 0
//            val SWc = data.substring(8, 10).toIntOrNull() ?: 0
//            val SWd = data.substring(10, 12).toIntOrNull() ?: 0
//            val SWe = data.substring(12, 14).toIntOrNull() ?: 0
//            val SWf = data.substring(14, 16).toIntOrNull() ?: 0
//
//            val sensorDataText = """
//                HRa (초음파 A): $HRa cm
//                HRb (초음파 B): $HRb cm
//                SWa (진동 A): ${if (SWa == 1) "ON" else "OFF"}
//                SWb (진동 B): ${if (SWb == 1) "ON" else "OFF"}
//                SWc (진동 C): ${if (SWc == 1) "ON" else "OFF"}
//                SWd (진동 D): ${if (SWd == 1) "ON" else "OFF"}
//                SWe (진동 E): ${if (SWe == 1) "ON" else "OFF"} // 여기가 조도?
//                SWf (조도 센서): ${if (SWf == 1) "ON" else "OFF"}
//            """.trimIndent()
//
//            textViewSensorData.text = sensorDataText
//
//            // 각 센서에 대한 LED 상태 업데이트
//            updateLedState(imageViewLedHRa, HRa)
//            updateLedState(imageViewLedHRb, HRb)
//            updateLedState(imageViewLedSWa, SWa)
//            updateLedState(imageViewLedSWb, SWb)
//            updateLedState(imageViewLedSWc, SWc)
//            updateLedState(imageViewLedSWd, SWd)
//            updateLedState(imageViewLedSWe, SWe)
//            updateLedState(imageViewLedSWf, SWf)
//
//            // 새로운 LED 상태 업데이트
//            updateNewLedState(HRa, HRb, SWa, SWb, SWc, SWd, SWe)
//        } else {
//            textViewSensorData.text = "잘못된 데이터 형식입니다."
//        }
//    }
//
//    private fun updateLedState(imageView: ImageView, sensorValue: Int) {
//        if (sensorValue == 0) {
//            imageView.setImageResource(R.drawable.led_off)
//        } else {
//            imageView.setImageResource(R.drawable.led_on)
//        }
//    }
//
//    private fun updateNewLedState(HRa: Int, HRb: Int, SWa: Int, SWb: Int, SWc: Int, SWd: Int, SWe: Int) {
//        val vibrationSensorsOn = listOf(SWa, SWb, SWc, SWd, SWe).count { it == 1 }
//        when {
//            HRa == 1 && SWe == 1 -> imageViewNewLed.setImageResource(R.drawable.led_green)
//            vibrationSensorsOn == 2 -> imageViewNewLed.setImageResource(R.drawable.led_yellow)
//            vibrationSensorsOn == 3 -> imageViewNewLed.setImageResource(R.drawable.led_orange)
//            vibrationSensorsOn >= 4 -> imageViewNewLed.setImageResource(R.drawable.led_red)
//            else -> imageViewNewLed.setImageResource(R.drawable.led_off)
//        }
//    }
//}
