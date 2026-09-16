package id.my.orchesnet.koperasi;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String URL = "https://orchesnet.my.id/koperasi";
    private static final int LOCATION_REQ = 1001;

    private WebView webView;
    private SwipeRefreshLayout refresh;
	private static final String CHANNEL_ID = "pengingat_angsuran";
	
	private void buatNotificationChannel() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        Uri soundUri = Uri.parse(
                "android.resource://" + getPackageName() + "/" + R.raw.bayar_angsuran
        );

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Pengingat Angsuran",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Notifikasi pengingat pembayaran angsuran KBJ"
        );

        channel.setSound(soundUri, audioAttributes);

        NotificationManager notificationManager =
                getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(channel);
    }
}

private void kirimNotifikasiTes() {

    NotificationManager notificationManager =
            getSystemService(NotificationManager.class);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    2001
            );

            return;
        }
    }

    Uri soundUri = Uri.parse(
            "android.resource://" + getPackageName()
                    + "/" + R.raw.bayar_angsuran
    );

    Notification.Builder builder;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        builder = new Notification.Builder(this, CHANNEL_ID);

    } else {

        builder = new Notification.Builder(this)
                .setSound(soundUri);
    }

    builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pengingat Angsuran")
            .setContentText(
                    "Ini adalah notifikasi percobaan KBJ."
            )
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true);

    notificationManager.notify(1001, builder.build());
}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buatNotificationChannel();

        refresh = findViewById(R.id.refresh);
        webView = findViewById(R.id.webview);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);

        /*
         * Jembatan dari JavaScript website ke Android.
         * window.print() akan diarahkan ke fungsi printPage().
         */
        webView.addJavascriptInterface(new PrintBridge(), "AndroidPrint");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                /*
                 * Ganti window.print() milik halaman web
                 * dengan fungsi cetak native Android.
                 */
                view.evaluateJavascript(
                        "window.print = function() { AndroidPrint.printPage(); };",
                        null
                );
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                Uri uri = request.getUrl();
                String host = uri.getHost();

                if (host != null &&
                        (host.equals("orchesnet.my.id")
                                || host.endsWith(".orchesnet.my.id"))) {

                    return false;
                }

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback
            ) {
                if (checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            LOCATION_REQ
                    );

                    callback.invoke(origin, true, false);

                } else {
                    callback.invoke(origin, true, false);
                }
            }
        });

        webView.setDownloadListener(
                (url, userAgent, contentDisposition, mimeType, contentLength) -> {
                    startActivity(
                            new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                            )
                    );
                }
        );

        refresh.setOnRefreshListener(() -> webView.reload());

        webView.setOnScrollChangeListener(
                (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        refresh.setEnabled(scrollY == 0)
        );

        webView.loadUrl(URL);
    }
	
	@Override
public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
) {
    super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
    );

    if (requestCode == 2001) {
        if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            kirimNotifikasiTes();
        }
    }
}

    /*
     * Jembatan JavaScript -> Android
     */
    private class PrintBridge {

        @JavascriptInterface
        public void printPage() {

            runOnUiThread(() -> {

                PrintManager printManager =
                        (PrintManager) getSystemService(Context.PRINT_SERVICE);

                PrintDocumentAdapter printAdapter =
                        webView.createPrintDocumentAdapter("Laporan Koperasi");

                PrintAttributes attributes =
                        new PrintAttributes.Builder()
                                .setMediaSize(
                                        PrintAttributes.MediaSize.ISO_A4
                                )
                                .setMinMargins(
                                        PrintAttributes.Margins.NO_MARGINS
                                )
                                .build();

                printManager.print(
                        "Laporan Koperasi",
                        printAdapter,
                        attributes
                );
            });
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}