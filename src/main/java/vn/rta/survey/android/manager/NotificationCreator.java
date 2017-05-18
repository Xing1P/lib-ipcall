package vn.rta.survey.android.manager;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import vn.rta.survey.android.R;

/**
 * Created by Genius Doan on 13/05/2017.
 */

public class NotificationCreator {
    private static final int NOTIFICATION_ID = 1408;

    private static Notification notification;
    private static int iconId = R.drawable.ic_stat_default;
    private static String notifTitle = "";
    private static String notifText = "";
    private static RemoteViews contentView;

    public static Notification getNotification(Context context) {
        if (notification == null) {
            notification = new NotificationCompat.Builder(context)
                    .setContentTitle(notifTitle)
                    .setContentText(notifText)
                    .setSmallIcon(iconId)
                    .setContent(contentView)
                    .build();
        }

        return notification;
    }

    public static void setNotificationTitle(String title) {
        notifTitle = title;
    }

    public static void setNotificationText(String text) {
        notifText = text;
    }

    public static void setContentView(RemoteViews view) {
        contentView = view;
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
