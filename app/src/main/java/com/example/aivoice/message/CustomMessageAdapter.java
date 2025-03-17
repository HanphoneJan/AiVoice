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


    // 通过布局文件处理
    public CustomMessageAdapter(HomeViewModel homeViewModel) {
        this.homeViewModel = homeViewModel;
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

        // 处理动态边距（建议通过ConstraintLayout约束条件替代）
//        setupLayoutGravity(holder, message.isUser());
    }

    // ViewHolder重构
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageBinding binding;

        public ViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // 初始化点击监听（建议通过DataBinding处理）
            binding.ivAudio.setOnClickListener(v ->
                    binding.getViewModel().playAudio(binding.getItem().getAudioFileUri()));
        }
    }

    // 布局方向处理（若无法通过XML实现）
    private void setupLayoutGravity(ViewHolder holder, boolean isUser) {
        // 获取正确的 LayoutParams 类型
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) holder.binding.getRoot().getLayoutParams();

        // 设置边距实现对齐效果
//        params.leftMargin = isUser ? userMargin : aiMargin;
//        params.rightMargin = isUser ? aiMargin : userMargin;

        holder.binding.getRoot().setLayoutParams(params);
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