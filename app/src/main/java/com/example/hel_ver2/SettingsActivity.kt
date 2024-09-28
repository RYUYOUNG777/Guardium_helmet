package com.example.hel_ver2

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.bluetooth.le.ScanResult
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000 // 10초간 스캔
    private val devices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var connectedDeviceLayout: LinearLayout
    private lateinit var inputText: EditText
    private lateinit var statusButton: Button

    // StatusActivity의 UI 요소 추가
    private lateinit var textViewSensorData: TextView
    private lateinit var imageViewLedHRa: ImageView
    private lateinit var imageViewLedHRb: ImageView
    private lateinit var imageViewLedSWa: ImageView
    private lateinit var imageViewLedSWb: ImageView
    private lateinit var imageViewLedSWc: ImageView
    private lateinit var imageViewLedSWd: ImageView
    private lateinit var imageViewLedSWe: ImageView
    private lateinit var imageViewLedSWf: ImageView
    private lateinit var imageViewNewLed: ImageView

    // 변수들을 클래스 멤버 변수로 선언
    private var SWa: Int = 0
    private var SWb: Int = 0
    private var SWc: Int = 0
    private var SWd: Int = 0
    private var SWe: Int = 0

    private val sendRunnable = object : Runnable {
        override fun run() {
            sendDataToBluno("S") // "S" 값을 전송
            handler.postDelayed(this, 800) // 0.8초 후에 다시 실행 0831
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private val BLUNO_SERVICE_UUID = UUID.fromString("0000dfb0-0000-1000-8000-00805f9b34fb")
        private val BLUNO_CHARACTERISTIC_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val bluetoothButton: Button = findViewById(R.id.button_bluetooth_connect)
        val backButton: Button = findViewById(R.id.button_back)
        sendButton = findViewById(R.id.button_send)
        recyclerView = findViewById(R.id.recycler_view_devices)
        connectedDeviceLayout = findViewById(R.id.connected_device_layout)
        inputText = findViewById(R.id.input_text)
        statusButton = findViewById(R.id.button_status_check)

        // StatusActivity의 UI 요소 초기화
        textViewSensorData = findViewById(R.id.textViewSensorData)
        imageViewLedHRa = findViewById(R.id.imageViewLedHRa)
        imageViewLedHRb = findViewById(R.id.imageViewLedHRb)
        imageViewLedSWa = findViewById(R.id.imageViewLedSWa)
        imageViewLedSWb = findViewById(R.id.imageViewLedSWb)
        imageViewLedSWc = findViewById(R.id.imageViewLedSWc)
        imageViewLedSWd = findViewById(R.id.imageViewLedSWd)
        imageViewLedSWe = findViewById(R.id.imageViewLedSWe)
        imageViewLedSWf = findViewById(R.id.imageViewLedSWf)
        imageViewNewLed = findViewById(R.id.imageViewNewLed)

        deviceAdapter = DeviceAdapter(devices) { device ->
            connectToDevice(device)
        }
        recyclerView.adapter = deviceAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        statusButton.visibility = View.GONE

        bluetoothButton.setOnClickListener {
            if (checkPermissions()) {
                initBluetooth()
                scanLeDevice(true)
            } else {
                requestPermissions()
            }
        }

        sendButton.setOnClickListener {
            sendDataToBluno("S")
        }

        statusButton.setOnClickListener {
            // 상태확인 버튼을 누르면 현재 수신된 데이터를 표시
            Toast.makeText(this, "상태를 확인하세요.", Toast.LENGTH_SHORT).show()
        }

        backButton.setOnClickListener {
            finish()  // 현재 액티비티를 종료하여 이전 액티비티로 돌아갑니다.
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun initBluetooth() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            } else {
                Toast.makeText(this, "블루투스 스캔 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        try {
            if (enable) {
                handler.postDelayed({
                    try {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                            bluetoothLeScanner.stopScan(leScanCallback)
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                        Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                    }
                }, SCAN_PERIOD)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.startScan(leScanCallback)
                } else {
                    Toast.makeText(this, "블루투스 스캔 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (!devices.contains(device)) {
                    devices.add(device)
                    deviceAdapter.notifyItemInserted(devices.size - 1)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                result.device?.let { device ->
                    if (!devices.contains(device)) {
                        devices.add(device)
                        deviceAdapter.notifyItemInserted(devices.size - 1)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@SettingsActivity, "스캔 실패: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
            } else {
                Toast.makeText(this, "블루투스 연결 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDataToBluno(data: String) {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "어떤 장치와도 연결되지 않았습니다", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val service = bluetoothGatt!!.getService(BLUNO_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BLUNO_CHARACTERISTIC_UUID)
            if (characteristic != null) {
                characteristic.value = data.toByteArray()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {
                    Toast.makeText(this, "블루투스 연결 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bluno 특성을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            try {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "GATT 서버에 연결되었습니다", Toast.LENGTH_SHORT).show()
                            recyclerView.visibility = View.GONE
                            connectedDeviceLayout.visibility = View.VISIBLE
                            statusButton.visibility = View.VISIBLE // 상태확인 버튼 표시
                        }
                        gatt?.discoverServices()

                        // 주기적으로 "S" 값을 보내기 시작
                        handler.post(sendRunnable)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "GATT 서버에서 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
                            bluetoothGatt?.close()
                            bluetoothGatt = null
                            recyclerView.visibility = View.VISIBLE
                            connectedDeviceLayout.visibility = View.GONE
                            statusButton.visibility = View.GONE // 상태확인 버튼 숨김

                            // 주기적인 데이터 전송 중지
                            handler.removeCallbacks(sendRunnable)
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt?.getService(BLUNO_SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(BLUNO_CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        setCharacteristicNotification(characteristic, true)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Bluno 특성을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "서비스 발견 실패: $status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            try {
                characteristic?.value?.let { value ->
                    val receivedData = String(value)
                    handleReceivedData(receivedData)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "특성 쓰기 성공", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        try {
            bluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
            if (enabled) {
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt?.writeDescriptor(descriptor)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleReceivedData(data: String) {
        if (data.length >= 16) {
            val HRa = data.substring(0, 2).toIntOrNull() ?: 0
            val HRb = data.substring(2, 4).toIntOrNull() ?: 0
            SWa = data.substring(4, 6).toIntOrNull() ?: 0
            SWb = data.substring(6, 8).toIntOrNull() ?: 0
            SWc = data.substring(8, 10).toIntOrNull() ?: 0
            SWd = data.substring(10, 12).toIntOrNull() ?: 0
            SWe = data.substring(12, 14).toIntOrNull() ?: 0
            val SWf = data.substring(14, 16).toIntOrNull() ?: 0

            val sensorDataText = """
                HRa (초음파 A): $HRa cm
                HRb (초음파 B): $HRb cm
                SWa (진동 A): ${if (SWa == 1) "ON" else "OFF"}
                SWb (진동 B): ${if (SWb == 1) "ON" else "OFF"}
                SWc (진동 C): ${if (SWc == 1) "ON" else "OFF"}
                SWd (진동 D): ${if (SWd == 1) "ON" else "OFF"}
                SWe (진동 E): ${if (SWe == 1) "ON" else "OFF"}
                SWf (조도 센서): ${if (SWf == 1) "ON" else "OFF"}
            """.trimIndent()

            textViewSensorData.text = sensorDataText

            // 각 센서에 대한 LED 상태 업데이트
            updateLedState(imageViewLedHRa, HRa)
            updateLedState(imageViewLedHRb, HRb)
            updateLedState(imageViewLedSWa, SWa)
            updateLedState(imageViewLedSWb, SWb)
            updateLedState(imageViewLedSWc, SWc)
            updateLedState(imageViewLedSWd, SWd)
            updateLedState(imageViewLedSWe, SWe)
            updateLedState(imageViewLedSWf, SWf)

            // 새로운 LED 상태 업데이트
            updateNewLedState(HRa, HRb, SWa, SWb, SWc, SWd, SWe)
        } else {
            textViewSensorData.text = "잘못된 데이터 형식입니다."
        }

        // 진동 센서 상태 확인 및 AlertActivity로 이동 조건
        val onSensors = listOf(SWa, SWb, SWc, SWd, SWe).count { it == 1 }

        // 4개 이상의 진동 센서가 ON이면 AlertActivity로 이동 0831-1
        if (onSensors >= 3) {
            runOnUiThread {
                val intent = Intent(this, AlertActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun updateLedState(imageView: ImageView, sensorValue: Int) {
        if (sensorValue == 0) {
            imageView.setImageResource(R.drawable.led_off)
        } else {
            imageView.setImageResource(R.drawable.led_on)
        }
    }

    private fun updateNewLedState(HRa: Int, HRb: Int, SWa: Int, SWb: Int, SWc: Int, SWd: Int, SWe: Int) {
        val vibrationSensorsOn = listOf(SWa, SWb, SWc, SWd, SWe).count { it == 1 }
        when {
            HRa == 1 && SWe == 1 -> imageViewNewLed.setImageResource(R.drawable.led_green)
            vibrationSensorsOn == 2 -> imageViewNewLed.setImageResource(R.drawable.led_yellow)
            vibrationSensorsOn == 3 -> imageViewNewLed.setImageResource(R.drawable.led_orange)
            vibrationSensorsOn >= 4 -> imageViewNewLed.setImageResource(R.drawable.led_red)
            else -> imageViewNewLed.setImageResource(R.drawable.led_off)
        }
    }
}
