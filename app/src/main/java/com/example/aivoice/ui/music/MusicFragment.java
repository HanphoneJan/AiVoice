package com.example.aivoice.ui.music;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.example.aivoice.bluetooth.Bluetooth;
import com.example.aivoice.ui.bluetooth.BluetoothViewModel;

import java.util.Set;

public class MusicFragment extends Fragment {

    private static final String TAG = "MusicFragment";
    private TextView nowAudioFile;
    private Spinner spinnerFileNameList; //歌曲文件列表
    private ArrayAdapter<String> fileNameListAdapter;
    private MusicViewModel musicViewModel;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music, container, false);
        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        musicViewModel.setContext(requireContext());


        spinnerFileNameList = root.findViewById(R.id.spinner_dispname);
        nowAudioFile = root.findViewById(R.id.dispname);
        fileNameListAdapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item);
        fileNameListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFileNameList.setAdapter(fileNameListAdapter);

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
        btnAudPlay.setOnClickListener(v -> musicViewModel.playAudio());
        btnAudStop.setOnClickListener(v -> musicViewModel.stopAudio());
        btnAudList.setOnClickListener(v -> musicViewModel.showAudioList());
        btnNext.setOnClickListener(v -> musicViewModel.playNextTrack());
        btnPrev.setOnClickListener(v -> musicViewModel.playPreviousTrack());
        btnDirBack.setOnClickListener(v -> musicViewModel.goBackDirectory());
        btnDispName.setOnClickListener(v -> musicViewModel.displayTrackName());

        btnPausresu.setOnClickListener(v -> musicViewModel.pauseResumeAudio());
        btnSeekBwd.setOnClickListener(v -> musicViewModel.seekBackward());
        btnFwd.setOnClickListener(v -> musicViewModel.seekForward());
        btnVoludec.setOnClickListener(v -> musicViewModel.decreaseVolume());
        btnVoluinc.setOnClickListener(v -> musicViewModel.increaseVolume());

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
            if( musicViewModel.togglePlaybackMode()){
                // 更新图标索引
                currentIconIndex[0] = (currentIconIndex[0] + 1) % iconModeResources.length;
                // 设置新的图标
                btnMode.setCompoundDrawablesWithIntrinsicBounds(iconModeResources[currentIconIndex[0]], 0, 0, 0);
            }
        });
        //音频文件列表
        musicViewModel.getAudList().observe(getViewLifecycleOwner(), this::onChangedFileList);
        musicViewModel.getNowPlayAudioFile().observe(getViewLifecycleOwner(),this::onChangedFile);

        return root;
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

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
