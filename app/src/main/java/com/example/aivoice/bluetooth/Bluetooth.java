package com.example.aivoice.bluetooth;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    private static final String TAG = "Bluetooth";
    private static final UUID DEFAULT_UUID = UUID.fromString("0000110B-0000-1000-8000-00805F9B34FB");  //识别为蓝牙音频服务，A2DP

    private  BluetoothAdapter bluetoothAdapter;
    private Context context; // For permission checks
    private BluetoothSocket bluetoothSocket;
    private BluetoothGatt bluetoothGatt;

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
            //startDiscovery() 方法会立即返回，而实际的搜索操作将在后台进行
            //一旦系统发现附近的蓝牙设备，它将通过广播发送一个带有 BluetoothDevice.ACTION_FOUND 动作的 Intent。
            // 这个 Intent 包含了被发现的蓝牙设备的信息，比如设备的地址和名称
            bluetoothAdapter.startDiscovery();
            Log.i(TAG, "蓝牙设备扫描已启动");

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
        if (device == null) {
            Log.e(TAG, "设备参数为空，无法连接到设备");
            return false;
        }
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "权限不足，无法连接到设备");
            requestBluetoothPermissions();
            return false;
        }

        try {
//            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
//            bluetoothAdapter.cancelDiscovery(); // Stop discovery to speed up connection
//            bluetoothSocket.connect();
            bluetoothGatt = device.connectGatt(context, false, gattCallback); // 第二个参数为 autoConnect，设置为 false 以避免自动连接
            Log.i(TAG, "成功连接设备：" + device.getName());
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "错误连接设备 " + device.getName(), e);
            closeConnection(); // Clean up resources
            return false;
        }
    }

    // 创建 BluetoothGattCallback 来监听服务发现等操作
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
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

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "已连接设备：" + gatt.getDevice().getName());
                // 开始服务发现
                boolean servicesDiscovered = gatt.discoverServices();
                if (servicesDiscovered) {
                    Log.i(TAG, "正在进行服务发现...");
                } else {
                    Log.e(TAG, "服务发现失败");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "设备已断开连接");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "服务发现成功");

                // 获取设备的服务
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    Log.i(TAG, "发现服务：" + service.getUuid().toString());
                    // 可以选择对每个服务进行进一步的操作，例如读取或写入特征值
                }
            } else {
                Log.e(TAG, "服务发现失败，状态码：" + status);
            }
        }
    };

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
