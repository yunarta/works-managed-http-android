package com.mobilesolutionworks.android.managedhttp;

import android.database.Cursor;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Created by yunarta on 18/5/15.
 */
public class WorksManagedHttp {

    public Cursor cursor;

    public String local;

    public String remote;

    public String content;

    public long expiry;

    public int error;

    public boolean loaded;

    public boolean detached;

    public Throwable trace;

    public int status;

    public boolean loadFinished = true;

    public void close() {
        if (!cursor.isClosed()) {
            cursor.close();
        }
    }

    public static WorksManagedHttp create(Cursor cursor) {
        WorksManagedHttp tag;
        tag = new WorksManagedHttp();
        tag.loaded = true;
        tag.remote = cursor.getString(0);
        tag.content = cursor.getString(1);
        tag.expiry = cursor.getLong(2);
        tag.error = cursor.getInt(3);

        byte[] trace = cursor.getBlob(4);
        if (trace != null) {
            try {
                ObjectInputStream out = new ObjectInputStream(new ByteArrayInputStream(trace));
                tag.trace = (Throwable) out.readObject();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        tag.status = cursor.getInt(5);
        return tag;
    }
}
