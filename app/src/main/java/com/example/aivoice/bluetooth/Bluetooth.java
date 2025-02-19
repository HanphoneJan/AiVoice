package com.example.aivoice.bluetooth;
//适用于 HC-05/HC-06、ESP32、CSR 蓝牙模块
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {
    private static final String TAG = "Bluetooth";
    //static，生命周期与应用程序相同
    private static BluetoothDevice connectedDevice;

    public static ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothConnectionListener bluetoothConnectionListener;
    private BluetoothGattCustom bluetoothGattCustom = new BluetoothGattCustom();

    //蓝牙数据监听回调
    private BluetoothDataListener dataListener; // 用于存储回调对象

    public void setDataListener(BluetoothDataListener listener) {
        this.dataListener = listener;
    }
    public interface DataListener {
        void onDataReceived(String data);
    }

    public Bluetooth(){
        //空
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setContext(Context context) {
        this.context = context;
    }
    public Bluetooth(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("该设备不支持蓝牙");
        }
    }
    public interface BluetoothDataListener {
        void onDataReceived(String data);
    }

    public static BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public String getConnectedDeviceName(){
        // 检查BLUETOOTH权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1); // 请求码可以是任意整数
        }

        // 检查BLUETOOTH_ADMIN权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH_ADMIN权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    2); // 请求码可以是任意整数
        }
        return connectedDevice.getName();
    }

    public void setConnectedDeviceName(BluetoothDevice newConnectedDevice) {
        connectedDevice = newConnectedDevice;
    }

    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean isDiscovering() {
        // 检查BLUETOOTH权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1); // 请求码可以是任意整数
        }

        // 检查BLUETOOTH_ADMIN权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求BLUETOOTH_ADMIN权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    2); // 请求码可以是任意整数
        }
        return bluetoothAdapter != null && bluetoothAdapter.isDiscovering();
    }


    public void requestBluetoothPermissions() {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    public boolean connectToDevice(BluetoothDevice device) {
        if (device == null || !hasBluetoothPermissions()) {
            Log.e(TAG, "设备参数为空或权限不足，无法连接");
            requestBluetoothPermissions();
            return false;
        }

        try {
            // 检查BLUETOOTH权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求BLUETOOTH权限
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.BLUETOOTH},
                        1); // 请求码可以是任意整数
            }

            // 检查BLUETOOTH_ADMIN权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求BLUETOOTH_ADMIN权限
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                        2); // 请求码可以是任意整数
            }
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                Log.i(TAG, "尝试使用Gatt连接设备：" + device.getName());
                if(bluetoothGattCustom.connectToDevice(device)){
                    connectedDevice = device;
                    return true;
                }
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            if (bluetoothConnectionListener != null) {
                bluetoothConnectionListener.onDeviceConnected(device);
            }
            startListening();
            connectedDevice = device;
            Log.i(TAG, "成功连接设备: " + connectedDevice.getName());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "连接失败", e);
            closeConnection();
            return false;
        }
    }

    public void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            try {
                while (true) {
                    bytes = inputStream.read(buffer);
                    if (bytes == -1) {
                        break; // 输入流结束，退出循环
                    }
                    String receivedData = new String(buffer, 0, bytes, "GBK");
                    Log.i(TAG, "接收到的数据: " + receivedData);

                    // 通过回调传递数据
                    if (dataListener != null) {
                        dataListener.onDataReceived(receivedData);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "读取数据错误", e);
                disconnect();
            }
        }).start();
    }


    public boolean sendSignal(String order) {
        if (outputStream == null) {
            Log.e(TAG, "未连接蓝牙");
            Toast.makeText(context, "请连接蓝牙", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            byte[] command = order.getBytes("GBK");
            outputStream.write(command);
            Log.i(TAG, "已发送数据: " + order);
            return  true;
        } catch (IOException e) {
            Log.e(TAG, "发送失败", e);
            return false;
        }
    }

    public void disconnect() {
        closeConnection();
        Log.i(TAG, "已断开蓝牙连接");
        Toast.makeText(context, "已断开连接", Toast.LENGTH_SHORT).show();
    }

    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            connectedDevice = null;
        } catch (IOException e) {
            Log.e(TAG, "关闭连接失败", e);
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "蓝牙权限未授予，无法获取已配对设备");
            requestBluetoothPermissions();
            return new HashSet<>();
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "蓝牙未启用或设备不支持蓝牙");
            return new HashSet<>();
        }
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            bluetoothDeviceList.addAll(pairedDevices);
            return pairedDevices;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while fetching paired devices.", e);
            return new HashSet<>();
        }
    }

    public void startDiscovery() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "蓝牙权限未授予，无法开始扫描");
            requestBluetoothPermissions();
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "蓝牙未启用，无法开始扫描");
            return;
        }
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

    public void stopScanning() {
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
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            Log.i(TAG, "蓝牙扫描已停止");
        }
    }

    public void setBluetoothConnectionListener(BluetoothConnectionListener listener) {
        this.bluetoothConnectionListener = listener;
    }


}
