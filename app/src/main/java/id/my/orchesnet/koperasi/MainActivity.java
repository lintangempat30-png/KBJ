package id.my.orchesnet.koperasi;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String URL =
            "https://orchesnet.my.id/koperasi";

    private static final int LOCATION_REQ = 1001;

    private WebView webView;
    private SwipeRefreshLayout refresh;

    private boolean rapatSudahDicek = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        buatNotificationChannel();


        refresh =
                findViewById(R.id.refresh);

        webView =
                findViewById(R.id.webview);


        /* =================================================
           WEBVIEW SETTINGS
           ================================================= */

        WebSettings s =
                webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);


        /* =================================================
           BRIDGE CETAK ANDROID
           ================================================= */

        webView.addJavascriptInterface(
                new PrintBridge(),
                "AndroidPrint"
        );


        /* =================================================
           WEBVIEW CLIENT
           ================================================= */

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {

                        super.onPageFinished(
                                view,
                                url
                        );


                        /*
                         * Jadwalkan pengingat angsuran
                         */

                        PengingatScheduler.jadwalkan(
                                MainActivity.this
                        );


                        /*
                         * Cek pengumuman rapat
                         * hanya sekali setiap APK dibuka
                         */

                        if (!rapatSudahDicek) {

                            rapatSudahDicek = true;

                            PengumumanRapat.cek(
                                    MainActivity.this
                            );
                        }


                        /*
                         * Hubungkan window.print()
                         * milik halaman website dengan
                         * PrintManager Android.
                         */

                        view.evaluateJavascript(
                                "window.print = function() {" +
                                "    AndroidPrint.print();" +
                                "};",
                                null
                        );
                    }


                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request
                    ) {

                        Uri uri =
                                request.getUrl();

                        String host =
                                uri.getHost();


                        /*
                         * Semua halaman milik
                         * orchesnet.my.id tetap
                         * dibuka di dalam WebView.
                         */

                        if (
                                host != null &&
                                (
                                    host.equals(
                                        "orchesnet.my.id"
                                    )
                                    ||
                                    host.endsWith(
                                        ".orchesnet.my.id"
                                    )
                                )
                        ) {

                            return false;
                        }


                        /*
                         * Link luar dibuka
                         * menggunakan browser Android.
                         */

                        startActivity(
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        uri
                                )
                        );

                        return true;
                    }
                }
        );


        /* =================================================
           WEB CHROME CLIENT
           ================================================= */

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        if (
                                checkSelfPermission(
                                        Manifest.permission
                                                .ACCESS_FINE_LOCATION
                                )
                                !=
                                PackageManager.PERMISSION_GRANTED
                        ) {

                            requestPermissions(
                                    new String[]{
                                        Manifest.permission
                                                .ACCESS_FINE_LOCATION,

                                        Manifest.permission
                                                .ACCESS_COARSE_LOCATION
                                    },
                                    LOCATION_REQ
                            );

                            callback.invoke(
                                    origin,
                                    true,
                                    false
                            );

                        } else {

                            callback.invoke(
                                    origin,
                                    true,
                                    false
                            );
                        }
                    }
                }
        );


        /* =================================================
           DOWNLOAD
           ================================================= */

        webView.setDownloadListener(
                (
                    url,
                    userAgent,
                    contentDisposition,
                    mimeType,
                    contentLength
                ) -> {

                    startActivity(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                            )
                    );
                }
        );


        /* =================================================
           SWIPE REFRESH
           ================================================= */

        refresh.setOnRefreshListener(
                () -> webView.reload()
        );


        /* =================================================
           ENABLE / DISABLE SWIPE REFRESH
           ================================================= */

        webView.setOnScrollChangeListener(
                (
                    v,
                    scrollX,
                    scrollY,
                    oldScrollX,
                    oldScrollY
                ) -> {

                    refresh.setEnabled(
                            scrollY == 0
                    );
                }
        );


        /* =================================================
           LOAD WEBSITE
           ================================================= */

        webView.loadUrl(URL);
    }


    /* =====================================================
       TOMBOL BACK ANDROID
       ===================================================== */

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    /* =====================================================
       BRIDGE CETAK
       ===================================================== */

    private class PrintBridge {

        @JavascriptInterface
        public void print() {

            runOnUiThread(
                    () -> cetakHalaman()
            );
        }
    }


    /* =====================================================
       CETAK HALAMAN WEBVIEW
       ===================================================== */

    private void cetakHalaman() {

        try {

            PrintManager printManager =
                    (PrintManager)
                            getSystemService(
                                    PRINT_SERVICE
                            );


            if (printManager == null) {

                Toast.makeText(
                        MainActivity.this,
                        "Fitur cetak tidak tersedia.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            String jobName =
                    "KBJ - " +
                    webView.getTitle();


            PrintDocumentAdapter
                    printAdapter =
                    webView.createPrintDocumentAdapter(
                            jobName
                    );


            PrintAttributes printAttributes =
                    new PrintAttributes.Builder()

                            .setMediaSize(
                                    PrintAttributes
                                            .MediaSize
                                            .ISO_A4
                            )

                            .setColorMode(
                                    PrintAttributes
                                            .COLOR_MODE_COLOR
                            )

                            .build();


            printManager.print(
                    jobName,
                    printAdapter,
                    printAttributes
            );


        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Gagal membuka fitur cetak.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    /* =====================================================
       NOTIFICATION CHANNEL
       ===================================================== */

    private void buatNotificationChannel() {

        if (
                Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O
        ) {

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );


            if (manager == null) {
                return;
            }


            /* =============================================
               CHANNEL PENGINGAT ANGSURAN
               ============================================= */

            Uri soundUri =
                    Uri.parse(
                            "android.resource://"
                            + getPackageName()
                            + "/"
                            + R.raw.bayar_angsuran
                    );


            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder()

                            .setUsage(
                                    AudioAttributes
                                            .USAGE_NOTIFICATION
                            )

                            .setContentType(
                                    AudioAttributes
                                            .CONTENT_TYPE_SONIFICATION
                            )

                            .build();


            NotificationChannel
                    channel =
                    new NotificationChannel(
                            "pengingat_angsuran",
                            "Pengingat Angsuran",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );


            channel.setDescription(
                    "Notifikasi pengingat pembayaran angsuran"
            );


            channel.setSound(
                    soundUri,
                    audioAttributes
            );


            manager.createNotificationChannel(
                    channel
            );


            /* =============================================
               CHANNEL PENGUMUMAN RAPAT
               ============================================= */

            Uri soundRapat =
                    Uri.parse(
                            "android.resource://"
                            + getPackageName()
                            + "/"
                            + R.raw.pertemuan
                    );


            AudioAttributes audioRapat =
                    new AudioAttributes.Builder()

                            .setUsage(
                                    AudioAttributes
                                            .USAGE_NOTIFICATION
                            )

                            .setContentType(
                                    AudioAttributes
                                            .CONTENT_TYPE_SONIFICATION
                            )

                            .build();


            NotificationChannel
                    channelRapat =
                    new NotificationChannel(
                            "pengumuman_rapat",
                            "Pengumuman Rapat",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );


            channelRapat.setDescription(
                    "Notifikasi pengumuman jadwal rapat koperasi"
            );


            channelRapat.setSound(
                    soundRapat,
                    audioRapat
            );


            manager.createNotificationChannel(
                    channelRapat
            );
        }
    }
}