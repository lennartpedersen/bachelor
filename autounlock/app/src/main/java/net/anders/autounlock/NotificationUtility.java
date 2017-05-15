package net.anders.autounlock;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.Serializable;
import java.util.List;

public class NotificationUtility {
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_YES = "action_yes";
    public static final String ACTION_NO = "action_no";

    public void displayUnlockNotification(Context context, int cluster) {

        Intent yesIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_YES);

        Intent noIntent = new Intent(context, NotificationActionService.class)
                .setAction(ACTION_NO);
        noIntent.putExtra("Cluster", cluster);

        // use System.currentTimeMillis() to have adapter unique ID for the pending intent
        PendingIntent pendingYesIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                yesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingNoIntent = PendingIntent.getService(
                context,
                (int) System.currentTimeMillis(),
                noIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.bekey_logo)
                        .setAutoCancel(true)
                        .setContentTitle(String.valueOf(BluetoothService.ANDERS_BEKEY) + " was unlocked")
                        .setContentText("Was this decision correct?")
                        .addAction(new NotificationCompat.Action(R.drawable.ic_check_black,
                                "Yes", pendingYesIntent))
                        .addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                                "No", pendingNoIntent))
                        .setVibrate(new long[] {0, 100});

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Notification notification = notificationBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            Log.d("Notification", "Received notification action: " + action);
            if (ACTION_YES.equals(action)) {
                CoreService.newTruePositive();
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            } else if (ACTION_NO.equals(action)) {
                CoreService.newFalsePositive();
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                Intent incorrecetUnlockIntent = new Intent("INCORRECT_UNLOCK");
                incorrecetUnlockIntent.putExtras(intent.getExtras());
                sendBroadcast(incorrecetUnlockIntent);
            }
        }
    }
}
