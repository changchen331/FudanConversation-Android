package com.fudan_conversation.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fudan_conversation.android.model.Message;
import com.fudan_conversation.android.permission.PermissionListener;
import com.fudan_conversation.android.permission.PermissionRequest;
import com.fudan_conversation.android.utils.DialogueInfoAdapter;
import com.fudan_conversation.android.utils.KeyboardStateMonitor;
import com.fudan_conversation.android.utils.KeyboardUtil;
import com.fudan_conversation.android.utils.LogUtil;
import com.fudan_conversation.android.utils.ToastUtil;
import com.fudan_conversation.android.utils.VoiceRecognitionUtil;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final PermissionRequest permissionRequest = new PermissionRequest(); // 权限申请
    private final VoiceRecognitionUtil voiceRecognitionUtil = new VoiceRecognitionUtil(this); // 语音识别工具类
    private final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
        String auth = Credentials.basic(BuildConfig.username, BuildConfig.password);
        Request request = chain.request().newBuilder().header("Authorization", auth).build();
        return chain.proceed(request);
    }).connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();

    private String asr_text = "";
    private Boolean is_asr_cancel = Boolean.FALSE;
    private Boolean is_asr_activated = Boolean.FALSE; // 语音识别是否激活
    private Boolean is_switch_activated = Boolean.FALSE; // 输入切换是否激活

    // 动态申请的权限
    protected String[] requestPermissionArray = new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    protected List<String> deniedPermissionList = new ArrayList<>();// 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限

    // UI
    private RecyclerView recyclerView; // 对话框滚动视图
    private DialogueInfoAdapter dialogueAdapter; // 对话框布局适配器

    private Button asr; // 语音输入
    private EditText keyboard_edit; // 键盘输入框
    private ImageButton switch_mod; // 输入切换
    private ImageButton switch_right; // 发送/取消按钮
    private ConstraintLayout keyboard_edit_layout;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化
        init(); // 对话框
        initPermissions(); // 权限
        initVoiceRecognition(); // 语音识别

        // 捕捉 UI
        asr = findViewById(R.id.asr); // 语音输入
        keyboard_edit = findViewById(R.id.input); // 键盘输入框
        switch_mod = findViewById(R.id.switch_mod); // 输入切换
        switch_right = findViewById(R.id.switch_right); // 发送/取消按钮
        keyboard_edit_layout = findViewById(R.id.input_layout);

        // 点击 语音输入
        asr.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // 语音识别激活
