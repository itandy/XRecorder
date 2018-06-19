package com.gzplanet.xposed.xrecorder.hook;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;

import com.gzplanet.xposed.xrecorder.util.Logger;
import com.gzplanet.xposed.xrecorder.util.SettingsHelper;

import java.io.IOException;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.gzplanet.xposed.xrecorder.util.Constants.CONNECTING;
import static com.gzplanet.xposed.xrecorder.util.Constants.RINGING;

public class HookSeparate extends BaseHook {
    private Object mCallRecordingService;
    private MyHandler mHandler;
    private Class<?> mClassCallRecordingGlobals;

    public HookSeparate(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        switch (loadPackageParam.packageName) {
            case "com.android.server.telecom":
                // to capture incoming or outgoing state
                mLogger.log("hook com.android.server.telecom.PhoneStateBroadcaster...");
                final Class<?> phoneStateBroadcaster = XposedHelpers.findClass("com.android.server.telecom.PhoneStateBroadcaster", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(phoneStateBroadcaster, "onCallAdded", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object call = param.args[0];
                            Context context = (Context) XposedHelpers.callMethod(call, "getContext");
                            mSettingsHelper.setRemotePreference(context);

                            int state = (Integer) XposedHelpers.callMethod(call, "getState");

                            if (state == RINGING)
                                mSettingsHelper.setPhoneState("INCOMING");
                            else if (state == CONNECTING)
                                mSettingsHelper.setPhoneState("OUTGOING");

                            mLogger.log("onCallAdded: " + state);

                        } catch (Exception e) {
                        }

                    }
                });
                break;

