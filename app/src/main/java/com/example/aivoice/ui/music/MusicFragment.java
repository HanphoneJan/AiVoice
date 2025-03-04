package com.example.aivoice.ui.music;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Set;

public class MusicFragment extends Fragment {

    private static final String TAG = "MusicFragment";
    private TextView nowAudioFile;

    private ArrayAdapter<String> fileNameListAdapter;
    private MusicViewModel musicViewModel;
    private Button btnAudPlay; // 成员变量
    // icon播放资源
    int[] iconPlayMusic = {
            R.drawable.audplay, // 第一个图标
            R.drawable.audstop, // 第二个图标
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music, container, false);
        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        musicViewModel.setContext(requireContext());
        musicViewModel.setDataListener();

        nowAudioFile = root.findViewById(R.id.dispname);
        fileNameListAdapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item);
        fileNameListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //按钮
        btnAudPlay = root.findViewById(R.id.btn_audplay);
        Button btnAudList = root.findViewById(R.id.btn_audlist);
        Button btnNext = root.findViewById(R.id.btn_audnext);
        Button btnPrev = root.findViewById(R.id.btn_audprev);
//        Button btnDispName = root.findViewById(R.id.btn_dispname);

        Button btnSeekBwd = root.findViewById(R.id.btn_seekbwd);
        Button btnFwd = root.findViewById(R.id.btn_seekfwd);
        ImageButton btnVoludec =root.findViewById(R.id.btn_voludec);
        ImageButton btnVoluinc =root.findViewById(R.id.btn_voluinc);


        btnAudPlay.setOnClickListener(v -> {
            // 播放
            if( musicViewModel.playAudio()){
                if(musicViewModel.getIsPlay()){
                    btnAudPlay.setCompoundDrawablesWithIntrinsicBounds(iconPlayMusic[1], 0, 0, 0);
                }else {
                    btnAudPlay.setCompoundDrawablesWithIntrinsicBounds(iconPlayMusic[0], 0, 0, 0);
                }
            }
        });

        btnAudList.setOnClickListener(v -> showMusicListDialog());
        btnNext.setOnClickListener(v -> musicViewModel.playNextTrack());
        btnPrev.setOnClickListener(v -> musicViewModel.playPreviousTrack());
//        btnDispName.setOnClickListener(v -> musicViewModel.displayTrackName());

        btnSeekBwd.setOnClickListener(v -> musicViewModel.seekBackward());
        btnFwd.setOnClickListener(v -> musicViewModel.seekForward());
        btnVoludec.setOnClickListener(v -> musicViewModel.decreaseVolume());
        btnVoluinc.setOnClickListener(v -> musicViewModel.increaseVolume());

        // 切换播放模式按钮
        ImageButton imgBtnMode = root.findViewById(R.id.imgBtn_mode);

        // 定义三个图标的资源 ID
        int[] iconModeResources = {
                R.drawable.modeoneplay, // 第一个图标：顺序播放
                R.drawable.modeoneloop, // 第二个图标：单曲循环
                R.drawable.modelistloop // 第三个图标：列表循环
        };

        // 计数器，用于记录当前显示的图标索引
        int[] currentIconIndex = {0}; // 使用数组以便在 Lambda 表达式中修改，直接用整数是不行的

        // 初始化按钮图标
        imgBtnMode.setImageResource(iconModeResources[currentIconIndex[0]]);

        imgBtnMode.setOnClickListener(v -> {
            // 切换播放模式
            if (musicViewModel.togglePlaybackMode()) {
                // 更新图标索引
                currentIconIndex[0] = (currentIconIndex[0] + 1) % iconModeResources.length;
                // 设置新的图标
                imgBtnMode.setImageResource(iconModeResources[currentIconIndex[0]]);
            }
            // 注意：这里可能还需要处理播放模式切换后的其他逻辑，比如更新 UI 或通知用户
        });
        //音频文件列表
        musicViewModel.getAudList().observe(getViewLifecycleOwner(), this::onChangedFileList);
        //当前播放文件
        musicViewModel.getNowPlayAudioFile().observe(getViewLifecycleOwner(),this::onChangedFile);

        return root;
    }

    private void onChangedFileList(Set<String> audioFileList) {
        fileNameListAdapter.clear();
        for (String audioFile : audioFileList) {
            fileNameListAdapter.add(audioFile);
        }
    }
    private void onChangedFile(String nowPlayAudioFile){
        Log.i(TAG,nowPlayAudioFile);
        if(nowPlayAudioFile.isEmpty()){
            nowAudioFile.setText("");
            return;
        }
        nowAudioFile.setText(nowPlayAudioFile);
    }

    // 显示歌曲列表弹框
    private void showMusicListDialog() {
        musicViewModel.showAudioList();
        // 创建 BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.music_list, null);
        bottomSheetDialog.setContentView(dialogView);

        // 获取 ListView
        ListView listViewSongs = dialogView.findViewById(R.id.listView_songs);

        // 观察 ViewModel 中的 audList 数据
        musicViewModel.getAudList().observe(getViewLifecycleOwner(), audioSet -> {
            // 将 Set 转换为数组
            String[] songs = audioSet.toArray(new String[0]);
            // 设置适配器
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, songs);
            listViewSongs.setAdapter(adapter);
            // 设置歌曲列表点击事件
            listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
                String selectedSong = songs[position];
                if(musicViewModel.playAudStart(selectedSong)){
                    btnAudPlay.setCompoundDrawablesWithIntrinsicBounds(iconPlayMusic[1], 0, 0, 0);
                }
            });
        });

        // 显示弹框
        bottomSheetDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
