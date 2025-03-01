package com.example.aivoice.bluetooth;

import androidx.annotation.NonNull;

// 自定义类表示蓝牙设备信息
public class BluetoothDeviceInfo {
    private String deviceName;
    private String deviceAddress;
    private boolean isConnected;

    public BluetoothDeviceInfo(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.isConnected = false;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public boolean isConnected() {
        return isConnected;
    }
    public void setConnected(boolean connected) {
        isConnected = connected;
    }

}
