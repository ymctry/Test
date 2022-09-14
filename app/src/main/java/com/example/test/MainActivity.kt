package com.example.test

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.os.Build
import android.os.Bundle
import android.os.PatternMatcher
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.test.wifi.IWifiConnectListener
import com.example.test.wifi.WifiManagerProxy
import org.altbeacon.beacon.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var beaconManager: BeaconManager

    private lateinit var text: TextView

    lateinit var beaconReferenceApplication: MyApplication
    var neverAskAgainPermissions = ArrayList<String>()


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        beaconReferenceApplication = application as MyApplication
        //2.把Activity移除,资源并没有回收.
        //finish()

        //3.启动Service
        val service = Intent(this, ServiceCrack::class.java)
        startService(service)
        Log.d("auto_xxx", "启动ServiceCrack服务....")

        checkPermissions()

        test()

        //text = this.findViewById<View>(R.id.text) as TextView

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持低功耗蓝牙", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "支持低功耗蓝牙", Toast.LENGTH_SHORT).show()
        }
        val regionViewModel = BeaconManager.getInstanceForApplication(this)
            .getRegionViewModel(beaconReferenceApplication.region)
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        //regionViewModel.regionState.observe(this, monitoringObserver)
        // observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
        //regionViewModel.rangedBeacons.observe(this, rangingObserver)

        /*beaconManager = BeaconManager.getInstanceForApplication(this)
        val region = Region("all-beacons-region", null, null, null)
        // Set up a Live Data observer so this Activity can get monitoring callbacks
        // observer will be called each time the monitored regionState changes (inside vs. outside region)
        beaconManager.getRegionViewModel(region).regionState.observe(this, monitoringObserver)
        beaconManager.startMonitoring(region)

        beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
        beaconManager.startRangingBeacons(region)*/


        /*Timer().schedule(object : TimerTask() {
            override fun run() {
                //需要执行的任务
                //Log.d("auto_xxx", "MainActivity 2秒执行一次")
                val regionState = regionViewModel.regionState
                if (regionState.value == MonitorNotifier.INSIDE) {
                    Log.d("auto_xxx", "Detected beacons(s)")
                } else {
                    Log.d("auto_xxx", "Stopped detecteing beacons${regionState.value}")
                }
                val rangedBeacons = regionViewModel.rangedBeacons
                Log.d("auto_xxx", "Ranged: ${rangedBeacons.value!!.size} beacon")

                rangedBeacons.value!!.forEach { beacon ->
                    Log.d("auto_xxx", "$beacon about ${beacon.distance} meters away")
                    Log.d(
                        "auto_xxx",
                        "${beacon.id1}\nid2: ${beacon.id2} id3:  rssi: ${beacon.rssi}\nest. distance: ${beacon.distance} m"
                    )
                    *//*runOnUiThread {
                        text.text =
                            "${text.text}\n${beacon.id1}\\nid2: ${beacon.id2} id3:  rssi: ${beacon.rssi}\\nest. distance: ${beacon.distance} m\""

                    }*//*
                }
            }
        }, 2000, 2000)*/

    }

    private val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d("auto_xxx", "Detected beacons(s)")
            text.text = "${text.text}\nDetected beacons(s)\n"
        } else {
            Log.d("auto_xxx", "Stopped detecteing beacons")
        }
    }
    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d("auto_xxx", "Ranged: ${beacons.count()} beacons")
        if (beacons.isEmpty())
            text.text = "搜索中。。。"
        for (beacon: Beacon in beacons) {
            Log.d("auto_xxx", "$beacon about ${beacon.distance} meters away")

            text.text =
                "${text.text}\n${beacon.id1}\\nid2: ${beacon.id2} id3:  rssi: ${beacon.rssi}\\nest. distance: ${beacon.distance} m\""
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun checkPermissions() {
        // basepermissions are for M and higher
        var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        var permissionRationale =
            "This app needs fine location permission to detect beacons.  Please grant this now."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN
            )
            permissionRationale =
                "This app needs fine location permission, and bluetooth scan permission to detect beacons.  Please grant all of these now."
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                permissionRationale =
                    "This app needs fine location permission to detect beacons.  Please grant this now."
            } else {
                permissions = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                permissionRationale =
                    "This app needs background location permission to detect beacons in the background.  Please grant this now."
            }
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            permissionRationale =
                "This app needs both fine location permission and background location permission to detect beacons in the background.  Please grant both now."
        }
        var allGranted = true
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) allGranted =
                false;
        }
        if (!allGranted) {
            if (neverAskAgainPermissions.count() == 0) {
                val builder =
                    AlertDialog.Builder(this)
                builder.setTitle("This app needs permissions to detect beacons")
                builder.setMessage(permissionRationale)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    requestPermissions(
                        permissions,
                        PERMISSION_REQUEST_FINE_LOCATION
                    )
                }
                builder.show()
            } else {
                val builder =
                    AlertDialog.Builder(this)
                builder.setTitle("Functionality limited")
                builder.setMessage("Since location and device permissions have not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location and device discovery permissions to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener { }
                builder.show()
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        val builder =
                            AlertDialog.Builder(this)
                        builder.setTitle("This app needs background location access")
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener {
                            requestPermissions(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                PERMISSION_REQUEST_BACKGROUND_LOCATION
                            )
                        }
                        builder.show()
                    } else {
                        val builder =
                            AlertDialog.Builder(this)
                        builder.setTitle("Functionality limited")
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener { }
                        builder.show()
                    }
                }
            } else if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.S &&
                (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED)
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
                    val builder =
                        AlertDialog.Builder(this)
                    builder.setTitle("This app needs bluetooth scan permission")
                    builder.setMessage("Please grant scan permission so this app can detect beacons.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener {
                        requestPermissions(
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                            PERMISSION_REQUEST_BLUETOOTH_SCAN
                        )
                    }
                    builder.show()
                } else {
                    val builder =
                        AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since bluetooth scan permission has not been granted, this app will not be able to discover beacons  Please go to Settings -> Applications -> Permissions and grant bluetooth scan permission to this app.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener { }
                    builder.show()
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            val builder =
                                AlertDialog.Builder(this)
                            builder.setTitle("This app needs background location access")
                            builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener {
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    PERMISSION_REQUEST_BACKGROUND_LOCATION
                                )
                            }
                            builder.show()
                        } else {
                            val builder =
                                AlertDialog.Builder(this)
                            builder.setTitle("Functionality limited")
                            builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                            builder.setPositiveButton(android.R.string.ok, null)
                            builder.setOnDismissListener { }
                            builder.show()
                        }
                    }
                }
            }
        }

    }


    companion object {
        val TAG = "MainActivity"
        val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
        val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
        val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
        val PERMISSION_REQUEST_FINE_LOCATION = 3
    }

    private fun test() {
        WifiManagerProxy.get().init(application)
        WifiManagerProxy.get().connect("AIcar.cn_5G", "aicar.cn", object : IWifiConnectListener {
            override fun onConnectStart() {
                Log.i("TAG", "onConnectStart: ")
            }

            override fun onConnectSuccess() {
                Log.i("TAG", "onConnectSuccess: ")
            }

            override fun onConnectFail(errorMsg: String) {
                Log.i("TAG", "onConnectFail: $errorMsg")
            }

        });
    }

    /*@RequiresApi(Build.VERSION_CODES.Q)
    fun test() {
            val specifier: NetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsidPattern(PatternMatcher("AIcar.cn", PatternMatcher.PATTERN_ADVANCED_GLOB))
                .setWpa2Passphrase("aicar.cn")
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback: NetworkCallback = object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // do success processing here..
                }

                override fun onUnavailable() {
                    // do failure processing here..
                }
            }
            connectivityManager.requestNetwork(request, networkCallback)
            // Release the request when done.
            // connectivityManager.unregisterNetworkCallback(networkCallback);
        }*/

}