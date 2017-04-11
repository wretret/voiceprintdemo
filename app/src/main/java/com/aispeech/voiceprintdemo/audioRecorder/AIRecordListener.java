package com.aispeech.voiceprintdemo.audioRecorder;

/**
 * Created by yuruilong on 16-8-8.
 */
public interface AIRecordListener {

    /**
     * 注册录音回调方法，在{@link AIAudioRecord}已经启动，但是尚未读取录音数据时调用;
     */
    void onRecordStarted();

    /**
     * 注册录音回调方法，在录音过程中，读取到部分音频数据时调用
     *
     * @param buffer
     * @param size
     */
    void onBufferReceived(final byte[] buffer, final int size);

    /**
     * 注册录音回调方法，在录音停止后调用
     */
    void onRecordStopped();

    /**
     * 注册录音回调方法，在录音机资源释放后调用
     */
    void onRecordReleased();

    /**
     * 注册录音回调方法，在异常发生时调用
     *
     * @param e Exception
     */
    void onException(Exception e);
}

