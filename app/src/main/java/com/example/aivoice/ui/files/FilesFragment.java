package com.example.aivoice.ui.files;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.ui.PlayerView;

import com.example.aivoice.R;
import com.example.aivoice.files.UriManager;


import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FilesFragment extends Fragment {

    private static final String TAG = "FilesFragment";
    private ActivityResultLauncher<Uri> openDirectoryLauncher;

    private FilesViewModel filesViewModel;
    private static Uri fileUri = null;
    private ListView lvFiles;
    private ArrayList<String> audioFileList;  // 存储音频文件路径的列表
    private PlayerView playerView;
    private boolean isFullScreen = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_files, container, false);
        // 使用ViewModelProvider获取BluetoothViewModel的实例
        filesViewModel = new ViewModelProvider(this).get(FilesViewModel.class);
        filesViewModel.setContext(requireContext());
        //
        Button fileButton = root.findViewById(R.id.btn_files);
        fileButton.setOnClickListener(v -> openDirectorySelector());

        // 找到显示文件列表的列表视图
        lvFiles = root.findViewById(R.id.lv_files);
        audioFileList = new ArrayList<>();  // 初始化音频文件列表
        // 设置适配器
        ArrayAdapter<String> audioFileadapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, audioFileList);
        lvFiles.setAdapter(audioFileadapter);
        loadAudioFiles();
        // 设置 ListView 的点击事件监听器
        lvFiles.setOnItemClickListener((parent, view, position, id) -> {
            String newSelectedFileName = audioFileList.get(position);
            playAudio(newSelectedFileName);
        });

        // 初始化 PlayerView
        playerView = root.findViewById(R.id.player_view);
        // 获取全屏按钮
        ImageButton fullScreenButton = playerView.findViewById(R.id.exo_fullscreen);
        fullScreenButton.setOnClickListener(v -> toggleFullScreen());
        // 将 PlayerView 绑定到 ExoPlayer
        playerView.setPlayer(filesViewModel.getExoPlayer());
        filesViewModel.getIsVideoPlaying().observe(getViewLifecycleOwner(), isVideoPlaying -> {
            if (isVideoPlaying) {
                playerView.setVisibility(View.VISIBLE); // 显示 PlayerView
                fullScreenButton.setVisibility(View.VISIBLE);
            }else{
                playerView.setVisibility(View.GONE); // 隐藏 PlayerView
                fullScreenButton.setVisibility(View.GONE);

            }
        });

        return root;
    }
    private void toggleFullScreen() {
        if (isFullScreen) {
            // 退出全屏
            exitFullScreen();
        } else {
            // 进入全屏
            enterFullScreen();
        }
        isFullScreen = !isFullScreen;
    }

    private void enterFullScreen() {
        // 获取 WindowInsetsController
        WindowInsetsController insetsController = requireActivity().getWindow().getInsetsController();
        if (insetsController != null) {
            // 隐藏状态栏和导航栏
            insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            // 设置沉浸式模式
            insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        // 设置 PlayerView 为全屏
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(params);

        // 强制横屏
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void exitFullScreen() {
        // 获取 WindowInsetsController
        WindowInsetsController insetsController = requireActivity().getWindow().getInsetsController();
        if (insetsController != null) {
            // 显示状态栏和导航栏
            insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        }

        // 恢复 PlayerView 的原始布局
        ViewGroup.LayoutParams params = playerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(params);

        // 恢复竖屏
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 退出全屏（如果正在全屏）
        if (isFullScreen) {
            exitFullScreen();
            isFullScreen = false;
        }
    }

    private void loadAudioFiles() {
        try {
            File musicDir;
            fileUri = UriManager.getUri(requireContext());
            if (fileUri != null) {
                // 使用已选择的目录
                DocumentFile pickedDir = DocumentFile.fromTreeUri(requireContext(), fileUri);
                if (pickedDir == null || !pickedDir.exists() || !pickedDir.isDirectory()) {
                    Toast.makeText(requireContext(), "无法访问选定目录", Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
                audioFileList.clear(); // 清空之前的数据
                for (DocumentFile file : pickedDir.listFiles()) {
                    if (file.isFile() && file.getName() != null) {
                        adapter.add(file.getName());
                        audioFileList.add(file.getName());
                    }
                }
                lvFiles.setAdapter(adapter);
            } else {
                // 默认路径
                musicDir = new File(requireContext().getFilesDir(), "Music");
                // 检查文件夹是否存在，如果不存在则创建
                if (!musicDir.exists()) {
                    boolean isCreated = musicDir.mkdirs();
                    if (!isCreated) {
                        Toast.makeText(requireContext(), "无法创建音频文件夹", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                if (!musicDir.isDirectory()) {
                    Toast.makeText(requireContext(), "指定路径不是文件夹", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 加载本地文件
                File[] files = musicDir.listFiles();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
                audioFileList.clear(); // 清空之前的数据
                if (files != null) {
                    for (File file : files) {
                        adapter.add(file.getName());
                        audioFileList.add(file.getName());
                    }
                }
                lvFiles.setAdapter(adapter);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "加载音频文件失败", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "加载音频文件失败", e);
        }
    }

    private void playAudio(String fileName){
            fileUri = UriManager.getUri(requireContext());
            if (fileUri != null) {
                Uri audioUri = filesViewModel.getFileByUri(fileName); // 用于存储要播放的文件的 URI
                if (audioUri == null) {
                    Toast.makeText(requireContext(), "已停止播放", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(filesViewModel.playFile(audioUri)){
                    Toast.makeText(requireContext(), "播放"+fileName, Toast.LENGTH_SHORT).show();
                }
            }else{
                File selectedFile = filesViewModel.getFile(fileName);
                if (selectedFile == null) {
                    Toast.makeText(requireContext(), "已停止播放", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(filesViewModel.playFile(selectedFile)){
                    Toast.makeText(requireContext(), "播放"+fileName, Toast.LENGTH_SHORT).show();
                }
            }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 注册 ActivityResultLauncher，用于打开文件夹选择器
        openDirectoryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    // 处理返回的 URI
                    if (uri != null) {
                        Log.i(TAG, "选中的目录: " + uri);
                        UriManager.setUri(requireContext(),uri);
                        loadAudioFiles();
                        // 保存目录权限
                        requireActivity().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );
                        // 将选中的 URI 存储到 ViewModel 或其他地方，以便后续使用
                    }
                });
    }

    // 点击按钮打开文件夹选择器
    public void openDirectorySelector() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        openDirectoryLauncher.launch(intent.getData());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        openDirectoryLauncher.unregister(); // 注销 ActivityResultLauncher
        playerView.setPlayer(null);
    }

}
