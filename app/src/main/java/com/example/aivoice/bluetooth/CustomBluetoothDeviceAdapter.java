package com.example.aivoice.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aivoice.R;

import java.util.ArrayList;

public class CustomBluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    private Context mContext;
    private int mResource;

    public CustomBluetoothDeviceAdapter(Context context, int resource, ArrayList<BluetoothDeviceInfo> objects) {
        super(context, resource);
        mContext = context;
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前位置的蓝牙设备对象
        BluetoothDevice device = getItem(position);

        // 检查 convertView 是否可复用
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
        }

        // 获取列表项中的控件
        ImageView bluetoothIcon = convertView.findViewById(R.id.bluetooth_icon);
        TextView deviceName = convertView.findViewById(R.id.tv_device_name);

        // 设置蓝牙图标
        bluetoothIcon.setImageResource(R.drawable.bluetooth);

        // 设置设备名称
        if (device != null) {
            // 检查BLUETOOTH权限
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求BLUETOOTH权限
                ActivityCompat.requestPermissions((Activity) mContext,
                        new String[]{Manifest.permission.BLUETOOTH},
                        1); // 请求码可以是任意整数
            }
            String name = device.getName();
            if (name != null && !name.isEmpty()) {
                deviceName.setText(name);
            } else {
                deviceName.setText("未命名设备");
            }
        }

        return convertView;
    }
}