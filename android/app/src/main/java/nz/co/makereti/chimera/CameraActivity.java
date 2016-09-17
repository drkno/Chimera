package nz.co.makereti.chimera;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

public class CameraActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String hostname = sharedPreferences.getString("garageServerHostname", "");
        int port = Integer.parseInt(sharedPreferences.getString("garageServerCameraPort", "8082"));
        String url = "http://" + hostname + ":" + Integer.toString(port) + "/";
        WebView view = (WebView) findViewById(R.id.cameraWebView);
        view.setBackgroundColor(Color.parseColor("#777777"));
        view.getSettings().setLoadWithOverviewMode(true);
        view.getSettings().setUseWideViewPort(true);
        view.loadUrl(url);
    }

    public void back(View view) {
        this.finish();
    }
}
