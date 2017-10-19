package ai.tgbot;

import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.os.*;

import android.support.v4.app.NotificationCompat;
import android.app.Notification;

import ai.tgbot.ui.MainActivity;

/*
 TGBot for Android
 Agus Ibrahim
 http://fb.me/mynameisagoes

 License: GPLv2
 http://www.gnu.org/licenses/gpl-2.0.html
 */
public class TGPoll extends Service {
    private TelegramBot bot;

    @Override
    public IBinder onBind(Intent p1) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bot = new TelegramBot(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_comment_alert_outline)
                .setContentTitle("Telegram SMS Gateway")
                .setContentText("Running")
                .setContentIntent(PendingIntent.getActivity(
                        this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .setOngoing(true)
                .build();

        bot.startListening();
        startForeground(1, notification);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}
