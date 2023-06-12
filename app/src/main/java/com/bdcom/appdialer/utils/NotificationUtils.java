package com.bdcom.appdialer.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bdcom.appdialer.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.os.Build.VERSION_CODES.O;

public class NotificationUtils {

    // notification icon
    public static final int icon = R.drawable.ic_kotha_dialer_notification_logo;

    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        // User invisible channel ID
        String mChannelId = context.getString(R.string.firebase_push_notification_channel_id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, mChannelId);
        }
        return new NotificationCompat.Builder(context, mChannelId);
    }

    @TargetApi(O)
    public static void prepareChannel(Context context, String mChannelId) {
        // Notification channel settings
        // Default Notification Priority
        int mChannelImportance = NotificationManager.IMPORTANCE_HIGH;
        int mChannelLockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC;

        // User visible channel name
        String mChannelName = context.getString(R.string.firebase_push_notification_channel_name);
        // User visible channel description
        String mChannelDescription =
                context.getString(R.string.firebase_push_notification_channel_description);
        final NotificationManager nm =
                (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

        if (nm != null) {
            NotificationChannel mChannel = nm.getNotificationChannel(mChannelId);
            if (mChannel == null) {
                mChannel = new NotificationChannel(mChannelId, mChannelName, mChannelImportance);
                mChannel.setDescription(mChannelDescription);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.enableVibration(false);
                //                mChannel.setSound(
                //                        Uri.parse(
                //                                "android.resource://"
                //                                        + context.getPackageName()
                //                                        + "/"
                //                                        + R.raw.voip_call_ringtone),
                //                        new AudioAttributes.Builder()
                //
                // .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                //                                .setLegacyStreamType(AudioManager.STREAM_RING)
                //
                // .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                //                                .build());
                mChannel.setShowBadge(true);
                mChannel.setLockscreenVisibility(mChannelLockScreenVisibility);
                nm.createNotificationChannel(mChannel);
            }
        }
    }

    public static Notification getBigTextNotification(
            Context context,
            String title,
            String body,
            long time,
            final PendingIntent pendingIntent) {
        final NotificationCompat.Builder mBuilder =
                NotificationUtils.getNotificationBuilder(context);
        return mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(icon)
                .setTicker(title)
                .setAutoCancel(true)
                .setWhen(time)
                .setColor(context.getColor(R.color.primary_color))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    public static Notification getIncomingCallNotification(
            Context context,
            String title,
            String body,
            long time,
            final PendingIntent contentPendingIntent,
            final PendingIntent declinePendingIntent,
            final PendingIntent receivePendingIntent) {
        final NotificationCompat.Builder mBuilder =
                NotificationUtils.getNotificationBuilder(context);
        return mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(icon)
                .setTicker(title)
                .setAutoCancel(true)
                .setWhen(time)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_baseline_clear_24, "Decline", declinePendingIntent)
                .addAction(R.drawable.ic_baseline_call_24, "Receive", receivePendingIntent)
                //                .addAction(R.drawable.call_screen_bg, "Decline",
                // declinePendingIntent)
                //                .addAction(R.drawable.call_screen_bg, "Receive",
                // receivePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(receivePendingIntent, true)
                .setAutoCancel(true)
                .build();
    }

    public static Notification getBigTextNotificationWithoutPendingIntent(
            Context context, String title, String body, long time) {
        final NotificationCompat.Builder mBuilder =
                NotificationUtils.getNotificationBuilder(context);
        return mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(icon)
                .setTicker(title)
                .setAutoCancel(true)
                .setWhen(time)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    public static Notification getBigImageNotification(
            Context context,
            String title,
            String body,
            long time,
            Bitmap bitmap,
            final PendingIntent pendingIntent) {
        // Bitmap bitmap = getBitmapFromURL(imageUrl);
        if (bitmap != null) {
            final NotificationCompat.Builder mBuilder =
                    NotificationUtils.getNotificationBuilder(context);

            NotificationCompat.BigPictureStyle bigPictureStyle =
                    new NotificationCompat.BigPictureStyle();
            bigPictureStyle
                    .setBigContentTitle(title)
                    .setSummaryText(Html.fromHtml(body).toString())
                    .bigPicture(bitmap);

            return mBuilder.setStyle(bigPictureStyle)
                    .setSmallIcon(icon)
                    .setLargeIcon(bitmap)
                    .setTicker(title)
                    .setAutoCancel(true)
                    .setWhen(time)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build();
        } else {
            return getBigTextNotification(context, title, body, time, pendingIntent);
        }
    }

    public static void showBigTextNotification(
            Context context,
            String title,
            String body,
            long time,
            final PendingIntent pendingIntent) {
        Notification notification =
                getBigTextNotification(context, title, body, time, pendingIntent);
        int uniqueNotificationId = AtomicNumberGenerator.getUniqueNumber();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(uniqueNotificationId, notification);
    }

    public static void showIncomingCallNotification(
            Context context,
            String title,
            String body,
            long time,
            int uniqueNotificationId,
            final PendingIntent contentPendingIntent,
            final PendingIntent declinePendingIntent,
            final PendingIntent receivePendingIntent) {
        Notification notification =
                getIncomingCallNotification(
                        context,
                        title,
                        body,
                        time,
                        contentPendingIntent,
                        declinePendingIntent,
                        receivePendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(uniqueNotificationId, notification);
    }

    public static void showBigImageNotification(
            Context context,
            String title,
            String body,
            long time,
            String imageUrl,
            final PendingIntent pendingIntent) {
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(
                        new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(
                                    @NonNull Bitmap resource,
                                    @Nullable Transition<? super Bitmap> transition) {
                                Notification notification =
                                        getBigImageNotification(
                                                context,
                                                title,
                                                body,
                                                time,
                                                resource,
                                                pendingIntent);
                                int uniqueNotificationId = AtomicNumberGenerator.getUniqueNumber();
                                NotificationManagerCompat notificationManager =
                                        NotificationManagerCompat.from(context);
                                notificationManager.notify(uniqueNotificationId, notification);
                            }

                            @Override
                            public void onLoadFailed(
                                    @Nullable Drawable errorDrawable) {
                                Notification notification =
                                        getBigTextNotification(
                                                context, title, body, time, pendingIntent);
                                int uniqueNotificationId = AtomicNumberGenerator.getUniqueNumber();
                                NotificationManagerCompat notificationManager =
                                        NotificationManagerCompat.from(context);
                                notificationManager.notify(uniqueNotificationId, notification);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
    }

    /** Downloading push notification image before displaying it in the notification tray */
    @Nullable
    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Uri getNotificationSound(Context context) {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + context.getPackageName()
                        + "/raw/voip_call_ringtone");
    }

    /** Play custom notification sound rather than the default one */
    public static void playNotificationSound(Context context) {
        try {
            Uri alarmSound =
                    Uri.parse(
                            ContentResolver.SCHEME_ANDROID_RESOURCE
                                    + "://"
                                    + context.getPackageName()
                                    + "/raw/voip_call_ringtone");
            Ringtone r = RingtoneManager.getRingtone(context, alarmSound);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Convert time stamp into human readable time */
    public static long getTimeMilliSec(String timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
            Date date = format.parse(timeStamp);
            if (date != null) return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