//                VibratorUtil.vibrate(this, 200); // 交互反馈
                    voiceRecognitionUtil.startListening(); // 开始识别
                    asr.setActivated(is_asr_activated = Boolean.TRUE);
                    asr.setText(R.string.asr_end);
                    switch_mod.setEnabled(Boolean.FALSE);
                    return true;
                case MotionEvent.ACTION_UP: // 语音识别休眠
                    v.performClick();
                    return true;
                default:
                    return false;
            }
        });
        asr.setOnClickListener(v -> {
            if (!is_asr_activated) {
                // 语音识别激活
//                VibratorUtil.vibrate(this, 200); // 交互反馈
                voiceRecognitionUtil.startListening(); // 开始识别
                asr.setActivated(is_asr_activated = Boolean.TRUE);
                asr.setText(R.string.asr_end);
//                switch_right.setVisibility(View.VISIBLE);
//                switch_mod.setVisibility(View.INVISIBLE);
            } else {
                // 语音识别休眠
//                VibratorUtil.vibrate(this, 200); // 交互反馈
                voiceRecognitionUtil.stopListening(); // 结束识别
                asr.setActivated(is_asr_activated = Boolean.FALSE);
                asr.setText(R.string.asr_start);
//                switch_right.setVisibility(View.INVISIBLE);
//                switch_mod.setVisibility(View.VISIBLE);
                switch_mod.setEnabled(Boolean.TRUE);
            }
        });

        // 监听编辑文本时的动作（按下回车）
        keyboard_edit.setOnEditorActionListener((v, actionId, event) -> {
            LogUtil.debug(TAG, "onCreate", String.valueOf(actionId), Boolean.TRUE);
            if (actionId == EditorInfo.IME_ACTION_SEND)
                chat(keyboard_edit.getText().toString()); // 发送键盘输入信息
            else if (actionId == EditorInfo.IME_ACTION_DONE)
                chat(keyboard_edit.getText().toString());
            return false;
        });

        // 点击 输入切换
        switch_mod.setOnClickListener(v -> {
            if (!is_switch_activated) {
                // 输入切换激活（语音->键盘）
                asr.setVisibility(View.INVISIBLE); // 隐藏语音输入
                keyboard_edit.setVisibility(View.VISIBLE); // 显示输入框
                switch_right.setActivated(Boolean.TRUE); // 发送按钮
                switch_right.setVisibility(View.VISIBLE); // 显示发送按钮

                keyboard_edit.requestFocus(); // 获取焦点
                KeyboardUtil.showSoftInput(keyboard_edit); // 弹出软键盘

                switch_mod.setScaleX(0.8f);
                switch_mod.setScaleY(0.8f);
                switch_mod.setActivated(is_switch_activated = Boolean.TRUE); // 输入切换激活
            } else {
                // 输入切换休眠（键盘->语音）
                keyboard_edit.setVisibility(View.INVISIBLE); // 隐藏输入框
                keyboard_edit.setText("");
                asr.setVisibility(View.VISIBLE); // 显示语音输入
                switch_right.setVisibility(View.INVISIBLE); // 隐藏发送按钮
                switch_right.setActivated(Boolean.FALSE); // 取消按钮
                switch_mod.setScaleX(1.0f);
                switch_mod.setScaleY(1.0f);
                switch_mod.setActivated(is_switch_activated = Boolean.FALSE); // 输入切换休眠
            }
        });

        // 点击 发送/取消按钮
        switch_right.setOnClickListener(v -> {
            if (!is_switch_activated) {
                // 语音（取消）
                is_asr_cancel = Boolean.TRUE;
                voiceRecognitionUtil.stopListening(); // 结束识别
                asr.setActivated(is_asr_activated = Boolean.FALSE); // 语音识别休眠
                asr.setText(R.string.asr_start);
                switch_right.setVisibility(View.INVISIBLE); // 隐藏取消按钮
                switch_mod.setVisibility(View.VISIBLE); // 显示输入切换
            } else {
                // 键盘（发送）
                keyboard_edit.clearFocus(); // 清除焦点
                KeyboardUtil.hideSoftInput(this); // 隐藏软键盘
                chat(keyboard_edit.getText().toString()); // 键盘（发送）
            }
        });

        // 唤醒软键盘 + 软键盘弹出之后不会遮挡 RecyclerView 的内容
        keyboard_edit_layout.setOnClickListener(v -> {
            if (is_switch_activated) {
                keyboard_edit.requestFocus(); // 获取焦点
                KeyboardUtil.showSoftInput(keyboard_edit); // 弹出软键盘
                recyclerView.postDelayed(() -> recyclerView.smoothScrollToPosition(dialogueAdapter.getItemCount() - 1), 250);
                keyboard_edit_layout.setVisibility(View.GONE);
            }
        });

        // 监听软键盘状态变化
        KeyboardStateMonitor keyboardStateMonitor = new KeyboardStateMonitor(findViewById(R.id.input_bar));
        keyboardStateMonitor.addSoftKeyboardStateListener(new KeyboardStateMonitor.SoftKeyboardStateListener() {
            @Override
            public void onSoftKeyboardOpened(int keyboardHeightInPx) {
                LogUtil.debug(TAG, "onSoftKeyboardOpened", String.valueOf(keyboardHeightInPx), Boolean.TRUE);
                keyboard_edit_layout.setVisibility(View.GONE);
            }

            @Override
            public void onSoftKeyboardClosed() {
                // 软键盘关闭
                keyboard_edit.clearFocus();
                keyboard_edit_layout.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * 初始化
     */
    private void init() {
        // 创建 LinearLayoutManager 实例
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL); // 设置 recyclerView 为竖向滚动
        // 滚动对话框
        recyclerView = findViewById(R.id.dialog);
        recyclerView.setLayoutManager(linearLayoutManager); // 将 linearLayoutManager 设置为 recyclerView 的布局管理器
        // 创建 RecyclerViewAdapter 实例
        dialogueAdapter = new DialogueInfoAdapter(recyclerView);
        recyclerView.setAdapter(dialogueAdapter);  // 将 recyclerViewAdapter 设置为 recyclerView 的适配器

        receive("复旦问答在线，博学笃志为您解答！"); // 起始语
    }

    /**
     * 初始化权限
     */
    private void initPermissions() {
        // Android 6.0 以上动态申请权限
        permissionRequest.requestRuntimePermission(this, requestPermissionArray, new PermissionListener() {
            @Override
            public void onGranted() {
                LogUtil.info(TAG, "initPermissions", "所有权限已被授予", Boolean.TRUE);
            }

            // 用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法
            @Override
            public void onDenied(List<String> deniedPermissions) {
                deniedPermissionList = deniedPermissions;
                for (String deniedPermission : deniedPermissions)
                    LogUtil.warning(TAG, "initPermissions", "被拒绝权限：" + deniedPermission, Boolean.TRUE);
            }
        });
    }

    /**
     * 初始化语音识别对象
     */
    private void initVoiceRecognition() {
        voiceRecognitionUtil.init(new RecognizerListener() {
            @Override
            public void onVolumeChanged(int volume, byte[] data) {
            }

            @Override
            public void onBeginOfSpeech() {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onResult(RecognizerResult results, boolean isLast) {
                if (results != null) asr_text += results.getResultString().replace("。", "");
                else {
                    ToastUtil.showShort(MainActivity.this, "语音识别失败");
                    LogUtil.warning(TAG, "initVoiceRecognition_onResult", "语音识别失败", Boolean.TRUE);
                }
                if (isLast) chat(asr_text);
            }

            @Override
            public void onError(SpeechError error) {
                LogUtil.warning(TAG, "initVoiceRecognition_onError", "您好像没有说话哦", Boolean.TRUE);
            }

            @Override
            public void onEvent(int eventType, int arg1, int arg2, Bundle extras) {
            }
        }, code -> {
            if (code == ErrorCode.SUCCESS)
                LogUtil.info(TAG, "initVoiceRecognition", "语音识别初始化成功", Boolean.TRUE);
            else
                LogUtil.warning(TAG, "initVoiceRecognition", "语音识别初始化失败，错误码：" + code, Boolean.TRUE);
        });
    }

    /**
     * 接收信息
     *
     * @param content 接收的信息内容
     */
    private void receive(String content) {
        // 检查 content 是否为空
        if (content == null) {
            LogUtil.warning(TAG, "receive", "Message content null or empty string", Boolean.TRUE);
            return;
        }

        // 接收消息
        dialogueAdapter.addMessage(new Message(content, Message.TYPE_RECEIVED)); // 将新消息添加到消息列表
        recyclerView.smoothScrollToPosition(dialogueAdapter.getItemCount() - 1); // 滚动到 RecyclerView 的最底部，显示最后一条消息
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
        dialogueAdapter.addMessage(new Message(content, Message.TYPE_SENT)); // 将新消息添加到消息列表
        recyclerView.smoothScrollToPosition(dialogueAdapter.getItemCount() - 1);// 滚动到 RecyclerView 的最底部，显示最后一条消息
    }

    /**
     * 与大模型进行交互
     */
    private void chat(String userInput) {
        if (userInput == null || userInput.isEmpty()) return;

        // 发送文本
        if (is_switch_activated) keyboard_edit.setText("");
        else asr_text = "";
        if (is_asr_cancel) {
            is_asr_cancel = Boolean.FALSE;
            return;
        }
        send(userInput);

        // 锁定输入栏状态
        receive("");
        asr.setEnabled(Boolean.FALSE);
        switch_right.setEnabled(Boolean.FALSE);

        // 加入上下文信息
        StringBuilder query = new StringBuilder("以下是我和AI的最近几轮对话，请参考它们继续回答我的问题。\n");
        int count = dialogueAdapter.getItemCount();
        for (int index = count - 6; index < count - 2; index++) {
            if (index < 0) continue;
            Message message = dialogueAdapter.getMessage(index);
            if (message.getType() == Message.TYPE_RECEIVED)
                query.append("AI:").append(message.getContent()).append("\n");
            else query.append("用户:").append(message.getContent()).append("\n");
        }
        query.append("这是我的新问题:").append(userInput);

        // 构建JSON请求
        JSONObject json = new JSONObject();
        try {
            json.put("query", query.toString());
            LogUtil.debug(TAG, "chat", json.toString(), Boolean.TRUE);
        } catch (JSONException e) {
            LogUtil.error(TAG, "chat", "创建 query 失败", e);
            return;
        }

        // 发送请求
        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url("http://121.37.233.219:80/api/chat").post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try (ResponseBody body = response.body()) {
                        InputStream inputStream = null;
                        if (body != null) inputStream = body.byteStream();

                        // 使用BufferedReader按字符流读取
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        StringBuilder contentBuffer = new StringBuilder();

                        String line;
                        while ((line = reader.readLine()) != null && !line.equals("data: [DONE]")) {
                            LogUtil.debug(TAG, "chat", line, Boolean.TRUE);
                            if (line.startsWith("data: ")) {
                                String chunk = line.substring(6).trim();
                                contentBuffer.append(chunk);
                                // 实时更新 UI
                                runOnUiThread(() -> dialogueAdapter.updateLastMessage(chunk));
                            } else if (line.isEmpty() && contentBuffer.length() > 0) {
                                // 处理事件结束（空行）
                                contentBuffer.setLength(0); // 清空缓冲区
                            }
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (!response.isSuccessful())
                        // 服务器返回 404 等
                        dialogueAdapter.updateLastMessage("服务器正忙，请稍后再试");
                    else if (dialogueAdapter.getMessage(dialogueAdapter.getItemCount() - 1).getContent().isEmpty())
                        // 服务器返回内容为空
                        dialogueAdapter.updateLastMessage("这个问题我不太清楚哦，要不要换个问题试试？");
                    recyclerView.smoothScrollToPosition(dialogueAdapter.getItemCount() - 1);
                    switch_right.setEnabled(Boolean.TRUE);
                    asr.setEnabled(Boolean.TRUE);
                });
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    dialogueAdapter.updateLastMessage("网络请求失败，请稍后再试");
                    recyclerView.smoothScrollToPosition(dialogueAdapter.getItemCount() - 1);
                    switch_right.setEnabled(Boolean.TRUE);
                    asr.setEnabled(Boolean.TRUE);
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
                    keyboard_edit.clearFocus(); // 清除焦点
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null)
                        inputMethodManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), 0);
                    keyboard_edit_layout.setVisibility(View.VISIBLE);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}