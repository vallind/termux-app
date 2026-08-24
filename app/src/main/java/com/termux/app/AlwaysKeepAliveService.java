package com.termux.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * A small Foreground Service that keeps the app process alive when the "Always keep Termux running"
 * preference is enabled. This avoids directly modifying the existing TermuxService and provides a
 * minimal, low-risk way to keep Termux running as a foreground service.
 *
 * To use:
 * - Start this service with an explicit intent when the preference is enabled:
 *   ContextCompat.startForegroundService(context, new Intent(context, AlwaysKeepAliveService.class));
 * - Stop this service when the preference is disabled:
 *   context.stopService(new Intent(context, AlwaysKeepAliveService.class));
 *
 * The Settings UI should send these start/stop intents when the preference toggles.
 */
public class AlwaysKeepAliveService extends Service {

    public static final String ACTION_START_KEEPALIVE = "com.termux.action.START_KEEPALIVE";
    public static final String ACTION_STOP_KEEPALIVE = "com.termux.action.STOP_KEEPALIVE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannelIfNeeded(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP_KEEPALIVE.equals(action)) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Default/start action -> run as foreground service
        Notification notification = buildKeepAliveNotification(this);
        startForeground(TermuxConstants.NOTIFICATION_ID_KEEPALIVE, notification);
        // Nothing else to do; keep running. The service will be stopped explicitly when preference is off.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static void createNotificationChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel c = nm.getNotificationChannel(TermuxConstants.NOTIFICATION_CHANNEL_KEEPALIVE);
            if (c == null) {
                c = new NotificationChannel(TermuxConstants.NOTIFICATION_CHANNEL_KEEPALIVE,
                        "Termux keep-alive",
                        NotificationManager.IMPORTANCE_LOW);
                c.setDescription("Keep Termux running in foreground");
                nm.createNotificationChannel(c);
            }
        }
    }

    private static Notification buildKeepAliveNotification(Context context) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, TermuxConstants.NOTIFICATION_CHANNEL_KEEPALIVE)
                .setContentTitle("Termux")
                .setContentText("Termux is running in the background")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return b.build();
    }
}
