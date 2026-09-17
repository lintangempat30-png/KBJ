package id.my.orchesnet.koperasi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
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
    private static final String URL = "https://orchesnet.my.id/koperasi";
    private static final int LOCATION_REQ = 1001;
    private WebView webView;
    private SwipeRefreshLayout refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    buatNotificationChannel();
	PengumumanRapat.cek(MainActivity.this);

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

        webView.setWebViewClient(new WebViewClient() {

        @Override
          public void onPageFinished(WebView view, String url) {
          super.onPageFinished(view, url);

          PengingatScheduler.jadwalkan(MainActivity.this);
        }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String host = uri.getHost();

        if (host != null &&
                (host.equals("orchesnet.my.id") ||
                 host.endsWith(".orchesnet.my.id"))) {

            return false;
        }

        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        return true;
    }
});

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ);
                    callback.invoke(origin, true, false);
                } else {
                    callback.invoke(origin, true, false);
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        refresh.setOnRefreshListener(() -> webView.reload());
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> refresh.setEnabled(scrollY == 0));

        webView.loadUrl(URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
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
                        "pengingat_angsuran",
                        "Pengingat Angsuran",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Notifikasi pengingat pembayaran angsuran"
        );

        channel.setSound(
                soundUri,
                audioAttributes
        );

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
		if (manager != null) {

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
                            AudioAttributes.USAGE_NOTIFICATION
                    )
                    .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build();

    NotificationChannel channelRapat =
            new NotificationChannel(
                    "pengumuman_rapat",
                    "Pengumuman Rapat",
                    NotificationManager.IMPORTANCE_HIGH
            );

    channelRapat.setDescription(
            "Notifikasi pengumuman jadwal rapat koperasi"
    );

    channelRapat.setSound(
            soundRapat,
            audioRapat
    );

    manager.createNotificationChannel(channelRapat);
}
    }
}
}