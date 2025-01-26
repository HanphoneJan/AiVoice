package com.example.aivoice.ui.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.bluetooth.Bluetooth;

import java.io.File;
import java.io.IOException;
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
    private final Bluetooth bluetooth = new Bluetooth();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private long startTime; // 扫描蓝牙开始时间
    private long elapsedTime = 0; // 已扫描的时间
    private final MutableLiveData<Long> recordingTime = new MutableLiveData<>(0L); // 时间，单位：秒
    private BluetoothDevice connectedDevice;
    private BluetoothA2dp bluetoothA2dp;

    private MediaPlayer mediaPlayer;

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

    public void checkBluetoothStatus() {
        try {
            isBluetoothEnabled.setValue(bluetooth.isBluetoothEnabled());
        } catch (Exception e) {
            postError("Error 检查蓝牙状态: " + e.getMessage());
        }
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
        try {
            bluetooth.startDiscovery();
            isReceiverRegistered.setValue(true);
            // 初始化并启动计时器
            startTime = System.currentTimeMillis();
            elapsedTime = 0; // 重置已录音时间
            Runnable updateTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    recordingTime.setValue(elapsedTime);
                }
            };

            // 延迟60秒后停止设备扫描
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                    Log.i(TAG,"停止扫描");
                }
            }, 60000); // 延迟60秒（60000毫秒）后执行
        } catch (Exception e) {
            postError("扫描蓝牙设备错误 " + e.getMessage());
        }
    }

    public void stopScanning() {
        bluetooth.stopDiscovery();
        isReceiverRegistered.setValue(false);
    }


    public void disconnectDevice(){
        bluetooth.disconnect();
        isConnected.setValue(false);
        connectionStatus.setValue("Disconnected");
        Log.i(TAG,"蓝牙设备已断开连接");
    }

    // 检查文件是否为音频文件（可以根据文件扩展名进行检查）
    private boolean isAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a");
    }
    public void playAudioFile(File file){

        if(!isAudioFile(file)){
            postError("文件不是音频文件");
            return;
        }
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        try {
            // 如果 MediaPlayer 正在播放，先停止之前的播放
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();  // 重置播放器，准备重新播放
            }
            else{mediaPlayer.reset(); } //默认进行重置，否则再次播放会出问题
            Log.i(TAG,connectedDevice.toString());
            // 这里可以选择通过蓝牙音频设备播放音频
            if (connectedDevice != null) {
                // 设置蓝牙设备为音频输出目标

                Log.i(TAG,"蓝牙播放");
            }
            // 设置音频文件的路径
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();  // 准备播放
            mediaPlayer.start();  // 开始播放

            Log.i(TAG,"开始播放");
        } catch (IOException e) {
//            Toast.makeText(this, "播放文件出错", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            postError("播放失败");
        }
    }

    public void pauseAudioFile() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.i(TAG,"暂停播放");
        }
    }

    public void stopAudioFile() {
            pauseAudioFile();
            mediaPlayer.release();
            mediaPlayer = null;
            Log.i(TAG,"停止播放");
    }

    public void playAudioBluetooth(){
        bluetooth.sendPlaySignal();
        Log.i(TAG,"蓝牙开始播放");
    }
    public void pauseAudioBluetooth(){
        bluetooth.sendPauseSignal();
        Log.i(TAG,"蓝牙暂停播放");
    }
    public void stopAudioBluetooth(){
        bluetooth.sendStopSignal();
        Log.i(TAG,"蓝牙停止播放");
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
            mediaPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel cleanup: " + e.getMessage(), e);
        }
    }
}
