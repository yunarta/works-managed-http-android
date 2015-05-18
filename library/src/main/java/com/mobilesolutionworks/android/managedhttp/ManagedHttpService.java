package com.mobilesolutionworks.android.managedhttp;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yunarta on 17/5/15.
 */
public abstract class ManagedHttpService extends Service {

    Looper mServiceLooper;

    ServiceHandler mServiceHandler;

    Handler mHandler;

    Map<String, CancelToken> mQueues;

    ManagedHttpConfiguration mConfiguration;

    boolean mRedelivery;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        mQueues = new HashMap<String, CancelToken>();

        mConfiguration = ManagedHttpConfiguration.configure(this);

        HandlerThread thread = new HandlerThread("managed-http");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            refreshData(intent);
        }
    }

    protected void refreshData(Intent intent) {
        String local = intent.getStringExtra("local");
        String remote = intent.getStringExtra("remote");

        Log.d("yunarta", "ManagedHttpService.attempt to query url = " + local);

        if (mQueues.containsKey(local)) {
            return;
        }

        String _method = intent.getStringExtra("method");
        Bundle params = intent.getBundleExtra("params");

        int cache = intent.getIntExtra("cache", 0);
        if (cache == 0) {
            cache = 60;
        }

        cache *= 1000;

        int timeout = intent.getIntExtra("timeout", 0);
        if (timeout == 0) {
            timeout = 10;
        }
        timeout *= 1000;

        Log.d("yunarta", "ManagedHttpService.query url = " + local);

        CancelToken token = executeRequest(local, remote, _method, params, cache, timeout);
        if (token != null) {
            mQueues.put(local, token);
        }
    }

    protected abstract CancelToken executeRequest(String local, String remote, String method, Bundle params, int cache, int timeout);

    protected void insert(WorksManagedHttp cache, String localUri) {
        ContentValues values = new ContentValues();
        values.put("local", cache.local);
        values.put("remote", cache.remote);
        values.put("data", cache.content);
        values.put("time", cache.expiry);
        values.put("error", cache.error);

        if (cache.trace != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
                ObjectOutputStream out = new ObjectOutputStream(baos);
                out.writeObject(cache.trace);

                values.put("trace", baos.toByteArray());
            } catch (IOException e) {
                // e.printStackTrace();
            }
        } else {
            values.putNull("trace");
        }

        values.put("status", cache.error);
        insert(values, localUri);
    }

    private void insert(ContentValues values, String localUri) {
        mQueues.remove(localUri);

        Uri uri = Uri.withAppendedPath(mConfiguration.authority, localUri);

        Log.d("yunarta", "ManagedHttpService.insert data");
        getContentResolver().insert(uri, values);
        getContentResolver().notifyChange(uri, null);
    }

    protected abstract class CancelToken {

        public abstract void cancel();
    }
}
