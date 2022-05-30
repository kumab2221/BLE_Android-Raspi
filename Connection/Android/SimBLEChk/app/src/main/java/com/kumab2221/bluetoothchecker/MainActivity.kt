package com.kumab2221.bluetoothchecker

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile

import android.bluetooth.BluetoothGattCallback
import java.util.*
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import android.widget.CheckBox
import android.speech.tts.TextToSpeech
import java.nio.ByteBuffer
import android.bluetooth.BluetoothGattDescriptor as BluetoothGattDescriptor1


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {

        // 定数（Bluetooth LE Gatt UUID）
        // Private Service
        private val UUID_SERVICE_PRIVATE: UUID = UUID.fromString("13A28130-8883-49A8-8BDB-42BC1A7107F4")
        private val UUID_CHARACTERISTIC_PRIVATE1: UUID = UUID.fromString("A2935077-201F-44EB-82E8-10CC02AD8CE1")
        private val UUID_CHARACTERISTIC_PRIVATE2: UUID = UUID.fromString("A2935077-201F-44EB-82E8-10CC02AD8CE2")

        // for Notification
        private val UUID_NOTIFY1 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val UUID_NOTIFY2 = UUID.fromString("00002902-0000-1000-8000-00805f9b34fc")

        private const val REQUEST_ENABLE_BLUETOOTH: Int = 1
        private const val REQUEST_CONNECT_DEVICE: Int = 2
        private const val PERMISSIONS_REQUEST_CODE: Int = 100

        private var strDeviceName : String = ""
        private var mDeviceAddress : String = ""

        private var mBluetoothAdapter : BluetoothAdapter? = null
        private var mBluetoothGatt : BluetoothGatt? = null

        private var mButtonConnect : Button? = null
        private var mButtonDisConnect : Button? = null

        private var mButtonReadChar1 : Button? = null
        private var mButtonReadChar2 : Button? = null
        private var mCheckBoxNotifyChara1 : CheckBox? = null

        private var TAG : String = "MainActivity"

        private var textToSpeech: TextToSpeech? = null

        private var flg : Boolean = false
    }

    // BluetoothGattコールバック
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        // 接続状態変更（connectGatt()の結果として呼ばれる。）
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return
            }
            if (BluetoothProfile.STATE_CONNECTED == newState) {    // 接続完了
                mBluetoothGatt?.discoverServices()    // サービス検索
                runOnUiThread { // GUIアイテムの有効無効の設定
                    // 切断ボタンを有効にする
                    mButtonDisConnect?.isEnabled = true
                }
                return
            }
            if (BluetoothProfile.STATE_DISCONNECTED == newState) {    // 切断完了（接続可能範囲から外れて切断された）
                // 接続可能範囲に入ったら自動接続するために、mBluetoothGatt.connect()を呼び出す。
                mBluetoothGatt!!.connect()
                runOnUiThread { // GUIアイテムの有効無効の設定
                    // 読み込みボタンを無効にする（通知チェックボックスはチェック状態を維持。通知ONで切断した場合、再接続時に通知は再開するので）
                    mButtonReadChar1?.isEnabled = false
                    mButtonReadChar2?.isEnabled = false
                    mCheckBoxNotifyChara1?.isEnabled = false
                }
                return
            }
        }

        // サービス検索が完了したときの処理（mBluetoothGatt.discoverServices()の結果として呼ばれる。）
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return
            }

            // 発見されたサービスのループ
            for (service in gatt.services) {
                // サービスごとに個別の処理
                if (null == service || null == service.uuid) {
                    continue
                }
                if (UUID_SERVICE_PRIVATE == service.uuid) {    // プライベートサービス
                    runOnUiThread { // GUIアイテムの有効無効の設定
                        mButtonReadChar1?.isEnabled = true
                        mButtonReadChar2?.isEnabled = true
                        mCheckBoxNotifyChara1?.isEnabled = true
                    }
                    continue
                }
            }
        }

        // キャラクタリスティックが読み込まれたときの処理
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if( BluetoothGatt.GATT_SUCCESS != status )
            {
                return
            }
            // キャラクタリスティックごとに個別の処理
            if (UUID_CHARACTERISTIC_PRIVATE1==characteristic.uuid) {

                val str = characteristic.getStringValue( 0 );
                runOnUiThread { // GUIアイテムへの反映
                    findViewById<TextView>(R.id.textview_readchara1).text = str
                }
                return
            }
            if (UUID_CHARACTERISTIC_PRIVATE2==characteristic.uuid) {    // キャラクタリスティック１：データサイズは、2バイト（数値を想定。0～65,535）
                //Log.d(TAG, String(characteristic.value))
                val str = characteristic.getStringValue( 0 );
                runOnUiThread { // GUIアイテムへの反映
                    findViewById<TextView>(R.id.textview_readchara2).text = str
                    startSpeak(str)
                }
                return
            }
        }
        // キャラクタリスティック変更が通知されたときの処理
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        ) {
            //super.onCharacteristicChanged(gatt, characteristic)

            // キャラクタリスティックごとに個別の処理
            if (UUID_CHARACTERISTIC_PRIVATE1==characteristic.uuid) {
                val str = characteristic.getStringValue( 0 );
                runOnUiThread { // GUIアイテムへの反映
                    findViewById<TextView>(R.id.textview_readchara1).text = str
                }
                return
            }
            if (UUID_CHARACTERISTIC_PRIVATE2==characteristic.uuid) {
                //Log.d(TAG, String(characteristic.value))
                val str = characteristic.getStringValue( 0 );
                runOnUiThread { // GUIアイテムへの反映
                    findViewById<TextView>(R.id.textview_readchara2).text = str
                    startSpeak(str)
                }
                return
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor1?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if(flg){
                return
            }
            flg = true

            if (null == mBluetoothGatt) {
                return
            }
            val bleChar =
                gatt!!.getService(UUID_SERVICE_PRIVATE).getCharacteristic(UUID_CHARACTERISTIC_PRIVATE2)
            gatt!!.setCharacteristicNotification(bleChar, true)
            val descriptor = bleChar.getDescriptor(UUID_NOTIFY1).apply {
                value = BluetoothGattDescriptor1.ENABLE_NOTIFICATION_VALUE
            }
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //BLE対応端末かどうかを調べる。対応していない場合はメッセージを出して終了
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        val manager : BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = manager.adapter

        if(mBluetoothAdapter == null){
            Toast.makeText(this,R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE)
        }

        mButtonConnect = findViewById<Button>(R.id.button_connect)
        mButtonConnect?.setOnClickListener{
            mButtonConnect?.isEnabled = false  // 接続ボタンの無効化（連打対策）
            connect()
            return@setOnClickListener
        }

        mButtonDisConnect = findViewById<Button>(R.id.button_disconnect)
        mButtonDisConnect?.setOnClickListener{
            mButtonDisConnect?.isEnabled = false
            disconnect()
            return@setOnClickListener
        }

        mButtonReadChar1 = findViewById(R.id.button_readchara1)
        mButtonReadChar1?.setOnClickListener {
            readCharacteristic( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE1 )
            return@setOnClickListener
        }

        mButtonReadChar2 = findViewById(R.id.button_readchara2)
        mButtonReadChar2?.setOnClickListener {
            readCharacteristic( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE2 );
            return@setOnClickListener
        }

        mCheckBoxNotifyChara1 = findViewById<CheckBox>( R.id.checkbox_notifychara1 );
        mCheckBoxNotifyChara1?.setOnClickListener{

            setCharacteristicNotification( UUID_SERVICE_PRIVATE, UUID_CHARACTERISTIC_PRIVATE1, UUID_NOTIFY1, mCheckBoxNotifyChara1!!.isChecked);

            return@setOnClickListener
        }
        textToSpeech = TextToSpeech(this, this)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        return
    }

    override fun onResume() {
        super.onResume()
        // Android端末のBluetooth機能の有効化要求
        requestBtFeature()
        // GUIアイテムの有効無効の設定
        mButtonConnect?.isEnabled = false
        mButtonDisConnect?.isEnabled = false
        mButtonReadChar1?.isEnabled = false
        mButtonReadChar2?.isEnabled = false

        mCheckBoxNotifyChara1?.isChecked = false
        mCheckBoxNotifyChara1?.isEnabled = false

        // デバイスアドレスが空でなければ、接続ボタンを有効にする。
        if(mDeviceAddress != "")
        {
            mButtonConnect?.isEnabled = true
        }
        // 接続ボタンを押す
        mButtonConnect?.callOnClick();
    }

    private fun requestBtFeature() {
        if(mBluetoothAdapter?.isEnabled == true) {
            return
        }

        val enableBt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBt, REQUEST_ENABLE_BLUETOOTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_ENABLE_BLUETOOTH ->{
                if(resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
            REQUEST_CONNECT_DEVICE -> {

                if(resultCode == Activity.RESULT_OK){
                    if (data != null) {
                        strDeviceName = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_NAME).toString()
                        mDeviceAddress = data.getStringExtra(DeviceListActivity.EXTRAS_DEVICE_ADDRESS).toString()
                    }else{
                        strDeviceName = ""
                        mDeviceAddress = ""
                    }
                }
                val deviceName = findViewById<TextView>(R.id.textview_devicename)
                deviceName.text = strDeviceName
                val deviceAddress = findViewById<TextView>(R.id.textview_deviceaddress)
                deviceAddress.text = mDeviceAddress
                val readChara1 = findViewById<TextView>(R.id.textview_readchara1 )
                readChara1.text = ""
                val readChara2  = findViewById<TextView>(R.id.textview_readchara2 )
                readChara2.text = ""
                val notifyChara1  = findViewById<TextView>(R.id.textview_notifychara1 )
                notifyChara1.text = ""
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_item_search -> {
                val deviceListActivityIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(deviceListActivityIntent, REQUEST_CONNECT_DEVICE)
                return true
            }
        }
        return false
    }

    // 接続
    private fun connect() {
        if (mDeviceAddress == "") {    // DeviceAddressが空の場合は処理しない
            return
        }
        if (null != mBluetoothGatt) {    // mBluetoothGattがnullでないなら接続済みか、接続中。
            return
        }

        // 接続
        val device: BluetoothDevice? = mBluetoothAdapter?.getRemoteDevice(mDeviceAddress)
        mBluetoothGatt = device?.connectGatt(this, false, mGattCallback)
    }

    // 切断
    private fun disconnect() {
        if (null == mBluetoothGatt) {
            return
        }

        // 切断
        //   mBluetoothGatt.disconnect()ではなく、mBluetoothGatt.close()しオブジェクトを解放する。
        //   理由：「ユーザーの意思による切断」と「接続範囲から外れた切断」を区別するため。
        //   ①「ユーザーの意思による切断」は、mBluetoothGattオブジェクトを解放する。再接続は、オブジェクト構築から。
        //   ②「接続可能範囲から外れた切断」は、内部処理でmBluetoothGatt.disconnect()処理が実施される。
        //     切断時のコールバックでmBluetoothGatt.connect()を呼んでおくと、接続可能範囲に入ったら自動接続する。
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
        // GUIアイテムの有効無効の設定
        // 接続ボタンのみ有効にする
        mButtonConnect?.isEnabled = true
        mButtonDisConnect?.isEnabled = false
        mButtonReadChar1?.isEnabled = false
        mButtonReadChar2?.isEnabled = false

        mCheckBoxNotifyChara1?.isChecked = false;
        mCheckBoxNotifyChara1?.isEnabled = false;
    }

    // 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
    override fun onPause() {
        super.onPause()

        // 切断
        disconnect()
    }

    // アクティビティの終了直前
    override fun onDestroy() {
        super.onDestroy()
        if (null != mBluetoothGatt) {
            mBluetoothGatt!!.close()
            mBluetoothGatt = null
        }

        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun readCharacteristic(uuid_service: UUID, uuid_characteristic: UUID) {
        if (null == mBluetoothGatt) {
            return
        }
        val blechar = mBluetoothGatt!!.getService(uuid_service).getCharacteristic(uuid_characteristic)
        mBluetoothGatt!!.readCharacteristic(blechar)
    }

    // キャラクタリスティック通知の設定
    private fun setCharacteristicNotification(
        uuid_service: UUID,
        uuid_characteristic: UUID,
        uuid_notify: UUID,
        enable: Boolean
    ) {
        if (null == mBluetoothGatt) {
            return
        }
        val bleChar =
            mBluetoothGatt!!.getService(uuid_service).getCharacteristic(uuid_characteristic)
        mBluetoothGatt!!.setCharacteristicNotification(bleChar, enable)
        val descriptor = bleChar.getDescriptor(uuid_notify)
        descriptor.value = BluetoothGattDescriptor1.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt!!.writeDescriptor(descriptor)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val locale = Locale.JAPAN
                if (tts.isLanguageAvailable(locale) > TextToSpeech.LANG_AVAILABLE) {
                    tts.language = Locale.JAPAN
                } else {
                    // 言語の設定に失敗
                }
            }
        } else {
            // Tts init 失敗
        }
    }

    private fun startSpeak(text: String){
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
    }

}
