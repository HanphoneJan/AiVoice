package com.example.aivoice.ui.files;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.IOException;

public class FilesViewModel extends ViewModel {
    private static final String TAG = "FilesViewModel";
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MediaPlayer mediaPlayer;
    private Context context;

    public FilesViewModel() {

    }
    public void setContext(Context context) {
        this.context = context;
    }
    private boolean isAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a");
    }
    public void playAudioFile(File file){

        if(!isAudioFile(file)){
            postError("文件不是音频文件");
            return;
        }
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        try {
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
        } catch (IOException e) {
//            Toast.makeText(this, "播放文件出错", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            postError("播放失败");
        }
    }

    public void pauseAudioFile() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.i(TAG,"暂停播放");
        }
    }

    public void jumpToFolder(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "无法打开文件管理器", Toast.LENGTH_SHORT).show();
        }
    }




    public void stopAudioFile() {
        pauseAudioFile();
        mediaPlayer.release();
        mediaPlayer = null;
        Log.i(TAG,"停止播放");
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
