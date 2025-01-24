package com.example.aivoice.ui.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aivoice.R;

import java.io.File;
import java.util.ArrayList;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TAG = "BluetoothFragment";

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> bluetoothDevicesAdapter;
    private TextView tvBluetoothStatus;
    private ListView lvFiles;
    private Spinner spinnerBluetoothDevices;
    private boolean isReceiverRegistered = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return root;
        }

        // Get UI elements
        Button btnScanBluetooth = root.findViewById(R.id.btn_scan_bluetooth);
        spinnerBluetoothDevices = root.findViewById(R.id.spinner_bluetooth_devices);
        Button btnConnectBluetooth = root.findViewById(R.id.btn_connect_bluetooth);
        tvBluetoothStatus = root.findViewById(R.id.tv_bluetooth_status);
        lvFiles = root.findViewById(R.id.lv_files);

        // Set up Bluetooth devices spinner
        bluetoothDevicesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        bluetoothDevicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBluetoothDevices.setAdapter(bluetoothDevicesAdapter);

        // Set up button click listeners
        btnScanBluetooth.setOnClickListener(v -> scanBluetoothDevices());
        btnConnectBluetooth.setOnClickListener(v -> connectToBluetoothDevice());

        // Load audio files
        loadAudioFiles();

        return root;
    }

    // Scan for Bluetooth devices
    private void scanBluetoothDevices() {
        // 第一步：检查蓝牙扫描权限
        if (!checkBluetoothPermissions()) {
            // 如果没有蓝牙扫描权限，弹出提示并返回，不进行扫描
            Toast.makeText(requireContext(), "缺少蓝牙扫描权限", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 第二步：如果正在进行蓝牙扫描，取消当前扫描
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery(); // 停止当前的蓝牙设备发现
            }

            // 清空之前的扫描结果
            bluetoothDevicesAdapter.clear(); // 清空之前添加的蓝牙设备信息

            // 启动蓝牙设备扫描
            bluetoothAdapter.startDiscovery(); // 开始扫描周围的蓝牙设备

            // 第三步：注册广播接收器（如果还没有注册）
            if (!isReceiverRegistered) {
                // 设置接收蓝牙扫描结果的广播接收器
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                requireContext().registerReceiver(bluetoothDeviceReceiver, filter);
                isReceiverRegistered = true; // 标记接收器已注册
            }
        } catch (SecurityException e) {
            // 如果没有权限来启动蓝牙扫描，捕获 SecurityException 并显示错误信息
            Toast.makeText(requireContext(), "蓝牙扫描失败: 权限不足", Toast.LENGTH_SHORT).show();
        }
    }


    // Connect to selected Bluetooth device
    private void connectToBluetoothDevice() {
        String deviceName = (String) spinnerBluetoothDevices.getSelectedItem();
        if (deviceName != null) {
            tvBluetoothStatus.setText("连接到: " + deviceName);
            Toast.makeText(requireContext(), "尝试连接到 " + deviceName, Toast.LENGTH_SHORT).show();
            // TODO: Implement actual Bluetooth connection logic here
        } else {
            Toast.makeText(requireContext(), "请先选择一个设备", Toast.LENGTH_SHORT).show();
        }
    }

    // Load audio files from the specified path
    private void loadAudioFiles() {
        try {
            // 获取音频文件夹路径
            File musicDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Android/data/com.example.aivoice/files/Music/");

            // 检查文件夹是否存在，如果不存在则创建
            if (!musicDir.exists()) {
                boolean isCreated = musicDir.mkdirs(); // 创建文件夹
                if (!isCreated) {
                    Toast.makeText(requireContext(), "无法创建音频文件夹", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 如果是文件夹且存在，加载音频文件
            if (!musicDir.isDirectory()) {
                Toast.makeText(requireContext(), "指定路径不是文件夹", Toast.LENGTH_SHORT).show();
                return;
            }

            File[] files = musicDir.listFiles();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);

            if (files != null) {
                for (File file : files) {
                    adapter.add(file.getName());
                }
            }

            lvFiles.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载音频文件失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading audio files", e);
        }
    }


    // Check Bluetooth permissions
    private boolean checkBluetoothPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };

        ArrayList<String> missingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(),
                    missingPermissions.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSION);
            return false;
        }
        return true;
    }

    // Broadcast receiver for Bluetooth device discovery
    private final BroadcastReceiver bluetoothDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "缺少蓝牙连接权限，无法发现设备", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (device != null && device.getName() != null) {
                    bluetoothDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(bluetoothDeviceReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(requireContext(), "蓝牙权限被拒绝，部分功能将无法使用", Toast.LENGTH_LONG).show();
            }
        }
    }
}
