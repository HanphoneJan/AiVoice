package com.example.aivoice.ui.bluetooth;

import android.bluetooth.BluetoothDevice;

import android.content.Context;
import android.os.Handler;
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
//观察类
public class BluetoothViewModel extends ViewModel implements Bluetooth.BluetoothDataListener {

    private static final String TAG = "BluetoothViewModel";
    //    MutableLiveData 和 LiveData 在 UI 和数据层之间传递数据，LiveData只读对外暴露， MutableLiveData则用于修改
    private final MutableLiveData<Boolean> isBluetoothEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> nowPlayAudioFile = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> audList = new MutableLiveData<>() ;
    private final MutableLiveData<Set<BluetoothDevice>> pairedDevices = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>();
    private final MutableLiveData<String> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReceiverRegistered = new MutableLiveData<>(false);
    private final Bluetooth bluetooth = new Bluetooth();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private long startTime; // 扫描蓝牙开始时间
    private long elapsedTime = 0; // 已扫描的时间
    private final MutableLiveData<Long> recordingTime = new MutableLiveData<>(0L); // 时间，单位：秒
    private static BluetoothDevice connectedDevice;



    public BluetoothViewModel() {
        bluetooth.setDataListener(this);
        //空
    }

    public void setContext(Context context) {
        bluetooth.setContext(context);
    }

    public LiveData<String> getNowPlayAudioFile() {
        return nowPlayAudioFile;
    }

    public LiveData<Set<String>> getAudList() {
        return audList;
    }

    public LiveData<Boolean> getIsBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public LiveData<Set<BluetoothDevice>> getPairedDevices() {
        fetchPairedDevices();
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

    //获取已匹配的蓝牙设备
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

    //连接蓝牙的过程是异步的
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
                if (success) {
                    connectedDevice = device;
                } else {
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
            postError("蓝牙扫描失败: " + e.getMessage());
            Log.e(TAG, "蓝牙扫描异常", e);
        }
    }

    public void disconnectDevice(){
        bluetooth.disconnect();
        isConnected.setValue(false);
        connectionStatus.setValue("Disconnected");
        Log.i(TAG,"蓝牙设备已断开连接");
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
        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel cleanup: " + e.getMessage(), e);
        }
    }

    public boolean playAudio() {
        return bluetooth.sendSignal("audplay");
    }
    public boolean stopAudio() {
        return bluetooth.sendSignal("audstop");
    }

    public boolean showAudioList() {
       return bluetooth.sendSignal("audlist");
    }

    public boolean playNextTrack() {
        return bluetooth.sendSignal("audnext");
    }

    public boolean playPreviousTrack() {
        return bluetooth.sendSignal("audprev");
    }

    public boolean goBackDirectory() {
       return bluetooth.sendSignal("dirback");
    }

    public boolean displayTrackName() {
        return bluetooth.sendSignal("dispname");
    }

    public boolean togglePlaybackMode() {
        return bluetooth.sendSignal("modechg");
    }

    public boolean pauseResumeAudio() {
        return bluetooth.sendSignal("pausresu");
    }

    public boolean seekBackward() {
        return bluetooth.sendSignal("seekbwd");
    }

    public boolean seekForward() {
        return bluetooth.sendSignal("seekfwd");
    }

    public boolean decreaseVolume() {
       return bluetooth.sendSignal("voludec");
    }

    public boolean increaseVolume() {
        return bluetooth.sendSignal("voluinc");
    }

    // 实现 onDataReceived 回调
    @Override
    public void onDataReceived(String data) {
        if (data.startsWith("dispname ")) {
            nowPlayAudioFile.postValue(data.replace("dispname ", ""));
        } else if (data.startsWith("audlist ")) {
            // 解析音频列表
            String audios = data.replace("audlist ", "");
            Set<String> audioSet = new HashSet<>();
            // 假设返回的数据是用逗号分隔的音频文件名
            String[] audioArray = audios.split(",");
            for (String audio : audioArray) {
                audioSet.add(audio.trim()); // 去除空格并添加到Set
            }
            audList.postValue(audioSet); // 更新LiveData
        }
    }
}
