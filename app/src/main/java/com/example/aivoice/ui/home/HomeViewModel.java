package com.example.aivoice.ui.home;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;

import android.media.MediaPlayer;
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
import com.example.aivoice.message.MessageInfo;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private MediaPlayer mediaPlayer;
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> audioFileUri = new MutableLiveData<>();
    private final MutableLiveData<Uri> fileUri = new MutableLiveData<>();
    private final MutableLiveData<String> audioFileName = new MutableLiveData<>();
    private final MutableLiveData<String> textFileName = new MutableLiveData<>();
    private final MutableLiveData<List<MessageInfo>> responseList = new MutableLiveData<>(new ArrayList<>());
    private Context context;

    private final MutableLiveData<String> recordingTime = new MutableLiveData<>(); // 录音时间，单位：秒
    private static Uri musicUri;

    private MediaRecorder mediaRecorder;
    private final Handler handler = new Handler(Looper.getMainLooper()); // 用于更新UI
    private Runnable updateTimeRunnable;
    private static Uri nowPlayAudio; //当前播放的文件
    // 初始化并启动计时器
    private static long startTime; // 录音开始时间
    private static long elapsedTime = 0; // 已录音的时间
    // 复制事件流
    private final MutableLiveData<String> copyEvent = new MutableLiveData<>();
    private static final String chatUrl="https://www.hanphone.top/aivoice/chat";
    public LiveData<String> getCopyEvent() {
        return copyEvent;
    }
    // 添加一个公共的无参构造函数
    public HomeViewModel() {

    }

    public void setContext(Context context) {
        this.context = context;
    }

    // LiveData Getters
    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }

    public void updateFileUri(Uri uri) {
        fileUri.setValue(uri);
        textFileName.setValue(getFileName(uri));
        Toast.makeText(context, "已选择文件"+textFileName.getValue(), Toast.LENGTH_SHORT).show();
    }

    public void addMessageInfo(String message,String recordTime, Uri audioUri, boolean isUser) {
        List<MessageInfo> currentList = responseList.getValue();
        Objects.requireNonNull(currentList).add(new MessageInfo(message,recordTime,audioUri,isUser));
        responseList.postValue(currentList);
    }

    public LiveData<List<MessageInfo>> getResponseInfoList() {
        return responseList;
    }

    // 选择普通文件
    public void chooseFile(ActivityResultLauncher<Intent> chooseFileLauncher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
//        String[] mimeTypes = {"text/plain", "application/vnd.openxmlformats-officedocument.presentationml.presentation"};
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
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

                            mediaRecorder.setOutputFile(Objects.requireNonNull(pfd).getFileDescriptor()); // 兼容 SAF 和本地文件
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
                        elapsedTime = (System.currentTimeMillis() - startTime)/1000 ;
                        long minutes = elapsedTime / 60;
                        long seconds = elapsedTime % 60;
                        @SuppressLint("DefaultLocale") String formattedTime = (minutes > 0)
                                ? String.format("%d'%02d''", minutes, seconds)
                                : String.format("%d''", seconds);
                        recordingTime.setValue(formattedTime);
                        handler.postDelayed(this, 100);
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
    public void uploadFiles(String subject,String model, String emotion, String speed,Boolean answerQuestion,Boolean internetSearch,String userInput) {
        if (model != null && emotion != null && speed != null) {
            // 设置超时时间
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES) // 连接超时
                    .readTimeout(3, TimeUnit.MINUTES)    // 读取超时
                    .writeTimeout(6, TimeUnit.MINUTES)   // 写入超时
                    .build();

            // 初始化MultipartBody.Builder并设置类型为FORM
            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

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
                requestBodyBuilder.addFormDataPart("subject",subject);
                requestBodyBuilder.addFormDataPart("model", model);
                requestBodyBuilder.addFormDataPart("emotion", emotion);
                requestBodyBuilder.addFormDataPart("speed", speed);
                requestBodyBuilder.addFormDataPart("answerQuestion", String.valueOf(answerQuestion));
                requestBodyBuilder.addFormDataPart("internetSearch", String.valueOf(internetSearch));

                //添加文本文件
                if (fileUri.getValue() != null) {
                    String fileMimeType = context.getContentResolver().getType(fileUri.getValue());
                    String regularFileName = getFileName(fileUri.getValue());
                    requestBodyBuilder.addFormDataPart("file", regularFileName,
                            RequestBody.create(
                                    getFileContent(fileUri.getValue()),MediaType.parse(Objects.requireNonNull(fileMimeType))));
                }

                if (audioFileUri.getValue()!=null) {
                    String fileMimeType = context.getContentResolver().getType(audioFileUri.getValue());
                    String regularFileName = getFileName(audioFileUri.getValue());
                    requestBodyBuilder.addFormDataPart("audio", regularFileName,
                            RequestBody.create(
                                    getFileContent(audioFileUri.getValue()),MediaType.parse(Objects.requireNonNull(fileMimeType))));
                   addMessageInfo(null,recordingTime.getValue(),audioFileUri.getValue(),true);
                } else {
                    if(!userInput.isEmpty()){
                        requestBodyBuilder.addFormDataPart("messageInput", userInput);
                        addMessageInfo(userInput,null,null,true);
                    }else{
                        Toast.makeText(context, "请输入语音或文本", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                // 构建完整的请求体
                RequestBody requestBody = requestBodyBuilder.build();
                Request request = new Request.Builder()
                        .url(chatUrl)
                        .post(requestBody)
                        .build();

                // 异步执行请求
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        // 使用runOnUiThread切换到主线程
                        audioFileUri.postValue(null);
                        fileUri.postValue(null);
                        audioFileName.postValue(null);
                        textFileName.postValue(null);
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            audioFileUri.postValue(null);
                            fileUri.postValue(null);
                            audioFileName.postValue(null);
                            textFileName.postValue(null);
                            if (response.isSuccessful()) {
                                MediaType contentType = Objects.requireNonNull(response.body()).contentType();
                                if (contentType != null && "multipart".equals(contentType.type())) {
                                    String boundary = contentType.parameter("boundary");
                                    if (boundary == null) {
                                        handleError("未找到 multipart 边界");
                                        return;
                                    }
                                    String messageAnswer = null;
                                    byte[] audioBytes;

                                    try (MultipartReader reader = new MultipartReader(response.body().source(), boundary)) {
                                        MultipartReader.Part part;
                                        while ((part = reader.nextPart()) != null) {
                                            Headers headers = part.headers();
                                            String contentDisposition = headers.get("Content-Disposition");
                                            if (contentDisposition == null) continue;

                                            // 解析name参数
                                            String name = extractNameFromContentDisposition(contentDisposition);
                                            // 新增：解析 filename 参数（服务器返回的文件名）
                                            String serverFileName = extractFilenameFromContentDisposition(contentDisposition);
                                            if ("messageAnswer".equals(name)) {
                                                messageAnswer = part.body().readUtf8();
                                            } else if ("audioFile".equals(name)) {
                                                audioBytes = part.body().readByteArray();
                                                // 优先使用服务器返回的文件名，若没有则生成默认名
                                                String finalFileName = (serverFileName != null) ? serverFileName :
                                                        "audio_" + System.currentTimeMillis() + ".mp3";
                                                String audioContentType = "audio/mpeg"; // 默认类型
                                                Uri audioUri = storeReturnedFile(audioBytes, audioContentType,finalFileName);
                                                addMessageInfo(messageAnswer, null,audioUri,false);
                                            }
                                        }
                                    } catch (IOException e) {
                                        handleError("解析响应失败: " + e.getMessage());
                                        return;
                                    }

                                } else {
                                    handleError("响应格式错误");
                                }
                            } else {
                                handleError("生成失败，状态码: " + response.code());
                            }
                        }
                        audioFileUri.postValue(null);
                        fileUri.postValue(null);
                        audioFileName.postValue(null);
                        textFileName.postValue(null);
                        handleError("服务器无响应");
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "文件读取错误");
            }
        } else {
            Log.e(TAG, "未选择参数");
        }
    }

    private byte[] getFileContent(Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            Log.i(TAG,"获取本地文件成功");
            return getBytes(Objects.requireNonNull(inputStream));
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


    private Uri storeReturnedFile(byte[] data, String contentType,String name) {
        String fileName;
        if(name!=null){
            fileName=name;
        }else{
            String fileExtension = getFileExtensionFromMimeType(contentType);
            fileName = "对话音频_" + System.currentTimeMillis() + "." + fileExtension;
        }

        Uri savedUri = null;
            File musicDir = new File(context.getFilesDir(), "Music");
            if (!musicDir.exists() && !musicDir.mkdirs()) {
                Log.w(TAG, "目录创建失败：" + musicDir.getAbsolutePath());
                return null;
            }
            File file = new File(musicDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(data);
                savedUri = FileProvider.getUriForFile(context,
                        context.getPackageName() + ".fileprovider",
                        file);
                Log.i(TAG, "本地保存成功");
            } catch (IOException | IllegalArgumentException e) {
                Log.e(TAG, "保存失败：" + e.getMessage());
            }
        return savedUri;
    }


    private boolean isAudioFile(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("audio/");
    }

    public void playAudio(Uri uri) {
        if (uri == null) {
            handleError("无效的文件");
            return;
        }
        if(isAudioFile(uri)){
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            try {
                // 如果 MediaPlayer 正在播放，先停止之前的播放
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();  // 重置播放器，准备重新播放
                    if(uri==nowPlayAudio){
                        return;
                    }
                } else {
                    mediaPlayer.reset(); // 默认进行重置，否则再次播放会出问题
                }
                // 设置音频文件的 Uri
                mediaPlayer.setDataSource(context, uri); // 注意：这里需要传入 Context
                mediaPlayer.prepare();  // 准备播放
                mediaPlayer.start();  // 开始播放
                nowPlayAudio=uri;
                Log.i(TAG, "开始播放");
            } catch (IOException e) {
                handleError("播放失败");
            }
        }
    }

    // 语音播放
    public void onAudioClick(MessageInfo message) {
        if (message != null && message.hasAudio()) {
            playAudio(message.getAudioFileUri());
            Log.i(TAG,"播放成功");
        }
    }

    // 复制文本
    public void onCopyClick(MessageInfo message) {
        if (message != null && !message.isUser()) {
            copyEvent.setValue(message.getContent());
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show();
            Log.i(TAG,"复制成功");
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

    // 辅助方法：从Content-Disposition中提取name参数
    private String extractNameFromContentDisposition(String contentDisposition) {
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("name=")) {
                return part.substring(5).replace("\"", "");
            }
        }
        return null;
    }

    private String extractFilenameFromContentDisposition(String contentDisposition) {
        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                String filename = part.substring("filename=".length());
                // 处理带引号的情况
                if (filename.startsWith("\"") && filename.endsWith("\"")) {
                    filename = filename.substring(1, filename.length() - 1);
                }
                return filename;
            }
        }
        return null;
    }


    // 辅助方法：处理错误
    private void handleError(String message) {
        Log.e(TAG, message);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
