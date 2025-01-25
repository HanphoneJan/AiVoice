package com.example.aivoice.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

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

    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    private final MutableLiveData<Set<BluetoothDevice>> pairedDevices = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReceiverRegistered = new MutableLiveData<>(false);
    private Context context;
    private  Bluetooth bluetooth = new Bluetooth();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public BluetoothViewModel() {
        //空
    }

    public void setContext(Context context) {
        bluetooth.setContext(context);
        this.context=context;
    }

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

    public LiveData<Boolean> getIsReceiverRegistered() {
        return isReceiverRegistered;
    }

    public void checkBluetoothStatus() {
        try {
            isBluetoothEnabled.setValue(bluetooth.isBluetoothEnabled());
        } catch (Exception e) {
            postError("Error 检查蓝牙状态: " + e.getMessage());
        }
    }

    public void fetchPairedDevices() {
        executorService.execute(() -> {
            try {
                Set<BluetoothDevice> devices = bluetooth.getPairedDevices();
                pairedDevices.postValue(devices != null ? devices : new HashSet<>());
            } catch (Exception e) {
                postError("Error 获取蓝牙设备列表: " + e.getMessage());
            }
        });
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            postError("连接设备不能为空.");
            return;
        }
        if (!bluetooth.hasBluetoothPermissions()) {
            postError("蓝牙连接权限未打开");
            return;
        }
        executorService.execute(() -> {
            try {
                boolean success = bluetooth.connectToDevice(device);
                isConnected.postValue(success);
                connectionStatus.postValue(success ? "Connected to " + device.getName() : "Connection failed");

                if (!success) {
                    postError("连接设备失败: " + device.getName());
                }
            } catch (SecurityException e) {
                postError("SecurityException: 蓝牙连接权限未打开");
            } catch (Exception e) {
                postError("连接设备错误: " + e.getMessage());
            }
        });
    }

    public void startScanning() {
        try {
            bluetooth.startDiscovery();
            isReceiverRegistered.setValue(true);
        } catch (Exception e) {
            postError("扫描蓝牙设备错误 " + e.getMessage());
        }
    }

    public void stopScanning() {
        bluetooth.stopDiscovery();
        isReceiverRegistered.setValue(false);
    }

    private void postError(String message) {
        Log.e(TAG, message);
        errorMessage.postValue(message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            executorService.shutdownNow();
            bluetooth.cleanup();
        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel cleanup: " + e.getMessage(), e);
        }
    }
}
