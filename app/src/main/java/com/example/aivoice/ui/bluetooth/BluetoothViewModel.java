package com.example.aivoice.ui.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.bluetooth.Bluetooth;
import com.example.aivoice.bluetooth.BluetoothDeviceInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//观察类
public class BluetoothViewModel extends ViewModel {

    private static final String TAG = "BluetoothViewModel";
    //    MutableLiveData 和 LiveData 在 UI 和数据层之间传递数据，LiveData只读对外暴露， MutableLiveData则用于修改
    private final MutableLiveData<Set<BluetoothDevice>> pairedDevices = new MutableLiveData<>();
    private final MutableLiveData<Set<BluetoothDevice>> bluetoothDevicesList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReceiverRegistered = new MutableLiveData<>(false);
    private final Bluetooth bluetooth = new Bluetooth();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private long startTime; // 扫描蓝牙开始时间
    private long elapsedTime = 0; // 已扫描的时间
    private final MutableLiveData<Long> recordingTime = new MutableLiveData<>(0L); // 时间，单位：秒
    private final MutableLiveData<String> connectedDeviceName= new MutableLiveData<>();
    private final MutableLiveData<String> connectedDeviceAddress = new MutableLiveData<>();

    public void setContext(Context context) {
        this.context=context;
        bluetooth.setContext(context);
    }

    public LiveData<Set<BluetoothDevice>> getPairedDevices() {
        fetchPairedDevices();
        return pairedDevices;
    }

    public LiveData<Set<BluetoothDevice>> getBluetoothDevicesList() {
        return bluetoothDevicesList;
    }
    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }
    public LiveData<String> getConnectedDeviceName() {
        return connectedDeviceName;
    }

    public void setConnectedDeviceName(String deviceName) {
        connectedDeviceName.postValue(deviceName);  //使用post，而不是set
    }

    public void setConnectedDeviceAddress(String deviceAddress){
        connectedDeviceAddress.postValue(deviceAddress);
    }

    public LiveData<String> getConnectedDeviceAddresss() {
        return connectedDeviceAddress;
    }

    public LiveData<Boolean> getIsReceiverRegistered() {
        return isReceiverRegistered;
    }

    //获取已匹配的蓝牙设备
    public void fetchPairedDevices() {
        executorService.execute(() -> {
            try {
                Set<BluetoothDevice> devices = bluetooth.getPairedDevices();
                pairedDevices.postValue(devices != null ? devices : new HashSet<>());
            } catch (Exception e) {
                 Toast.makeText(context, "获取已配对蓝牙设备失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //连接蓝牙的过程是异步的
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(context, "蓝牙设备不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetooth.hasBluetoothPermissions()) {
            Toast.makeText(context, "蓝牙连接权限未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        executorService.execute(() -> {
            try {
                boolean success = bluetooth.connectToDevice(device);
                isConnected.postValue(success);
                if (success) {
                    setConnectedDeviceName(bluetooth.getConnectedDeviceName());
                    setConnectedDeviceAddress(bluetooth.getConnectedDeviceAddress());
                } else {
                    Log.i(TAG, "蓝牙连接失败");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "蓝牙连接权限异常", e);
            } catch (Exception e) {
               Log.e(TAG, "蓝牙连接异常", e);
            }
        });
    }

    public void startScanning() {
        if (bluetooth.isDiscovering()) {
            Log.i(TAG, "蓝牙扫描已在进行中，跳过重复启动");
            return;
        }

        try {
            bluetooth.startDiscovery();
            isReceiverRegistered.setValue(true);
            startTime = System.currentTimeMillis();
            elapsedTime = 0; // 重置扫描时间
            recordingTime.setValue(elapsedTime);

            Handler handler = new Handler();
            Runnable updateTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    long newElapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    recordingTime.postValue(newElapsedTime);
                    handler.postDelayed(this, 1000); // 每秒更新一次
                }
            };
            handler.post(updateTimeRunnable);

            // 60 秒后自动停止扫描
            handler.postDelayed(() -> {
                bluetooth.stopScanning();
                isReceiverRegistered.setValue(false);
                Log.i(TAG, "蓝牙扫描已自动停止");
            }, 60000);

        } catch (Exception e) {
            Toast.makeText(context, "扫描蓝牙失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "蓝牙扫描异常", e);
        }
    }

    public void disconnectDevice(){
        bluetooth.disconnect();
        isConnected.setValue(false);
        setConnectedDeviceName("");
        setConnectedDeviceAddress("");
        Log.i(TAG,"蓝牙设备已断开连接");
    }
    

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            Log.e(TAG, "Error 清除viewModel错误: " + e.getMessage(), e);
        }
    }


}
