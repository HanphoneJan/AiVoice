package com.example.aivoice.message;

import android.view.View;
import android.widget.FrameLayout;

import androidx.databinding.BindingAdapter;

public class CustomBindingAdapter {
    @BindingAdapter("android:layout_gravity")
    public static void bindLayoutGravity(View view, int gravity) {
        if (view.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            params.gravity = gravity;
            view.setLayoutParams(params);
        }
    }
}
