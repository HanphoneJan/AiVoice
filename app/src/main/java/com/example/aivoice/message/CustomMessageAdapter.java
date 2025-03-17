package com.example.aivoice.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aivoice.R;
import com.example.aivoice.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class CustomMessageAdapter extends RecyclerView.Adapter<CustomMessageAdapter.ViewHolder>{
    private List<ResponseInfo> responseInfoList;
    private final HomeViewModel homeViewModel;
    public  CustomMessageAdapter(List<ResponseInfo> data,HomeViewModel homeViewModel) {
        this.responseInfoList = data;
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
        ResponseInfo responseInfo = responseInfoList.get(position);
        holder.tvMessage.setText(responseInfo.getMessageAnswer());

        // 音频文件处理
        if (responseInfo.getAudioFileUri() != null) {
            holder.ivAudio.setVisibility(View.VISIBLE);
            holder.ivAudio.setOnClickListener(v -> homeViewModel.playAudio(responseInfo.getAudioFileUri()));
        } else {
            holder.ivAudio.setVisibility(View.GONE);
        }
    }

    // 3. 数据量统计
    @Override
    public int getItemCount() {
        return responseInfoList.size();
    }

    // ViewHolder 定义（优化视图复用）
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivAudio;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            ivAudio = itemView.findViewById(R.id.iv_audio_icon);
        }
    }

    // 数据更新方法（关键！）
    public void submitList(List<ResponseInfo> newList) {
        responseInfoList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
}
