package com.gzplanet.xposed.xrecorder;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;
import com.gzplanet.xposed.xrecorder.util.Constants;

public class PreferenceProvider extends RemotePreferenceProvider {
    public PreferenceProvider() {
        super(Constants.PREF_AUTHORITY, new String[]{Constants.PREF_NAME});
    }
}
