package com.kumab2221.bluetoothchecker

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import android.bluetooth.le.ScanCallback




class DeviceListActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    internal class DeviceListAdapter(activity: Activity) : BaseAdapter() {
        private val mDeviceList: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
        private val mInflator: LayoutInflater = activity.layoutInflater

        fun addDevice(device: BluetoothDevice) {
            if (!mDeviceList.contains(device)) {
                mDeviceList.add(device)
                notifyDataSetChanged()
            }
        }

        fun clear() {
            mDeviceList.clear()
            notifyDataSetChanged() // ListViewの更新
        }

        override fun getCount(): Int {
            return mDeviceList.size
        }

        override fun getItem(position: Int): Any {
            return mDeviceList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        internal class ViewHolder {
            var deviceName: TextView? = null
            var deviceAddress: TextView? = null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            var convertView: View? = convertView
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (null == convertView) {
                convertView = mInflator.inflate(R.layout.listitem_device, parent, false)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress =
                    convertView.findViewById(R.id.textview_deviceaddress) as TextView
                viewHolder.deviceName =
                    convertView.findViewById(R.id.textview_devicename) as TextView
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            val device: BluetoothDevice = mDeviceList[position]
            val deviceName: String ?= device.name
            if (deviceName != null && deviceName.isNotEmpty()) {
                viewHolder.deviceName?.text = deviceName
            } else {
                viewHolder.deviceName?.setText(R.string.unknown_device)
            }
            viewHolder.deviceAddress?.text = device.address
            return convertView
        }



    }

    companion object {
        private const val REQUEST_ENABLE_BLUETOOTH = 1
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private var mBluetoothAdapter  : BluetoothAdapter? = null
        private var mDeviceListAdapter : DeviceListAdapter? = null
        private var mScanning : Boolean = false

    }

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                runOnUiThread {
                    if (device != null) {
                        mDeviceListAdapter?.addDevice(device)
                    }
                }
                return
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                runOnUiThread {
                    mScanning = false
                    // メニューの更新
                    invalidateOptionsMenu()
                }
                return
            }
        }
    }
/*
    // デバイススキャンコールバック
    private val mLeScanCallback: ScanCallback = object : ScanCallback() {
        // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread { mDeviceListAdapter!!.addDevice(result.getDevice()) }
        }

        // スキャンに失敗
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }
 */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        setResult(Activity.RESULT_CANCELED)
        mDeviceListAdapter = DeviceListAdapter(this)
        val listView = findViewById<View>(R.id.device_list) as ListView
        listView.adapter = mDeviceListAdapter
        listView.onItemClickListener = this

        val manager: BluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = manager.adapter

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_is_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val device = mDeviceListAdapter?.getItem(position) as BluetoothDevice
        if(device==null)
            return
        device.createBond()
        val intent = Intent()
        intent.putExtra(EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, device.address)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun requestBtFeature() {
        if(mBluetoothAdapter?.isEnabled == true)
            return

        val enableBt = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBt, REQUEST_ENABLE_BLUETOOTH)
    }

    private fun startScan(){
        mDeviceListAdapter?.clear()
        mScanning = true

        if(mBluetoothAdapter?.isDiscovering == true){
            mBluetoothAdapter?.cancelDiscovery();
        }

        val pairedDevices: Set<BluetoothDevice>? = mBluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            runOnUiThread {
                if (device != null) {
                    mDeviceListAdapter?.addDevice(device)
                }
            }
        }

        mBluetoothAdapter?.startDiscovery()
        invalidateOptionsMenu()
    }

    private fun stopScan()
    {
        mBluetoothAdapter?.cancelDiscovery()
    }

    override fun onResume() {
        super.onResume()
        requestBtFeature()

        registerReceiver(mBroadcastReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        registerReceiver(mBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        val ret = startScan()
    }

    override fun onPause() {
        super.onPause()
        stopScan()
        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_ENABLE_BLUETOOTH -> {
                if(resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_device_list, menu)
        if(!mScanning){
            menu?.findItem(R.id.menuitem_stop)?.isVisible = true
            menu?.findItem(R.id.menuitem_scan)?.isVisible = false
            menu?.findItem(R.id.menuitem_progress)?.actionView = null
        }else{
            menu?.findItem(R.id.menuitem_stop)?.isVisible = false
            menu?.findItem(R.id.menuitem_scan)?.isVisible = true
            menu?.findItem(R.id.menuitem_progress)?.setActionView(R.layout.actionbar_indeterminate_progress)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menuitem_stop ->
                startScan()
            R.id.menuitem_scan ->
                stopScan()
        }
        return true
    }
}