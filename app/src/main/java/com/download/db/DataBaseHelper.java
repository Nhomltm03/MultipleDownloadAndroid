package com.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.download.entity.DownloadBean;
import com.download.entity.DownloadFlag;
import com.download.entity.DownloadRecord;
import com.download.entity.DownloadStatus;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.download.db.Db.RecordTable.COLUMN_DATE;
import static com.download.db.Db.RecordTable.COLUMN_DOWNLOAD_FLAG;
import static com.download.db.Db.RecordTable.COLUMN_DOWNLOAD_SIZE;
import static com.download.db.Db.RecordTable.COLUMN_EXTRA1;
import static com.download.db.Db.RecordTable.COLUMN_EXTRA2;
import static com.download.db.Db.RecordTable.COLUMN_EXTRA3;
import static com.download.db.Db.RecordTable.COLUMN_EXTRA4;
import static com.download.db.Db.RecordTable.COLUMN_EXTRA5;
import static com.download.db.Db.RecordTable.COLUMN_ID;
import static com.download.db.Db.RecordTable.COLUMN_IS_CHUNKED;
import static com.download.db.Db.RecordTable.COLUMN_MISSION_ID;
import static com.download.db.Db.RecordTable.COLUMN_SAVE_NAME;
import static com.download.db.Db.RecordTable.COLUMN_SAVE_PATH;
import static com.download.db.Db.RecordTable.COLUMN_TOTAL_SIZE;
import static com.download.db.Db.RecordTable.COLUMN_URL;
import static com.download.db.Db.RecordTable.TABLE_NAME;
import static com.download.db.Db.RecordTable.insert;
import static com.download.db.Db.RecordTable.read;
import static com.download.db.Db.RecordTable.update;
import static com.download.entity.DownloadFlag.PAUSED;

public class DataBaseHelper {

    private volatile static DataBaseHelper singleton;
    private final Object databaseLock = new Object();
    private volatile SQLiteDatabase readableDatabase;
    private volatile SQLiteDatabase writableDatabase;
    private DbOpenHelper mDbOpenHelper;

    private DataBaseHelper(Context context) {
        this.mDbOpenHelper = new DbOpenHelper(context);
    }

    public static DataBaseHelper getSingleton(Context context) {
        if (singleton == null) {
            synchronized (DataBaseHelper.class) {
                if (singleton == null) {
                    singleton = new DataBaseHelper(context);
                }
            }
        }
        return singleton;
    }

