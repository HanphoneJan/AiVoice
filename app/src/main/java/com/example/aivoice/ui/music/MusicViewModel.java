package com.example.aivoice.ui.music;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aivoice.bluetooth.Bluetooth;

import java.util.HashSet;
import java.util.Set;

public class MusicViewModel extends ViewModel implements Bluetooth.BluetoothDataListener{

    private final MutableLiveData<Set<String>> audList = new MutableLiveData<>() ;
    private final MutableLiveData<String> nowPlayAudioFile = new MutableLiveData<>();
    private final Bluetooth bluetooth = new Bluetooth();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MusicViewModel() {
        bluetooth.setDataListener(this);
        //空
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
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }


    public boolean playAudio() {
        return bluetooth.sendSignal("audplay");
    }
    public boolean stopAudio() {
        return bluetooth.sendSignal("audstop");
    }

    public boolean showAudioList() {
        return bluetooth.sendSignal("audlist");
    }

    public boolean playNextTrack() {
        return bluetooth.sendSignal("audnext");
    }

    public boolean playPreviousTrack() {
        return bluetooth.sendSignal("audprev");
    }

    public boolean goBackDirectory() {
        return bluetooth.sendSignal("dirback");
    }

    public boolean displayTrackName() {
        return bluetooth.sendSignal("dispname");
    }

    public boolean togglePlaybackMode() {
        return bluetooth.sendSignal("modechg");
    }

    public boolean pauseResumeAudio() {
        return bluetooth.sendSignal("pausresu");
    }

    public boolean seekBackward() {
        return bluetooth.sendSignal("seekbwd");
    }

    public boolean seekForward() {
        return bluetooth.sendSignal("seekfwd");
    }

    public boolean decreaseVolume() {
        return bluetooth.sendSignal("voludec");
    }

    public boolean increaseVolume() {
        return bluetooth.sendSignal("voluinc");
    }
    // 实现 onDataReceived 回调
    @Override
    public void onDataReceived(String data) {
        if (data.startsWith("dispname ")) {
            nowPlayAudioFile.postValue(data.replace("dispname ", ""));
        } else if (data.startsWith("audlist ")) {
            // 解析音频列表
            String audios = data.replace("audlist ", "");
            Set<String> audioSet = new HashSet<>();
            // 假设返回的数据是用逗号分隔的音频文件名
            String[] audioArray = audios.split(",");
            for (String audio : audioArray) {
                audioSet.add(audio.trim()); // 去除空格并添加到Set
            }
            audList.postValue(audioSet); // 更新LiveData
        }
    }
}
