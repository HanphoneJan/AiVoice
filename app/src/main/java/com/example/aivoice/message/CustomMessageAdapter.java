package com.example.aivoice.message;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aivoice.R;
import com.example.aivoice.ui.home.HomeViewModel;
import java.util.ArrayList;
import java.util.List;

public class CustomMessageAdapter extends RecyclerView.Adapter<CustomMessageAdapter.ViewHolder> {

    private List<MessageInfo> messageInfoList = new ArrayList<>();
    private final HomeViewModel homeViewModel;
    private final int userMargin;
    private final int aiMargin;

    public CustomMessageAdapter(HomeViewModel homeViewModel, Context context) {
        this.homeViewModel = homeViewModel;
        // 预计算常用尺寸
        this.userMargin = dpToPx(context, 64);
        this.aiMargin = dpToPx(context, 16);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageInfo message = messageInfoList.get(position);
        applyMessageStyle(holder, message);
        setupContentVisibility(holder, message);
        setupAudioInteraction(holder, message);
    }

    @Override
    public int getItemCount() {
        return messageInfoList.size();
    }

    @Override
    public long getItemId(int position) {
        return messageInfoList.get(position).getId();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // 释放资源
        holder.ivAudio.setOnClickListener(null);
    }

    // 核心优化方法
    public void submitList(List<MessageInfo> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                new MessageDiffCallback(messageInfoList, newList));

        messageInfoList = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
    }

    // 样式应用封装
    private void applyMessageStyle(ViewHolder holder, MessageInfo message) {
        Context context = holder.itemView.getContext();
        boolean isUser = message.isUser();

        // 背景与文字颜色
        int bgRes = isUser ? R.drawable.user_message_bg : R.drawable.ai_message_bg;
        int textColor = ContextCompat.getColor(context,
                isUser ? R.color.user_text : R.color.ai_text);

        holder.tvMessage.setBackgroundResource(bgRes);
        holder.tvMessage.setTextColor(textColor);

        // 布局方向与边距
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();

        if (isUser) {
            params.leftMargin = userMargin;
            params.rightMargin = aiMargin;
            ((FrameLayout.LayoutParams) params).gravity = Gravity.END;
        } else {
            params.leftMargin = aiMargin;
            params.rightMargin = userMargin;
            ((FrameLayout.LayoutParams) params).gravity = Gravity.START;
        }
    }

    // 内容可见性控制
    private void setupContentVisibility(ViewHolder holder, MessageInfo message) {
        // 用户消息：文字或语音二选一
        if (message.isUser()) {
            boolean showText = message.isText();
            holder.tvMessage.setVisibility(showText ? View.VISIBLE : View.GONE);
            holder.ivAudio.setVisibility(showText ? View.GONE : View.VISIBLE);
        }
        // AI消息：始终显示文字+操作按钮
        else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivAudio.setVisibility(message.hasAudio() ? View.VISIBLE : View.GONE);
        }
    }

    // 音频交互处理
    private void setupAudioInteraction(ViewHolder holder, MessageInfo message) {
        if (message.getAudioFileUri() != null) {
            holder.ivAudio.setOnClickListener(v ->
                    homeViewModel.playAudio(message.getAudioFileUri()));
        }
    }

    // 尺寸转换工具
    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // ViewHolder 优化
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivAudio;
        ImageView ivCopy;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            ivAudio = itemView.findViewById(R.id.iv_play);
            ivCopy = itemView.findViewById(R.id.iv_copy);

            // 初始化时设置点击效果
            itemView.setBackgroundResource(R.drawable.selector_message_click);
        }
    }

    // DiffUtil 实现
    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<MessageInfo> oldList, newList;

        MessageDiffCallback(List<MessageInfo> oldList, List<MessageInfo> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId() == newList.get(newPos).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            MessageInfo oldItem = oldList.get(oldPos);
            MessageInfo newItem = newList.get(newPos);

            return oldItem.getContent().equals(newItem.getContent())
                    && oldItem.getAudioFileUri().equals(newItem.getAudioFileUri())
                    && oldItem.isUser() == newItem.isUser();
        }
    }
}