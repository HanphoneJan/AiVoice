package com.example.aivoice.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    private static final String TAG = "Bluetooth";
    private static final UUID DEFAULT_UUID = UUID.randomUUID(); // 替换为实际 UUID

    private final BluetoothAdapter bluetoothAdapter;
    private final Context context; // 用于权限检查
    private BluetoothSocket bluetoothSocket;

    public Bluetooth(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("Bluetooth not supported on this device.");
        }
    }

    /**
     * 检查是否具有指定权限
     *
     * @param permission 权限名称
     * @return 如果授予权限返回 true，否则返回 false。
     */
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查蓝牙权限是否已授予
     *
     * @return 如果所有需要的权限都已授予返回 true，否则返回 false。
     */
    public boolean hasBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            return hasPermission(Manifest.permission.BLUETOOTH) && hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
        }
    }

    /**
     * 检查蓝牙是否启用
     *
     * @return 如果蓝牙启用返回 true，否则返回 false。
     */
    public boolean isBluetoothEnabled() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions.");
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * 启用蓝牙
     */
    public void enableBluetooth() {
        try {
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "Cannot enable Bluetooth: Missing permissions.");
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while enabling Bluetooth.", e);
        }
    }


    /**
     * 获取已配对设备
     *
     * @return 返回已配对设备的集合。如果缺少权限或发生异常，返回空集合。
     */
    public Set<BluetoothDevice> getPairedDevices() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Cannot fetch paired devices: Missing permissions.");
            return new HashSet<>();
        }

        try {
            return bluetoothAdapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while fetching paired devices.", e);
            return new HashSet<>();
        }
    }
    /**
     * 尝试连接到指定蓝牙设备
     *
     * @param device 目标蓝牙设备
     * @return 成功连接返回 true，失败返回 false。
     */
    public boolean connectToDevice(BluetoothDevice device) {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Cannot connect to device: Missing permissions.");
            return false;
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            bluetoothAdapter.cancelDiscovery(); // 停止设备搜索以加快连接速度
            bluetoothSocket.connect();
            Log.i(TAG, "Successfully connected to " + device.getName());
            return true;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Error connecting to device: " + device.getName(), e);
            closeConnection(); // 清理资源
            return false;
        }
    }

    /**
     * 断开当前蓝牙连接
     */
    public void disconnect() {
        closeConnection();
        Log.i(TAG, "Bluetooth connection closed.");
    }

    /**
     * 清理资源（关闭 BluetoothSocket）
     */
    public void cleanup() {
        closeConnection();
    }

    /**
     * 关闭 BluetoothSocket
     */
    private void closeConnection() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Bluetooth socket.", e);
            } finally {
                bluetoothSocket = null;
            }
        }
    }
}
