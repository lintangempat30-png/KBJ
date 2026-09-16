package id.my.orchesnet.koperasi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PengingatReceiver extends BroadcastReceiver {

    private static final String API_URL =
            "https://orchesnet.my.id/koperasi/api/jadwal_angsuran.php";

    @Override
    public void onReceive(Context context, Intent intent) {

        final PendingResult pendingResult = goAsync();

        new Thread(() -> {

            try {

                cekJadwal(context);

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                pendingResult.finish();
            }

        }).start();
    }


    private void cekJadwal(Context context) throws Exception {

        /*
         * Ambil cookie login anggota dari WebView.
         */
        CookieManager cookieManager =
                CookieManager.getInstance();

        String cookie =
                cookieManager.getCookie(API_URL);

        if (cookie == null || cookie.trim().isEmpty()) {
            return;
        }


        /*
         * Hubungi API.
         */
        URL url =
                new URL(API_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        connection.setRequestProperty(
                "Cookie",
                cookie
        );

        connection.setRequestProperty(
                "Accept",
                "application/json"
        );


        int responseCode =
                connection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {

            connection.disconnect();
            return;
        }


        /*
         * Baca hasil API.
         */
        InputStream inputStream =
                connection.getInputStream();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                "UTF-8"
                        )
                );

        StringBuilder response =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        inputStream.close();
        connection.disconnect();


        /*
         * Baca JSON.
         */
        JSONObject data =
                new JSONObject(
                        response.toString()
                );


        if (!data.optBoolean("success", false)) {
            return;
        }


        /*
         * Kalau tidak punya pinjaman,
         * jangan kirim notifikasi.
         */
        if (!data.optBoolean("ada_pinjaman", false)) {
            return;
        }


        /*
         * Kalau pinjaman sudah lunas,
         * jangan kirim notifikasi.
         */
        if (data.optBoolean("sudah_lunas", false)) {
            return;
        }


        int hariTersisa =
                data.optInt("hari_tersisa", 999);


        /*
         * Kita hanya mengirim pengingat pada:
         *
         * H-7
         * H-3
         * H-1
         * Hari H
         */
        if (
                hariTersisa != 7 &&
                hariTersisa != 3 &&
                hariTersisa != 1 &&
                hariTersisa != 0
        ) {
            return;
        }


        int angsuranKe =
                data.optInt("angsuran_ke", 0);

        String tanggal =
                data.optString(
                        "tanggal_jatuh_tempo_tampilan",
                        "-"
                );

        String nomorPinjaman =
                data.optString(
                        "nomor_pinjaman",
                        ""
                );


        /*
         * Tentukan isi notifikasi.
         */
        String isi;

        if (hariTersisa == 0) {

            isi =
                    "Hari ini jatuh tempo angsuran ke-"
                    + angsuranKe
                    + ". Silakan melakukan pembayaran.";

        } else {

            isi =
                    "Angsuran ke-"
                    + angsuranKe
                    + " jatuh tempo "
                    + tanggal
                    + " (H-"
                    + hariTersisa
                    + ").";
        }


        kirimNotifikasi(
                context,
                angsuranKe,
                nomorPinjaman,
                isi
        );
    }


    private void kirimNotifikasi(
            Context context,
            int angsuranKe,
            String nomorPinjaman,
            String isi
    ) {

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );


        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (
                    context.checkSelfPermission(
                            android.Manifest.permission.POST_NOTIFICATIONS
                    ) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return;
            }
        }


        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            builder =
                    new Notification.Builder(
                            context,
                            "pengingat_angsuran"
                    );

        } else {

            builder =
                    new Notification.Builder(context);
        }


        builder
                .setSmallIcon(
                        R.mipmap.ic_launcher
                )
                .setContentTitle(
                        "Pengingat Angsuran KBJ"
                )
                .setContentText(isi)
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(isi)
                )
                .setAutoCancel(true)
                .setPriority(
                        Notification.PRIORITY_HIGH
                );


        /*
         * ID dibuat berdasarkan nomor angsuran
         * supaya pengingat angsuran yang sama
         * tidak menggunakan ID yang sama
         * dengan angsuran lainnya.
         */
        int notificationId =
                7000 + angsuranKe;


        manager.notify(
                notificationId,
                builder.build()
        );
    }
}