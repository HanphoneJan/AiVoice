package com.example.aivoice.ui.music;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.bluetooth.Bluetooth;

import java.util.HashSet;
import java.util.Set;

public class MusicViewModel extends ViewModel implements Bluetooth.BluetoothDataListener{
    private static final String TAG = "MusicViewModel";
    private final MutableLiveData<Set<String>> audList = new MutableLiveData<>() ;
    private final MutableLiveData<String> nowPlayAudioFile = new MutableLiveData<>();
    private final Bluetooth bluetooth = new Bluetooth();
    private final MutableLiveData<Boolean>  isPlay = new MutableLiveData<>();
    public MusicViewModel() {
        bluetooth.setDataListener(this);
    }
    public void setContext(Context context) {
        bluetooth.setContext(context);
    }
    public LiveData<String> getNowPlayAudioFile() {
        return nowPlayAudioFile;
    }

    public LiveData<Set<String>> getAudList() {
        return audList;
    }
    // 更新 audList 数据

    public LiveData<Boolean> getIsPlay(){
        return isPlay;
    }

    public void setDataListener(){
        bluetooth.setDataListener(this);
    }

    public void playAudio() {
        if(Boolean.TRUE.equals(isPlay.getValue())){
            isPlay.setValue(!bluetooth.sendSignal("pausresu"));
        }else{
            if(bluetooth.sendSignal("audplay")){
                isPlay.setValue(true);
            }
        }
        if(Boolean.TRUE.equals(isPlay.getValue())){
            displayTrackName();
        }
    }

    public void playAudStart(String selectedSong) {
        if(Boolean.TRUE.equals(isPlay.getValue())){
            isPlay.setValue(!bluetooth.sendSignal("pausresu"));
            isPlay.getValue();
            return;
        }
        isPlay.setValue(bluetooth.sendSignal("audstart "+selectedSong));
        if(Boolean.TRUE.equals(isPlay.getValue())){
            displayTrackName();
        }
    }

    public void showAudioList() {
        bluetooth.sendSignal("audlist");
    }

    public void playNextTrack() {
        if(bluetooth.sendSignal("audnext")){
            isPlay.setValue(true);
        }
    }

    public void playPreviousTrack() {
        if(bluetooth.sendSignal("audprev")){
            isPlay.setValue(true);
        }
    }

    public void displayTrackName() {
        bluetooth.sendSignal("dispname");
    }

    public boolean togglePlaybackMode() {
        return bluetooth.sendSignal("modechg");
    }


    public void seekBackward() {
        bluetooth.sendSignal("seekbwd");
    }

    public void seekForward() {
        bluetooth.sendSignal("seekfwd");
    }

    public void decreaseVolume() {
        bluetooth.sendSignal("voludec");
    }

    public void increaseVolume() {
        bluetooth.sendSignal("voluinc");
    }
    // 实现 onDataReceived 回调
    @Override
    public void onDataReceived(String data) {
        Log.i(TAG,data);
        if (data.startsWith("dispname")) {
            Log.i(TAG,"尝试歌曲名");
            // 更新当前播放的音频文件名
            nowPlayAudioFile.postValue(data.replace("dispname ", ""));
        } else if (data.startsWith("audlist")) {
            // 解析音频列表
            String audios = data.replace("audlist ", "").trim();
            if (!audios.isEmpty()) {
                Set<String> audioSet = new HashSet<>();
                // 假设返回的数据是用换行符分隔的音频文件名
                String[] audioArray = audios.split("\n");
                for (String audio : audioArray) {
                    if (!audio.trim().isEmpty()) {
                        audioSet.add(audio.trim()); // 去除空格并添加到Set
                    }
                }
                audList.postValue(audioSet); // 更新LiveData
            }
        }
    }

}
