package com.example.aivoice.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.aivoice.databinding.FragmentHomeBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

  //用于存储音频文件路径、录音器实例、 录音状态、 音频文件 Uri 和普通文件 Uri。

    private FragmentHomeBinding binding;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 300;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private Uri audioFileUri;
    private Uri fileUri;

    //用于处理选择音频文件、录音和选择普通文件的返回结果。
    private ActivityResultLauncher<Intent> chooseAudioLauncher;
    private ActivityResultLauncher<Intent> chooseFileLauncher;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化 ActivityResultLaunchers
        chooseAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        audioFileUri = result.getData().getData();
                        // 处理选择的音频文件
                        Toast.makeText(getContext(), "已选择音频文件: " + audioFileUri.getPath(), Toast.LENGTH_SHORT).show();
                    }
                });

        // 录音完成，audioFileUri 已在 startRecording() 中设置
        ActivityResultLauncher<Intent> recordAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        // 录音完成，audioFileUri 已在 startRecording() 中设置
                        Toast.makeText(getContext(), "录音完成: " + audioFileUri.getPath(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "录音失败", Toast.LENGTH_SHORT).show();
                    }
                });

        chooseFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        fileUri = result.getData().getData();
                        // 处理选择的文件
                        Toast.makeText(getContext(), "已选择文件: " + fileUri.getPath(), Toast.LENGTH_SHORT).show();
                    }
                });

        // 设置按钮点击事件
        binding.btnChooseAudio.setOnClickListener(v -> chooseAudio());
        binding.btnRecordAudio.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });
        binding.btnChooseFile.setOnClickListener(v -> chooseFile());
        binding.btnUpload.setOnClickListener(v -> uploadFiles());

        return root;
    }


    // 选择音频文件
    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        chooseAudioLauncher.launch(intent);
    }

    // 开始录音
    private void startRecording() {
        if (checkRecordAudioPermission()) {
            try {
                String currentAudioFilePath = createAudioFile().getAbsolutePath();
                Log.d("HomeFragment", "Audio file path: " + currentAudioFilePath);
                Toast.makeText(getContext(), "路径"+currentAudioFilePath, Toast.LENGTH_SHORT).show();
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setOutputFile(currentAudioFilePath);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                binding.btnRecordAudio.setText("停止录音");
                // 设置 audioFileUri
                audioFileUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        new File(currentAudioFilePath));
                binding.tvRecordingTime.setVisibility(View.VISIBLE);
                startRecordingTimer();
            } catch (IOException e) {
                Log.e("HomeFragment", "录音失败", e);
                Toast.makeText(getContext(), "录音失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 停止录音
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                binding.btnRecordAudio.setText("录音");
                // 录音完成后更新UI或提示
                Toast.makeText(getContext(), "录音已停止", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                // Handle stop failure
                Log.e("HomeFragment", "停止录音失败", e);
                Toast.makeText(getContext(), "停止录音失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 选择文件
    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        chooseFileLauncher.launch(intent);
    }

    // 创建录音文件
    private File createAudioFile() throws IOException {
        File dir = new File(requireContext().getFilesDir(), "Music");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("无法创建录音文件夹");
            }
        }
        String fileName = "录音文件：" + System.currentTimeMillis() + ".mp3"; // 动态命名
        return new File(dir, fileName);
    }

    // 检查录音权限
    private boolean checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    // 可选：计时器更新UI（比如一个录音计时器）
    private void startRecordingTimer() {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (isRecording) {
                try {
                    Thread.sleep(100);
                    long elapsedTime = (System.currentTimeMillis() - startTime) / 100;
                    getActivity().runOnUiThread(() -> binding.tvRecordingTime.setText("录音时间: " + elapsedTime / 10.0 + "秒"));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 检查读取外部存储权限
    private boolean checkReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
            return false;
        }
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void uploadFiles() {
        if (audioFileUri != null && fileUri != null) {
            Toast.makeText(getContext(), "开始上传文件...", Toast.LENGTH_SHORT).show();

            OkHttpClient client = new OkHttpClient();

            // Create request body with multipart form data
            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            try {
                // Add audio file (use dynamic file name and MIME type)
                String audioMimeType = getContext().getContentResolver().getType(audioFileUri);
                if (audioMimeType == null) {
                    audioMimeType = "application/octet-stream";  // Default to binary if MIME type is not available
                }
                String audioFileName = getFileName(audioFileUri);  // Dynamically get file name
                requestBodyBuilder.addFormDataPart("audio", audioFileName,
                        RequestBody.create(MediaType.parse(audioMimeType),
                                getAudioFileContent(audioFileUri)));

                // Add regular file (support any file type)
                String fileMimeType = getContext().getContentResolver().getType(fileUri);
                if (fileMimeType == null) {
                    fileMimeType = "application/octet-stream";  // Default to binary if MIME type is not available
                }
                String regularFileName = getFileName(fileUri);  // Dynamically get file name
                requestBodyBuilder.addFormDataPart("file", regularFileName,
                        RequestBody.create(MediaType.parse(fileMimeType),
                                getFileContent(fileUri)));

                RequestBody requestBody = requestBodyBuilder.build();

                // Replace with your actual API endpoint URL
                String url = "https://www.hanphone.top/aivoice/api/upload";
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        // Handle failure
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        // Handle success
                        if (response.isSuccessful()) {
                            byte[] responseBody = response.body().bytes();

                            // Store the returned file
                            storeReturnedFile(responseBody);

                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "上传成功", Toast.LENGTH_SHORT).show());
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "上传失败: " + response.code(), Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "文件读取错误", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "请选择音频文件和普通文件", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper functions to get file content and store returned file
    private byte[] getAudioFileContent(Uri uri) throws IOException {
        byte[] data;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                data = getBytes(inputStream); // Use the getBytes() helper function
            } else {
                throw new IOException("打开输入流失败，URI: " + uri);
            }
        }
        return data;
    }

    private byte[] getFileContent(Uri uri) throws IOException {
        byte[] data;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                data = getBytes(inputStream); // Use the getBytes() helper function
            } else {
                throw new IOException("打开输入流失败，URI: " + uri);
            }
        }
        return data;
    }

    // Helper function to read bytes from an InputStream
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // Helper function to get the file name from the URI
    private String getFileName(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);  // Extract file name from URI
        }
        return fileName != null ? fileName : "unknown_file";
    }

    private void storeReturnedFile(byte[] data) {
        // Get a file path to store the returned file
        String fileName = "生成音频文件_" + System.currentTimeMillis() + ".mp3"; // Example file name
        File file = new File(requireContext().getFilesDir(), fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(data);
            Toast.makeText(requireContext(), "文件已保存到: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "文件保存失败", Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



}