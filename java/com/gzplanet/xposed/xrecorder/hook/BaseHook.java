package com.gzplanet.xposed.xrecorder.hook;

import android.text.TextUtils;

import com.gzplanet.xposed.xrecorder.util.Logger;
import com.gzplanet.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class BaseHook {
    public SettingsHelper mSettingsHelper;
    public Logger mLogger;

    public String callType;
    public String callerName;
    public String phoneNumber;

    public BaseHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        this.mSettingsHelper = mSettingsHelper;
        this.mLogger = mLogger;
    }

    public abstract void hook(XC_LoadPackage.LoadPackageParam loadPackageParam);

    public void changeFileName(XC_MethodHook.MethodHookParam param) {
        phoneNumber = phoneNumber.replaceAll(" ", "");

        if (callerName != null)
            callerName = callerName.replaceAll(" ", "");
        if (callerName.equals(""))
            callerName = "unknown";

        String fileName = mSettingsHelper.getFileFormat();

        String[] results = ((String) param.getResult()).split("\\.");
        String[] names = results[0].split("-");
        fileName = fileName
                .replaceAll("yyyy", names[0])
                .replaceAll("MM", names[1])
                .replaceAll("dd", names[2])
                .replaceAll("HH", names[3])
                .replaceAll("mm", names[4])
                .replaceAll("ss", names[5])
                .replaceAll("tt", callType)
                .replaceAll("nn", callerName)
                .replaceAll("pp", phoneNumber);

        param.setResult(fileName + "." + results[1]);

        callerName = null;
        phoneNumber = null;
    }
}