            case "com.sonymobile.android.dialer":
                // to capture phone number and name
                mLogger.log("hook com.android.incallui.CallCardPresenter...");
                final Class<?> callCardPresenter = XposedHelpers.findClass("com.android.incallui.CallCardPresenter", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callCardPresenter, "updateContactEntry", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mLogger.log("updateContactEntry");
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                            String number = (String) XposedHelpers.getObjectField(param.args[0], "number");
                            String name = (String) XposedHelpers.getObjectField(param.args[0], "name");
                            if (context != null)
                                setCallerInfo(context, false, name, number);
                        } catch (Exception e) {
                            XposedBridge.log(e.getMessage());
                        }
                    }
                });
                break;

            case "com.sonymobile.callrecording":
                mLogger.log("hook com.sonymobile.callrecording.CallRecordingService...");
                final Class<? extends Enum> State = (Class<? extends Enum>) XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingStateMachine$State", loadPackageParam.classLoader);
                final Class<? extends Enum> Transition = (Class<? extends Enum>) XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingStateMachine$Transition", loadPackageParam.classLoader);
                Class<?> callRecordingService = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingService", loadPackageParam.classLoader);

                XposedBridge.hookAllConstructors(callRecordingService, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        mCallRecordingService = param.thisObject;
                        mHandler = new MyHandler(mLogger, mCallRecordingService, Transition);
                        mLogger.log("CallRecordingService constructor");
                    }
                });

                XposedBridge.hookAllMethods(callRecordingService, "onStartCommand", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        mSettingsHelper.setRemotePreference((Context) param.thisObject);
                        mHandler.setContext((Context) param.thisObject);

                        Intent intent = (Intent) param.args[0];
                        if (intent == null) {
                            return;
                        }
                        if (!mSettingsHelper.isEnableAutoRecord()) {
                            mLogger.log("auto recording disabled. Killing service in 5s");
                            mHandler.sendEmptyMessageDelayed(2, 5000);
                            return;
                        }
                        mLogger.log(String.format("onStartCommand action:%s", intent.getAction()));
                        switch (intent.getAction()) {
                            case "com.sonymobile.callrecording.callstarted":
                                Object mState = XposedHelpers.getObjectField(param.thisObject, "mState");

                                // call START and END both trigger "com.sonymobile.callrecording.callstarted" Action
                                if (mSettingsHelper.isRecordingStopped()) {
                                    // call end and manual stop recording, skip
                                    // otherwise it will start recording again
                                    mSettingsHelper.setRecordingStopped(false);
                                    mLogger.log("isRecordingStopped mState:" + mState.toString());
                                    if (Enum.valueOf(State, "OFF") == mState) {
                                        mLogger.log("killing service in 5s");
                                        mHandler.sendEmptyMessageDelayed(2, 5000);
                                    }
                                    return;
                                }
                                if (mSettingsHelper.isWaitingForRecording()) {
                                    // clear if delay recording is failed
                                    mSettingsHelper.setWaitingForRecording(false);
                                }
                                if (Enum.valueOf(State, "IDLE") == mState) {
                                    mLogger.log("onStartCommand mState:IDLE");
                                    if ("INCOMING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordIncoming()
                                            || "OUTGOING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordOutgoing()) {
                                        return;
                                    }
                                    if (!mSettingsHelper.existsLiveCall()) {
                                        // don't start recording if no call alive
                                        // but try again after 1s,
                                        // because the call state may be changed delayed
                                        mLogger.log("no active calls, skip start recording");
                                        mHandler.sendEmptyMessageDelayed(0, 1000);
                                        return;
                                    }
                                    mLogger.log("start recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                                } else if (Enum.valueOf(State, "RECORDING") == mState) {
                                    mLogger.log("onStartCommand mState:RECORDING");
                                    if (mSettingsHelper.existsLiveCall()) {
                                        // don't stop recording until all(primary and secondary) calls ended
                                        // but try again after 1s,
                                        // because the call state may be changed delayed
                                        mLogger.log("active calls exist, skip stop recording");
                                        mHandler.sendEmptyMessageDelayed(1, 1000);
                                        return;
                                    }
                                    mLogger.log("end recording");
                                    XposedHelpers.callMethod(param.thisObject, "transitionToState", Enum.valueOf(Transition, "STOP_RECORDING"));
                                    mLogger.log("killing service in 5s");
                                    mHandler.sendEmptyMessageDelayed(2, 5000);
                                } else if (Enum.valueOf(State, "OFF") == mState) {
                                    mLogger.log("onStartCommand mState:OFF");
                                    // CallRecorder is not prepared, wait...
                                    // often occurs when CallRecorder first create
                                    mSettingsHelper.setWaitingForRecording(true);
                                }
                                break;
                            case "com.sonymobile.callrecording.stoprecodring":
                                // manual stop recording
                                mLogger.log("end recording(manual)");
                                mSettingsHelper.setRecordingStopped(true);
                                mLogger.log("killing service in 5s");
                                mHandler.sendEmptyMessageDelayed(2, 5000);
                                break;
                            case "com.sonymobile.callwidgetframework.WIDGET_ACTION_SELECTED":
                                // manual start recording
                                mLogger.log("start recording(manual)");
                                mSettingsHelper.setRecordingStopped(false);
                                break;
                        }
                    }
                });

                XposedBridge.hookAllMethods(callRecordingService, "setEnable", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        Context context = (Context) param.thisObject;
                        boolean enable = (boolean) param.args[0];
                        String state = XposedHelpers.getObjectField(param.thisObject, "mState").toString();
                        boolean isUINull = XposedHelpers.getObjectField(param.thisObject, "mCallRecordingRemoteUI") == null;
                        int somcCallRecording = Settings.System.getInt(context.getContentResolver(), "somc.call_recording", 0);
                        mLogger.log(String.format("setEnable - enable:%b, state:%s, isUINull:%b, somcCallRecording:%d", enable, state, isUINull, somcCallRecording));
                    }
                });

                XposedBridge.hookAllMethods(callRecordingService, "transitionToState", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        Context context = (Context) param.thisObject;
                        String transaction = param.args[0].toString();
                        String state = XposedHelpers.getObjectField(param.thisObject, "mState").toString();
                        boolean isUINull = XposedHelpers.getObjectField(param.thisObject, "mCallRecordingRemoteUI") == null;
                        int somcCallRecording = Settings.System.getInt(context.getContentResolver(), "somc.call_recording", 0);
                        mLogger.log(String.format("transitionToState - transaction:%s, state:%s, isUINull:%b, somcCallRecording:%d", transaction, state, isUINull, somcCallRecording));
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecorder...");
                Class<?> callRecorder = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecorder", loadPackageParam.classLoader);
                mClassCallRecordingGlobals = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingGlobals", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecorder, "generateFilename", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.callStaticMethod(mClassCallRecordingGlobals, "getContext");
                        mSettingsHelper.setRemotePreference(context);

                        if ("INCOMING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordIncoming()
                                || "OUTGOING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordOutgoing()) {
                            return;
                        }
                        String[] value = mSettingsHelper.getFileCallType();
                        callType = "INCOMING".equals(mSettingsHelper.getPhoneState()) ? value[0] : value[1];
                        callerName = mSettingsHelper.getCallerName();
                        phoneNumber = mSettingsHelper.getPhoneNumber();
                        param.setResult(((String)param.getResult()).replace(".amr", ".3gp"));
                        changeFileName(param);
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecordingRemoteUI...");
                Class<?> callRecordingRemoteUI = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecordingRemoteUI", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecordingRemoteUI, "setEnabled", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.callStaticMethod(mClassCallRecordingGlobals, "getContext");
                        mSettingsHelper.setRemotePreference(context);

                        mLogger.log("setEnabled");
                        if ("INCOMING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordIncoming()
                                || "OUTGOING".equals(mSettingsHelper.getPhoneState()) && !mSettingsHelper.isEnableRecordOutgoing()) {
                            return;
                        }
                        if (mSettingsHelper.isWaitingForRecording() && (Boolean) param.args[0] && mCallRecordingService != null) {
                            mLogger.log("start recording(initial)");
                            XposedHelpers.callMethod(mCallRecordingService, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                            mSettingsHelper.setWaitingForRecording(false);
                        }
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.CallRecorder.RecorderTask...");
                Class<?> callRecordingRecorderTask = XposedHelpers.findClass("com.sonymobile.callrecording.CallRecorder.RecorderTask", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(callRecordingRecorderTask, "record", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        mLogger.log("replaced record");
                        MediaRecorder mediaRecorder = (MediaRecorder) param.args[0];
                        String path = (String) param.args[1];
                        Object callRecorder = XposedHelpers.getSurroundingThis(param.thisObject);
                        Object recordingStartedRegistant = XposedHelpers.getObjectField(callRecorder, "mRecordingStartedRegistant");
                        Object recordingStoppedRegistrant = XposedHelpers.getObjectField(callRecorder, "mRecordingStoppedRegistrant");
                        Handler handler = (Handler) XposedHelpers.getObjectField(callRecorder, "mHandler");
                        Object checkStorageRunnable = XposedHelpers.getObjectField(callRecorder, "mCheckStorageRunnable");

                        int step = 0;
                        try {
//                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                            step = 1;
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
                            step = 2;
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
                            step = 3;
                            mediaRecorder.setOutputFile(path);
                            step = 4;
                            mediaRecorder.prepare();
                            step = 5;
                            mediaRecorder.start();
                            step = 6;
                            XposedHelpers.callMethod(recordingStartedRegistant, "notifyRegistrant");
                            step = 7;
                            handler.post((Runnable) checkStorageRunnable);
                            step = 8;
                            XposedHelpers.setObjectField(param.thisObject, "mFilePath", path);
                            step = 9;
                            boolean isCancelled = (boolean) XposedHelpers.callMethod(param.thisObject, "isCancelled");
                            while (!isCancelled) {
                                XposedHelpers.callMethod(callRecorder, "wait");
                                isCancelled = (boolean) XposedHelpers.callMethod(param.thisObject, "isCancelled");
                            }
                            step = 10;
                            mediaRecorder.stop();

                        } catch (IllegalStateException ex2) {
                            mLogger.log("IllegalStateException step:" + step);
                            XposedHelpers.callMethod(param.thisObject, "cancel", true);
                        } catch (IOException ex3) {
                            mLogger.log("IOException step:" + step);
                            XposedHelpers.callMethod(param.thisObject, "cancel", true);
                        } catch (RuntimeException ex4) {
                            mLogger.log(ex4.toString());
                            mLogger.log("RuntimeException step:" + step);
                            if ((boolean) XposedHelpers.callMethod(callRecorder, "isRecording")) {
                                mLogger.log("Recording -> Cancel");
                                XposedHelpers.callMethod(param.thisObject, "cancel", true);
                                XposedHelpers.callMethod(recordingStoppedRegistrant, "notifyError", 903);
                            }
                        } finally {
                            mediaRecorder.release();
                        }

                        return null;
                    }
                });

                mLogger.log("hook com.sonymobile.callrecording.BeepTonePlayer...");
                Class<?> beepTonePlayer = XposedHelpers.findClass("com.sonymobile.callrecording.BeepTonePlayer", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(beepTonePlayer, "startBeepTones", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        mLogger.log("replaced startBeepTones");
                        return null;
                    }
                });
                XposedBridge.hookAllMethods(beepTonePlayer, "stopBeepTones", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        mLogger.log("replaced stopBeepTones");
                        return null;
                    }
                });

                break;

            case "android":
                // add permission CAPTURE_AUDIO_OUTPUT to CallRecording app
                mLogger.log("hook com.android.server.pm.PackageManagerService...");
                final Class<?> packageManagerService = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", loadPackageParam.classLoader);
                XposedBridge.hookAllMethods(packageManagerService, "grantPermissionsLPw", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        final String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");
                        if (pkgName.contentEquals("com.sonymobile.callrecording")) {
                            final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
                            final Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");
                            final List<String> grantedPerms = (List<String>) XposedHelpers.getObjectField(param.args[0], "requestedPermissions");
                            final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
                            final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

                            if (!grantedPerms.contains("android.permission.CAPTURE_AUDIO_OUTPUT")) {
                                final Object pCaptureAudioOutput = XposedHelpers.callMethod(permissions, "get", "android.permission.CAPTURE_AUDIO_OUTPUT");
                                XposedHelpers.callMethod(ps, "grantInstallPermission", pCaptureAudioOutput);
                                mLogger.log("added permission CAPTURE_AUDIO_OUTPUT to CallRecording app");
                            }
                        }
                    }
                });
                break;
        }
    }

    private void setCallerInfo(Context context, boolean nameIsNumber, String callerName, String phoneNumber) {
        mSettingsHelper.setRemotePreference(context);

        if (!mSettingsHelper.isEnableAutoRecord()) {
            return;
        }
        // share data via remote preference between different processes
        if (nameIsNumber) {
            mSettingsHelper.setCallerName(null);
            mSettingsHelper.setPhoneNumber(callerName);
        } else {
            mSettingsHelper.setCallerName(callerName);
            mSettingsHelper.setPhoneNumber(phoneNumber);
        }
        mLogger.log(String.format("setPrimary PhoneNo:%s, CallerName:%s, nameIsNumber:%b",
                phoneNumber, callerName, nameIsNumber));

    }

    private static class MyHandler extends Handler {
        private Logger mLogger;
        private Object mCallRecordingService;
        private Class<? extends Enum> Transition;
        SettingsHelper mSettingsHelper = new SettingsHelper();

        MyHandler(Logger logger, Object callRecordingService, Class<? extends Enum> transition) {
            mLogger = logger;
            mCallRecordingService = callRecordingService;
            Transition = transition;
        }

        public void setContext(Context context) {
            mSettingsHelper.setRemotePreference(context);
        }

        @Override
        public void handleMessage(Message msg) {
            try {

                switch (msg.what) {
                    case 0:
                        if (!mSettingsHelper.existsLiveCall()) {
                            // don't stop recording until all(primary and secondary) calls ended
                            return;
                        }
                        mLogger.log("start recording(delay)");
                        XposedHelpers.callMethod(mCallRecordingService, "transitionToState", Enum.valueOf(Transition, "START_RECORDING"));
                        break;
                    case 1:
                        if (mSettingsHelper.existsLiveCall()) {
                            // don't stop recording until all(primary and secondary) calls ended
                            return;
                        }
                        mLogger.log("end recording(delay)");
                        XposedHelpers.callMethod(mCallRecordingService, "transitionToState", Enum.valueOf(Transition, "STOP_RECORDING"));
                        mLogger.log("killing service in 5s");
                        sendEmptyMessageDelayed(2, 5000);
                        break;
                    case 2:
                        mLogger.log("kill CallRecordingService");
                        XposedHelpers.callMethod(mCallRecordingService, "stopSelf");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
