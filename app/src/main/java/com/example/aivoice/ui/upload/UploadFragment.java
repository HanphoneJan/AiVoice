package com.example.aivoice.ui.upload;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.example.aivoice.databinding.FragmentUploadBinding;

import java.util.Locale;


public class UploadFragment extends Fragment {

    private FragmentUploadBinding binding;
    private UploadViewModel uploadViewModel;
    private TextView recordingTimeTextView;
    private TextView audioFileTextView;
    private TextView textFileTextView;

    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        uploadViewModel = new ViewModelProvider(this).get(UploadViewModel.class);
        uploadViewModel.setContext(requireContext()); // 设置Context
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Spinner spinnerModel = root.findViewById(R.id.spinner_model);
        Spinner spinnerEmotion = root.findViewById(R.id.spinner_emotion);
        Spinner spinnerSpeed = root.findViewById(R.id.spinner_speed);
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

        // 找到所有的 CheckBox
        CheckBox checkBoxOption1 = root.findViewById(R.id.output_option1);
        CheckBox checkBoxOption2 = root.findViewById(R.id.output_option2);
        CheckBox checkBoxOption3 = root.findViewById(R.id.output_option3);


        audioFileTextView = root.findViewById(R.id.audioFileTextView_status);
        textFileTextView = root.findViewById(R.id.textFileTextView_status);

        EditText inputText = root.findViewById(R.id.inputText);

        // 初始化 ActivityResultLaunchers
        chooseAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        assert result.getData() != null;
                        uploadViewModel.updateAudioFileUri(result.getData().getData());
                    }
                });

        chooseFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        assert result.getData() != null;
                        uploadViewModel.updateFileUri(result.getData().getData());
                    }
                });

        // 设置按钮点击事件
        binding.btnChooseAudio.setOnClickListener(v -> uploadViewModel.chooseAudio(chooseAudioLauncher));
        binding.btnRecordAudio.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(uploadViewModel.getIsRecording().getValue())) {
                uploadViewModel.stopRecording();
            } else {
                uploadViewModel.startRecording();
            }
        });
        binding.btnChooseFile.setOnClickListener(v -> uploadViewModel.chooseFile(chooseFileLauncher));

        binding.btnUpload.setOnClickListener(v -> uploadViewModel.uploadFiles(
                ((Spinner) root.findViewById(R.id.spinner_model)).getSelectedItem().toString(),
                ((Spinner) root.findViewById(R.id.spinner_emotion)).getSelectedItem().toString(),
                ((Spinner) root.findViewById(R.id.spinner_speed)).getSelectedItem().toString(),
                checkBoxOption1.isChecked(),
                checkBoxOption2.isChecked(),
                checkBoxOption3.isChecked(),
                inputText.getText().toString()));

        uploadViewModel.getAudioFileName().observe(getViewLifecycleOwner(),audioFileName-> audioFileTextView.setText(audioFileName));
        uploadViewModel.getTextFileName().observe(getViewLifecycleOwner(),textFileName-> textFileTextView.setText(textFileName));

        // 找到UI上的TextView
        recordingTimeTextView = root.findViewById(R.id.recordingTimeTextView);

        // 观察 recordingTime LiveData
        uploadViewModel.getRecordingTime().observe(getViewLifecycleOwner(), time -> {
            // 格式化时间并更新UI
            int minutes = (int) (time / 6000);
            int seconds = (int) (time % 6000/100);
            int minseconds= (int) (time%100);
            String timeFormatted = String.format(Locale.US,"录音时长：%02d:%02d:%02d", minutes, seconds,minseconds);
            recordingTimeTextView.setText(timeFormatted);
        });

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


