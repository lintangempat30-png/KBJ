package id.my.orchesnet.koperasi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PengumumanRapat {

    private static final String API_URL =
            "https://orchesnet.my.id/koperasi/api/jadwal_rapat.php";

    public static void cek(Context context) {

        new Thread(() -> {

            try {

                cekJadwal(context);

            } catch (Exception e) {

                e.printStackTrace();

            }

        }).start();
    }


    private static void cekJadwal(Context context) throws Exception {

        /*
         * Ambil cookie login dari WebView.
         */
        CookieManager cookieManager =
                CookieManager.getInstance();

        String cookie =
                cookieManager.getCookie(API_URL);

        if (cookie == null || cookie.trim().isEmpty()) {
            return;
        }


        /*
         * Hubungi API jadwal rapat.
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
         * Baca response API.
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
         * Tidak ada jadwal rapat aktif.
         */
        if (!data.optBoolean("ada_rapat", false)) {
            return;
        }


        String judul =
                data.optString(
                        "judul",
                        "Pengumuman Rapat"
                );

        String tanggal =
                data.optString(
                        "tanggal_rapat_tampilan",
                        "-"
                );

        String jam =
                data.optString(
                        "jam_rapat_tampilan",
                        "-"
                );

        String tempat =
                data.optString(
                        "tempat",
                        "-"
                );


        /*
         * Isi notifikasi.
         */
        String isi =
                "Ada pengumuman Pertemuan Koperasi "
                + "Keluarga Bani Jumropi. "
                + "Rapat "
                + judul
                + " pada "
                + tanggal
                + " pukul "
                + jam
                + " di "
                + tempat
                + ".";


        kirimNotifikasi(
                context,
                judul,
                isi
        );
    }


    private static void kirimNotifikasi(
            Context context,
            String judul,
            String isi
    ) {

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );


        if (manager == null) {
            return;
        }


        /*
         * Android 13+ membutuhkan izin notifikasi.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (
                    context.checkSelfPermission(
                            android.Manifest.permission.POST_NOTIFICATIONS
                    )
                    != PackageManager.PERMISSION_GRANTED
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
                            "pengumuman_rapat"
                    );

        } else {

            builder =
                    new Notification.Builder(
                            context
                    );
        }


        builder
                .setSmallIcon(
                        R.mipmap.ic_launcher
                )
                .setContentTitle(
                        "Pengumuman Rapat KBJ"
                )
                .setContentText(
                        judul
                )
                .setStyle(
                        new Notification.BigTextStyle()
                                .bigText(isi)
                )
                .setAutoCancel(true)
                .setPriority(
                        Notification.PRIORITY_HIGH
                );


        /*
         * ID dibuat baru setiap APK dibuka,
         * sehingga pengumuman dapat muncul lagi.
         */
        int notificationId =
                (int) (
                        System.currentTimeMillis()
                                & 0x7fffffff
                );


        manager.notify(
                notificationId,
                builder.build()
        );
    }
}