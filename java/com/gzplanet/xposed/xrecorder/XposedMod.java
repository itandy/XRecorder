package com.gzplanet.xposed.xrecorder;

import android.content.res.Resources;

import com.gzplanet.xposed.xrecorder.hook.BaseHook;
import com.gzplanet.xposed.xrecorder.hook.HookSeparate;
import com.gzplanet.xposed.xrecorder.util.Logger;
import com.gzplanet.xposed.xrecorder.util.SettingsHelper;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
    private BaseHook mHookSeparate;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        SettingsHelper mSettingsHelper = new SettingsHelper();
        Logger mLogger = new Logger(mSettingsHelper);
        mHookSeparate = new HookSeparate(mSettingsHelper, mLogger);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        switch (loadPackageParam.packageName) {
            case "com.sonymobile.android.dialer":
            case "com.sonymobile.callrecording":
            case "android":
            case "com.android.server.telecom":
                mHookSeparate.hook(loadPackageParam);
                break;
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (initPackageResourcesParam.packageName.endsWith("com.android.phone")) {
            try {
                initPackageResourcesParam.res.setReplacement("com.android.phone", "bool", "enable_call_recording", true);
            } catch (Resources.NotFoundException e) {
            }
        }
    }
}
