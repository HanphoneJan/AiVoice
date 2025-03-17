package com.example.aivoice.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aivoice.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private RecyclerView chatMessageList;
    private ImageButton voiceInputButton;
    private EditText messageInput;
    private ImageButton settingButton;
    private CheckBox answerQuestion;
    private CheckBox internetSearch;
    private ImageButton moreButton;
    private ImageButton sendButton;
    private String model="标准发声";
    private String emotion="自然";
    private String speed="正常";
    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;

    int[] iconChatMode = {
            R.drawable.record, // 第一个图标
            R.drawable.keyboard, // 第二个图标
    };
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载布局文件
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        homeViewModel=new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.setContext(requireContext());
        // 初始化视图
        initViews(view);
        homeViewModel.getIsRecording().observe(getViewLifecycleOwner(),isRecording->{
            if(isRecording){
                voiceInputButton.setImageResource(iconChatMode[1]);
                messageInput.setEnabled(false);
                sendButton.setEnabled(false);
            }else{
                voiceInputButton.setImageResource(iconChatMode[0]);
                messageInput.setEnabled(true);
                sendButton.setEnabled(true);
            }
        });

        chooseAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        assert result.getData() != null;
                        homeViewModel.updateAudioFileUri(result.getData().getData());
                    }
                });

        chooseFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        assert result.getData() != null;
                        homeViewModel.updateFileUri(result.getData().getData());
                    }
                });

        // 设置事件监听器
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        chatMessageList = view.findViewById(R.id.chat_message_list);
        voiceInputButton = view.findViewById(R.id.voice_input_button);
        messageInput = view.findViewById(R.id.message_input);
        settingButton = view.findViewById(R.id.setting_button);
        internetSearch = view.findViewById(R.id.output_option2);
        answerQuestion = view.findViewById(R.id.output_option1);
        moreButton = view.findViewById(R.id.more_button);
        sendButton = view.findViewById(R.id.send_button);
    }

    private void setupListeners() {
        // 示例：为语音输入按钮设置点击监听器
        voiceInputButton.setOnClickListener(v -> {
            // 处理语音输入按钮点击事件
        });

        moreButton.setOnClickListener(v -> homeViewModel.chooseFile(chooseFileLauncher));

        voiceInputButton.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(homeViewModel.getIsRecording().getValue())) {
                homeViewModel.stopRecording();
                Toast.makeText(requireContext(), "停止录音", Toast.LENGTH_SHORT).show();
            } else {
                homeViewModel.startRecording();
                Toast.makeText(requireContext(), "开始录音", Toast.LENGTH_SHORT).show();
            }
        });

        // 示例：为发送按钮设置点击监听器
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString();
            if (!message.isEmpty()) {
                // 处理发送消息逻辑
                messageInput.setText("");
            }
        });

        // 为设置按钮设置点击监听器
        settingButton.setOnClickListener(v -> showBottomSheetDialog());

        sendButton.setOnClickListener(v -> homeViewModel.uploadFiles(
                model, emotion, speed,
                answerQuestion.isChecked(),
                internetSearch.isChecked(),
                messageInput.getText().toString()));
    }

    private void showBottomSheetDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.chat_setting, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();

        // 使用 bottomSheetView 查找 Spinner
        Spinner spinnerModel = bottomSheetView.findViewById(R.id.spinner_model_chat);
        Spinner spinnerEmotion = bottomSheetView.findViewById(R.id.spinner_emotion_chat);
        Spinner spinnerSpeed = bottomSheetView.findViewById(R.id.spinner_speed_chat);

        // 创建 ArrayAdapter 并设置到 Spinner 中
        ArrayAdapter<CharSequence> modelAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.chat_options,
                android.R.layout.simple_spinner_item
        );
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);

        ArrayAdapter<CharSequence> emotionAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.emotion_options,
                android.R.layout.simple_spinner_item
        );
        emotionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmotion.setAdapter(emotionAdapter);

        ArrayAdapter<CharSequence> speedAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.speed_options,
                android.R.layout.simple_spinner_item
        );
        speedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(speedAdapter);

        // 为 model Spinner 添加选中事件监听器
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                model = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 当没有选中项时，可根据需求处理
            }
        });

        // 为 emotion Spinner 添加选中事件监听器
        spinnerEmotion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                emotion = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 当没有选中项时，可根据需求处理
            }
        });

        // 为 speed Spinner 添加选中事件监听器
        spinnerSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                speed = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 当没有选中项时，可根据需求处理
            }
        });

    }
}    