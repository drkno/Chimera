package nz.co.makereti.chimera.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import nz.co.makereti.chimera.R;

public class InRangeService extends Service {
    private final IBinder binder;
    private final AtomicBoolean shouldScan;
    private final AtomicReference<String> scanSSID;
    private final AtomicInteger scanWait;
    private final RemoteCallbackList<IInRangeCallback> callbackList;
    private final Handler handler;
    public static final int SCANNING_NOTIFICATION = 0x101;
    public static final int FOUND_NOTIFICATION = 0x102;

    private WifiManager wifiManager;
    private PowerManager powerManager;
    private NotificationManager notificationManger;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private BroadcastReceiver onScanResult;
    private AtomicBoolean directionFound, isLeaving, ignoreFirst;

    public InRangeService() {
        binder = new InRangeServiceBinder();
        shouldScan = new AtomicBoolean(false);
        scanWait = new AtomicInteger(1000);
        scanSSID = new AtomicReference<>(null);
        callbackList = new RemoteCallbackList<>();
        handler = new Handler();
        directionFound = new AtomicBoolean(false);
        isLeaving = new AtomicBoolean(false);
        ignoreFirst = new AtomicBoolean(false);
    }

    public class InRangeServiceBinder extends Binder {
        public InRangeService getService() {
            return InRangeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startNewScan() {
        if (!wifiManager.isWifiEnabled() && !wifiManager.isScanAlwaysAvailable()) {
            wifiManager.setWifiEnabled(true);
            notifyCallbacks(new INotifyCallback() {
                @Override
                public void notify(IInRangeCallback callback) {
                    callback.onWifiEnabled();
                }
            });
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                wifiManager.startScan();
            }
        }, scanWait.get());
    }

    @Override
    public void onCreate() {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        notificationManger = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        onScanResult = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    return;
                }

                Log.d("InRangeService", "Scan results have arrived.");

                if (!ignoreFirst.get()) {
                    Log.d("InRangeService", "Ignoring first scan result.");
                    ignoreFirst.set(true);
                    startNewScan();
                    return;
                }

                final List<ScanResult> scanResults = wifiManager.getScanResults();
                Log.d("", "There are " + scanResults.size() + " scan results.");
                final String ssid = scanSSID.get();
                if (ssid == null) {
                    return; // prevent null reference
                }

                boolean networkFound = false;
                for (final ScanResult ap : scanResults) {
                    if (ap.SSID.equals(ssid)) {
                        networkFound = true;
                        break;
                    }
                }

                if (!directionFound.get()) {
                    directionFound.set(true);
                    isLeaving.set(networkFound);
                    Log.d("InRangeService", "You are " + (networkFound ? "leaving." : "arriving."));
                    notifyCallbacks(new INotifyCallback() {
                        @Override
                        public void notify(IInRangeCallback callback) {
                            callback.onDirectionFound(directionFound.get());
                        }
                    });
                }

                if (networkFound && !isLeaving.get()) {
                    notifyCallbacks(new INotifyCallback() {
                        @Override
                        public void notify(IInRangeCallback callback) {
                            callback.onSSIDFound();
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            boolean show = sharedPreferences.getBoolean("showFoundNotification", true);
                            if (show) {
                                Notification notification = buildNotification("Welcome Home!", false);
                                notificationManger.notify(FOUND_NOTIFICATION, notification);
                            }
                        }
                    });
                    stopScanning();
                }
                else if (!networkFound && isLeaving.get()) {
                    notifyCallbacks(new INotifyCallback() {
                        @Override
                        public void notify(IInRangeCallback callback) {
                            callback.onSSIDLost();
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            boolean show = sharedPreferences.getBoolean("showFoundNotification", true);
                            if (show) {
                                Notification notification = buildNotification("Have A Good Day!", false);
                                notificationManger.notify(FOUND_NOTIFICATION, notification);
                            }
                        }
                    });
                    stopScanning();
                }

                if (!shouldScan.get()) {
                    return;
                }

                startNewScan();
            }
        };
        IntentFilter scanResultIntentFilter = new IntentFilter();
        scanResultIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(onScanResult, scanResultIntentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(onScanResult);
        wifiManager = null;
        powerManager = null;
        notificationManger = null;
        callbackList.kill();
        super.onDestroy();
    }

    public void registerCallback(IInRangeCallback callback) {
        if (callback != null) {
            callbackList.register(callback);
        }
    }

    public void unregisterCallback(IInRangeCallback callback)  {
        if (callback != null) {
            callbackList.unregister(callback);
        }
    }

    private Notification buildNotification(String content, boolean ongoing) {
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(ongoing);
        builder.setContentText(content);
        builder.setContentTitle("Chimera");
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }

    public void startScanning(final String ssid, final int scanWaitTime) {
        if (scanSSID.get() != null) {
            throw new RuntimeException("Cannot start scanning when already scanning.");
        }
        scanSSID.set(ssid);
        scanWait.set(scanWaitTime);

        if (!wifiManager.isWifiEnabled() && !wifiManager.isScanAlwaysAvailable()) {
            wifiManager.setWifiEnabled(true);
            notifyCallbacks(new INotifyCallback() {
                @Override
                public void notify(IInRangeCallback callback) {
                    callback.onWifiEnabled();
                }
            });
        }

        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "RangeServiceScanLock");
        wifiLock.acquire();

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RangeServiceWakeLock");
        wakeLock.acquire();

        notifyCallbacks(new INotifyCallback() {
            @Override
            public void notify(IInRangeCallback callback) {
                callback.onStart();
            }
        });

        Notification scanningNotification = buildNotification("Scanning for '" + ssid + "'...", false);
        notificationManger.notify(SCANNING_NOTIFICATION, scanningNotification);
        startForeground(SCANNING_NOTIFICATION, scanningNotification);

        ignoreFirst.set(false);
        shouldScan.set(true);
        wifiManager.startScan();

        Log.d("InRangeService", "Scanning for SSID: " + ssid);
    }

    public void stopScanning() {
        if (scanSSID.get() == null) {
            throw new RuntimeException("Cannot stop scanning when not started.");
        }
        shouldScan.set(false);
        scanSSID.set(null);
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        stopForeground(true);
        notificationManger.cancel(SCANNING_NOTIFICATION);

        notifyCallbacks(new INotifyCallback() {
            @Override
            public void notify(IInRangeCallback callback) {
                callback.onStop();
            }
        });
    }

    public boolean isScanning() {
        return scanSSID.get() != null;
    }

    private synchronized void notifyCallbacks(INotifyCallback callback) {
        int n = callbackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            callback.notify(callbackList.getBroadcastItem(i));
        }
        callbackList.finishBroadcast();
    }
}
