package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootBroadcastReceiver : BroadcastReceiver() {
    private val ACTION = "android.intent.action.BOOT_COMPLETED"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("xxx", "intent.getAction() ===== " + intent!!.action)
        if (intent.action.equals(ACTION)) {

            val mainActivityIntent =  Intent(context, MainActivity::class.java)
            Log.d("auto_xxx","开机自启动一个Activity")
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context!!.startActivity(mainActivityIntent)
        }
    }

}