package com.example.aivoice.ui.upload;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.files.UriManager;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.concurrent.TimeUnit;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadViewModel extends ViewModel {
    private static final String TAG = "UploadViewModel";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> audioFileUri = new MutableLiveData<>();
    private final MutableLiveData<Uri> fileUri = new MutableLiveData<>();
    private final MutableLiveData<String> audioFileName = new MutableLiveData<>();
    private final MutableLiveData<String> textFileName = new MutableLiveData<>();
    private Context context;
    private final MutableLiveData<Long> recordingTime = new MutableLiveData<>(0L); // 录音时间，单位：秒
    private static Uri musicUri;

    private MediaRecorder mediaRecorder;
    private Handler handler = new Handler(Looper.getMainLooper()); // 用于更新UI
    private Runnable updateTimeRunnable;
    // 初始化并启动计时器
    private static long startTime; // 录音开始时间
    private static long elapsedTime = 0; // 已录音的时间

    // 添加一个公共的无参构造函数
    public UploadViewModel() {

    }

    public void setContext(Context context) {
        this.context = context;
    }


    // LiveData Getters
    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }

    public LiveData<Long> getRecordingTime() {
        return recordingTime; // 用于更新UI的录音时间
    }
    // Setters for updating LiveData
    public void updateAudioFileUri(Uri uri) {
        audioFileUri.setValue(uri);
        audioFileName.setValue(getFileName(uri));
    }

    public void updateFileUri(Uri uri) {

        fileUri.setValue(uri);
        textFileName.setValue(getFileName(uri));
    }

    public LiveData<String> getAudioFileName(){
        return audioFileName;
    }
    public LiveData<String> getTextFileName(){
        return textFileName;
    }

    // 选择音频文件
    public void chooseAudio(ActivityResultLauncher<Intent> chooseAudioLauncher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");  // 允许选择所有文件类型，包括 wav
        //在我的手机上有bug，被迫改成audio/*
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"audio/*"});  // 限制选择 wav 文件
        chooseAudioLauncher.launch(intent);
    }


    // 选择普通文件
    public void chooseFile(ActivityResultLauncher<Intent> chooseFileLauncher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/vnd.openxmlformats-officedocument.presentationml.presentation"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        chooseFileLauncher.launch(intent);
    }

    // 创建录音文件
    private File createAudioFile() throws IOException {
        File dir = new File(context.getFilesDir(), "Music");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("无法创建录音文件夹");
            }
        }
        String fileName = "录音文件：" + System.currentTimeMillis() + ".wav"; // 动态命名
        return new File(dir, fileName);
    }

    // 检查录音权限
    public boolean checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }
        return true;
    }

    // 开始录音
    public void startRecording() {
        if (checkRecordAudioPermission()) {
            try {
                ParcelFileDescriptor pfd;
                Uri outputUri;

                // 1. 获取用户选择的目录
                musicUri = UriManager.getUri(context);
                Log.d(TAG, "Music URI: " + musicUri);

                if (musicUri != null) {
                    // 使用 SAF 目录存储录音文件
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, musicUri);
                    if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory()) {
                        String fileName = "录音_" + System.currentTimeMillis() + ".wav";
                        DocumentFile audioFileDoc = pickedDir.createFile("audio/wav", fileName);

                        if (audioFileDoc != null) {
                            outputUri = audioFileDoc.getUri();
                            pfd = context.getContentResolver().openFileDescriptor(outputUri, "rw");
                            mediaRecorder = new MediaRecorder();
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            assert pfd != null;
                            mediaRecorder.setOutputFile(pfd.getFileDescriptor()); // 兼容 SAF 和本地文件
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                            isRecording.setValue(true);
                            audioFileUri.setValue(outputUri);
                            audioFileName.setValue(getFileName(outputUri));
                        } else {
                            Toast.makeText(context, "无法创建音频文件", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(context, "无法访问选定目录", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    String currentAudioFilePath = createAudioFile().getAbsolutePath();
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mediaRecorder.setOutputFile(currentAudioFilePath);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    isRecording.setValue(true);
                    Uri uri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider",
                            new File(currentAudioFilePath));
                    audioFileUri.setValue(uri);
                    audioFileName.setValue(getFileName(uri));
                }

                // 6. 启动计时器
                startTime = System.currentTimeMillis();
                elapsedTime = 0; // 重置已录音时间
                updateTimeRunnable = new Runnable() {
                    @Override
                    public void run() {
                        elapsedTime = (System.currentTimeMillis() - startTime) / 10;
                        recordingTime.setValue(elapsedTime);
                        handler.postDelayed(this, 10); // 每秒更新一次
                    }
                };
                handler.post(updateTimeRunnable);

            } catch (IOException e) {
                Log.e(TAG, "录音失败", e);
                Toast.makeText(context, "录音失败", Toast.LENGTH_SHORT).show();
            }
        }
    }




    // 停止录音
    public void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording.setValue(false);
                handler.removeCallbacks(updateTimeRunnable);
            } catch (RuntimeException e) {
                Log.e("HomeViewModel", "停止录音失败", e);
                Toast.makeText(context, "停止录音失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 上传文件
    public void uploadFiles(String model, String emotion, String speed,Boolean output_audio,Boolean output_video,Boolean output_text,String userInput) {
        if (model != null && emotion != null && speed != null) {
            // 设置超时时间
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES) // 连接超时
                    .readTimeout(3, TimeUnit.MINUTES)    // 读取超时
                    .writeTimeout(3, TimeUnit.MINUTES)   // 写入超时
                    .build();

            // 初始化MultipartBody.Builder并设置类型为FORM
            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            if(!output_audio && !output_text && !output_video){
                Toast.makeText(context, "请选择输出形式", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // 添加Model参数部分
                switch (speed) {
                    case "正常":
                        speed = "x1.0";
                        break;
                    case "稍快":
                        speed = "x1.1";
                        break;
                    case "稍慢":
                        speed = "x0.9";
                        break;
                    case "快速":
                        speed = "x1.2";
                        break;
                    case "慢速":
                        speed = "x0.8";
                        break;
                }

                requestBodyBuilder.addFormDataPart("model", model);
                requestBodyBuilder.addFormDataPart("emotion", emotion);
                requestBodyBuilder.addFormDataPart("speed", speed);
                requestBodyBuilder.addFormDataPart("output_audio", String.valueOf(output_audio));
                requestBodyBuilder.addFormDataPart("output_video", String.valueOf(output_video));
                requestBodyBuilder.addFormDataPart("output_text", String.valueOf(output_text));
                if(model.equals("克隆音色")){
                    // 添加音频文件部分
                    if(audioFileUri.getValue()!=null){
                        String audioMimeType = context.getContentResolver().getType(audioFileUri.getValue());
                        String audioFileName = getFileName(audioFileUri.getValue());
                        assert audioMimeType != null;
                        requestBodyBuilder.addFormDataPart("audio", audioFileName,
                                RequestBody.create(getAudioFileContent(audioFileUri.getValue()),MediaType.parse(audioMimeType)
                                ));
                    }else{
                        Toast.makeText(context, "请选择音频文件", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 添加常规文件部分
                if (userInput.isEmpty()) {
                    // 如果 userInput 为空，使用 fileUri 中的文件
                    if (fileUri.getValue() != null) {
                        String fileMimeType = context.getContentResolver().getType(fileUri.getValue());
                        String regularFileName = getFileName(fileUri.getValue());
                        assert fileMimeType != null;
                        requestBodyBuilder.addFormDataPart("file", regularFileName,
                                RequestBody.create(
                                        getFileContent(fileUri.getValue()),MediaType.parse(fileMimeType)));
                    } else {
                        // 如果 fileUri 也为空，提示用户选择文件或输入文本
                        Toast.makeText(context, "请选择文本文件或输入文本", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    // 如果 userInput 不为空
                    if (fileUri.getValue() != null) {
                        Toast.makeText(context, "请勿同时选择文本和输入文本", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        // 如果 fileUri 为空，将 userInput 保存为文件并构建 RequestBody
                        File textFile = saveTextAsFile(userInput); // 保存文本为文件
                        if (textFile != null) {
                            requestBodyBuilder.addFormDataPart("file", textFile.getName(),
                                    RequestBody.create(textFile,MediaType.parse("text/plain")));
                            Log.i(TAG,"生成txt文件成功");
                        } else {
                            Log.i(TAG,"生成txt文件失败");
                            return;
                        }
                    }
                }
                // 构建完整的请求体
                RequestBody requestBody = requestBodyBuilder.build();
                String url = "https://www.hanphone.top/aivoice/upload";
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                // 异步执行请求
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        // 使用runOnUiThread切换到主线程
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                byte[] responseBody = response.body().bytes();
                                String contentType = response.header("Content-Type");
                                storeReturnedFile(responseBody, contentType);
                                Log.i(TAG, "生成音频成功");
                                // 使用runOnUiThread切换到主线程
                                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "生成文件成功", Toast.LENGTH_SHORT).show());
                            } else {
                                Log.e(TAG, "生成音频失败");
                                // 使用runOnUiThread切换到主线程
                                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "生成音频失败", Toast.LENGTH_SHORT).show());
                            }
                        }
                        //释放资源
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "文件读取错误");
            }
        } else {
            Toast.makeText(context, "未选择参数", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getAudioFileContent(Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            Log.i(TAG,"获取本地音频成功");
            assert inputStream != null;
            return getBytes(inputStream);
        }
    }

    private byte[] getFileContent(Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            Log.i(TAG,"获取本地文件成功");
            assert inputStream != null;
            return getBytes(inputStream);
        }
    }

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

    private String getFileName(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        return fileName != null ? fileName : "未命名文件";
    }


    private void storeReturnedFile(byte[] data,String contentType) {
        String fileExtension = getFileExtensionFromMimeType(contentType);
        String fileName = "生成音频文件_" + System.currentTimeMillis() + "." + fileExtension;
        musicUri = UriManager.getUri(context);
        if (musicUri != null) {
            // 如果 uri 不为空，使用用户选择的目录
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, musicUri);
            if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory()) {
                // SAF 目录下创建文件
                DocumentFile newFile = pickedDir.createFile(contentType, fileName);
                if (newFile != null) {
                    try (OutputStream outputStream = context.getContentResolver().openOutputStream(newFile.getUri())) {
                        if (outputStream != null) {
                            outputStream.write(data);
                            Log.i(TAG,"保存成功");
                        }
                    } catch (IOException e) {
                        Log.e(TAG,"文件IO错误");
                    }
                } else {
                    // 创建文件失败
                    Log.i(TAG,"无法保存生成的音频文件");
                }
            } else {
                Log.i(TAG,"无法访问选定的目录");
            }
        } else {
            // 默认目录路径
            File musicDir = new File(context.getFilesDir(), "Music");
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }
            File file = new File(musicDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(data);
            } catch (IOException e) {
                Log.i(TAG,"IO错误");
            }
        }
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return "wav"; // 默认扩展名
        }
        switch (mimeType) {
            // 音频文件类型
            case "audio/wav":
                return "wav";
            case "audio/mpeg":
                return "mp3";
            case "audio/ogg":
                return "ogg";
            case "audio/aac":
                return "aac";
            case "audio/flac":
                return "flac";
            case "audio/webm":
            case "video/webm":
                return "webm";

            // 视频文件类型
            case "video/mp4":
                return "mp4";
            case "video/quicktime":
                return "mov";
            case "video/x-msvideo":
                return "avi";
            case "video/x-matroska":
                return "mkv";
            case "video/3gpp":
                return "3gp";
            case "video/mpeg":
                return "mpeg";

            // 其他类型
            default:
                return "wav"; // 默认扩展名
        }
    }
    private File saveTextAsFile(String text) {
        // 使用时间戳生成唯一的文件名
        String fileName = "text_file_" + System.currentTimeMillis() + ".txt";

        // 保存到应用的默认目录
        File textDir = new File(context.getFilesDir(), "TextFiles");
        if (!textDir.exists()) {
            textDir.mkdirs();
        }

        File file = new File(textDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(text.getBytes());
            return file; // 返回保存的文件对象
        } catch (IOException e) {
            return null; // 保存失败，返回 null
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
