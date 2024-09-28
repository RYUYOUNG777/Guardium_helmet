package com.example.hel_ver2

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class AlertActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var buttonImmediately: Button
    private var countdownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var rootLayout: ConstraintLayout

    private val handler = Handler(Looper.getMainLooper())
    private var isOriginalColor = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        timerTextView = findViewById(R.id.timerTextView)
        rootLayout = findViewById(R.id.rootLayout)
        buttonImmediately = findViewById(R.id.button_immediately)

        // 진동 시작 (새로운 방식 사용)
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE)) // 5초 동안 진동

        // 소리 시작
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        mediaPlayer?.start()

        // 배경색을 주기적으로 변경
        startBackgroundColorToggle()

        startCountdown()

        buttonImmediately.setOnClickListener {
            // 15초 대기 없이 즉시 문자를 보냄
            countdownTimer?.cancel()  // 기존 타이머 취소
            startTextActivity()  // 바로 문자 전송 액티비티 시작
        }
    }

    private fun startCountdown() {
        countdownTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                timerTextView.text = "$secondsRemaining 초 이내 입력이 없을 시 자동으로 119로 연락됩니다."
            }

            override fun onFinish() {
                timerTextView.text = "자동으로 119로 연락을 시도합니다."
                startTextActivity()
            }
        }.start()
    }

    private fun startTextActivity() {
        val intent = Intent(this, TextActivity::class.java)
        intent.putExtra("auto_send_trigger", true)  // 자동 전송 트리거 설정
        startActivity(intent)
        finish()  // 현재 액티비티를 종료하여 다시 돌아오지 않도록 합니다.
    }

    private fun startBackgroundColorToggle() {
        handler.post(object : Runnable {
            override fun run() {
                if (isOriginalColor) {
                    rootLayout.setBackgroundColor(resources.getColor(android.R.color.white, null))
                } else {
                    rootLayout.setBackgroundColor(resources.getColor(R.color.original_background_color, null))
                }
                isOriginalColor = !isOriginalColor
                handler.postDelayed(this, 1000) // 1초마다 반복
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        mediaPlayer?.release()  // MediaPlayer 자원 해제
        vibrator?.cancel()  // 진동 중지
        handler.removeCallbacksAndMessages(null)  // 핸들러의 모든 작업 중지
    }
}

/**buttonAcknowledge.setOnClickListener {
val intent = Intent(this, MapsActivity::class.java)
startActivity(intent)
}*/