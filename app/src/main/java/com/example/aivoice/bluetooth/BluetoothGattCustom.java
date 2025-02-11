package com.example.aivoice.bluetooth;
//BLE，如基于 nRF52、蓝牙 5.0 模块


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothGattCallback;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.bluetooth.BluetoothGattCharacteristic;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;



public class BluetoothGattCustom {

    private static final String TAG = "BluetoothGattCustom";
    private static String connectedDeviceName;//当前连接的设备
    public ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();  //存储蓝牙设备
    private  BluetoothAdapter bluetoothAdapter;
    private Context context; // For permission checks
    private BluetoothSocket bluetoothSocket;
    private BluetoothGatt bluetoothGatt;
    //蓝牙串口服务UUID
    //蓝牙串口服务UUID
    private static UUID selectedDeviceUUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 获取到对应的服务和特征值
    private BluetoothGattCharacteristic playCharacteristic;

    private BluetoothConnectionListener bluetoothConnectionListener;
    public BluetoothGattCustom(){
        //空
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    public BluetoothGattCustom(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            throw new UnsupportedOperationException("该设备不支持蓝牙");
        }
    }

    public static void setConnectedDeviceName(String newConnectedDeviceName) {
        connectedDeviceName = newConnectedDeviceName;
    }

    public static String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    //允许设置回调监听器
    public void setBluetoothConnectionListener(BluetoothConnectionListener listener) {
        this.bluetoothConnectionListener = listener;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPermission(Manifest.permission.BLUETOOTH)
                    && hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
                    && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    && hasPermission(Manifest.permission.BLUETOOTH_SCAN);
        }
        return hasPermission(Manifest.permission.BLUETOOTH)
                && hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
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
            bluetoothGatt = device.connectGatt(context, false, gattCallback); // 第二个参数为 autoConnect，设置为 false 以避免自动连接
            Log.i(TAG, "成功连接设备：" + device.getName());
            // 调用 discoverServices() 开始发现服务
//boolean servicesDiscovered = bluetoothGatt.discoverServices();
//            if (servicesDiscovered) {
//                Log.i(TAG, "开始服务发现...");
//            } else {
//                Log.e(TAG, "服务发现失败");
//            }

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
                // 通知应用层设备已连接
                if (bluetoothConnectionListener != null) {
                    bluetoothConnectionListener.onDeviceConnected(gatt.getDevice());
                }
                // 开始服务发现
                boolean servicesDiscovered = gatt.discoverServices();
                if (servicesDiscovered) {
                    Log.i(TAG, "正在进行服务发现...");
                } else {
                    Log.e(TAG, "服务发现失败");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "设备已断开连接");
                // 通知应用层设备已断开
                if (bluetoothConnectionListener != null) {
                    bluetoothConnectionListener.onDeviceDisconnected(gatt.getDevice());
                }
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
                    discoverAudioControlCharacteristics(gatt);
                }
            } else {
                Log.e(TAG, "服务发现失败，状态码：" + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            // 获取接收到的字节数组
            byte[] receivedData = characteristic.getValue();

            // 将字节数组转换为字符串（使用 GBK 编码）
            try {
                String receivedString = new String(receivedData, "GBK");
                Log.i(TAG, "接收到的数据: " + receivedString);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "不支持的编码格式: GBK", e);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 获取接收到的字节数组
                byte[] receivedData = characteristic.getValue();

                // 将字节数组转换为字符串（使用 GBK 编码）
                try {
                    String receivedString = new String(receivedData, "GBK");
                    Log.i(TAG, "读取到的数据: " + receivedString);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "不支持的编码格式: GBK", e);
                }
            } else {
                Log.e(TAG, "读取特征值失败，状态码: " + status);
            }
        }

    };

    private void discoverAudioControlCharacteristics(BluetoothGatt gatt) {
        if (gatt == null) return;
        // 音频控制服务的 UUID
        BluetoothGattService service = gatt.getService(selectedDeviceUUID);
        if (service != null) {
            playCharacteristic = service.getCharacteristic(selectedDeviceUUID);
        }
    }


    // 发送信号，蓝牙通信是基于二进制数据的
    public void sendPlaySignal(String order) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "未连接蓝牙");
            return;
        }
        byte[] command;
        // 将字符串转换为字节数组，使用 GBK 编码
        try {
            command = order.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        // 设置特征值
        playCharacteristic.setValue(command);

        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1); // 请求码可以是任意整数
            return; // 等待用户响应权限请求
        }

        // 发送指令
        bluetoothGatt.writeCharacteristic(playCharacteristic);
        Log.i(TAG, "已发送控制信号: " + order);
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
