package com.example.aivoice.bluetooth;

// 自定义类表示蓝牙设备信息
public class BluetoothDeviceInfo {
    private String deviceName;
    private String deviceAddress;

    public BluetoothDeviceInfo(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String toString() {
        return deviceName + " (" + deviceAddress + ")";
    }
}
