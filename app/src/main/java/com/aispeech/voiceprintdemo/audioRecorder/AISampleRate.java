package com.aispeech.voiceprintdemo.audioRecorder;

import android.util.Log;

/**
 * Created by yuruilong on 16-8-8.
 */
public class AISampleRate {

    private static final String TAG = AISampleRate.class.getName();

    public static final String KEY_SAMPLE_RATE = "sampleRate";

    /**
     * 16000采样率
     */
    public static final AISampleRate SAMPLE_RATE_16K = new AISampleRate(16000);

    /**
     * 8000采样率
     */
    public static final AISampleRate SAMPLE_RATE_8K = new AISampleRate(8000);

    /**
     * 将数值转换为采样率实例
     *
     * @param sampleRate
     * @return AISampleRate
     */
    public static AISampleRate toAISampleRate(int sampleRate) {
        if (sampleRate == SAMPLE_RATE_16K.getValue()) {
            return SAMPLE_RATE_16K;
        } else if (sampleRate == SAMPLE_RATE_8K.getValue()) {
            return SAMPLE_RATE_8K;
        } else {
            Log.w(TAG, "Unsupported sampleRate!");
            return null;
        }
    }

    int sampleRate;

    public AISampleRate(int rate) {
        this.sampleRate = rate;
    }

    /**
     * 获取Int类型的采样率值
     *
     * @return Int类型的采样率
     */
    public int getValue() {
        return sampleRate;
    }

}