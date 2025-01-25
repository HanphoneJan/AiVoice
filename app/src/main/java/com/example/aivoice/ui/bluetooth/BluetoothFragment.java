package com.example.aivoice.ui.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;

import java.io.File;
import java.util.ArrayList;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TAG = "BluetoothFragment";

    private BluetoothViewModel bluetoothViewModel;
    private ArrayAdapter<String> bluetoothDevicesAdapter;
    private TextView tvBluetoothStatus;
    private ListView lvFiles;
    private Spinner spinnerBluetoothDevices;

    private BluetoothAdapter bluetoothAdapter;

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // 动态检查蓝牙扫描权限
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED){

                    // 获取设备
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String deviceName = device.getName();
                    if (deviceName != null && bluetoothDevicesAdapter.getPosition(deviceName) == -1) {
                        // 更新设备列表
                        bluetoothDevicesAdapter.add(deviceName);
                        bluetoothDevicesAdapter.notifyDataSetChanged();
                    }
                }
            }
            }
        }
    };


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // 使用ViewModelProvider获取BluetoothViewModel的实例
        bluetoothViewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        bluetoothViewModel.setContext(requireContext());
// 从布局文件中找到扫描蓝牙设备的按钮
        Button btnScanBluetooth = root.findViewById(R.id.btn_scan_bluetooth);

// 找到显示可用蓝牙设备的下拉列表（Spinner）
        spinnerBluetoothDevices = root.findViewById(R.id.spinner_bluetooth_devices);

// 找到连接蓝牙设备的按钮
        Button btnConnectBluetooth = root.findViewById(R.id.btn_connect_bluetooth);

// 找到显示蓝牙连接状态的文本视图
        tvBluetoothStatus = root.findViewById(R.id.tv_bluetooth_status);

// 找到显示文件列表的列表视图（可能用于显示从蓝牙设备接收的文件）
        lvFiles = root.findViewById(R.id.lv_files);

// 创建一个ArrayAdapter来管理下拉列表中的蓝牙设备项
// 使用requireContext()来获取当前的上下文，android.R.layout.simple_spinner_item作为列表项的布局
        bluetoothDevicesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);

// 设置下拉列表展开时使用的布局
        bluetoothDevicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// 将适配器设置到下拉列表（Spinner）上
        spinnerBluetoothDevices.setAdapter(bluetoothDevicesAdapter);

        // 设置扫描蓝牙设备的按钮的点击监听器
        btnScanBluetooth.setOnClickListener(v -> scanBluetoothDevices()); // 当按钮被点击时，调用scanBluetoothDevices()方法来扫描蓝牙设备

        // 设置连接蓝牙设备的按钮的点击监听器
        btnConnectBluetooth.setOnClickListener(v -> connectToBluetoothDevice()); // 当按钮被点击时，调用connectToBluetoothDevice()方法来尝试连接到选中的蓝牙设备

        loadAudioFiles();

        bluetoothViewModel.getPairedDevices().observe(getViewLifecycleOwner(), devices -> {
            // 清除下拉列表（Spinner）的适配器中的现有项
            bluetoothDevicesAdapter.clear();

            if (ContextCompat.checkSelfPermission( requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                for (BluetoothDevice device : devices) {
                    bluetoothDevicesAdapter.add(device.getName());
                }
            }
        });

        bluetoothViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    private void scanBluetoothDevices() {
        if (checkBluetoothPermissions()) {
            bluetoothViewModel.startScanning();
            // 注册接收器监听设备发现的广播
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            requireContext().registerReceiver(bluetoothReceiver, filter);
            Toast.makeText(requireContext(), "蓝牙设备扫描中...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "缺少蓝牙扫描权限", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToBluetoothDevice() {
        String deviceName = (String) spinnerBluetoothDevices.getSelectedItem();
        if (deviceName != null) {
            tvBluetoothStatus.setText("连接到: " + deviceName);
            Toast.makeText(requireContext(), "尝试连接到 " + deviceName, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "请先选择一个设备", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAudioFiles() {
        try {
            // 获取音频文件夹路径
            File musicDir = new File(requireContext().getFilesDir(),"Music");

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
}
