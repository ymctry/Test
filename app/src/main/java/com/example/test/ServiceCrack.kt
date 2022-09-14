package com.example.test

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.widget.Toast
import java.util.*


class ServiceCrack : Service() {


    private var pm: PowerManager? = null
    private var wakeLock: WakeLock? = null


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("auto_xxx", "Service 已经启动成功")

    }


    @SuppressLint("InvalidWakeLockTag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //创建PowerManager对象
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        //保持cpu一直运行，不管屏幕是否黑屏
        wakeLock = pm!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUKeepRunning")
        wakeLock!!.acquire()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (wakeLock != null)
            wakeLock!!.release()
        super.onDestroy()

        Log.d("auto_xxx", "Service 已销毁")
    }

}