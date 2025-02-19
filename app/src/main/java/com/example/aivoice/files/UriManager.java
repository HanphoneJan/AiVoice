package com.example.aivoice.files;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class UriManager {

    private static final String PREFS_NAME = "UriPreferences";
    private static final String URI_KEY = "savedUri";

    // 使用SharedPreferences来持久化存储Uri
    public static void setUri(Context context, Uri newUri) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (newUri != null) {
            editor.putString(URI_KEY, newUri.toString());  // 将Uri转化为字符串存储
        } else {
            editor.remove(URI_KEY);  // 如果传入null，删除存储的Uri
        }
        editor.apply();  // 异步提交
    }

    // 恢复存储的Uri
    public static Uri getUri(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = preferences.getString(URI_KEY, null);  // 获取保存的Uri字符串
        if (uriString != null) {
            return Uri.parse(uriString);  // 将字符串转化为Uri对象
        }
        return null;  // 如果没有存储过Uri，则返回null
    }
}
