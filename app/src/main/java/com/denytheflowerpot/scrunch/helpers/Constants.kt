package com.denytheflowerpot.scrunch.helpers

import com.denytheflowerpot.scrunch.ScrunchApplication

class Constants {
    companion object {
        val SharedPreferencesFile = "${ScrunchApplication.instance.packageName}.PREFS"
    }
}