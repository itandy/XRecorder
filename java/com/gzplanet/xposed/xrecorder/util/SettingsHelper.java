package com.gzplanet.xposed.xrecorder.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.crossbowffs.remotepreferences.RemotePreferences;
import com.gzplanet.xposed.xrecorder.BuildConfig;

import de.robv.android.xposed.XSharedPreferences;

import static com.gzplanet.xposed.xrecorder.util.Constants.DEFAULT_NOTIFICATION_TIMEOUT_SECONDS;

@SuppressWarnings("unused")
public class SettingsHelper {
    private SharedPreferences mPreferences = null;
    private XSharedPreferences mXPreferences = null;

    public SettingsHelper() {
        mXPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        mXPreferences.makeWorldReadable();
        this.reload();
    }

    public void setRemotePreference(Context context) {
        mPreferences = new RemotePreferences(context, Constants.PREF_AUTHORITY, Constants.PREF_NAME);
    }

    public boolean isEnableAutoRecord() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true);
    }

    public boolean isEnableRecordOutgoing() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true) && getBoolean("pref_enable_outgoing_call_recording", true);
    }

    public boolean isEnableRecordIncoming() {
        reload();
        return getBoolean("pref_enable_auto_call_recording", true) && getBoolean("pref_enable_incoming_call_recording", true);
    }

    public String getFileFormat() {
        reload();
        return getString("pref_file_format", Constants.DEFAULT_FILE_FORMAT);
    }

    public String[] getFileCallType() {
        reload();
        return getString("pref_file_calltype", Constants.DEFAULT_FILE_CALLTYPE).split(":");
    }

    public boolean isEnableLogging() {
        return getBoolean("pref_enable_logging", true);
    }

    public boolean isEnableNotification() {
        return getBoolean("pref_enable_notification", false);
    }

    public int getNotificationTimeout() {
        return getInt("pref_notification_timeout", DEFAULT_NOTIFICATION_TIMEOUT_SECONDS);
    }

    // migrated functions from CustomService
    public boolean isWaitingForRecording() {
        return getBoolean("waiting_for_recording", false);
    }

    public void setWaitingForRecording(boolean waitingForRecording) {
        setBoolean("waiting_for_recording", waitingForRecording);
    }

    public boolean isRecordingStopped() {
        return getBoolean("recording_stopped", false);
    }

    public void setRecordingStopped(boolean recordingStopped) {
        setBoolean("recording_stopped", recordingStopped);
    }

    public String getCallerName() {
        return getString("caller_name", "");
    }

    public void setCallerName(String callerName) {
        setString("caller_name", callerName);
    }

    public String getFilename() {
        return getString("filename", "");
    }

    public void setFilename(String filename) {
        setString("filename", filename);
    }

    public String getPhoneNumber() {
        return getString("phone_number", "");
    }

    public void setPhoneNumber(String phoneNumber) {
        setString("phone_number", phoneNumber);
    }

    public boolean isSetSaveDirectoryable() {
        return getBoolean("set_save_directoryable", false);
    }

    public void setSetSaveDirectoryable(boolean setSaveDirectoryable) {
        setBoolean("set_save_directoryable", setSaveDirectoryable);
    }

    public String getPhoneState() {
        return getString("phone_state", "");
    }

    public void setPhoneState(String phoneState) {
        setString("phone_state", phoneState);
    }

    public boolean existsLiveCall() {
        return getBoolean("exist_live_call", false);
    }

    public void setExistsLiveCall(boolean existsLiveCall) {
        setBoolean("exist_live_call", existsLiveCall);
    }

    private String getString(String key, String defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getString(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getString(key, defaultValue);
        }

        return defaultValue;
    }

    private int getInt(String key, int defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getInt(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getInt(key, defaultValue);
        }

        return defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        if (mPreferences != null) {
            return mPreferences.getBoolean(key, defaultValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getBoolean(key, defaultValue);
        }

        return defaultValue;
    }

    private void setString(String key, String value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putString(key, value);
            editor.apply();
        }
    }

    private void setBoolean(String key, boolean value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    private void setInt(String key, int value) {
        Editor editor = null;
        if (mPreferences != null) {
            editor = mPreferences.edit();
        } else if (mXPreferences != null) {
            editor = mXPreferences.edit();
        }

        if (editor != null) {
            editor.putInt(key, value);
            editor.apply();
        }
    }

    private boolean contains(String key) {
        if (mPreferences != null) {
            return mPreferences.contains(key);
        } else if (mXPreferences != null) {
            return mXPreferences.contains(key);
        }

        return false;
    }

    private void reload() {
        if (mXPreferences != null) {
            mXPreferences.reload();
        }
    }
}
