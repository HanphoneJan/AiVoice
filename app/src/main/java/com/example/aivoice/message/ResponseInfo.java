package com.example.aivoice.message;

import android.net.Uri;

public class ResponseInfo {
    private final String messageAnswer;
    private final Uri audioFileUri;

    public ResponseInfo(String messageAnswer, Uri audioFileUri) {
        this.messageAnswer = messageAnswer;
        this.audioFileUri = audioFileUri;
    }

    // Getters
    public String getMessageAnswer() {
        return messageAnswer;
    }

    public Uri getAudioFileUri() {
        return audioFileUri;
    }
}