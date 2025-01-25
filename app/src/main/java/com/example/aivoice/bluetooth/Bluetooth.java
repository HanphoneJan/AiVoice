package com.example.aivoice.bluetooth;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    private static final String TAG = "Bluetooth";
    private static final UUID DEFAULT_UUID = UUID.randomUUID(); // Replace with actual UUID
    private  BluetoothAdapter bluetoothAdapter;
    private Context context; // For permission checks
    private BluetoothSocket bluetoothSocket;

    public Bluetooth(){
        //空
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    public Bluetooth(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("该设备不支持蓝牙");
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Boolean isBluetoothEnabled() {
        if(bluetoothAdapter.isEnabled()){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    // Check if permission is granted
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Check if all necessary Bluetooth permissions are granted
    public boolean hasBluetoothPermissions() {
        return hasPermission(Manifest.permission.BLUETOOTH)
                && hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
                && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                && hasPermission(Manifest.permission.BLUETOOTH_SCAN);
    }

    // Request Bluetooth permissions at runtime
    public void requestBluetoothPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.BLUETOOTH)) {
            // Show rationale if needed
            Toast.makeText(context, "该功能需要蓝牙权限", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions((Activity) context, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        }, 1); // 1 is a request code for permissions
    }



    // Enable Bluetooth (with permission check)
    public void enableBluetooth() {
        try {
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "Cannot enable Bluetooth: Missing permissions.");
                requestBluetoothPermissions();
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                Log.i(TAG, "Bluetooth enabled.");
            } else {
                Log.i(TAG, "Bluetooth already enabled.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while enabling Bluetooth.", e);
        }
    }

    // Disable Bluetooth (with permission check)
    public void disableBluetooth() {
        try {
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "Cannot disable Bluetooth: Missing permissions.");
                requestBluetoothPermissions();
                return;
            }

            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
                Log.i(TAG, "Bluetooth disabled.");
            } else {
                Log.i(TAG, "Bluetooth is already disabled.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while disabling Bluetooth.", e);
        }
    }

    // Get paired devices (with permission check)
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

    // Start Bluetooth discovery (with permission check)
    public void startDiscovery() {
        try {
            // 检查BLUETOOTH权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求BLUETOOTH权限
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.BLUETOOTH},
                        1); // 请求码可以是任意整数
                return; // 等待用户响应权限请求
            }

            // 检查BLUETOOTH_ADMIN权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求BLUETOOTH_ADMIN权限
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                        2); // 请求码可以是任意整数
                return; // 等待用户响应权限请求
            }

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery(); // Cancel any ongoing discovery
            }
            bluetoothAdapter.startDiscovery(); // Start discovery for nearby Bluetooth devices
            Log.i(TAG, "Bluetooth discovery started.");
        } catch (Exception e) {
            Log.e(TAG, "Error starting Bluetooth discovery.", e);
        }
    }

    // Stop Bluetooth discovery (with permission check)
    public void stopDiscovery() {
        // 检查BLUETOOTH权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1); // 请求码可以是任意整数
            return; // 等待用户响应权限请求
        }

        // 检查BLUETOOTH_ADMIN权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH_ADMIN权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    2); // 请求码可以是任意整数
            return; // 等待用户响应权限请求
        }

        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.i(TAG, "Bluetooth discovery stopped.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping Bluetooth discovery.", e);
        }
    }

    // Connect to Bluetooth device (with permission check)
    public boolean connectToDevice(BluetoothDevice device) {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "权限不足，无法连接到设备");
            requestBluetoothPermissions();
            return false;
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            bluetoothAdapter.cancelDiscovery(); // Stop discovery to speed up connection
            bluetoothSocket.connect();
            Log.i(TAG, "成功连接设备：" + device.getName());
            return true;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "错误连接设备 " + device.getName(), e);
            closeConnection(); // Clean up resources
            return false;
        }
    }

    // Disconnect from Bluetooth device (with permission check)
    public void disconnect() {
        closeConnection();
        Log.i(TAG, "Bluetooth connection closed.");
    }

    // Close BluetoothSocket connection
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


    // Cleanup resources
    public void cleanup() {
        closeConnection();
    }


}
