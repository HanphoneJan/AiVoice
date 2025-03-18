package com.example.aivoice.message;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aivoice.R;
import com.example.aivoice.databinding.ItemMessageBinding;
import com.example.aivoice.ui.home.HomeViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomMessageAdapter extends RecyclerView.Adapter<CustomMessageAdapter.ViewHolder> {

    private List<MessageInfo> messageInfoList = new ArrayList<>();
    private final HomeViewModel homeViewModel;
    private final int userMargin;  // 用户消息边距
    private final int aiMargin;    // AI消息边距

    // 通过布局文件处理
    public CustomMessageAdapter(HomeViewModel homeViewModel,Context context) {
        this.homeViewModel = homeViewModel;
        // 转换DP值为像素（建议值：用户消息右侧留白64dp，AI消息左侧留白16dp）
        this.userMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        this.aiMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMessageBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.item_message, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageInfo message = messageInfoList.get(position);
        holder.binding.setItem(message); // 绑定消息数据
        holder.binding.setViewModel(homeViewModel); // 绑定ViewModel
        holder.binding.executePendingBindings(); // 立即执行绑定

    }

    // ViewHolder重构
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageBinding binding;

        public ViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // 初始化点击监听
            binding.ivAudio.setOnClickListener(v ->
                    binding.getViewModel().onAudioClick(binding.getItem()));
            binding.ivCopy.setOnClickListener(v ->
                    binding.getViewModel().onCopyClick(binding.getItem()));
        }
    }


    // 布局方向处理（若无法通过XML实现）
    private void setupLayoutGravity(ViewHolder holder, boolean isUser) {
        // 获取父容器布局参数
        View rootView = holder.binding.getRoot();
        ViewGroup.LayoutParams params = rootView.getLayoutParams();

        // 适配 RecyclerView 的 LayoutParams
        if (params instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams rvParams = (RecyclerView.LayoutParams) params;
            // 通过边距实现对齐
            rvParams.setMargins(
                    isUser ? 0 : aiMargin,  // left
                    4,                               // top
                    isUser ? userMargin : 0,  // right
                    8                                // bottom
            );
            rootView.setLayoutParams(rvParams);
        }
    }

    @Override
    public int getItemCount() {
        return messageInfoList.size();
    }

    @Override
    public long getItemId(int position) {
        return messageInfoList.get(position).getId();
    }

    // 核心优化方法
    public void submitList(List<MessageInfo> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
                new MessageDiffCallback(messageInfoList, newList));

        messageInfoList = new ArrayList<>(newList);
        result.dispatchUpdatesTo(this);
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

            return Objects.equals(oldItem.getContent(), newItem.getContent())
                    && Objects.equals(oldItem.getAudioFileUri(), newItem.getAudioFileUri())
                    && oldItem.isUser() == newItem.isUser();
        }
    }


}