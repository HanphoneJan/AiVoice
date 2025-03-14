package com.example.aivoice.ui.home;

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
import com.example.aivoice.databinding.FragmentHomeBinding;

import java.util.Locale;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private TextView recordingTimeTextView;
    private TextView audioFileTextView;
    private TextView textFileTextView;

    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.setContext(requireContext()); // 设置Context
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Spinner spinnerModel = root.findViewById(R.id.spinner_chat);
        Spinner spinnerEmotion = root.findViewById(R.id.spinner_chat_emotion);
        // 创建ArrayAdapter并设置到Spinner中
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

        EditText inputText = root.findViewById(R.id.message_input);


//        binding.moreButton.setOnClickListener(v -> homeViewModel.chooseFile(chooseFileLauncher));



        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


