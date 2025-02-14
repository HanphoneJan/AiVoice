package com.example.aivoice.ui.bluetooth;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
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

import android.widget.ArrayAdapter;
import android.widget.Button;

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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;


public class BluetoothFragment extends Fragment {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final String TAG = "BluetoothFragment";

    private BluetoothViewModel bluetoothViewModel;
    private ArrayAdapter<String> bluetoothDevicesAdapter;//蓝牙设备列表适配器
    private ArrayAdapter<String> fileNameListAdapter;

    private TextView tvBluetoothStatus;
    private TextView nowAudioFile;

    private Spinner spinnerBluetoothDevices;  //蓝牙设备下拉列表
    private Spinner spinnerFileNameList; //歌曲文件列表

    private BluetoothAdapter  bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter(); //连接设备适配器

    private  Bluetooth bluetooth = new Bluetooth();




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
        spinnerFileNameList = root.findViewById(R.id.spinner_dispname);
        // 找到连接蓝牙设备的按钮
        Button btnConnectBluetooth = root.findViewById(R.id.btn_connect_bluetooth);

        // 找到显示蓝牙连接状态的文本视图
        tvBluetoothStatus = root.findViewById(R.id.tv_bluetooth_status);
        nowAudioFile = root.findViewById(R.id.dispname);
        // 创建一个ArrayAdapter来管理下拉列表中的蓝牙设备项
        // 使用requireContext()来获取当前的上下文，android.R.layout.simple_spinner_item作为列表项的布局
        bluetoothDevicesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);

        // 设置下拉列表展开时使用的布局
        bluetoothDevicesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 将适配器设置到下拉列表（Spinner）上
        spinnerBluetoothDevices.setAdapter(bluetoothDevicesAdapter);

        fileNameListAdapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item);
        fileNameListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFileNameList.setAdapter(fileNameListAdapter);


        // 设置扫描蓝牙设备的按钮的点击监听器
        btnScanBluetooth.setOnClickListener(v -> scanBluetoothDevices()); // 当按钮被点击时，调用scanBluetoothDevices()方法来扫描蓝牙设备

        // 设置连接蓝牙设备的按钮的点击监听器
        btnConnectBluetooth.setOnClickListener(v -> connectToBluetoothDevice()); // 当按钮被点击时，调用connectToBluetoothDevice()方法来尝试连接到选中的蓝牙设备

        //按钮
        Button btnAudPlay = root.findViewById(R.id.btn_audplay);
        Button btnAudStop = root.findViewById(R.id.btn_audstop);
        Button btnAudList = root.findViewById(R.id.btn_audlist);
        Button btnNext = root.findViewById(R.id.btn_audnext);
        Button btnPrev = root.findViewById(R.id.btn_audprev);
        Button btnDirBack = root.findViewById(R.id.btn_dirback);
        Button btnDispName = root.findViewById(R.id.btn_dispname);

        Button btnPausresu = root.findViewById(R.id.btn_pausresu);
        Button btnSeekBwd = root.findViewById(R.id.btn_seekbwd);
        Button btnFwd = root.findViewById(R.id.btn_seekfwd);
        Button btnVoludec =root.findViewById(R.id.btn_voludec);
        Button btnVoluinc =root.findViewById(R.id.btn_voluinc);

        // 按钮对应的功能绑定
        btnAudPlay.setOnClickListener(v -> bluetoothViewModel.playAudio());
        btnAudStop.setOnClickListener(v -> bluetoothViewModel.stopAudio());
        btnAudList.setOnClickListener(v -> bluetoothViewModel.showAudioList());
        btnNext.setOnClickListener(v -> bluetoothViewModel.playNextTrack());
        btnPrev.setOnClickListener(v -> bluetoothViewModel.playPreviousTrack());
        btnDirBack.setOnClickListener(v -> bluetoothViewModel.goBackDirectory());
        btnDispName.setOnClickListener(v -> bluetoothViewModel.displayTrackName());

        btnPausresu.setOnClickListener(v -> bluetoothViewModel.pauseResumeAudio());
        btnSeekBwd.setOnClickListener(v -> bluetoothViewModel.seekBackward());
        btnFwd.setOnClickListener(v -> bluetoothViewModel.seekForward());
        btnVoludec.setOnClickListener(v -> bluetoothViewModel.decreaseVolume());
        btnVoluinc.setOnClickListener(v -> bluetoothViewModel.increaseVolume());

        //切换播放模式按钮
        Button btnMode = root.findViewById(R.id.btn_mode);
        // 定义三个图标的资源 ID
        int[] iconModeResources = {
                R.drawable.modeoneplay, // 第一个图标
                R.drawable.modeoneloop, // 第二个图标
                R.drawable.modelistloop  // 第三个图标
        };
        // 计数器，用于记录当前显示的图标索引
        int[] currentIconIndex = {0}; // 使用数组以便在 Lambda 表达式中修改
        // 初始化按钮图标
        btnMode.setCompoundDrawablesWithIntrinsicBounds(iconModeResources[currentIconIndex[0]], 0, 0, 0);
        btnMode.setOnClickListener(v -> {
            // 切换播放模式
            if( bluetoothViewModel.togglePlaybackMode()){
                // 更新图标索引
                currentIconIndex[0] = (currentIconIndex[0] + 1) % iconModeResources.length;
                // 设置新的图标
                btnMode.setCompoundDrawablesWithIntrinsicBounds(iconModeResources[currentIconIndex[0]], 0, 0, 0);
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
        //音频文件列表
        bluetoothViewModel.getAudList().observe(getViewLifecycleOwner(), this::onChangedFileList);
        bluetoothViewModel.getNowPlayAudioFile().observe(getViewLifecycleOwner(),this::onChangedFile);
        bluetooth.setBluetoothConnectionListener(new BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(BluetoothDevice device) {
                // 更新UI或处理连接成功后的逻辑
                tvBluetoothStatus.setText(bluetooth.getConnectedDeviceName());
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
                            bluetooth.bluetoothDeviceList.add(device);
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
            for (BluetoothDevice device : bluetooth.bluetoothDeviceList) {
                if (device.getName() != null && device.getName().equals(deviceName)) {
                    return device;  // 返回找到的设备
                }
            }
        }
        return null;  // 如果没有找到，返回null
    }
    private void connectToBluetoothDevice() {
        String deviceName = (String) spinnerBluetoothDevices.getSelectedItem();
        String connectedDeviceName = bluetooth.getConnectedDeviceName();
        if(Objects.equals(deviceName, connectedDeviceName)){
            bluetoothViewModel.disconnectDevice();
            tvBluetoothStatus.setText("未连接");
            Toast.makeText(requireContext(), "断开连接" + deviceName, Toast.LENGTH_SHORT).show();
            bluetooth.setConnectedDeviceName(null);
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

        }
        else {
            Toast.makeText(requireContext(), "请先选择一个设备", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkBluetoothPermissions() {
        String[] permissions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }

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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void onChangedFileList(Set<String> audioFileList) {
        fileNameListAdapter.clear();
        for (String audioFile : audioFileList) {
            fileNameListAdapter.add(audioFile.toString());
        }
    }
    private void onChangedFile(String nowPlayAudioFile){
        nowAudioFile.setText(nowPlayAudioFile);
    }
}
