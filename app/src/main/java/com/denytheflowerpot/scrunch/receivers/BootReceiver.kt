package com.denytheflowerpot.scrunch.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.denytheflowerpot.scrunch.ScrunchApplication

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Scrunch", "BootReceiver received action ${intent.action}")
        ScrunchApplication.instance.startServiceIfNeeded()
    }
}