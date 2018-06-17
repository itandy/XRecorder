package com.gzplanet.xposed.xrecorder.util;

import de.robv.android.xposed.XposedBridge;

public class Logger {
    private SettingsHelper settingsHelper;

    public Logger(SettingsHelper settingsHelper) {
        this.settingsHelper = settingsHelper;
    }

    public void log(String msg, boolean force) {
        if (force || settingsHelper.isEnableLogging()) {
            XposedBridge.log("[XRecorder] " + msg);
        }
    }

    public void log(String msg) {
        log(msg, false);
    }
}
