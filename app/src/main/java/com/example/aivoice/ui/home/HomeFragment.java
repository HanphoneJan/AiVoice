package com.example.aivoice.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.example.aivoice.databinding.FragmentHomeBinding;

import java.util.Locale;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private TextView recordingTimeTextView;
    private TextView audioFileTextView;
    private TextView textFileTextView;
    private Spinner spinnerModel;
    private Spinner spinnerEmotion;
    private Spinner spinnerSpeed;

    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.setContext(requireContext()); // 设置Context
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        spinnerModel = root.findViewById(R.id.spinner_model);
        spinnerEmotion = root.findViewById(R.id.spinner_emotion);
        spinnerSpeed = root.findViewById(R.id.spinner_speed);
        // 创建ArrayAdapter并设置到Spinner中
        ArrayAdapter<CharSequence> modelAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.model_options,
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

        audioFileTextView = root.findViewById(R.id.audioFileTextView_status);
        textFileTextView = root.findViewById(R.id.textFileTextView_status);

        EditText inputText = root.findViewById(R.id.inputText);

        // 初始化 ActivityResultLaunchers
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

        // 设置按钮点击事件
        binding.btnChooseAudio.setOnClickListener(v -> homeViewModel.chooseAudio(chooseAudioLauncher));
        binding.btnRecordAudio.setOnClickListener(v -> {
            if (homeViewModel.getIsRecording().getValue()) {
                homeViewModel.stopRecording();
            } else {
                homeViewModel.startRecording();
            }
        });
        binding.btnChooseFile.setOnClickListener(v -> homeViewModel.chooseFile(chooseFileLauncher));

        binding.btnUpload.setOnClickListener(v -> homeViewModel.uploadFiles(
                ((Spinner) root.findViewById(R.id.spinner_model)).getSelectedItem().toString(),
                ((Spinner) root.findViewById(R.id.spinner_emotion)).getSelectedItem().toString(),
                ((Spinner) root.findViewById(R.id.spinner_speed)).getSelectedItem().toString(),
                inputText.getText().toString()));

        homeViewModel.getAudioFileName().observe(getViewLifecycleOwner(),audioFileName->{
            audioFileTextView.setText(audioFileName);
        });
        homeViewModel.getTextFileName().observe(getViewLifecycleOwner(),textFileName->{
            textFileTextView.setText(textFileName);
        });

        // 找到UI上的TextView
        recordingTimeTextView = root.findViewById(R.id.recordingTimeTextView);

        // 观察 recordingTime LiveData
        homeViewModel.getRecordingTime().observe(getViewLifecycleOwner(), time -> {
            // 格式化时间并更新UI
            int minutes = (int) (time / 6000);
            int seconds = (int) (time % 6000/100);
            int minseconds= (int) (time%100);
            String timeFormatted = String.format(Locale.US,"录音时长：%02d:%02d:%02d", minutes, seconds,minseconds);
            recordingTimeTextView.setText(timeFormatted);
        });

        return root;
    }

    private void setupSpinnerActions(Spinner spinner) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 处理 spinner 选项选择事件
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 处理没有选项的事件
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


