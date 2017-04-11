package com.aispeech.voiceprintdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aispeech.R;
import com.aispeech.Vocalprint;
import com.aispeech.voiceprintdemo.audioRecorder.AIAudioRecord;
import com.aispeech.voiceprintdemo.audioRecorder.AIRecordListener;
import com.aispeech.voiceprintdemo.audioRecorder.AISampleRate;
import com.aispeech.voiceprintdemo.audioRecorder.WavFileWriter;
import com.aispeech.voiceprintdemo.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;


//typedef enum {
//        SpeechNoError = 0,
//        SpeechVprintCoreError = -10000,
//        SpeechVprintNoField, -9999
//        SpeechVprintNotJson,-9998
//        SpeechVprintModeError,-9997
//        SpeechVprintOperationNotSupport, -9996
//        SpeechVprintNotRegister, -9995
//        SpeechVprintStartTrainError, -9994
//        SpeechVprintStartTestError,-9993
//        SpeechVprintTrainExist,-9992
//        SpeechVprintTrainFull,-9991
//        SpeechVprintFeedTestError,-9990
//        SpeechVprintNoMem,-9989
//        SpeechVprintEndTestError,-9988
//        SpeechVprintTestResultError,-9987
//        SpeechVprintUnRegisterError,-9986
//        SpeechVprintOpenFileError,-9985
//        SpeechVprintReadFileError-9984
//        }SpeechErrorCode;
public class MainActivity extends Activity implements OnClickListener, Vocalprint.vocalprint_callback {
    String TAG = "MainActivity";
    String resourceDir = "/sdcard/voiceprint/";
    String TEST_RES = "spk_cfg_test.r";
    String TRAIN_RES = "spk_cfg_train.r";
    Button btn_register, btn_login, btn_register_stop, btn_login_stop, houmen;
    TextView result , note , vp_register, vp_login;
    View view1, view2, layout1, layout2;
    EditText editText, editText2 ;
    Toast mToast;
    Vocalprint mTrainEngine;
    Vocalprint mTestEngine;
    String mTrainConfiguration = "{\"native\":{\"resBinPath\":\"/sdcard/voiceprint/spk_cfg_train.r\",\"mode\":0}}";

    String mTestConfiguration = "{\n" +
            "        \"native\":{\n" +
            "            \"resBinPath\":\"/sdcard/voiceprint/spk_cfg_test.r\",\n" +
            "            \"mode\":1\n" +
            "        }\n" +
            "    }";

    ViewPager vp;
    List<View> viewList;

    AIAudioRecord mRecorder;

    AIRecordListenerImpl mAImpl = new AIRecordListenerImpl();

    WavFileWriter mWavFileWriter = new WavFileWriter();

    AISampleRate mAiSampleRate = new AISampleRate(16000);
    String mUserId;

    int mIndex;
    int mTotalCount = 6;

    private boolean mFlag = false; //false:not recording  ;  true:recording

    ExecutorService mPool;
    LinkedBlockingQueue mQueue = new LinkedBlockingQueue();;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater lf = getLayoutInflater().from(this);
        layout1 = lf.inflate(R.layout.layout1, null);
        layout2 = lf.inflate(R.layout.layout2, null);

        viewList = new ArrayList<View>();// 将要分页显示的View装入数组中
        viewList.add(layout1);
        viewList.add(layout2);


        houmen = (Button) this.findViewById(R.id.houmen);
        houmen.setOnClickListener(this);
        result = (TextView) this.findViewById(R.id.result);

