package com.example.aivoice.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.R;
import com.example.aivoice.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private TextView recordingTimeTextView;
    private TextView audioFileTextView;
    private TextView textFileTextView;
    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.setContext(requireContext()); // 设置Context

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //ModelSpinner
        Spinner spinnerModel = root.findViewById(R.id.spinner_model);
        ArrayAdapter<CharSequence> adapterModel = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.model_options,
                android.R.layout.simple_spinner_item
        );
        adapterModel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(adapterModel);


        Spinner spinnerEmotion = root.findViewById(R.id.spinner_emotion);
        ArrayAdapter<CharSequence> adapterEmotion = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.emotion_options,
                android.R.layout.simple_spinner_item
        );
        adapterEmotion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmotion.setAdapter(adapterEmotion);

        Spinner spinnerSpeed = root.findViewById(R.id.spinner_speed);
        ArrayAdapter<CharSequence> adapterSpeed = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.speed_options,
                android.R.layout.simple_spinner_item
        );
        adapterSpeed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpeed.setAdapter(adapterSpeed);

        audioFileTextView = root.findViewById(R.id.audioFileTextView_status);
        textFileTextView = root.findViewById(R.id.textFileTextView_status);

        // 初始化 ActivityResultLaunchers
        chooseAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        homeViewModel.updateAudioFileUri(result.getData().getData());
                        audioFileTextView.setText("已选择");
                    }
                });

        chooseFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        homeViewModel.updateFileUri(result.getData().getData());
                        textFileTextView.setText("已选择");
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
                spinnerModel.getSelectedItem().toString(),spinnerEmotion.getSelectedItem().toString(),
                spinnerSpeed.getSelectedItem().toString()));

        // 观察录音状态，更新 UI
        homeViewModel.getIsRecording().observe(getViewLifecycleOwner(), isRecording -> {
            if (isRecording) {
                binding.btnRecordAudio.setText("停止录音");
                audioFileTextView.setText("已选择");
            } else {
                binding.btnRecordAudio.setText("开始录音");
            }
        });

        // 找到UI上的TextView
        recordingTimeTextView = root.findViewById(R.id.recordingTimeTextView);

        // 观察 recordingTime LiveData
        homeViewModel.getRecordingTime().observe(getViewLifecycleOwner(), time -> {
            // 格式化时间并更新UI
            int minutes = (int) (time / 60);
            int seconds = (int) (time % 60);
            String timeFormatted = String.format("录音时长：%02d:%02d", minutes, seconds);
            recordingTimeTextView.setText(timeFormatted);
        });

        // 观察错误信息
        homeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
