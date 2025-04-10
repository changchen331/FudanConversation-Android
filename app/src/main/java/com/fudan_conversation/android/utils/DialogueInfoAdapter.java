package com.fudan_conversation.android.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fudan_conversation.android.R;
import com.fudan_conversation.android.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于在 RecyclerView 中显示气泡对话的适配器
 * 负责将数据集合中的数据显示在 RecyclerView 上
 */
public class DialogueInfoAdapter extends RecyclerView.Adapter<DialogueInfoAdapter.ViewHolder> {
    private static final String TAG = "DialogueInfoAdapter";
    private static final long MIN_UPDATE_INTERVAL = 100; // 50ms
    private final List<Message> messages = new ArrayList<>(); // 存储对话信息列表

    private long lastUpdateTime = 0;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 通过 LayoutInflater 加载 item 布局
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.dialogue_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 获取当前位置的对话信息
        Message message = messages.get(position);
        holder.bind(message); // 消息绑定
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateLastMessage(String newContent) {
        Message lastMessage = messages.get(messages.size() - 1);
        if (System.currentTimeMillis() - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            lastMessage.setContent(lastMessage.getContent() + newContent);
            return;
        }
        lastUpdateTime = System.currentTimeMillis();

        if (!messages.isEmpty()) {
            if (lastMessage.getType() == Message.TYPE_RECEIVED) {
                lastMessage.setContent(lastMessage.getContent() + newContent);
                notifyItemChanged(messages.size() - 1);
            }
        }
    }

    /**
     * ViewHolder 内部类，用于缓存 item 视图中各个子视图的引用
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout dialogLeft; // 问题对话框
        TextView dialogLeftText; // 问题文本显示框
        ConstraintLayout dialogRight; // 回复对话框
        TextView dialogRightText; // 回复文本显示框

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dialogLeft = itemView.findViewById(R.id.dialog_left);
            dialogLeftText = itemView.findViewById(R.id.dialog_left_text);
            dialogRight = itemView.findViewById(R.id.dialog_right);
            dialogRightText = itemView.findViewById(R.id.dialog_right_text);
        }

        void bind(Message message) {
            // 根据消息类型显示对应的布局
            boolean isReceived = message.getType() == Message.TYPE_RECEIVED;
            dialogLeft.setVisibility(isReceived ? View.VISIBLE : View.GONE);
            dialogLeftText.setText(message.getContent());
            dialogRight.setVisibility(isReceived ? View.GONE : View.VISIBLE);
            dialogRightText.setText(message.getContent());
        }
    }
}
