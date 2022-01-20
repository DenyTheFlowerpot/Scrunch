package com.denytheflowerpot.scrunch.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object PermissionUtils {
    fun needsToGrantReadLogs(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return false

        return context.checkCallingOrSelfPermission(android.Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED
    }
}