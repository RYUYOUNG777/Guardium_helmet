package com.example.hel_ver2

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextActivity : AppCompatActivity() {

    private lateinit var recipientsContainer: LinearLayout
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSendSMS: Button
    private lateinit var buttonAddRecipient: Button
    private lateinit var buttonRemoveRecipient: Button
    private lateinit var buttonSaveRecipients: Button
    private lateinit var buttonClearRecipients: Button
    private lateinit var locationManager: LocationManager
    private var progressDialog: AlertDialog? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val SMS_PERMISSION_CODE = 1001
        private const val LOCATION_PERMISSION_CODE = 1002
        private const val DEFAULT_MESSAGE = "가디움 헬멧 사용자가 사고를 당했습니다.\n 사용자의 현재위치( ) 빠른 조치 부탁드립니다.\n 지도 URL: https://www.google.com/maps/?hl=ko"
        private const val TAG = "TextActivity"
        private const val PREFS_NAME = "recipients_prefs"
        private const val PREF_RECIPIENTS = "recipients"
        private const val PREF_MESSAGE = "message"
        private const val LOCATION_TIMEOUT = 10000L // 10초
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text)

        // UI 요소 초기화
        recipientsContainer = findViewById(R.id.recipients_container)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSendSMS = findViewById(R.id.buttonSendSMS)
        buttonAddRecipient = findViewById(R.id.button_add_recipient)
        buttonRemoveRecipient = findViewById(R.id.button_remove_recipient)
        buttonSaveRecipients = findViewById(R.id.button_save_recipients)
        buttonClearRecipients = findViewById(R.id.button_clear_recipients)

        // 위치 서비스 초기화
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // 자동 전송 모드 확인 및 실행
        val recipients = loadRecipients()
        val message = loadMessage()
        val isAutoSendTriggered = intent.getBooleanExtra("auto_send_trigger", false)

        if (isAutoSendTriggered && recipients.isNotEmpty() && message.isNotEmpty()) {
            // 자동 전송 모드 실행
            editTextMessage.setText(message)
            recipients.forEach { addRecipientField(it) }
            onSendSMSClicked()  // 권한 확인 후 자동으로 SMS 전송
        } else {
            // 기본 메시지 설정
            editTextMessage.setText(DEFAULT_MESSAGE)
            // 버튼 이벤트 설정
            buttonAddRecipient.setOnClickListener { addRecipientField() }
            buttonRemoveRecipient.setOnClickListener { removeRecipientField() }
            buttonSendSMS.setOnClickListener { onSendSMSClicked() }
            buttonSaveRecipients.setOnClickListener { saveRecipients() }
            buttonClearRecipients.setOnClickListener { clearRecipients() }
            // 저장된 수신자 및 메시지 불러오기
            recipients.forEach { addRecipientField(it) }
            editTextMessage.setText(message)
        }
    }

    private fun addRecipientField(text: String = "") {
        val editText = EditText(this).apply {
            hint = "수신자 번호"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(text)
        }
        recipientsContainer.addView(editText)
    }

    private fun removeRecipientField() {
        val childCount = recipientsContainer.childCount
        if (childCount > 0) {
            recipientsContainer.removeViewAt(childCount - 1)
        }
    }

    private fun saveRecipients() {
        val recipients = mutableListOf<String>()
        for (i in 0 until recipientsContainer.childCount) {
            val recipientView = recipientsContainer.getChildAt(i) as EditText
            val recipient = recipientView.text.toString()
            if (recipient.isNotEmpty()) {
                recipients.add(recipient)
            }
        }

        val message = editTextMessage.text.toString()

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putStringSet(PREF_RECIPIENTS, recipients.toSet())
            putString(PREF_MESSAGE, message)
            apply()
        }

        Toast.makeText(this, "수신자 목록 및 메시지가 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun loadRecipients(): Set<String> {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getStringSet(PREF_RECIPIENTS, emptySet()) ?: emptySet()
    }

    private fun loadMessage(): String {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(PREF_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE
    }

    private fun clearRecipients() {
        recipientsContainer.removeAllViews()
        editTextMessage.setText(DEFAULT_MESSAGE)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove(PREF_RECIPIENTS)
            remove(PREF_MESSAGE)
            apply()
        }
        Toast.makeText(this, "수신자 목록 및 메시지가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun onSendSMSClicked() {
        if (checkAndRequestPermissions()) {
            // 권한이 모두 허용된 경우 위치 정보 요청
            Log.d(TAG, "모든 권한 부여됨, 위치 정보 가져오기 시작")
            requestLocation()
        } else {
            // 필요한 권한 요청
            Log.d(TAG, "필요한 권한 요청 중")
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), LOCATION_PERMISSION_CODE)
            return false
        }
        return true
    }

    private fun requestLocation() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "위치 서비스가 비활성화되어 있습니다. 활성화해주세요.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            dismissProgressDialog()
            return
        }

        try {
            // "문자 전송중" 다이얼로그 표시
            showProgressDialog()
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)

            // 타임아웃 설정
            handler.postDelayed({
                locationManager.removeUpdates(locationListener)
                Toast.makeText(this, "위치 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                dismissProgressDialog()
            }, LOCATION_TIMEOUT)

        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            dismissProgressDialog()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val locationText = "위도: ${location.latitude}, 경도: ${location.longitude}"
            Log.d(TAG, "위치 정보 수신: $locationText")
            try {
                val message = editTextMessage.text.toString().replace("( )", "($locationText)")
                // 비동기적으로 SMS 전송
                sendSMSAsync(message)
            } catch (e: Exception) {
                Log.e(TAG, "문자 메시지 생성 오류: ${e.message}")
                Toast.makeText(this@TextActivity, "문자 메시지 생성에 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                // 위치 업데이트 중지
                locationManager.removeUpdates(this)
                dismissProgressDialog()
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun sendSMSAsync(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                sendSMS(message)
            }
        }
    }

    private fun sendSMS(message: String) {
        val recipients = mutableListOf<String>()
        for (i in 0 until recipientsContainer.childCount) {
            val recipientView = recipientsContainer.getChildAt(i) as EditText
            val recipient = recipientView.text.toString()
            if (recipient.isNotEmpty()) {
                recipients.add(recipient)
            }
        }

        if (message.isBlank() || recipients.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "수신자 번호와 메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
                dismissProgressDialog()
            }
            return
        }

        val smsManager = SmsManager.getDefault()
        try {
            // 병렬 처리로 SMS 전송 속도 개선
            CoroutineScope(Dispatchers.IO).launch {
                recipients.forEach { recipient ->
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(recipient.trim(), null, parts, null, null)
                    Log.d(TAG, "Multipart SMS 전송 대상: $recipient")
                }
                runOnUiThread {
                    Toast.makeText(this@TextActivity, "메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "메시지 전송 완료")
                    dismissProgressDialog()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 실패: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "메시지 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                dismissProgressDialog()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "모든 권한 부여됨, 위치 정보 가져오기 시작")
                requestLocation()
            } else {
                Log.d(TAG, "권한 거부됨")
                Toast.makeText(this, "위치 및 SMS 전송 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("문자 전송중")
            val progressBar = ProgressBar(this)
            builder.setView(progressBar)
            builder.setCancelable(false)
            progressDialog = builder.create()
        }
        progressDialog?.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }
}
