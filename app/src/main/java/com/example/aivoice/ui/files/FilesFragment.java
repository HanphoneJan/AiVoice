package com.example.aivoice.ui.files;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;


import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FilesFragment extends Fragment {

    private static final String TAG = "FilesFragment";
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1001;
    private FilesViewModel filesViewModel;
    private ListView lvFiles;
    private ArrayList<String> audioFileList;  // 存储音频文件路径的列表
    private static String selectedFileName; //当前播放的文件
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_files, container, false);
        // 使用ViewModelProvider获取BluetoothViewModel的实例
        filesViewModel = new ViewModelProvider(this).get(FilesViewModel.class);
        filesViewModel.setContext(requireContext());
        //
        Button fileButton = root.findViewById(R.id.btn_files);
        fileButton.setOnClickListener(v -> filesViewModel.jumpToFolder(requireActivity(), REQUEST_CODE_OPEN_DIRECTORY));


        // 找到显示文件列表的列表视图
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
                String newSelectedFileName = audioFileList.get(position);
                if(Objects.equals(newSelectedFileName, selectedFileName)){
                    filesViewModel.stopAudioFile();
                    Toast.makeText(requireContext(),"已停止播放",Toast.LENGTH_SHORT).show();
                }else {
                    selectedFileName = newSelectedFileName;
                    // 调用播放方法
                    playAudio(selectedFileName);
                }
            }
        });
        return root;
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
    private void playAudio(String fileName){
            File selectedFile = new File(requireContext().getFilesDir(), "Music/" +selectedFileName);
            Toast.makeText(requireContext(), "即将播放"+fileName, Toast.LENGTH_SHORT).show();
            filesViewModel.playAudioFile(selectedFile);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                requireActivity().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );

                Toast.makeText(getContext(), "已选择目录：" + uri.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }


}
