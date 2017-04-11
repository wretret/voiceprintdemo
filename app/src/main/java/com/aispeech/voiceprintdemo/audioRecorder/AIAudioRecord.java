package com.aispeech.voiceprintdemo.audioRecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuruilong on 16-8-8.
 */
public class AIAudioRecord {

    public static String TAG = AIAudioRecord.class.getCanonicalName();
    public static  int INTERVAL = 100; // read buffer interval in ms.
    public static  int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static  int AUDIO_ENCODING_16BIT = AudioFormat.ENCODING_PCM_16BIT; // bits/16
    public static  int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO; // channel MONO单声道，STEREO立体声

    private static  int AUDIORECORD_RETRY_TIMES = 10;

    public static int audio_source = AUDIO_SOURCE;
    private static int audio_encoding = AUDIO_ENCODING_16BIT;
    public static int audio_channel = (AUDIO_CHANNEL == AudioFormat.CHANNEL_IN_STEREO) ? 2 : 1;

    private AISampleRate audioSampleRate;
    private boolean cancelByFront;
    private boolean stopped;

    private AIRecordListener aiRecordListener;
    private AudioRecord recorder;
    private int readBufferSize;

    /**
     * 工厂模式创建AIAudioRecord实例(默认使用16k音频采样率)
     *
     * @return AIAudioRecord实例
     * @see AISampleRate
     */
    public static AIAudioRecord create() {
        return create(null);
    }

    /**
     * 工厂模式创建AIAudioRecord实例
     *
     * @param sampleRate
     *            指定音频采样率，默认值 {@link AISampleRate#SAMPLE_RATE_16K}； 可选值
     *            {@link AISampleRate#SAMPLE_RATE_8K}；
     * @return AIAudioRecord实例
     * @see AISampleRate
     */
    public static AIAudioRecord create(AISampleRate sampleRate) {
        return create(0, sampleRate);
    }

    /**
     * 工厂模式创建AIAudioRecord实例
     *
     * @param readBufferSize
     *            指定录音读取缓冲区大小
     * @param sampleRate
     *            指定音频采样率，默认值 {@link AISampleRate#SAMPLE_RATE_16K}； 可选值
     *            {@link AISampleRate#SAMPLE_RATE_8K}；
     * @return AIAudioRecord实例
     * @see AISampleRate
     */
    public static AIAudioRecord create(int readBufferSize, AISampleRate sampleRate) {
        if (sampleRate == null) {
            sampleRate = AISampleRate.SAMPLE_RATE_16K;
        }


        if (readBufferSize <= 0) {
            readBufferSize = AudioRecordUtils.getReadBufferSize(sampleRate);
        }

        AudioRecord _ar = AudioRecordUtils.newInstance(sampleRate);
        if (_ar == null) {
            return null;
        }

        return new AIAudioRecord(readBufferSize, sampleRate, _ar);
    }

    /**
     * 使用指定读取录音数据的缓冲区大小来初始化录音机
     *
     * @param readBufferSize
     * @param sampleRate
     * @param recorder
     */
    private AIAudioRecord(int readBufferSize, AISampleRate sampleRate, AudioRecord recorder) {
        this.readBufferSize = readBufferSize;
        this.audioSampleRate = sampleRate;
        this.recorder = recorder;
    }

    /**
     * 设置录音时间监听接口
     *
     * @param listener
     *            {@link AIRecordListener}
     */
    public void setAIRecordListener(AIRecordListener listener) {
        this.aiRecordListener = listener;
    }

    /**
     * 设置读取录音数据的缓冲区大小，默认根据系统情况自动分配大小.
     *
     * @param readBufferSize
     *            读取录音数据的缓冲区大小
     */
    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    /**
     * 获取当前读取录音数据的缓冲区大小
     *
     * @return 当前录音数据读取缓冲区大小
     */
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * 获取当前音频采样率
     *
     * @return 音频采样率
     * @see AISampleRate
     */
    public AISampleRate getSampleRate() {
        return audioSampleRate;
    }

