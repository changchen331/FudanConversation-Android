package com.fudan_conversation.android.utils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fudan_conversation.android.R;
import com.fudan_conversation.android.model.Message;

import java.util.List;

/**
 * 用于在 RecyclerView 中显示气泡对话的适配器
 * 负责将数据集合中的数据显示在 RecyclerView 上
 */
public class DialogueInfoAdapter extends RecyclerView.Adapter<DialogueInfoAdapter.ViewHolder> {
    private static final String TAG = "DialogueInfoAdapter";
    private final List<Message> messages; // 存储对话信息列表

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    /**
     * ViewHolder 内部类，用于缓存 item 视图中各个子视图的引用
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout dialogLeft; // 问题对话框
        TextView dialogLeftText; // 问题文本显示框
        LinearLayout dialogRight; // 回复对话框
        TextView dialogRightText; // 回复文本显示框

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dialogLeft = itemView.findViewById(R.id.extra_dialog_left);
            dialogLeftText = itemView.findViewById(R.id.extra_dialog_left_text);
            dialogRight = itemView.findViewById(R.id.extra_dialog_right);
            dialogRightText = itemView.findViewById(R.id.extra_dialog_right_text);
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
