package com.mobilesolutionworks.android.managedhttp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

/**
 * Created by yunarta on 17/5/15.
 */
public class ManagedHttpContentProvider extends ContentProvider {

    private static final String TABLE_NAME = "managed_http";

    SQLiteDatabase mDatabase;

    @Override
    public boolean onCreate() {
        SQLiteOpenHelper SQLiteOpenHelper = new SQLiteOpenHelper(getContext(), "tag-cache", null, 11) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        /* local uri    */ "local TEXT," +
                        /* remote uri   */ "remote TEXT," +
                        /* data content */ "data TEXT," +
                        /* expiry time  */ "time INTEGER," +
                        /* error code   */ "error INTEGER DEFAULT 0," +
                        /* exception    */ "trace BLOB," +
                        /* exception    */ "status INTEGER," +
                        "PRIMARY KEY (local) ON CONFLICT REPLACE" +
                        ")");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        };
        mDatabase = SQLiteOpenHelper.getWritableDatabase();

        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return mDatabase.query(TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        mDatabase.insert(TABLE_NAME, null, values);
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return mDatabase.delete(TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return mDatabase.update(TABLE_NAME, values, selection, selectionArgs);
    }
}