    /**
     * 获取录音机Channel
     *
     * @return channel值
     */
    public int getAudioChannel() {
        return audio_channel;
    }

    /**
     * 返回音频编码格式
     *
     * @return audioEncoding, 始终为2
     */
    public int getAudioEncoding() {
        return audio_encoding;
    }

    /**
     * 计算指定时长的音频大小
     *
     * @param sec
     *            音频时长，单位秒
     * @return 音频字节大小
     */
    public int calcAudioSize(int sec) {
        return audio_channel * audioSampleRate.getValue() * audio_encoding * sec;
    }

    /**
     * 返回当前录音机是否正在录音
     *
     * @return true isRecording, false otherwise.
     */
    public boolean isRecording() {
        if (recorder == null)
            return false;
        return recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
    }

    /**
     * 启动语音引擎，开始录音
     */
    public boolean start() {
        Log.i(TAG, "start AIAudioRecord");
        cancelByFront = false;
        stopped = false;
        int count = AUDIORECORD_RETRY_TIMES;
        try {
            // 尝试启动录音机，直到启动成功
            while (true) {
                recorder.startRecording();
                if (isRecording()) {
                    break;
                } else if (--count < 0) {
                    throw new Exception("AudioRecord start failed.");
                }
            }
            // TSStatistics.addTs("Record.start ... WorkThread");

            Thread t = new Thread() {
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    processRecord();
                }
            };
            if (aiRecordListener != null) {
                // TSStatistics.addTs("onRecordStarted callback before .... WorkThread");
                aiRecordListener.onRecordStarted();
                // TSStatistics.addTs("onRecordStarted callback after .... WorkThread");
            }
            // TSStatistics.addTs("AudioThread begine start .... WorkThread");
            t.start();
            // TSStatistics.addTs("AudioThread started ... WorkThread");
            return true;

        } catch (Exception ex) {
            Log.e(TAG, "catch exception when start AIAudioRecord. " + ex.getMessage());
            release();
            return false;
        }
    }

    /**
     * 停止录音
     */
    public void stop() {
        // TSStatistics.addTs("Stop Pressed");
        Log.i(TAG, "stop AIAudioRecord");
        stopped = true;
    }

    /**
     * 取消录音
     */
    public void cancel() {
        Log.i(TAG, "Cancel AIAudioRecord");
        cancelByFront = true;
    }

    /**
     * 停止录音，释放录音机资源
     */
    private void release() {

        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        Log.i(TAG, "Release AIAudioRecord, AudioRecord = null");
    }

    /**
     * loop read buffer
     */
    private void processRecord() {

        int useReadBufferSize = calcReadBufferSize();
        byte[] readBuffer = new byte[useReadBufferSize];
        int readSize = 0;
        // int count = 0;
        try {
            // TSStatistics.addTs("AudioThread Loop start ... AudioThread");
            for (;;) {
                // 录音机对象不存在、前台取消、停止录音等都会跳出循环
                if (recorder == null || cancelByFront || !isRecording()) {
                    break;
                }
                // TSStatistics.addTs("Recorder.read " + count +
                // " before ... AudioThread");
                readSize = recorder.read(readBuffer, 0, useReadBufferSize);
                // TSStatistics.addTs("Recorder.read " + count +
                // " after ... AudioThread");
                if (aiRecordListener != null && !cancelByFront && (readSize > 0)) {
//                    Log.d(TAG, "byte received && onBufferReceived callback in AIAudioRecord. [readSize= " + readSize
//                            + " ]");
                    // TSStatistics.addTs("onBufferReceived "
                    // + count +
                    // " callback before ... AudioThread");
                    aiRecordListener.onBufferReceived(readBuffer, readSize);
                    // TSStatistics.addTs("onBufferReceived "
                    // + count +
                    // " callback after ... AudioThread");
                    // count++;
                }
                if (stopped && isRecording()) {
                    // TSStatistics.addTs("onRecordStopped callback before ... AudioThread");
                    if (aiRecordListener != null) {
                        aiRecordListener.onRecordStopped();
                    }
                    // TSStatistics.addTs("onRecordStopped callback after ... AudioThread");
                    // TSStatistics.addTs("Record.stop before");
                    recorder.stop();
                    break;
                    // TSStatistics.addTs("Record.stop after");
                }
            }

            // Log.i(TAG, TSStatistics.outPutAll().toString() +
            // "\n all TS = " + ((float) count * 100) / 1000 +
            // "S\n");
        } catch (Exception e) {
            if (aiRecordListener != null) {
                aiRecordListener.onException(new RuntimeException("录音异常停止"));
            }
            e.printStackTrace();
        }
        // release recorder
        // TSStatistics.addTs("Record.release before");
        release();
        if (aiRecordListener != null) {
            aiRecordListener.onRecordReleased();
        }
        // TSStatistics.addTs("Record.release after");
        // Log.i(TAG, TSStatistics.outPutAll().toString());
    }

    /**
     * calculate readBufferSize.
     */
    private int calcReadBufferSize() {
        return readBufferSize > 0 ? readBufferSize : AudioRecordUtils.getReadBufferSize(audioSampleRate);
    }

    /**
     * Util to get record device.
     */
    private static class AudioRecordUtils {

        private static Map<AISampleRate, Integer> buffer_size_cache = new HashMap<AISampleRate, Integer>();
        private static Map<AISampleRate, Integer> read_buffer_size_cache = new HashMap<AISampleRate, Integer>();

        private static int calc_buffer_size(AISampleRate sampleRate) {
            int _sample_rate = sampleRate.getValue();
            int bufferSize = _sample_rate * audio_channel * audio_encoding;
            int minBufferSize = AudioRecord.getMinBufferSize(_sample_rate, audio_channel, audio_encoding);

            if (minBufferSize > bufferSize) {
                int inc_buffer_size = bufferSize * 4; // 4s
                // audio
                if (inc_buffer_size < minBufferSize)
                    bufferSize = minBufferSize * 2;
                else if (inc_buffer_size < 2 * minBufferSize)
                    bufferSize = inc_buffer_size * 2;
                else
                    bufferSize = inc_buffer_size;
            }

            // bufferSize = minBufferSize;
            buffer_size_cache.put(sampleRate, bufferSize);
            Log.d(TAG, "[MinBufferSize = " + minBufferSize + ", BufferSize = " + bufferSize + "]");
            // return bufferSize;
            return bufferSize;
        }

        public static int getReadBufferSize(AISampleRate sampleRate) {
            Integer read_buffer_size = read_buffer_size_cache.get(sampleRate);
            if (read_buffer_size == null) {
                int _sample_rate = sampleRate.getValue();
                read_buffer_size = _sample_rate * audio_channel * audio_encoding * INTERVAL / 1000;
                // read_buffer_size /= 4;
                read_buffer_size_cache.put(sampleRate, read_buffer_size);
                Log.d(TAG, "[SampleRate = " + _sample_rate + ", ReadBufferSize = " + read_buffer_size + "]");
            }
            return read_buffer_size;
        }

        /**
         * 根据默认配置初始化一个AudioRecord对象
         *
         * @return AudioRecord 成功初始化录音设备 null 如果无法获取录音设备
         */
        public static AudioRecord newInstance(AISampleRate sampleRate) {
            Integer buffer_size = buffer_size_cache.get(sampleRate);

            if (buffer_size == null) {
                buffer_size = calc_buffer_size(sampleRate);
            }

            AudioRecord audioRecord = new AudioRecord(audio_source, sampleRate.getValue(), AUDIO_CHANNEL,
                    audio_encoding, buffer_size);

            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                return audioRecord;
            }
            return null;
        }
    }

}
