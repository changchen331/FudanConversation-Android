package com.fudan_conversation.android;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        // 初始化语音识别
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + BuildConfig.appId);

        super.onCreate();
    }
}
