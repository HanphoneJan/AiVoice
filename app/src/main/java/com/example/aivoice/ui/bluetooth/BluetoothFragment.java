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
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.example.aivoice.bluetooth.Bluetooth;
import com.example.aivoice.bluetooth.BluetoothConnectionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;


public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TAG = "BluetoothFragment";

    private BluetoothViewModel bluetoothViewModel;
    private ArrayAdapter<String> bluetoothDevicesAdapter;//适配器
    private ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();  //存储蓝牙设备
    private TextView tvBluetoothStatus;
    private ListView lvFiles;
    private ArrayList<String> audioFileList;  // 存储音频文件路径的列表
    private static String selectedFileName; //当前播放的文件
    private static String connectedDeviceName;//当前连接的设备
    private Spinner spinnerBluetoothDevices;  //下拉列表

    private BluetoothAdapter  bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();

    private Bluetooth bluetooth = new Bluetooth();




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



        //播放、暂停、停止按钮
        Button btnPlay = root.findViewById(R.id.btn_play);
        Button btnPause = root.findViewById(R.id.btn_pause);
        Button btnStop = root.findViewById(R.id.btn_stop);
        btnPlay.setOnClickListener((v -> playAudio(selectedFileName)));
        btnPause.setOnClickListener((v -> pauseAudio()));
        btnStop.setOnClickListener((v -> stopAudio()));
        // 找到显示文件列表的列表视图（用于显示从蓝牙设备接收的文件）
        lvFiles = root.findViewById(R.id.lv_files);
        audioFileList = new ArrayList<>();  // 初始化音频文件列表
        // 设置适配器
        ArrayAdapter<String> audioFileadapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, audioFileList);
        lvFiles.setAdapter(audioFileadapter);
        loadAudioFiles();
        // 设置 ListView 的点击事件监听器
        lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedFileName = audioFileList.get(position);
                // 调用播放方法
                playAudio(selectedFileName);
            }
        });
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

        bluetooth.setBluetoothConnectionListener(new BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                // 更新UI或处理连接成功后的逻辑

            }
            @Override
            public void onDeviceDisconnected(BluetoothDevice device) {
                // 更新UI或处理断开连接后的逻辑
                tvBluetoothStatus.setText("未连接");
            }
        });

        bluetoothViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

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
                    if (device != null ) {
                        String deviceName = device.getName();
                        if (deviceName != null) {
                            // 更新设备列表
                            Toast.makeText(requireContext(), "已增加一个蓝牙设备", Toast.LENGTH_SHORT).show();
                            Log.i(TAG,"增加一个蓝牙设备");
                            bluetoothDeviceList.add(device);
                            bluetoothDevicesAdapter.add(deviceName);
                            bluetoothDevicesAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    };

    //开启扫描，注册广播
    private void scanBluetoothDevices() {
        if (checkBluetoothPermissions()) {
            bluetoothViewModel.startScanning();
            // 注册接收器监听设备发现的广播，有注册就要有注销
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            requireContext().registerReceiver(bluetoothReceiver, filter);
            Toast.makeText(requireContext(), "蓝牙设备扫描中...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "缺少蓝牙扫描权限", Toast.LENGTH_SHORT).show();
        }
    }

    // 根据设备名称查找BluetoothDevice
    private BluetoothDevice getDeviceByName(String deviceName) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN)
                == PackageManager.PERMISSION_GRANTED) {
            for (BluetoothDevice device : bluetoothDeviceList) {
                if (device.getName() != null && device.getName().equals(deviceName)) {
                    return device;  // 返回找到的设备
                }
            }
        }
        return null;  // 如果没有找到，返回null
    }
    private void connectToBluetoothDevice() {
        String deviceName = (String) spinnerBluetoothDevices.getSelectedItem();
        if(Objects.equals(deviceName, connectedDeviceName)){
            bluetoothViewModel.disconnectDevice();
            tvBluetoothStatus.setText("未连接");
            Toast.makeText(requireContext(), "断开连接" + deviceName, Toast.LENGTH_SHORT).show();
            connectedDeviceName = null;
        }
        else if (deviceName != null) {
            Toast.makeText(requireContext(), "尝试连接到 " + deviceName, Toast.LENGTH_SHORT).show();
            // 获取BluetoothAdapter并检查是否可用
            if (bluetoothAdapter == null) {
                Log.e(TAG, "蓝牙适配器不可用");
                Toast.makeText(requireContext(), "蓝牙适配器不可用", Toast.LENGTH_SHORT).show();
                return;
            }
            BluetoothDevice selectedDevice = getDeviceByName(deviceName);
            bluetoothViewModel.connectToDevice(selectedDevice);
            tvBluetoothStatus.setText("连接到: " + deviceName);
            connectedDeviceName = deviceName;
        }
        else {
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
            if (!musicDir.isDirectory()) {
                Toast.makeText(requireContext(), "指定路径不是文件夹", Toast.LENGTH_SHORT).show();
                return;
            }
            // 如果是文件夹且存在，加载音频文件
            File[] files = musicDir.listFiles();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);

            if (files != null) {
                for (File file : files) {
                    adapter.add(file.getName());
                    audioFileList.add(file.getName());
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

    private void playAudio(String fileName){
        if(!connectedDeviceName.isEmpty()){
            bluetoothViewModel.playAudioBluetooth();
            Toast.makeText(requireContext(), "蓝牙播放", Toast.LENGTH_SHORT).show();
        }
        else if(!fileName.isEmpty()){
            File selectedFile = new File(requireContext().getFilesDir(), "Music/" + selectedFileName);
            Toast.makeText(requireContext(), "即将播放"+fileName, Toast.LENGTH_SHORT).show();
            bluetoothViewModel.playAudioFile(selectedFile);
        }else {
            Toast.makeText(requireContext(), "请先选择一个文件", Toast.LENGTH_SHORT).show();
        }

    }

    private void pauseAudio() {
        if (!connectedDeviceName.isEmpty()) {
            bluetoothViewModel.pauseAudioBluetooth();
            Toast.makeText(requireContext(), "蓝牙暂停播放", Toast.LENGTH_SHORT).show();
        } else {
            bluetoothViewModel.pauseAudioFile();
            Toast.makeText(requireContext(), "已暂停播放", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio(){
        if (!connectedDeviceName.isEmpty()) {
            bluetoothViewModel.stopAudioBluetooth();
            Toast.makeText(requireContext(), "蓝牙暂停播放", Toast.LENGTH_SHORT).show();
        } else {
        bluetoothViewModel.stopAudioFile();
        selectedFileName = null ;
        Toast.makeText(requireContext(), "已停止播放", Toast.LENGTH_SHORT).show();
    }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Release resources and unregister receivers here
        //bluetoothViewModel.stopScanning(); // Stop Bluetooth scanning if it's running
         // Unregister the Bluetooth receiver
//        bluetoothViewModel.onCleared(); // Clean up Bluetooth resources
//        bluetoothViewModel = null; // Release the ViewModel
//        bluetoothDevicesAdapter = null; // Release the adapter
//        bluetoothDeviceList.clear(); // Clear the device list
        // ... release any other resources held by the fragment ...
    }
}
