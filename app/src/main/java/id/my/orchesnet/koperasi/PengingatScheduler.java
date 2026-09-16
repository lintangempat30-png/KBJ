package id.my.orchesnet.koperasi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class PengingatScheduler {

    private static final int REQUEST_CODE = 3001;

    public static void jadwalkan(Context context) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent =
                new Intent(context, PengingatReceiver.class);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * Jalankan pengecekan pertama sekitar 1 menit
         * setelah aplikasi dibuka.
         */
        long pertama =
                SystemClock.elapsedRealtime()
                        + (60 * 1000);

        /*
         * Setelah itu cek setiap 24 jam.
         */
        long satuHari =
                24 * 60 * 60 * 1000L;

        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                pertama,
                satuHari,
                pendingIntent
        );
    }
}