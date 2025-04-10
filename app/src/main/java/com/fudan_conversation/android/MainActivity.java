package com.fudan_conversation.android;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fudan_conversation.android.model.Message;
import com.fudan_conversation.android.utils.DialogueInfoAdapter;
import com.fudan_conversation.android.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build();

    private RecyclerView recyclerView; // 对话框滚动视图
    private DialogueInfoAdapter dialogueInfoAdapter; // 对话框布局适配器

    private Button keyboard_send; // 键盘输入发送按钮
    private EditText keyboard_edit; // 键盘输入框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化
        init();

        // 捕捉 UI
        keyboard_edit = findViewById(R.id.input); // 键盘输入框
        keyboard_send = findViewById(R.id.send); // 键盘输入发送按钮

        // 监听编辑文本时的动作（按下回车）
        keyboard_edit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendKeyboard(keyboard_edit); // 发送键盘输入信息
            }
            return false;
        });
        // 点击 键盘输入发送按钮
        keyboard_send.setOnClickListener(v -> {
            sendKeyboard(keyboard_edit); // 发送键盘输入信息
        });
    }

    private void init() {
        // 创建 LinearLayoutManager 实例
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL); // 设置 recyclerView 为竖向滚动
        // 滚动对话框
        recyclerView = findViewById(R.id.dialog);
        recyclerView.setLayoutManager(linearLayoutManager); // 将 linearLayoutManager 设置为 recyclerView 的布局管理器
        // 设置在软键盘弹出之后不会遮挡 RecyclerView 的内容
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> recyclerView.postDelayed(() -> recyclerView.scrollToPosition(dialogueInfoAdapter.getItemCount() - 1), 100));
        // 创建 RecyclerViewAdapter 实例
        dialogueInfoAdapter = new DialogueInfoAdapter();
        recyclerView.setAdapter(dialogueInfoAdapter);  // 将 recyclerViewAdapter 设置为 recyclerView 的适配器

        receive("复旦问答在线，博学笃志为您解答！");
    }

    /**
     * 接收信息
     *
     * @param content 接收的信息内容
     */
    private void receive(String content) {
        // 检查 content 是否为空
        if (content == null || content.isEmpty()) {
            LogUtil.warning(TAG, "receive", "Message content null or empty string", Boolean.TRUE);
            return;
        }

        // 接收消息
        dialogueInfoAdapter.addMessage(new Message(content, Message.TYPE_RECEIVED)); // 将新消息添加到消息列表
        recyclerView.smoothScrollToPosition(dialogueInfoAdapter.getItemCount() - 1); // 滚动到 RecyclerView 的最底部，显示最后一条消息
    }

    /**
     * 发送消息
     *
     * @param content 发送的信息内容
     */
    private void send(String content) {
        // 检查 content 是否为空
        if (content == null || content.isEmpty()) {
            LogUtil.warning(TAG, "send", "Message content null or empty string", Boolean.TRUE);
            return;
        }

        // 发送消息
        dialogueInfoAdapter.addMessage(new Message(content, Message.TYPE_SENT)); // 将新消息添加到消息列表
        recyclerView.scrollToPosition(dialogueInfoAdapter.getItemCount() - 1);// 滚动到 RecyclerView 的最底部，显示最后一条消息
    }

    /**
     * 发送键盘输入信息
     *
     * @param editText 文本编辑框
     */
    private void sendKeyboard(EditText editText) {
        // 发送消息
        String response = editText.getText().toString(); // 获取发送信息
        editText.setText(""); // 重置文本输入框的内容
        send(response);

        // 获取回复
        // 构建JSON请求
        JSONObject json = new JSONObject();
        try {
            json.put("query", response);
            LogUtil.debug(TAG, "sendKeyboard", json.toString(), Boolean.TRUE);
        } catch (JSONException e) {
            LogUtil.error(TAG, "sendKeyboard", "创建 query 失败", e);
            return;
        }

        // 发送请求
        keyboard_edit.setEnabled(Boolean.FALSE);
        keyboard_send.setEnabled(Boolean.FALSE);
        dialogueInfoAdapter.addMessage(new Message("", Message.TYPE_RECEIVED));
        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url("http://121.37.233.219:5000/api/chat").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                LogUtil.debug(TAG, "sendKeyboard", "请求成功！", Boolean.TRUE);

                try (ResponseBody body = response.body()) {
                    InputStream inputStream = null;
                    if (body != null) {
                        inputStream = body.byteStream();
                    }

                    // 使用BufferedReader按字符流读取
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder contentBuffer = new StringBuilder();

                    String line;
                    while (!(line = reader.readLine()).equals("data: [DONE]")) {
                        LogUtil.debug(TAG, "sendKeyboard", line, Boolean.TRUE);
                        if (line.startsWith("data: ")) {
                            String chunk = line.substring(6).trim();
                            contentBuffer.append(chunk);

                            // 实时更新 UI
                            runOnUiThread(() -> dialogueInfoAdapter.updateLastMessage(chunk));
                        } else if (line.isEmpty() && contentBuffer.length() > 0) {
                            // 处理事件结束（空行）
                            contentBuffer.setLength(0); // 清空缓冲区
                        }
                    }
                }

                runOnUiThread(() -> {
                    keyboard_edit.setEnabled(Boolean.TRUE);
                    keyboard_send.setEnabled(Boolean.TRUE);
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    dialogueInfoAdapter.updateLastMessage("网络请求失败，请重试");
                    recyclerView.smoothScrollToPosition(dialogueInfoAdapter.getItemCount() - 1);
                    keyboard_edit.setEnabled(Boolean.TRUE);
                    keyboard_send.setEnabled(Boolean.TRUE);
                });
            }
        });
    }

    /**
     * 点击软键盘和输入框的外部 收起软键盘
     *
     * @param ev 触摸事件对象
     * @return 布尔值，表示事件是否被处理
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 检查触摸事件的行动类型是否为按下
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View currentFocusedView = getCurrentFocus(); // 获取当前获得焦点的视图
            // 如果当前焦点的视图是 EditText
            if (currentFocusedView instanceof EditText) {
                Rect rect = new Rect(); // 创建一个 Rect 对象来获取 EditText 的全局可见区域
                currentFocusedView.getGlobalVisibleRect(rect); // 获取当前焦点视图的全局可见区域
                // 如果触摸位置不在 EditText 的可视范围内
                if (!rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    // 收起软键盘
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null)
                        inputMethodManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}