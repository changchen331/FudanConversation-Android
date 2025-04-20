package com.fudan_conversation.android.utils;

import android.content.Context;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;

import java.util.Locale;

/**
 * 语音识别工具类
 * 提供语音识别功能的初始化、开始监听、停止监听等功能
 */
public class VoiceRecognitionUtil {
    private static final String TAG = "VoiceRecognitionUtil";
    private final Context context;

    private InitListener initListener; // 初始化监听器，用于接收初始化结果
    private SpeechRecognizer speechRecognizer; // 语音识别对象
    private RecognizerListener recognizerListener; // 识别结果监听器，用于接收识别结果

    public VoiceRecognitionUtil(Context context) {
        this.context = context;
    }

    /**
     * 初始化语音识别对象
     *
     * @param recognizerListener 识别结果监听器，用于接收识别过程中的事件和结果
     * @param initListener       初始化监听器，用于接收初始化完成事件
     */
    public void init(RecognizerListener recognizerListener, InitListener initListener) {
        this.recognizerListener = recognizerListener;
        this.initListener = initListener;
        initVoiceRecognizer();
    }

    /**
     * 初始化语音识别对象，并设置相关参数
     */
    private void initVoiceRecognizer() {
        // 获取系统默认的语言和地区设置
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage() + "-" + locale.getCountry();
        LogUtil.info(TAG, "initVoiceRecognizer", "系统默认语言:" + language, Boolean.TRUE);

        // 创建语音识别对象
        speechRecognizer = SpeechRecognizer.createRecognizer(context, initListener);
        if (speechRecognizer != null) {
            // 设置语音识别语言
//            if ("zh-CN".equalsIgnoreCase(language))
//                speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
//            else speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "en_us");
            speechRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn"); // 直接设置语音输入语言为中文
            speechRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin"); // 设置结果返回语言

            // 用户多长时间未开始说话则当做超时处理（取值范围{1000～10000}，默认值5000ms）
            speechRecognizer.setParameter(SpeechConstant.VAD_BOS, "10000"); // 设置语音开始检测的静音时长（毫秒）
            // 用户停止说话多长时间内即认为不再输入（取值范围{1000～10000}，默认值5000ms）
            speechRecognizer.setParameter(SpeechConstant.VAD_EOS, "10000"); // 设置语音结束检测的静音时长（毫秒）

            // 结果类型包括：xml, json, plain。xml和json即对应的结构化文本结构，plain即自然语言的文本
            speechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "plain"); // 设置识别结果的类型为纯文本

            // 通过设置此参数可偏向输出数字结果格式（0：倾向于汉字，1：倾向于数字）
            speechRecognizer.setParameter("nunum", "1");

            // （仅中文支持）标点符号添加（1：开启（默认值）0：关闭）
            speechRecognizer.setParameter(SpeechConstant.ASR_PTT, "1");

            LogUtil.info(TAG, "initVoiceRecognizer", "语音识别对象完成初始化", Boolean.TRUE);
        } else LogUtil.info(TAG, "initVoiceRecognizer", "语音识别对象为空", Boolean.TRUE);
    }

    /**
     * 开始监听语音输入
     */
    public void startListening() {
        // 开始监听语音输入，并设置识别结果监听器
        if (speechRecognizer != null) speechRecognizer.startListening(recognizerListener);
    }

    /**
     * 停止监听语音输入
     */
    public void stopListening() {
        if (speechRecognizer != null) speechRecognizer.stopListening(); // 停止监听语音输入
    }
}