        note = (TextView) this.findViewById(R.id.note);
        vp_register = (TextView) this.findViewById(R.id.vp_register);
        vp_login = (TextView) this.findViewById(R.id.vp_login);
        vp_register.setOnClickListener(this);
        vp_login.setOnClickListener(this);
        view1 = (View) this.findViewById(R.id.view1);
        view2 = (View) this.findViewById(R.id.view2);
        mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        vp = (ViewPager) this.findViewById(R.id.viewpager);
        vp.setAdapter(pagerAdapter);
        vp.setOnPageChangeListener(pageChangeListener);
        Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                prepareResource();
                initTrainEngine();
                subscriber.onCompleted();
            }
        }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError ");
                e.printStackTrace();
            }

            @Override
            public void onNext(String s) {
                Log.d(TAG, "onNext");
            }
        });
        mPool = Executors.newFixedThreadPool(1);
        mPool.execute(new ReadRunnbale());
    }

    byte[] buffer;
    /**
     * read buffer task
     */
    class ReadRunnbale implements Runnable {
        private long sessionId = 0;

        @Override
        public void run() {

            try {
                while((buffer = (byte[])mQueue.take()) != null) {
                    if(mMode == 0) {
                        mWavFileWriter.writeWavData(buffer);
                    } else {
                        Observable.create(new Observable.OnSubscribe<String>() {

                            @Override
                            public void call(Subscriber<? super String> subscriber) {
                                mWavFileWriter.writeWavData(buffer);
                                //feed aiengine
                                mTestEngine.feed(buffer);
                                subscriber.onCompleted();
                            }
                        }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d(TAG, "onError ");
                                e.printStackTrace();
                            }

                            @Override
                            public void onNext(String s) {
                                Log.d(TAG, "onNext");
                            }
                        });

                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareResource() {
        File file = new File(resourceDir);
        if(!file.exists()) {
            file.mkdirs();
        }
        Util.copyResource(MainActivity.this, TRAIN_RES, null);
        Util.copyResource(MainActivity.this, TEST_RES, null);
        Util.copyResource(MainActivity.this, "final.dubm.txt", null);
        Util.copyResource(MainActivity.this, "final.ie", null);
        Util.copyResource(MainActivity.this, "train_ivct.txt", null);
    }


    @Override
    protected void onDestroy() {
        destroyTestEngine();
        destroyTrainEngine();
        mPool.shutdown();
        super.onDestroy();
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }




    @Override
    public void onClick(View v) {
        result.setText("");
        switch(v.getId()) {
            case R.id.vp_login:
                vp.setCurrentItem(1);
                break;
            case R.id.vp_register:
                vp.setCurrentItem(0);
                break;
            default:
                break;
        }
    }


    @Override
    public void onBackPressed() {
        MainActivity.this.finish();
        super.onBackPressed();
    }

    private void startRecord() {
        mRecorder = AIAudioRecord.create(mAiSampleRate);
        if(mRecorder == null) {
            Toast.makeText(this, "请打开麦克风权限", Toast.LENGTH_LONG).show();
        }
        if(mRecorder != null) {
            mRecorder.setAIRecordListener(mAImpl);
            boolean started = mRecorder.start();
            if (!started) {
                Toast.makeText(this, "请打开麦克风权限", Toast.LENGTH_LONG).show();
            } else {
                mFlag = true;
            }
        }
    }

    private void stopRecord() {
        if(mRecorder != null) {
            mRecorder.stop();
            mFlag = false;
        }
    }


    PagerAdapter pagerAdapter = new PagerAdapter() {

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {

            return arg0 == arg1;
        }

        @Override
        public int getCount() {

            return viewList.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position,
                                Object object) {
            container.removeView(viewList.get(position));

        }

        @Override
        public int getItemPosition(Object object) {

            return super.getItemPosition(object);
        }

        @Override
        public CharSequence getPageTitle(int position) {

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = viewList.get(position);
            container.addView(view);
            if(position == 0) {//register
                btn_register = (Button) view.findViewById(R.id.register);
                btn_register.setOnClickListener(listener);
//                btn_register.setEnabled(false);
                btn_register_stop = (Button) view.findViewById(R.id.register_stop);
                btn_register_stop.setOnClickListener(listener);
//                btn_register_stop.setEnabled(false);
                editText = (EditText) view.findViewById(R.id.et);
            } else if(position == 1){//login
                btn_login = (Button) view.findViewById(R.id.login);
                btn_login_stop = (Button) view.findViewById(R.id.login_stop);
                btn_login.setOnClickListener(listener);
//                btn_login.setEnabled(false);
                btn_login_stop.setOnClickListener(listener);
//                btn_login_stop.setEnabled(false);
                editText2 =  (EditText) view.findViewById(R.id.et2);
            }
            return viewList.get(position);
        }

    };

    OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            result.setText("");
            if(v == btn_register) {
                mUserId = editText.getText().toString();
                if(TextUtils.isEmpty(mUserId)) {

                    showTip("请输入用户名");
                } else {
                    if(!mFlag) {
                        startRecord();
                    } else {
                        showTip("当前正在录音，请先结束录音");
                    }
                }
            } else if(v == btn_register_stop) {
                if(mFlag) {
                    stopRecord();
                } else {
                    showTip("当前不在录音");
                }
            } else if(v == btn_login) {
                if(!mFlag) {
                    startRecord();
                } else {
                    showTip("当前正在录音，请先结束录音");
                }
            } else if(v == btn_login_stop) {
                if(mFlag) {
                    stopRecord();
                } else {
                    showTip("当前不在录音");
                }
            }
        }
    };

    int mMode = 0;

    OnPageChangeListener pageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int pos) {
            if(pos == 0) {
                mMode = 0;
                note.setText("登录时 ，开始录音--->口令---->结束录音");
                view1.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.orange));
                view2.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray));
                if(mFlag) {
                    stopRecord();
                }
                Observable.create(new Observable.OnSubscribe<String>() {

                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        destroyTestEngine();
                        initTrainEngine();
                        subscriber.onCompleted();
                    }
                }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext");
                    }
                });
            } else if(pos == 1) {
                note.setText("注册时需要录音5次，开始录音--->口令---->结束录音，反复5次");
                view2.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.orange));
                view1.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray));
                mMode = 1;
                if(mFlag) {
                    stopRecord();
                }
                btn_login.setEnabled(false);
                btn_register_stop.setEnabled(false);
                Observable.create(new Observable.OnSubscribe<String>() {

                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        destroyTrainEngine();
                        initTestEngine();
                        subscriber.onCompleted();
                    }
                }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted");
                        btn_login.setEnabled(true);
                        btn_register_stop.setEnabled(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext");
                    }
                });
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    @Override
    public int run(byte[] id, int dataType, byte[] retData, int size) {
        String recordId = new String(id).trim();
        byte[] data = new byte[size];
        System.arraycopy(retData, 0, data, 0, size);
        String tag = "";
        if (dataType == 1) { //json
            String retString = Util.newUTF8String(data);
            Log.d(TAG, "callback:" + retString);
            if(retString.contains("name")) {
                JSONObject jo = null;
                try {
                    jo = new JSONObject(retString);
                    if(jo.has("result")) {
                        showResult(jo.getString("result"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                showResult(retString);
                return 0;
            }
            if(retString.contains("error") && !retString.contains("user exist")) {
                showResult(retString);
            }
        }

        return 0;
    }

    class AIRecordListenerImpl implements AIRecordListener {

        @Override
        public void onRecordStarted() {
            Log.e(TAG, "onRecordStarted");
            if(mMode == 0) {
                try {
                    final String wavPath = "/sdcard/voiceprint/" + mUserId + "/"+ mIndex + ".pcm";
                    mWavFileWriter.openWav(new File(wavPath), mRecorder);
                    showResult("正在录音........录完请按结束录音");
                } catch (IOException e) {
                    mWavFileWriter.closeWav();
                    e.printStackTrace();
                }
            } else {
                //start aiengine
                showResult("正在录音........录完请按结束录音");
                Observable.create(new Observable.OnSubscribe<String>() {

                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        try {
                            final String wavPath = "/sdcard/voiceprint/" + System.currentTimeMillis() + ".pcm";
                            mWavFileWriter.openWav(new File(wavPath), mRecorder);
                        } catch (IOException e) {
                            mWavFileWriter.closeWav();
                            e.printStackTrace();
                        }
                        startActionCheck();
                        subscriber.onCompleted();
                    }
                }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
//                        Log.d(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext");
                    }
                });
            }
        }

        @Override
        public void onBufferReceived(final byte[] buffer, final int size) {
            final byte[] bytes = new byte[size];
            System.arraycopy(buffer, 0, bytes, 0, size);
            mQueue.add(bytes);
        }

        @Override
        public void onRecordStopped() {
            mWavFileWriter.closeWav();
            if(mMode == 0) {
                if (mIndex < mTotalCount-1) {
                    showResult("第" + (mIndex + 1) + "次成功，请继续按开始录音然后继续说口令然后点击结束录音");
                }
                if (mIndex == mTotalCount - 1) {
                    //start aiengine
                    showResult("声纹密码生成中，请耐心等待...");
                    startTrain();
                }
                mIndex = (mIndex + 1) % mTotalCount;
            } else {
                //stop aiengine
                showResult("正在分析你的声音，请耐心等待...");
                Observable.create(new Observable.OnSubscribe<Integer>() {

                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        int ret =  mTestEngine.stop();
                        Log.d(TAG, "check stop ret : " + ret);
                        subscriber.onNext(ret);
                    }
                }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
//                        Log.d(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer ret) {
                        if(ret != 0) {
                            showResult("内部error : " + ret);
                        }
                    }
                });

            }
        }

        @Override
        public void onRecordReleased() {

        }

        @Override
        public void onException(Exception e) {
            mWavFileWriter.closeWav();
        }
    }

    private void cancelRegistration() {
        mIndex = 0;
    }

    private void startTrain() {
        Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> subscriber) {
                int ret = startActionNew();
                if(ret == 0) {
                    ret = startActionTrain();
                    if(ret == 0) subscriber.onNext("模型训练成功");
                } else if (ret == -9992) {
                    ret = startActionDelete();
                    ret = startActionNew();
                    ret = startActionTrain();
                    if(ret == 0) subscriber.onNext("模型训练成功");
                }
            }
        }).subscribeOn(AndroidSchedulers.from(getSdkLooper())).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "onCompleted1");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError1 ");
                e.printStackTrace();
            }

            @Override
            public void onNext(String s) {
                showResult(s);
            }
        });
    }

    private int startActionNew() {
        String parameter = String.format("{\"request\":{\"action\":\"new\",\"name\":\"%s\",\"num\":6}}", editText.getText().toString().trim());
        Log.d(TAG, "action new param : " + parameter);
        int ret = mTrainEngine.start(parameter);
        Log.d(TAG, "action new ret is " + ret);
        return ret;
    }

    private int startActionTrain() {
        String  parameter = "{\n" +
                "        \"request\":{\n" +
                "            \"action\":\"train\",\n" +
                "            \"list\":[\"/sdcard/voiceprint/%s/0.pcm\", \"/sdcard/voiceprint/%s/1.pcm\", \"/sdcard/voiceprint/%s/2.pcm\", \"/sdcard/voiceprint/%s/3.pcm\", \"/sdcard/voiceprint/%s/4.pcm\", \"/sdcard/voiceprint/%s/5.pcm\"]\n" +
                "        }\n" +
                "    }";
        parameter = String.format(parameter, mUserId, mUserId, mUserId, mUserId, mUserId, mUserId);
        Log.d(TAG, "action train param : " + parameter);
        int ret =  mTrainEngine.start(parameter);
        Log.d(TAG, "action train ret is " + ret);
        return ret;
    }

    private int startActionDelete() {
        String  parameter = String.format("{\n" +
                "        \"request\":{\n" +
                "            \"action\":\"delete\",\n" +
                "            \"name\":\"%s\"\n" +
                "        }\n" +
                "    }", editText.getText().toString().trim());

        Log.d(TAG, "action delete param : " + parameter);
        int ret =  mTrainEngine.start(parameter);
        Log.d(TAG, "action delete ret is " + ret);
        return ret;
    }

    private int startActionCheck() {
        String  parameter = String.format("{\n" +
                "        \"request\":{\n" +
                "            \"action\":\"check\"\n" +
                "        }\n" +
                "    }");

        Log.d(TAG, "action delete param : " + parameter);
        int ret =  mTestEngine.start(parameter);
        Log.d(TAG, "action check ret is " + ret);
        return ret;
    }

    private long initTrainEngine() {
        Log.d(TAG, "initTrainEngine");
        long ret = 0;
        destroyTrainEngine();
        if(mTrainEngine == null) {
            mTrainEngine = new Vocalprint();
            ret = mTrainEngine.init(mTrainConfiguration, MainActivity.this);
            Log.d(TAG, mTrainConfiguration);
        }
        return ret;
    }

    private long initTestEngine() {
        Log.d(TAG, "initTestEngine");
        long ret = 0;
        destroyTestEngine();;
        if(mTestEngine == null) {
            mTestEngine = new Vocalprint();
            ret = mTestEngine.init(mTestConfiguration, MainActivity.this);
            Log.d(TAG, mTestConfiguration);
        }
        return ret;
    }

    private void destroyTrainEngine() {
        if(mTrainEngine != null) {
            mTrainEngine.destroy();
            mTrainEngine = null;
        }
    }

    private void destroyTestEngine() {
        if(mTestEngine != null) {
            mTestEngine.destroy();
            mTestEngine = null;
        }
    }



    private void showResult(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.setText(str);
            }
        });
    }

    private Looper mLooper;
    private Looper getSdkLooper() {
        if(mLooper == null) {
            HandlerThread thread = new HandlerThread("sdk-thread");
            thread.start();
            mLooper = thread.getLooper();
        }
        return mLooper;
    }

}
