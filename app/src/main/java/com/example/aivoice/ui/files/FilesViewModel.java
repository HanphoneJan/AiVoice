package com.example.aivoice.ui.files;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.aivoice.files.UriManager;

import java.io.File;
import java.io.IOException;

public class FilesViewModel extends ViewModel {
    private static final String TAG = "FilesViewModel";
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MediaPlayer mediaPlayer;
    private ExoPlayer exoPlayer;
    private Context context;
    private static Uri fileUri = null;
    private static String selectedFileName; //当前播放的文件
    private Boolean isVideoPlaying = false;
    public FilesViewModel() {
    }
    public void setContext(Context context) {
        this.context = context;
        exoPlayer = new ExoPlayer.Builder(context).build();
    }
    public Boolean getIsVideoPlaying() {
        return isVideoPlaying;
    }
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
    private boolean isAudioFile(File file) {
        String mimeType = getMimeType(file.getAbsolutePath());
        return mimeType != null && mimeType.startsWith("audio/");
    }

    private boolean isAudioFile(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("audio/");
    }

    private boolean isVideoFile(File file) {
        String mimeType = getMimeType(file.getAbsolutePath());
        return mimeType != null && mimeType.startsWith("video/");
    }
    private boolean isVideoFile(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
    }
    // 获取文件的 MIME 类型
    public String getMimeType(String filePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public String getMimeTypeByUri(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        return mimeType;
    }

    public boolean playFile(File file){
        if(isAudioFile(file)){
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                }
                // 如果 MediaPlayer 正在播放，先停止之前的播放
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();  // 重置播放器，准备重新播放
                }
                else{mediaPlayer.reset(); } //默认进行重置，否则再次播放会出问题
                // 设置音频文件的路径
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.prepare();  // 准备播放
                mediaPlayer.start();  // 开始播放
                Log.i(TAG,"开始播放");
                return true;
            } catch (IOException e) {
                postError("播放失败");
                return false;
            }
        }else if(isVideoFile(file)){
            // 使用 ExoPlayer 播放视频
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(context).build();
            }
            try {
                // 创建 MediaItem
                MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(file));
                exoPlayer.setMediaItem(mediaItem);
                isVideoPlaying=true;
                exoPlayer.prepare();
                exoPlayer.play(); // 开始播放
                Log.i(TAG, "开始播放视频（ExoPlayer）");
                return true;
            } catch (Exception e) {
                postError("视频播放失败");
                return false;
            }
        }
        return false;
    }

    public boolean playFile(Uri uri) {
        if (uri == null) {
            postError("无效的文件");
            return false;
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
                } else {
                    mediaPlayer.reset(); // 默认进行重置，否则再次播放会出问题
                }
                // 设置音频文件的 Uri
                mediaPlayer.setDataSource(context, uri); // 注意：这里需要传入 Context
                mediaPlayer.prepare();  // 准备播放
                mediaPlayer.start();  // 开始播放
                Log.i(TAG, "开始播放");
                return true;
            } catch (IOException e) {
                postError("播放失败");
                e.printStackTrace();
            }
        }else if(isVideoFile(uri)){
            // 使用 ExoPlayer 播放视频
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(context).build();
            }
            MediaItem mediaItem = MediaItem.fromUri(uri);
            exoPlayer.setMediaItem(mediaItem);
            isVideoPlaying=true;
            exoPlayer.prepare();
            exoPlayer.play();
            Log.i(TAG, "开始播放视频（ExoPlayer）");
            return true;
        }
        return false;
    }
    // 获取文件的 MIME 类型

    public void stopAudioFile() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            selectedFileName = null;
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
            selectedFileName = null;
        }
        Log.i(TAG,"已停止播放");
    }

    public Uri getFileByUri(String fileName){
        if(fileName.equals(selectedFileName) && mediaPlayer != null && mediaPlayer.isPlaying()){
            stopAudioFile();
            return null;
        }
        fileUri = UriManager.getUri(context);
        Uri audioUri = null;
        if (fileUri != null) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, fileUri);
            if (pickedDir != null && pickedDir.isDirectory()) {
                // 遍历目录，查找与 fileName 匹配的文件
                for (DocumentFile file : pickedDir.listFiles()) {
                    if (file.isFile() && fileName.equals(file.getName())) {
                        // 找到匹配的文件，获取其 URI
                        audioUri = file.getUri();
                        selectedFileName = fileName;
                        return audioUri;
                    }
                }
            }
        }
        return null;
    }

    public File getFile(String fileName){
        if(fileName.equals(selectedFileName) && mediaPlayer != null && mediaPlayer.isPlaying()){
            stopAudioFile();
            return null;
        }
        selectedFileName = fileName;
        return new File(context.getFilesDir(), "Music/"+selectedFileName);
    }

    private void postError(String message) {
        Log.e(TAG, message);
        errorMessage.postValue(message);

    }
    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            mediaPlayer.release();
        } catch (Exception e) {
            Log.e(TAG, "Error during ViewModel cleanup: " + e.getMessage(), e);
        }
    }
}
