package com.gzplanet.xposed.xrecorder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.FileProvider;
import android.widget.Toast;
import com.gzplanet.xposed.xrecorder.util.Constants;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.gzplanet.xposed.xrecorder.util.Constants.ACTION_DELETE_FILE;
import static com.gzplanet.xposed.xrecorder.util.Constants.ACTION_DELETE_NOTIF;
import static com.gzplanet.xposed.xrecorder.util.Constants.ACTION_TAP_NOTIF;
import static com.gzplanet.xposed.xrecorder.util.Constants.NOTIF_CHANNEL_ID;
import static com.gzplanet.xposed.xrecorder.util.Constants.NOTIF_GROUP_ID;

public class RecorderReceiver extends BroadcastReceiver {
    static Context mContext;
    static Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 0:
                        if (mContext != null)
                            cancelNotification(mContext, msg.arg1);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        mContext = context;
        Resources res = context.getResources();

        String action = intent.getAction();
        if (action.equals(Constants.ACTION_DELETE_FILE)) {
            if (bundle != null) {
                String path = bundle.getString("path");
                String filename = bundle.getString("filename");
                int id = bundle.getInt("id");
                File file = new File(path + "/" + filename);
                if (file.exists()) {
                    if (file.delete())
                        Toast.makeText(context,
                                String.format(res.getString(R.string.alert_delete_successful), filename),
                                Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(context, String.format(res.getString(R.string.alert_file_not_found), filename),
                            Toast.LENGTH_SHORT).show();

                cancelNotification(context, id);

                collapseStatusPanel(context);
            }
        } else if (action.equals(Constants.ACTION_SHOW_DELETE_NOTIF)) {
            if (bundle != null) {
                String path = bundle.getString("path");
                String filename = bundle.getString("filename");
                String callerName = bundle.getString("caller_name");
                String phoneNo = bundle.getString("phone_no");
                int timeout = bundle.getInt("timeout");
                showNotification(context, path, filename, callerName, phoneNo, timeout);
            }
        } else if (action.equals(Constants.ACTION_DELETE_NOTIF)) {
            checkAndCancelGroupSummary(context);
        } else if (action.equals(Constants.ACTION_TAP_NOTIF)) {
            String filename = bundle.getString("filename");
            String path = bundle.getString("path");
            File file = new File(path + "/" + filename);

            Intent intent1 = new Intent();
            intent1.setAction(Intent.ACTION_VIEW);
            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                intent1.setDataAndType(Uri.fromFile(file), "audio/*");
            } else {
                intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri uri = FileProvider.getUriForFile(context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        file);
                intent1.setDataAndType(uri, "audio/*");
            }
            context.startActivity(intent1);
            checkAndCancelGroupSummary(context);
        }
    }

    private boolean showNotification(Context context, String path, String filename, String callerName, String phoneNo, int timeout) {
        File file = new File(path + "/" + filename);
        if (file.exists()) {
            int notifId = createId();
            Resources res = context.getResources();

            String displayName = callerName.equals("") ? phoneNo : callerName;

            // Intent for handling delete action
            Intent intentDeleteFile = new Intent(context, RecorderReceiver.class);
            intentDeleteFile.setAction(ACTION_DELETE_FILE);
            intentDeleteFile.putExtra("path", path);
            intentDeleteFile.putExtra("filename", filename);
            intentDeleteFile.putExtra("id", notifId);
            PendingIntent pendingIntentDeleteFile = PendingIntent.getBroadcast(context, createId(), intentDeleteFile, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Action action = new Notification.Action.Builder(R.drawable.ic_notification,
                    res.getString(R.string.notification_action_delete_button), pendingIntentDeleteFile).build();

            // Intent for handling notification delete
            Intent intentDeleteNotif = new Intent(context, RecorderReceiver.class);
            intentDeleteNotif.setAction(ACTION_DELETE_NOTIF);
            PendingIntent pendingIntentDeleteNotif = PendingIntent.getBroadcast(context, createId(), intentDeleteNotif, PendingIntent.FLAG_UPDATE_CURRENT);

            // Intent for handling notification tap
            Intent intentTapNotif = new Intent(context, RecorderReceiver.class);
            intentTapNotif.setAction(ACTION_TAP_NOTIF);
            intentTapNotif.putExtra("path", path);
            intentTapNotif.putExtra("filename", filename);
            PendingIntent pendingIntentTapNotif = PendingIntent.getBroadcast(context, createId(), intentTapNotif, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder summary = new Notification.Builder(context)
                    .setContentTitle(res.getString(R.string.notification_summary_title))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setGroup(NOTIF_GROUP_ID)
                    .setGroupSummary(true);

            Notification.Builder builder = new Notification.Builder(context)
                    .setContentTitle(res.getString(R.string.notification_action_title) + " " + displayName)
                    .setContentText(res.getString(R.string.notification_action_text_short))
                    .setSmallIcon(R.drawable.ic_notification)
                    .addAction(action)
                    .setContentIntent(pendingIntentTapNotif)
                    .setDeleteIntent(pendingIntentDeleteNotif)
                    .setStyle(new Notification.BigTextStyle().bigText(
                            String.format(res.getString(R.string.notification_action_text_long), path + "/" + filename)))
                    .setPriority(Notification.PRIORITY_LOW)
                    .setGroup(NOTIF_GROUP_ID)
                    .setShowWhen(true)
                    .setAutoCancel(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(NOTIF_CHANNEL_ID);
                if (timeout > 0)
                    builder.setTimeoutAfter(timeout * 1000);

                NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID,
                        res.getString(R.string.notification_action_title), NotificationManager.IMPORTANCE_LOW);
                channel.setDescription(res.getString(R.string.notification_channel_descr));

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(notifId, builder.build());
            nm.notify(0, summary.build());

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && timeout > 0) {
                Message msg = new Message();
                msg.what = 0;
                msg.arg1 = notifId;
                mHander.sendMessageDelayed(msg, timeout * 1000);
            }

            return true;
        } else
            return false;
    }

    private int createId(){
        Date now = new Date();
        int id = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));
        return id;
    }

    private void collapseStatusPanel(Context context) {
        try {
            @SuppressLint("WrongConstant")
            Object service = context.getSystemService(Context.STATUS_BAR_SERVICE);
            Class<?> statusbarManager = Class.forName( "android.app.StatusBarManager" );
            Method hidesb = statusbarManager.getMethod( "collapsePanels" );
            hidesb.setAccessible(true);
            hidesb.invoke( service );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cancelNotification(Context context, int id) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.cancel(id);

        checkAndCancelGroupSummary(context);
    }

    private static void checkAndCancelGroupSummary(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

        for (StatusBarNotification notification : notifications) {
            Notification notif = notification.getNotification();
            if (notif.getGroup().equals(NOTIF_GROUP_ID) && !notif.isGroupSummary())
                return;
        }

        notificationManager.cancel(0);
    }
}
