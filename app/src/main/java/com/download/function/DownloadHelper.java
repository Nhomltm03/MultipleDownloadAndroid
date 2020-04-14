package com.download.function;

import android.content.Context;

import androidx.annotation.Nullable;

import com.download.db.DataBaseHelper;
import com.download.entity.DownloadBean;
import com.download.entity.DownloadRecord;
import com.download.entity.DownloadStatus;
import com.download.entity.DownloadType;
import com.download.entity.TemporaryRecord;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Function;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.download.function.Constant.DOWNLOAD_URL_EXISTS;
import static com.download.function.Constant.REQUEST_RETRY_HINT;
import static com.download.function.Constant.TEST_RANGE_SUPPORT;
import static com.download.function.Constant.URL_ILLEGAL;
import static com.download.function.Utils.formatStr;
import static com.download.function.Utils.log;
import static com.download.function.Utils.retry;

public class DownloadHelper {
    private int maxRetryCount = 3;
    private int maxThreads = 3;

    private String defaultSavePath;
    private DownloadApi downloadApi;

    private DataBaseHelper dataBaseHelper;
    private TemporaryRecordTable recordTable;

    public DownloadHelper(Context context) {
        this.downloadApi = RetrofitProvider.getInstance().create(DownloadApi.class);
        this.defaultSavePath = context.getExternalFilesDir(DIRECTORY_DOWNLOADS).getPath();
        this.recordTable = new TemporaryRecordTable();
        this.dataBaseHelper = DataBaseHelper.getSingleton(context.getApplicationContext());
    }

    public void setRetrofit(Retrofit retrofit) {
        this.downloadApi = retrofit.create(DownloadApi.class);
    }

    public void setDefaultSavePath(String defaultSavePath) {
        this.defaultSavePath = defaultSavePath;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * return Files
     *
     * @param url url
     * @return Files = {file,tempFile,lmfFile}
     */
    @Nullable
    public File[] getFiles(String url) {
        DownloadRecord record = this.dataBaseHelper.readSingleRecord(url);
        if (record == null) {
            return null;
        } else {
            return Utils.getFiles(record.getSaveName(), record.getSavePath());
        }
    }

    /**
     * dispatch download
     *
     * @param bean download bean
     * @return DownloadStatus
     */
    public Observable<DownloadStatus> downloadDispatcher(final DownloadBean bean) {
        return Observable.just(1)
                .doOnSubscribe(disposable -> addTempRecord(bean))
                .flatMap((Function<Integer, ObservableSource<DownloadType>>) integer -> getDownloadType(bean.getUrl()))
                .flatMap((Function<DownloadType, ObservableSource<DownloadStatus>>) this::download)
                .doOnError(this::logError)
                .doFinally(() -> this.recordTable.delete(bean.getUrl()));
    }

    private ObservableSource<DownloadStatus> download(DownloadType downloadType) throws IOException, ParseException {
        downloadType.prepareDownload();
        return downloadType.startDownload();
    }

    private void logError(Throwable throwable) {
        if (throwable instanceof CompositeException) {
            CompositeException realException = (CompositeException) throwable;
            List<Throwable> exceptions = realException.getExceptions();
            for (Throwable each : exceptions) {
                log(each);
            }
        } else {
            log(throwable);
        }
    }

    /**
     * Add a temporary record to the record recordTable.
     *
     * @param bean download bean
     */
    private void addTempRecord(DownloadBean bean) {
        if (this.recordTable.contain(bean.getUrl())) {
            throw new IllegalArgumentException(formatStr(DOWNLOAD_URL_EXISTS, bean.getUrl()));
        }
        this.recordTable.add(bean.getUrl(), new TemporaryRecord(bean));
    }

    /**
     * get download type.
     *
     * @param url url
     * @return download type
     */
    private Observable<DownloadType> getDownloadType(final String url) {
        return Observable.just(1)
                .flatMap((Function<Integer, ObservableSource<Object>>) integer -> checkUrl(url))
                .flatMap((Function<Object, ObservableSource<Object>>) o -> checkRange(url))
                .doOnNext(o -> this.recordTable.init(url, this.maxThreads, this.maxRetryCount, this.defaultSavePath,
                        this.downloadApi, this.dataBaseHelper))
                .flatMap((Function<Object, ObservableSource<DownloadType>>) o -> this.recordTable.fileExists(url) ? existsType(url) : nonExistsType(url));
    }

    /**
     * Gets the download type of file non-existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> nonExistsType(final String url) {
        return Observable.just(1)
                .flatMap((Function<Integer, ObservableSource<DownloadType>>) integer -> Observable.just(this.recordTable.generateNonExistsType(url)));
    }

    /**
     * Gets the download type of file existence.
     *
     * @param url file url
     * @return Download Type
     */
    private Observable<DownloadType> existsType(final String url) {
        return Observable.just(1)
                .map(integer -> recordTable.readLastModify(url))
                .flatMap((Function<String, ObservableSource<Object>>) s -> checkFile(url, s))
                .flatMap((Function<Object, ObservableSource<DownloadType>>) o -> Observable.just(this.recordTable.generateFileExistsType(url)));
    }

    /**
     * check url
     *
     * @param url url
     * @return empty
     */
    private ObservableSource<Object> checkUrl(final String url) {
        return this.downloadApi.check(url)
                .flatMap((Function<Response<Void>, ObservableSource<Object>>) resp -> {
                    if (!resp.isSuccessful()) {
                        return checkUrlByGet(url);
                    } else {
                        return saveFileInfo(url, resp);
                    }
                })
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    private ObservableSource<Object> saveFileInfo(final String url, final Response<Void> resp) {
        return Observable.create(emitter -> {
            this.recordTable.saveFileInfo(url, resp);
            emitter.onNext(new Object());
            emitter.onComplete();
        });
    }

    private ObservableSource<Object> checkUrlByGet(final String url) {
        return downloadApi.checkByGet(url)
                .doOnNext(response -> {
                    if (!response.isSuccessful()) {
                        throw new IllegalArgumentException(formatStr(URL_ILLEGAL, url));
                    } else {
                        this.recordTable.saveFileInfo(url, response);
                    }
                })
                .map(response -> new Object())
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    /**
     * http checkRangeByHead request,checkRange need info.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> checkRange(final String url) {
        return downloadApi.checkRangeByHead(TEST_RANGE_SUPPORT, url)
                .doOnNext(response -> recordTable.saveRangeInfo(url, response))
                .map(response -> new Object())
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    /**
     * http checkRangeByHead request,checkRange need info, check whether if server file has changed.
     *
     * @param url url
     * @return empty Observable
     */
    private ObservableSource<Object> checkFile(final String url, String lastModify) {
        return this.downloadApi.checkFileByHead(lastModify, url)
                .doOnNext(response -> this.recordTable.saveFileState(url, response))
                .map(response -> new Object())
                .compose(retry(REQUEST_RETRY_HINT, maxRetryCount));
    }

    public Observable<List<DownloadRecord>> readAllRecords() {
        return this.dataBaseHelper.readAllRecords();
    }

    public Observable<DownloadRecord> readRecord(String url) {
        return this.dataBaseHelper.readRecord(url);
    }
}
