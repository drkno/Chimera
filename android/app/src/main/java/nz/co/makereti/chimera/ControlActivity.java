package nz.co.makereti.chimera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import nz.co.makereti.chimera.garage.DoorStatus;
import nz.co.makereti.chimera.garage.GarageDoorControl;
import nz.co.makereti.chimera.garage.IGarageStatus;
import nz.co.makereti.chimera.service.IInRangeCallback;
import nz.co.makereti.chimera.service.InRangeService;
import nz.co.makereti.chimera.settings.SettingsActivity;

public class ControlActivity extends AppCompatActivity {

    private Intent service;
    private InRangeService inRangeService;
    private Timer timer;

    private IInRangeCallback inRangeCallback = new IInRangeCallback() {
        private IBinder binder = new Binder();

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
            GarageDoorControl.openDoor(getApplicationContext());
            toggleStop();
            startActivity(new Intent(ControlActivity.this, TimeoutActivity.class));
        }

        @Override
        public void onSSIDLost() {
            GarageDoorControl.closeDoor(getApplicationContext());
            toggleStop();
        }

        @Override
        public void onDirectionFound(boolean direction) {
            if (direction) {
                GarageDoorControl.openDoor(getApplicationContext());
            }
        }
    };

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

        if (checkSelfPermission(Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WAKE_LOCK}, 0x02);
            return false;
        }

        if (checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.VIBRATE}, 0x03);
            return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        timer.purge();
        timer = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final TextView doorStatus = (TextView) findViewById(R.id.doorStatus);
        final Context context = getApplicationContext();
        final IGarageStatus status = new IGarageStatus() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDoorStatus(final DoorStatus state) {
                ControlActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        doorStatus.setText("Door " + state.toString());
                    }
                });
            }
        };
        final long period = 1000;
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                GarageDoorControl.getDoorStatus(context, status);
            }
        }, 0, period);
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
        else {
            startActivity(new Intent(this, CameraActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void toggleStop() {
        final TextView scanningText = (TextView) findViewById(R.id.toggleScanningText);
        final ImageButton scanButton = (ImageButton) findViewById(R.id.toggleScanningButton);
        scanningText.setText("Start Scanning");
        scanButton.setRotation(0);
    }

    @SuppressLint("SetTextI18n")
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
        GarageDoorControl.getDoorStatus(getApplicationContext(), new IGarageStatus() {
            @Override
            public void onDoorStatus(DoorStatus state) {
                if (state == DoorStatus.Open) {
                    GarageDoorControl.closeDoor(getApplicationContext());
                } else if (state == DoorStatus.Closed) {
                    GarageDoorControl.openDoor(getApplicationContext());
                } else {
                    Toast.makeText(getApplicationContext(), "Can not do that at this time...", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
