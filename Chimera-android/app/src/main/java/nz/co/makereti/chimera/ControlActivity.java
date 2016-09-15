package nz.co.makereti.chimera;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

import nz.co.makereti.chimera.garage.ApiResult;
import nz.co.makereti.chimera.garage.GarageDoorControl;
import nz.co.makereti.chimera.service.IInRangeCallback;
import nz.co.makereti.chimera.service.InRangeService;
import nz.co.makereti.chimera.settings.SettingsActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ControlActivity extends AppCompatActivity {

    private Intent service;
    private InRangeService inRangeService;
    private AtomicBoolean doorState = new AtomicBoolean(false);

    private IInRangeCallback inRangeCallback = new IInRangeCallback() {
        class LocalBinder extends Binder {
            IInRangeCallback getUnderlyingInterface() {
                return inRangeCallback;
            }
        }

        private IBinder binder = new LocalBinder();

        @Override
        public IBinder asBinder() {
            return binder;
        }

        @Override
        public void onStart() {
            Toast.makeText(getApplicationContext(), "Scanning Started", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStop() {
            NotificationManager notificationManger = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManger.cancel(0);
        }

        @Override
        public void onWifiEnabled() {
            Toast.makeText(getApplicationContext(), "Wifi enabled.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSSIDFound() {
            openDoor();
            toggleStop();
        }

        @Override
        public void onSSIDLost() {
            closeDoor();
            toggleStop();
        }

        @Override
        public void onDirectionFound(boolean direction) {
            if (direction) {
                openDoor();
            }
        }
    };

    private void openDoor() {
        Log.d("ControlActivity", "Open Door");
        Call<ApiResult> result = GarageDoorControl.get(getApplicationContext()).open();
        result.enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                Toast.makeText(getApplicationContext(), "The garage door was opened for you.", Toast.LENGTH_LONG).show();
                doorState.set(true);
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Hmmmm. The garage door refuses to open.", Toast.LENGTH_LONG).show();
                Log.wtf("ControlActivity", "Failed to open the door.", t);
            }
        });
    }

    private void closeDoor() {
        Log.d("ControlActivity", "Close Door");
        Call<ApiResult> result = GarageDoorControl.get(getApplicationContext()).close();
        result.enqueue(new Callback<ApiResult>() {
            @Override
            public void onResponse(Call<ApiResult> call, Response<ApiResult> response) {
                Toast.makeText(getApplicationContext(), "The garage door was closed for you.", Toast.LENGTH_LONG).show();
                doorState.set(false);
            }

            @Override
            public void onFailure(Call<ApiResult> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Hmmmm. The garage door refuses to close.", Toast.LENGTH_LONG).show();
                Log.wtf("ControlActivity", "Failed to close the door.", t);
            }
        });
    }

    protected ServiceConnection mServerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            inRangeService = ((InRangeService.InRangeServiceBinder) binder).getService();
            inRangeService.registerCallback(inRangeCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            inRangeService.unregisterCallback(inRangeCallback);
            inRangeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        service = new Intent(this, InRangeService.class);
        bindService(service, mServerConn, Context.BIND_AUTO_CREATE);
        startService(service);

        checkPermissionsAreMet();
    }

    private boolean checkPermissionsAreMet() {
        String packageName = getApplicationContext().getPackageName();
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
            return false;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x01);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(service);
        unbindService(mServerConn);
        stopService(service);
        service = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleStop() {
        final TextView scanningText = (TextView) findViewById(R.id.toggleScanningText);
        final ImageButton scanButton = (ImageButton) findViewById(R.id.toggleScanningButton);
        scanningText.setText("Start Scanning");
        scanButton.setRotation(0);
    }

    private void toggleStart() {
        final TextView scanningText = (TextView) findViewById(R.id.toggleScanningText);
        final ImageButton scanButton = (ImageButton) findViewById(R.id.toggleScanningButton);
        scanningText.setText("Stop Scanning");
        scanButton.setRotation(180);
    }

    public void toggleScanning(View view) {

        if (inRangeService.isScanning()) {
            inRangeService.stopScanning();
            toggleStop();
        }
        else {
            if (!checkPermissionsAreMet()) return;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String ssid = sharedPreferences.getString("garageSSIDTrigger", "RabbitLodge");
            int scanDelay = Integer.parseInt(sharedPreferences.getString("wifiScanDelay", "1000"));
            inRangeService.startScanning(ssid, scanDelay);
            toggleStart();
        }
    }

    public void toggleDoor(View view) {
        if (doorState.get()) {
            closeDoor();
        }
        else {
            openDoor();
        }
    }
}
