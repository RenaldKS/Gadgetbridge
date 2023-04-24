package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BLEScanService extends Service {
    public static final String COMMAND_SCAN_DEVICE = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.START_SCAN_FOR_DEVICE";
    public static final String COMMAND_START_SCAN_ALL = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.START_SCAN_ALL";
    public static final String COMMAND_STOP_SCAN_ALL = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.command.STOP_SCAN_ALL";

    public static final String EVENT_DEVICE_FOUND = "nodomain.freeyourgadget.gadgetbridge.service.ble.scan.event.DEVICE_FOUND";

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";
    public static final String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";

    private static final String TAG = "BLEScanService";

    LocalBroadcastManager localBroadcastManager;

    private BluetoothLeScanner scanner;
    // private final ArrayList<ScanFilter> currentFilters = new ArrayList<>();

    private enum ScanningState {
        NOT_SCANNING,
        SCANNING_WITHOUT_FILTERS,
        SCANNING_WITH_FILTERS;

        public boolean isDoingAnyScan(){
            return ordinal() > NOT_SCANNING.ordinal();
        }

        public boolean shouldDiscardAfterFirstMatch(){
            return this == SCANNING_WITH_FILTERS;
        }
    };
    private ScanningState currentState = ScanningState.NOT_SCANNING;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            Intent intent = new Intent(EVENT_DEVICE_FOUND);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());
            localBroadcastManager.sendBroadcast(intent);

            // device found, attempt connection
            // stop scanning for device for now
            // will restart when connection attempt fails
            if(currentState.shouldDiscardAfterFirstMatch()) {
                // stopScanningForDevice(device.getAddress());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        scanner = manager.getAdapter().getBluetoothLeScanner();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        startForeground();
    }

    private void startForeground(){
        Notification serviceNotification =
                new NotificationCompat
                    .Builder(this, GB.NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Scan service")
                    .setContentText("Scanning x devices")
                    .build();

        startForeground(GB.NOTIFICATION_ID_SCAN, serviceNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null){
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if(action == null){
         return START_NOT_STICKY;
        }
        switch (action) {
            case COMMAND_SCAN_DEVICE:
                handleScanDevice(intent);
                break;
            case COMMAND_START_SCAN_ALL:
                handleScanAll(intent);
                break;
            case COMMAND_STOP_SCAN_ALL:
                handleStopScanAll(intent);
                break;
            default:
                return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void handleStopScanAll(Intent intent){
        restartScan(true);
    }

    private void handleScanAll(Intent intent){
        if(currentState != ScanningState.SCANNING_WITHOUT_FILTERS){
            restartScan(false);
        }
    }

    private void handleScanDevice(Intent intent){
        /*
        GBDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        if(device == null){
            return;
        }
        scanForDevice(device);
         */
        restartScan(true);
    }


    /*private boolean isDeviceIncludedInCurrentFilters(GBDevice device){
        for(ScanFilter currentFilter : currentFilters){
            if(device.getAddress().equals(currentFilter.getDeviceAddress())){
                return true;
            }
        }
        return false;
    }
    */

    /*
    private void stopScanningForDevice(GBDevice device){
        this.stopScanningForDevice(device.getAddress());
    }
     */

    /*
    private void stopScanningForDevice(String deviceAddress){
        currentFilters.removeIf(scanFilter -> scanFilter
                .getDeviceAddress()
                .equals(deviceAddress)
        );

        restartScan(true);
    }
    */

    /*
    private void scanForDevice(GBDevice device){
        if(isDeviceIncludedInCurrentFilters(device)){
            // already scanning for device
            return;
        }
        ScanFilter deviceFilter = new ScanFilter.Builder()
                .setDeviceAddress(device.getAddress())
                .build();

        currentFilters.add(deviceFilter);

        // restart scan here
        restartScan(true);
    }
    */

    BroadcastReceiver deviceStateUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GBDevice.DeviceUpdateSubject subject =
                    (GBDevice.DeviceUpdateSubject)
                            intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

            if(subject != GBDevice.DeviceUpdateSubject.CONNECTION_STATE){
                return;
            }
            Log.d(TAG, "onReceive: received device connection change: " + subject);
            restartScan(true);
        }
    };

    private void registerReceivers(){
        localBroadcastManager.registerReceiver(
                deviceStateUpdateReceiver,
                new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED)
        );
    }

    private void restartScan(boolean applyFilters){
        if(currentState.isDoingAnyScan()){
            scanner.stopScan(scanCallback);
        }
        ArrayList<ScanFilter> scanFilters = null;

        if(applyFilters) {
            List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();

            scanFilters = new ArrayList<>(devices.size());

            for (GBDevice device : devices) {
                if (device.getState() == GBDevice.State.WAITING_FOR_SCAN) {
                    scanFilters.add(new ScanFilter.Builder()
                            .setDeviceAddress(device.getAddress())
                            .build()
                    );
                }
            }

            if(scanFilters.size() == 0){
                // no need to start scanning
                Log.d(TAG, "restartScan: stopping BLE scan, no devices");
                currentState = ScanningState.NOT_SCANNING;
                return;
            }
        }

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // enforced anyway in background
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setLegacy(false)
                .build();

        scanner.startScan(scanFilters, scanSettings, scanCallback);
        if(applyFilters) {
            Log.d(TAG, "restartScan: started scan for " + scanFilters.size() + " devices");
            currentState = ScanningState.SCANNING_WITH_FILTERS;
        }else{
            Log.d(TAG, "restartScan: started scan for all devices");
            currentState = ScanningState.SCANNING_WITHOUT_FILTERS;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}