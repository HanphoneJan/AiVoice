package com.example.aivoice.message;

import android.net.Uri;

public class MessageInfo {

    boolean isText;   // 文字消息标识
    boolean hasAudio; // 语音消息标识
    private boolean isUser;
    private final String content;
    private final Uri audioFileUri;
    public MessageInfo(String messageAnswer, Uri audioFileUri, boolean isUser) {
        this.content = messageAnswer;
        this.audioFileUri = audioFileUri;
        this.isUser=isUser;
    }

    // Getters
    public String getContent() {
        return content;
    }

    public Uri getAudioFileUri() {
        return audioFileUri;
    }
    public boolean isUser() {
        return isUser;
    }
}