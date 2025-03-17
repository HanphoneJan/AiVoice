package com.example.aivoice.message;

import android.net.Uri;

public class MessageInfo {
    private final long id = System.currentTimeMillis(); // 必须的唯一标识
    boolean isText;   // 文字消息标识
    boolean hasAudio; // 语音消息标识
    private final boolean isUser;
    private final String content;
    private final Uri audioFileUri;
    // 增加构造参数验证
    public MessageInfo(String content, Uri audioFileUri, boolean isUser) {
        if (isUser && (content == null) == (audioFileUri == null)) {
            throw new IllegalArgumentException("用户消息必须为文字或语音二选一");
        }

        this.content = content;
        this.audioFileUri = audioFileUri;
        this.isUser = isUser;

        // 自动推断消息类型
        this.isText = (content != null && !content.isEmpty());
        this.hasAudio = (audioFileUri != null);
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
    public boolean isText(){return isText;}
    public boolean hasAudio(){return  hasAudio;}
    public long getId(){return id;}
}