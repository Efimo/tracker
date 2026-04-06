package com.stepan.tracker;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class NotificationHelper {

    public static final String CHANNEL_ID = "stepan_tracker";
    public static final String CHANNEL_NAME = "Трекер Степана";

    // Schedule data: {hour, minute, title, message, icon_emoji}
    private static final String[][] SCHEDULE = {
        {"6", "25", "Доброе утро!", "Пора вставать. Стакан воды с лимоном ждёт \uD83D\uDCA7"},
        {"6", "50", "Тренировка через 10 мин!", "Подготовь коврик. День A или B — проверь в трекере \uD83D\uDCAA"},
        {"7", "40", "Время завтрака", "Творог 200г + банан = 430 ккал. Не забудь воду перед едой \uD83C\uDF73"},
        {"11", "0", "Перекус!", "Яблоко + орехи. Выпей стакан воды \uD83C\uDF4E"},
        {"12", "50", "Обед через 10 мин", "Грудка + гречка + салат = 540 ккал. Вода перед едой! \uD83C\uDF72"},
        {"16", "0", "Перекус: творог", "150г творога. Осталось 2 стакана воды до нормы? \uD83E\uDD5B"},
        {"17", "50", "Конец работы через 10 мин!", "Впереди прогулка 30 мин. Подготовь наушники \uD83D\uDEB6"},
        {"18", "35", "Скоро ужин", "Рыба + овощи = 490 ккал. Лёгкий и полезный \uD83C\uDF5D"},
        {"19", "25", "Время с Таней!", "Отложи телефон. Минимум 1 час вместе \u2764\uFE0F"},
        {"21", "30", "До сна — 1 час", "Заверши дела. Отложи экраны. Свет приглушить \uD83C\uDF19"},
        {"22", "0", "Вечерний чек-лист", "Запиши вес, отметь задачи в трекере. Как прошёл день? \u2705"},
        {"22", "25", "Пора спать!", "8 часов сна = 80% потери — жир, а не мышцы. Доброй ночи! \uD83D\uDE34"},
    };

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Напоминания по расписанию");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    public static void scheduleAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        for (int i = 0; i < SCHEDULE.length; i++) {
            int hour = Integer.parseInt(SCHEDULE[i][0]);
            int minute = Integer.parseInt(SCHEDULE[i][1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            // If time already passed today, schedule for tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(ctx, NotifReceiver.class);
            intent.putExtra("id", i);
            intent.putExtra("title", SCHEDULE[i][2]);
            intent.putExtra("msg", SCHEDULE[i][3]);

            PendingIntent pi = PendingIntent.getBroadcast(
                ctx, i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Repeating daily
            am.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pi
            );
        }
    }

    // BroadcastReceiver that shows the notification
    public static class NotifReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            int id = intent.getIntExtra("id", 0);
            String title = intent.getStringExtra("title");
            String msg = intent.getStringExtra("msg");
            if (title == null) title = "Трекер";
            if (msg == null) msg = "";

            // Open app on tap
            Intent openApp = new Intent(ctx, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent tapIntent = PendingIntent.getActivity(
                ctx, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(tapIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(id, builder.build());
        }
    }

    // Re-schedule after reboot
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                createChannel(ctx);
                scheduleAll(ctx);
            }
        }
    }
}
