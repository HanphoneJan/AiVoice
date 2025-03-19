package com.example.aivoice.ui.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.example.aivoice.bluetooth.Bluetooth;

import com.example.aivoice.bluetooth.BluetoothDeviceInfo;
import com.example.aivoice.bluetooth.CustomBluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.Objects;

public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TAG = "BluetoothFragment";

    private BluetoothViewModel bluetoothViewModel;
    private CustomBluetoothDeviceAdapter bluetoothDevicesAdapter; // 蓝牙设备列表适配器
    private final ArrayList<BluetoothDeviceInfo> bluetoothDeviceInfoList = new ArrayList<>();
    private ListView lvBluetoothDevices; // 蓝牙设备列表视图
    private final ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>(); // 蓝牙设备列表
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_bluetooth, container, false);

        // 使用ViewModelProvider获取BluetoothViewModel的实例
        bluetoothViewModel = new ViewModelProvider(this).get(BluetoothViewModel.class);
        bluetoothViewModel.setContext(requireContext());

        lvBluetoothDevices = root.findViewById(R.id.lv_bluetooth);
        ImageButton btnScanBluetooth = root.findViewById(R.id.btn_scan_bluetooth);

        // 初始化蓝牙设备列表适配器
        bluetoothDevicesAdapter = new CustomBluetoothDeviceAdapter(requireContext(),R.layout.list_item_bluetooth,bluetoothDeviceInfoList);
        lvBluetoothDevices.setAdapter(bluetoothDevicesAdapter);

        // 设置扫描蓝牙设备的按钮点击事件
        btnScanBluetooth.setOnClickListener(v -> scanBluetoothDevices());

        // 设置蓝牙设备列表的点击事件
        lvBluetoothDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDeviceInfo deviceInfo = bluetoothDevicesAdapter.getItem(position);
            assert deviceInfo != null;
            BluetoothDevice device = getDeviceByAddress(deviceInfo.getDeviceAddress());
            connectToBluetoothDevice(device);
        });

        // 观察已配对设备列表
        bluetoothViewModel.getPairedDevices().observe(getViewLifecycleOwner(), devices -> {
            bluetoothDevicesAdapter.clear();
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                for (BluetoothDevice device : devices) {
                    String deviceAddress = device.getAddress();
                    String deviceName = device.getName() != null && !device.getName().isEmpty() ? device.getName() : "未知设备";
                    if(Objects.equals(deviceAddress, bluetoothViewModel.getConnectedDeviceAddresss().getValue())){
                        bluetoothDevicesAdapter.add(new BluetoothDeviceInfo(deviceName, deviceAddress,true));
                    }else{
                        bluetoothDevicesAdapter.add(new BluetoothDeviceInfo(deviceName, deviceAddress));
                    }
                   // 将设备添加到列表中
                    bluetoothDeviceList.add(device); // 将设备添加到列表中
                }
            }
        });

        // 观察连接状态变化
        bluetoothViewModel.getConnectedDeviceAddresss().observe(getViewLifecycleOwner(), connectedDeviceAddress -> {
            for (BluetoothDeviceInfo deviceInfo : bluetoothDeviceInfoList) {
                boolean isConnected = connectedDeviceAddress != null && connectedDeviceAddress.equals(deviceInfo.getDeviceAddress());
                deviceInfo.setConnected(isConnected);
            }
            bluetoothDevicesAdapter.notifyDataSetChanged();
        });
        return root;
    }

    // 广播接收器，用于接收蓝牙设备发现的广播
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && device.getName() != null) {
                        // 检查设备是否已经存在
                        boolean isDeviceExists = false;
                        String deviceAddress = device.getAddress();
                        for (BluetoothDeviceInfo info : bluetoothDeviceInfoList) {
                            if (info.getDeviceAddress().equals(deviceAddress)) {
                                isDeviceExists = true;
                                break;
                            }
                        }
                        // 如果设备不存在，则添加到列表中
                        if (!isDeviceExists) {
                            bluetoothDeviceList.add(device); // 将设备添加到列表中
                            String deviceName = device.getName() != null && !device.getName().isEmpty() ? device.getName() : "未知设备";
                            bluetoothDeviceInfoList.add(new BluetoothDeviceInfo(deviceName, device.getAddress()));
                            bluetoothDevicesAdapter.notifyDataSetChanged(); // 刷新列表
                            Log.i(TAG, "发现设备: " + device.getName() + " (" + device.getAddress() + ")");
                        }
                    }
                }
            }
        }
    };

    // 开始扫描蓝牙设备
    private void scanBluetoothDevices() {
        if (checkBluetoothPermissions()) {
            bluetoothViewModel.startScanning();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            requireContext().registerReceiver(bluetoothReceiver, filter);
            Toast.makeText(requireContext(), "蓝牙设备扫描中...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "缺少蓝牙扫描权限", Toast.LENGTH_SHORT).show();
        }
    }

    // 根据设备 MAC 地址查找 BluetoothDevice
    private BluetoothDevice getDeviceByAddress(String deviceAddress) {
        for (BluetoothDevice device : bluetoothDeviceList) {
            if (device.getAddress().equals(deviceAddress)) {
                return device;
            }
        }
        return null;
    }

    // 连接或断开蓝牙设备
    private void connectToBluetoothDevice(BluetoothDevice device) {
        BluetoothDevice connectedDevice = Bluetooth.getConnectedDevice();
        if (Objects.equals(device, connectedDevice)) {
            bluetoothViewModel.disconnectDevice();
            Toast.makeText(requireContext(), "断开连接", Toast.LENGTH_SHORT).show();
        } else if (device != null) {
            Toast.makeText(requireContext(), "连接中", Toast.LENGTH_SHORT).show();
            bluetoothViewModel.connectToDevice(device);
        } else {
            Toast.makeText(requireContext(), "请先选择一个设备", Toast.LENGTH_SHORT).show();
        }
    }

    // 检查蓝牙权限
    private boolean checkBluetoothPermissions() {
        String[] permissions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }

        ArrayList<String> missingPermissions = new ArrayList<>();
        for (String permission : Objects.requireNonNull(permissions)) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}