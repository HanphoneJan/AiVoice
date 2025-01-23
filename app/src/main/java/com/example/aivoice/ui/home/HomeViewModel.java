package com.example.aivoice.ui.home;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Uri> audioFileUri = new MutableLiveData<>();
    private final MutableLiveData<Uri> fileUri = new MutableLiveData<>();

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setAudioFileUri(Uri uri) {
        audioFileUri.setValue(uri);
    }

    public LiveData<Uri> getAudioFileUri() {
        return audioFileUri;
    }

    public void setFileUri(Uri uri) {
        fileUri.setValue(uri);
    }

    public LiveData<Uri> getFileUri() {
        return fileUri;
    }
}