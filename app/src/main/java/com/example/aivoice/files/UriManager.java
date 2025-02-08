package com.example.aivoice.files;

import android.net.Uri;

public class UriManager {
    private static Uri uri;

    public static void setUri(Uri newUri) {
        uri = newUri;
    }

    public static Uri getUri() {
        return uri;
    }
}

