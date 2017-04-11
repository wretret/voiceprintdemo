package com.aispeech;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by yuruilong on 2017/3/28.
 */

public class Vocalprint {

    final static String TAG = "Vocalprint";

    static {
        try {
            Log.d(TAG, "before load aiengine library");
            Log.d(TAG, "library path = " + System.getProperty("java.library.path"));
            System.loadLibrary("voice");
            Log.d(TAG, "after load aiengine library");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Please check useful libaiengine.so, and put it in your libs dir!");
        }
    }


    private long engineId;

    public long init(String cfg, vocalprint_callback callback) {
        engineId = vprint_new(cfg, callback);
        Log.d(TAG, "Vocalprint.new():" + engineId + "\n" + cfg);
        return engineId;
    }

    public int start(String param) {
        int ret = 0;
        Log.d(TAG, "Vocalprint.start():" + engineId + "\n" + param);
        ret = vprint_start(engineId, param);
        return ret;
    }

    public int feed(byte[] buffer) {
//        Log.d(TAG, "AIEngine.feed():" + engineId);
        int opt = vprint_feed(engineId, buffer, buffer.length);
//        Log.d(TAG, "AIEngine.feed() end");
        return opt;
    }


    public int stop() {
        Log.d(TAG, "Vocalprint.stop():" + engineId);
        return vprint_stop(engineId);
    }

    public int cancel() {
        Log.d(TAG, "Vocalprint.cancel():" + engineId);
        return vprint_cancel(engineId);
    }

    public void destroy() {
        Log.d(TAG, "Vocalprint.delete():" + engineId);
        vprint_delete(engineId);
        Log.d(TAG, "AIEngine.delete() finished:" + engineId);
        engineId = 0;
    }

    public interface vocalprint_callback {

        /**
         * 回调方法
         *
         * @param id
         *            recordId
         * @param type
         *            json/binary
         * @param data
         *            data
         * @param size
         *            data size
         * @return
         */
        public abstract int run(byte[] id, int type, byte[] data, int size);
    }
    public static native long vprint_new(String cfg, vocalprint_callback callback);

    public static native int vprint_start(long engine, String param);

    public static native int vprint_feed(long engine, byte[] data, int size);

    public static native int vprint_stop(long engine);

    public static native int vprint_cancel(long engine);

    public static native void vprint_delete(long engine);

}
