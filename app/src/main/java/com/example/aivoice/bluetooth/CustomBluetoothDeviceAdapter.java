package com.example.aivoice.bluetooth;


import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.aivoice.R;

import java.util.ArrayList;

public class CustomBluetoothDeviceAdapter extends ArrayAdapter<BluetoothDeviceInfo> {

    private Context mContext;
    private int mResource;

    public CustomBluetoothDeviceAdapter(Context context, int resource, ArrayList<BluetoothDeviceInfo> objects) {
        super(context, resource,objects);
        mContext = context;
        mResource = resource;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前位置的蓝牙设备对象
        BluetoothDeviceInfo deviceInfo = getItem(position);

        // 检查 convertView 是否可复用
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mResource, parent, false);
        }

        // 获取列表项中的控件
        ImageView bluetoothIcon = convertView.findViewById(R.id.bluetooth_icon);
        TextView deviceName = convertView.findViewById(R.id.tv_device_name);
        TextView deviceAddress = convertView.findViewById(R.id.tv_device_address);
        ImageView successIcon = convertView.findViewById(R.id.success);
        //蓝牙图标
        bluetoothIcon.setImageResource(R.drawable.bluetooth);
        // 设置设备名称,地址
        if (deviceInfo != null) {
            deviceName.setText(deviceInfo.getDeviceName());
            deviceAddress.setText(deviceInfo.getDeviceAddress());
            // 根据连接状态设置 success 图标的可见性
            if (deviceInfo.isConnected()) {
                successIcon.setVisibility(View.VISIBLE);
                successIcon.setImageResource(R.drawable.success_square);
            } else {
                successIcon.setVisibility(View.GONE); // 重置为不可见
            }
        }
        return convertView;
    }
}