    /**
     * Judge the url's record exists.
     *
     * @param url url
     * @return true if not exists
     */
    public boolean recordNotExists(String url) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, new String[]{COLUMN_ID},
                    COLUMN_URL + "=?", new String[]{url}, null, null, null);
            cursor.moveToFirst();
            return cursor.getCount() == 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertRecord(DownloadBean downloadBean, int flag) {
        return this.getWritableDatabase().insert(TABLE_NAME, null, insert(downloadBean, flag, null));
    }

    public long insertRecord(DownloadBean downloadBean, int flag, String missionId) {
        return this.getWritableDatabase().insert(TABLE_NAME, null, insert(downloadBean, flag, missionId));
    }

    public long updateStatus(String url, DownloadStatus status) {
        return this.getWritableDatabase().update(TABLE_NAME, update(status), COLUMN_URL + "=?", new String[]{url});
    }

    public long updateRecord(String url, int flag) {
        return this.getWritableDatabase().update(TABLE_NAME, update(flag), COLUMN_URL + "=?", new String[]{url});
    }

    public long updateRecord(String url, int flag, String missionId) {
        return this.getWritableDatabase().update(TABLE_NAME, update(flag, missionId), COLUMN_URL + "=?", new String[]{url});
    }

    public long updateRecord(String url, String saveName, String savePath, int flag) {
        return this.getWritableDatabase().update(TABLE_NAME, update(saveName, savePath, flag), COLUMN_URL + "=?", new String[]{url});
    }

    public int deleteRecord(String url) {
        return this.getWritableDatabase().delete(TABLE_NAME, COLUMN_URL + "=?", new String[]{url});
    }

    public long repairErrorFlag() {
        return this.getWritableDatabase().update(TABLE_NAME, update(PAUSED), COLUMN_DOWNLOAD_FLAG + "=? or " + COLUMN_DOWNLOAD_FLAG + "=?",
                new String[]{DownloadFlag.WAITING + "", DownloadFlag.STARTED + ""});
    }

    /**
     * Read single Record.
     *
     * @param url url
     * @return Record
     */
    @Nullable
    public DownloadRecord readSingleRecord(String url) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME,
                    new String[]{COLUMN_ID, COLUMN_URL, COLUMN_SAVE_NAME, COLUMN_SAVE_PATH,
                            COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED,
                            COLUMN_EXTRA1, COLUMN_EXTRA2, COLUMN_EXTRA3, COLUMN_EXTRA4,
                            COLUMN_EXTRA5, COLUMN_DOWNLOAD_FLAG, COLUMN_DATE, COLUMN_MISSION_ID},
                    COLUMN_URL + "=?", new String[]{url}, null, null, null);
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                return null;
            } else {
                return read(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Read missionId all records.
     *
     * @param missionId missionId
     * @return Records
     */
    public List<DownloadRecord> readMissionsRecord(String missionId) {
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(TABLE_NAME,
                    new String[]{COLUMN_ID, COLUMN_URL, COLUMN_SAVE_NAME, COLUMN_SAVE_PATH,
                            COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED,
                            COLUMN_EXTRA1, COLUMN_EXTRA2, COLUMN_EXTRA3, COLUMN_EXTRA4,
                            COLUMN_EXTRA5, COLUMN_DOWNLOAD_FLAG, COLUMN_DATE, COLUMN_MISSION_ID},
                    COLUMN_MISSION_ID + "=?", new String[]{missionId}, null, null, null);
            List<DownloadRecord> result = new ArrayList<>();
            cursor.moveToFirst();
            if (cursor.getCount() > 0) {
                do {
                    result.add(read(cursor));
                } while (cursor.moveToNext());
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Read the url's download status.
     *
     * @param url url
     * @return download status
     */
    public DownloadStatus readStatus(String url) {
        try (Cursor cursor = this.getReadableDatabase().query(
                TABLE_NAME,
                new String[]{COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED},
                COLUMN_URL + "=?", new String[]{url}, null, null, null)) {
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                return new DownloadStatus();
            } else {
                return Db.RecordTable.readStatus(cursor);
            }
        }
    }

    /**
     * Read all records from database.
     *
     * @return All records.
     */
    public Observable<List<DownloadRecord>> readAllRecords() {
        return Observable
                .create((ObservableOnSubscribe<List<DownloadRecord>>) emitter -> {
                    Cursor cursor = null;
                    try {
                        cursor = getReadableDatabase().query(TABLE_NAME,
                                new String[]{COLUMN_ID, COLUMN_URL, COLUMN_SAVE_NAME, COLUMN_SAVE_PATH,
                                        COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED,
                                        COLUMN_EXTRA1, COLUMN_EXTRA2, COLUMN_EXTRA3, COLUMN_EXTRA4,
                                        COLUMN_EXTRA5, COLUMN_DOWNLOAD_FLAG, COLUMN_DATE, COLUMN_MISSION_ID},
                                null, null, null, null, null);
                        List<DownloadRecord> result = new ArrayList<>();
                        cursor.moveToFirst();
                        if (cursor.getCount() > 0) {
                            do {
                                result.add(read(cursor));
                            } while (cursor.moveToNext());
                        }
                        emitter.onNext(result);
                        emitter.onComplete();
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Read the url's record.
     * <p>
     * If record not exists, return an empty record.
     *
     * @param url url
     * @return record
     */
    public Observable<DownloadRecord> readRecord(final String url) {
        return Observable
                .create((ObservableOnSubscribe<DownloadRecord>) emitter -> {
                    try (Cursor cursor = getReadableDatabase().query(TABLE_NAME,
                            new String[]{COLUMN_ID, COLUMN_URL, COLUMN_SAVE_NAME, COLUMN_SAVE_PATH,
                                    COLUMN_DOWNLOAD_SIZE, COLUMN_TOTAL_SIZE, COLUMN_IS_CHUNKED,
                                    COLUMN_EXTRA1, COLUMN_EXTRA2, COLUMN_EXTRA3, COLUMN_EXTRA4,
                                    COLUMN_EXTRA5, COLUMN_DOWNLOAD_FLAG, COLUMN_DATE, COLUMN_MISSION_ID},
                            COLUMN_URL + "=?", new String[]{url}, null, null, null)) {
                        cursor.moveToFirst();
                        if (cursor.getCount() == 0) {
                            emitter.onNext(new DownloadRecord());
                        } else {
                            emitter.onNext(read(cursor));
                        }
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void closeDataBase() {
        synchronized (this.databaseLock) {
            this.readableDatabase = null;
            this.writableDatabase = null;
            this.mDbOpenHelper.close();
        }
    }

    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = this.writableDatabase;
        if (db == null) {
            synchronized (this.databaseLock) {
                db = this.writableDatabase;
                if (db == null) {
                    db = this.writableDatabase = this.mDbOpenHelper.getWritableDatabase();
                }
            }
        }
        return db;
    }

    private SQLiteDatabase getReadableDatabase() {
        SQLiteDatabase db = this.readableDatabase;
        if (db == null) {
            synchronized (this.databaseLock) {
                db = this.readableDatabase;
                if (db == null) {
                    db = this.readableDatabase = this.mDbOpenHelper.getReadableDatabase();
                }
            }
        }
        return db;
    }
}
