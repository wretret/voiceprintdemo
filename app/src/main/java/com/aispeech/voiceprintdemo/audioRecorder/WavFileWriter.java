package com.aispeech.voiceprintdemo.audioRecorder;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by yuruilong on 16-8-8.
 */
public class WavFileWriter {

    private static final String TAG = WavFileWriter.class.getCanonicalName();

    private RandomAccessFile wavFile = null;

    /**
     * 生成和打开pcm文件
     * @param file wav文件对象
     * @param aiRecord 录音机对象
     * @throws IOException
     */
    public synchronized void openWav(File file, AIAudioRecord aiRecord) throws IOException {
        closeWav();
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            if (parentDir.exists()) {
                if (parentDir.isFile()) {
                    parentDir.delete();
                    parentDir.mkdirs();
                }
            } else {
                parentDir.mkdirs();
            }
            wavFile = new RandomAccessFile(file, "rw");
            AISampleRate sampleRate = aiRecord.getSampleRate();
            int channelCount = aiRecord.getAudioChannel();
            int audioEncoding = aiRecord.getAudioEncoding();
//            writeWavHeader(sampleRate, channelCount, audioEncoding);
        }
    }


    private void writeWavHeader(AISampleRate sampleRate, int channelCount, int audioEncoding)
            throws IOException {
        Log.d(TAG, "writer header to Wav File.");
        int bytesPerSample = channelCount * audioEncoding;

	        /* RIFF header */
        wavFile.writeBytes("RIFF"); // riff id
        wavFile.writeInt(0); // riff chunk size *PLACEHOLDER*
        wavFile.writeBytes("WAVE"); // wave type
        wavFile.writeBytes("fmt "); // fmt id
        wavFile.writeInt(Integer.reverseBytes(0x10)); // 16 for PCM
        // format
        wavFile.writeShort(Short.reverseBytes((short) 1)); // 1 for
        // PCM
        // format
        wavFile.writeShort(Short.reverseBytes((short) (channelCount))); // number
        // of
        // channel
        wavFile.writeInt(Integer.reverseBytes(sampleRate.getValue())); // sampling
        // frequency
        wavFile.writeInt(Integer.reverseBytes(bytesPerSample * sampleRate.getValue())); // bytes
        // per
        // second
        wavFile.writeShort(Short.reverseBytes((short) (bytesPerSample))); // bytes
        // by
        // capture
        wavFile.writeShort(Short.reverseBytes((short) (audioEncoding * 8))); // bits
        // per
        // sample
	        /* data chunk */
        wavFile.writeBytes("data"); // data id
        wavFile.writeInt(0); // data chunk size *PLACEHOLDER*
    }


    /**
     * 向wav文件中写入音频数据
     * @param data 数据块
     */
    public void writeWavData(byte[] data) {
        // may run in thread
        if (wavFile != null) {
            try {
                wavFile.write(data, 0, data.length);
            } catch (Exception e) {
                Log.e(TAG, (e.getMessage() == null) ? "unknown exception in writeWavData" : e.getMessage());
                closeWav();
            }
        }
    }

    /**
     * 关闭wav文件
     */
    public synchronized void closeWav() {
        if (wavFile != null) {
//            try {
//                int fLength = (int) wavFile.length();
//                wavFile.seek(4); // riff chunk size
//                wavFile.writeInt(Integer.reverseBytes(fLength - 8));
//                wavFile.seek(40); // data chunk size
//                wavFile.writeInt(Integer.reverseBytes(fLength - 44));
//            } catch (Exception e) {
//                Log.e(TAG, (e.getMessage() == null) ? "unknown exception in closeWav" : e.getMessage());
//            } finally {
                try {
                    wavFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                wavFile = null;
//            }
        }
    }
}
