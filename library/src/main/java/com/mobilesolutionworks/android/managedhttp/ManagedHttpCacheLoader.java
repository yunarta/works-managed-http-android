package com.mobilesolutionworks.android.managedhttp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by yunarta on 17/5/15.
 */
public class ManagedHttpCacheLoader {

    private static final String[] PROJECTION = new String[]{"remote", "data", "time", "error", "trace", "status"};

    private ManagedHttpRequest mRequest;

    private Context mContext;

    private Uri mUri;

    public ManagedHttpCacheLoader(Context context, ManagedHttpRequest request) {
        mContext = context;
        mRequest = request;

        final Uri authority = ManagedHttpConfiguration.configure(mContext).authority;
        mUri = authority.buildUpon().appendEncodedPath(mRequest.local).build();
    }

    public <T> Task<T> execute(Continuation<WorksManagedHttp, T> continuation) {
        final Task<WorksManagedHttp>.TaskCompletionSource source = Task.create();

        ContentResolver cr = mContext.getContentResolver();
        cr.registerContentObserver(mUri, false, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                ContentResolver cr = mContext.getContentResolver();
                cr.unregisterContentObserver(this);

                Cursor cursor = cr.query(mUri, PROJECTION, "local = ?", new String[]{mRequest.local}, null);
                if (cursor.moveToFirst()) {
                    WorksManagedHttp tag = WorksManagedHttp.create(cursor);
                    source.trySetResult(tag);
                }

                cursor.close();
            }
        });

        boolean sendRequest = true;
        Cursor cursor = cr.query(mUri, PROJECTION, "local = ?", new String[]{mRequest.local}, null);
        if (cursor.moveToFirst()) {
            WorksManagedHttp tag = WorksManagedHttp.create(cursor);
            if (tag.expiry >= System.currentTimeMillis()) {
                sendRequest = false;
                source.trySetResult(tag);
            }
        }
        cursor.close();

        if (sendRequest) {
            ManagedHttpConfiguration configure = ManagedHttpConfiguration.configure(mContext);
            Intent service = new Intent(mContext, configure.className);
            service.setAction(configure.action);

            service.putExtra("local", mRequest.local);
            service.putExtra("remote", mRequest.remote);
            service.putExtra("params", mRequest.param);
            service.putExtra("method", mRequest.method);

            mContext.startService(service);
        }

        return source.getTask().continueWith(continuation).onSuccess(new Continuation<T, T>() {
            @Override
            public T then(Task<T> task) throws Exception {
                ContentResolver cr = mContext.getContentResolver();
                cr.delete(mUri, "local = ?", new String[]{mRequest.local});

                return task.getResult();
            }
        });
    }
}
