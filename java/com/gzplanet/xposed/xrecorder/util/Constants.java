package com.gzplanet.xposed.xrecorder.util;

import android.Manifest;

public class Constants {
    public static final String DEFAULT_FILE_FORMAT = "yyyyMMddHHmmss_tt_pp_nn";
    public static final String DEFAULT_FILE_CALLTYPE = "incoming:outgoing";
    public static String PREF_AUTHORITY = "com.gzplanet.app.xrecorder_preferences";
    public final static String PREF_NAME = "com.gzplanet.xposed.xrecorder_preferences";

    public static final int CONNECTING = 1;
    public static final int RINGING = 4;

    public static final String AUDIO_FILE_PATH = "/CallRecorder";

    public static final String CALLER_UNKNOWN = "unknown";

    public static final String ACTION_DELETE_FILE = "com.gzplanet.xposed.xrecorder.ACTION_DELETE_FILE";
    public static final String ACTION_DELETE_NOTIF = "com.gzplanet.xposed.xrecorder.ACTION_DELETE_NOTIF";
    public static final String ACTION_TAP_NOTIF = "com.gzplanet.xposed.xrecorder.ACTION_TAP_NOTIF";
    public static final String ACTION_SHOW_DELETE_NOTIF = "com.gzplanet.xposed.xrecorder.ACTION_SHOW_DELETE_NOTIF";

    public static final String NOTIF_GROUP_ID = "com.gzplanet.xposed.xrecorder.GROUP";
    public static final String NOTIF_CHANNEL_ID = "com.gzplanet.xposed.xrecorder.CHANNEL_DELETE";

    public static final int DEFAULT_NOTIFICATION_TIMEOUT_SECONDS = 20;

    public static final int REQUEST_CODE_STORAGE_PERMS = 321;
    public static final String[] REALTIME_PERMISSION = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
}
