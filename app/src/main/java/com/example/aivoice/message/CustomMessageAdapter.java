package com.example.aivoice.message;

import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aivoice.R;
import com.example.aivoice.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class CustomMessageAdapter extends RecyclerView.Adapter<CustomMessageAdapter.ViewHolder>{
    private List<MessageInfo> messageInfoList;
    private final HomeViewModel homeViewModel;
    public  CustomMessageAdapter(List<MessageInfo> data, HomeViewModel homeViewModel) {
        this.messageInfoList = data;
        this.homeViewModel = homeViewModel;
    }

    // 1. 创建视图模板
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false); // 消息项的布局文件
        return new ViewHolder(view);
    }

    // 2. 数据与视图绑定
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageInfo messageInfo = messageInfoList.get(position);
        holder.tvMessage.setText(messageInfo.getContent());
        // 设置消息内容
        holder.tvMessage.setText(messageInfo.getContent());

        // 根据消息类型设置布局方向
        setMessageDirection(holder.itemView, messageInfo.isUser());

        // 音频文件处理
        if (messageInfo.getAudioFileUri() != null) {
            holder.ivAudio.setVisibility(View.VISIBLE);
            holder.ivAudio.setOnClickListener(v -> homeViewModel.playAudio(messageInfo.getAudioFileUri()));
        } else {
            holder.ivAudio.setVisibility(View.GONE);
        }
    }

    private void setMessageDirection(View itemView, boolean isUser) {
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();

        // 根据消息类型设置边距和对齐方式
        if (isUser) {
            params.leftMargin = dpToPx(64);  // 右侧留出空间
            params.rightMargin = dpToPx(16);
            ((FrameLayout.LayoutParams) itemView.getLayoutParams()).gravity = Gravity.END;
        } else {
            params.leftMargin = dpToPx(16);
            params.rightMargin = dpToPx(64); // 左侧留出空间
            ((FrameLayout.LayoutParams) itemView.getLayoutParams()).gravity = Gravity.START;
        }
    }

    private void setMessageBackground(TextView textView, boolean isUser) {
        Context context = textView.getContext();
        if (isUser) {
            textView.setBackgroundResource(R.drawable.user_message_bg);
            textView.setTextColor(ContextCompat.getColor(context, R.color.user_text));
        } else {
            textView.setBackgroundResource(R.drawable.ai_message_bg);
            textView.setTextColor(ContextCompat.getColor(context, R.color.ai_text));
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    // 3. 数据量统计
    @Override
    public int getItemCount() {
        return messageInfoList.size();
    }

    // ViewHolder 定义（优化视图复用）
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivAudio;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);

        }
    }

    // 数据更新方法（关键！）
    public void submitList(List<MessageInfo> newList) {
        messageInfoList = new ArrayList<>(newList);
        notifyDataSetChanged();
//        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
//                new MessageDiffCallback(responseInfoList, newList));
//        responseInfoList.clear();
//        responseInfoList.addAll(newList);
//        result.dispatchUpdatesTo(this);
    }


}
