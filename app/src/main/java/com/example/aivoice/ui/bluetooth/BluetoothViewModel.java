package com.example.aivoice.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.bluetooth.Bluetooth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BluetoothViewModel extends ViewModel {

    private static final String TAG = "BluetoothViewModel";

    // LiveData fields for UI updates
    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    private final MutableLiveData<Set<BluetoothDevice>> pairedDevices = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final Bluetooth bluetooth;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Constructor
     *
     * @param context Application context (cannot be null)
     */
    public BluetoothViewModel(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        bluetooth = new Bluetooth(context);
    }

    // Public LiveData getters
    public LiveData<Boolean> getIsBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public LiveData<Set<BluetoothDevice>> getPairedDevices() {
        return pairedDevices;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public LiveData<String> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Check if Bluetooth is enabled and update LiveData.
     */
    public void checkBluetoothStatus() {
        try {
            isBluetoothEnabled.setValue(bluetooth.isBluetoothEnabled());
        } catch (Exception e) {
            postError("Error checking Bluetooth status: " + e.getMessage());
        }
    }

    /**
     * Fetch paired Bluetooth devices and update LiveData.
     */
    public void fetchPairedDevices() {
        executorService.execute(() -> {
            try {
                Set<BluetoothDevice> devices = bluetooth.getPairedDevices();
                pairedDevices.postValue(devices != null ? devices : new HashSet<>());
            } catch (Exception e) {
                postError("Error fetching paired devices: " + e.getMessage());
            }
        });
    }

    /**
     * Connect to a Bluetooth device.
     *
     * @param device The target Bluetooth device
     */
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            postError("Cannot connect to a null device.");
            return;
        }

        // 检查权限
        if (!bluetooth.hasBluetoothPermissions()) {
            postError("Missing BLUETOOTH_CONNECT permission.");
            return;
        }

        executorService.execute(() -> {
            try {
                // 调用连接方法
                boolean success = bluetooth.connectToDevice(device);
                isConnected.postValue(success);
                connectionStatus.postValue(success ? "Connected to " + device.getName() : "Connection failed");

                if (!success) {
                    postError("Failed to connect to device: " + device.getName());
                }
            } catch (SecurityException e) {
                postError("SecurityException: Missing BLUETOOTH_CONNECT permission.");
            } catch (Exception e) {
                postError("Error connecting to device: " + e.getMessage());
            }
        });
    }



    /**
     * Disconnect from the current Bluetooth device.
     */
    public void disconnect() {
        executorService.execute(() -> {
            try {
                bluetooth.disconnect();
                isConnected.postValue(false);
                connectionStatus.postValue("Disconnected");
            } catch (Exception e) {
                postError("Error disconnecting: " + e.getMessage());
            }
        });
    }

    /**
     * Post an error message to the LiveData and log it.
     *
     * @param message The error message to post
     */
    private void postError(String message) {
        Log.e(TAG, message);
        errorMessage.postValue(message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            executorService.shutdownNow(); // Gracefully shut down the executor service
            if (bluetooth != null) {
                bluetooth.cleanup(); // Clean up Bluetooth resources
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel cleanup: " + e.getMessage(), e);
        }
    }
}
