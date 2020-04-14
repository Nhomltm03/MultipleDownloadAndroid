package com.download.entity;

import com.download.db.DataBaseHelper;
import com.download.function.DownloadApi;
import com.download.function.FileHelper;
import com.download.function.Utils;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static android.text.TextUtils.concat;
import static com.download.entity.DownloadFlag.COMPLETED;
import static com.download.entity.DownloadFlag.FAILED;
import static com.download.entity.DownloadFlag.PAUSED;
import static com.download.entity.DownloadFlag.STARTED;
import static com.download.function.Constant.CACHE;
import static com.download.function.Constant.RANGE_DOWNLOAD_STARTED;
import static com.download.function.Utils.empty;
import static com.download.function.Utils.getPaths;
import static com.download.function.Utils.mkdirs;
import static java.io.File.separator;

public class TemporaryRecord {
    private DownloadBean bean;

    private String filePath;
    private String tempPath;
    private String lmfPath;

    private int maxRetryCount;
    private int maxThreads;

    private long contentLength;
    private String lastModify;

    private boolean rangeSupport = false;
    private boolean serverFileChanged = false;

    private DataBaseHelper dataBaseHelper;
    private FileHelper fileHelper;
    private DownloadApi downloadApi;

    public TemporaryRecord(DownloadBean bean) {
        this.bean = bean;
    }

    /**
     * init needs info
     *
     * @param maxThreads      Max download threads
     * @param maxRetryCount   Max retry times
     * @param defaultSavePath Default save path;
     * @param downloadApi     API
     * @param dataBaseHelper  DataBaseHelper
     */
    public void init(int maxThreads, int maxRetryCount, String defaultSavePath, DownloadApi downloadApi, DataBaseHelper dataBaseHelper) {
        this.maxThreads = maxThreads;
        this.maxRetryCount = maxRetryCount;
        this.downloadApi = downloadApi;
        this.dataBaseHelper = dataBaseHelper;
        this.fileHelper = new FileHelper(maxThreads);

        String realSavePath;
        if (empty(this.bean.getSavePath())) {
            realSavePath = defaultSavePath;
            this.bean.setSavePath(defaultSavePath);
        } else {
            realSavePath = this.bean.getSavePath();
        }

        String cachePath = concat(realSavePath, separator, CACHE).toString();
        mkdirs(realSavePath, cachePath);
        String[] paths = getPaths(this.bean.getSaveName(), realSavePath);
        this.filePath = paths[0];
        this.tempPath = paths[1];
        this.lmfPath = paths[2];
    }


    /**
     * prepare normal download, create files and save last-modify.
     *
     * @throws IOException
     * @throws ParseException
     */
    public void prepareNormalDownload() throws IOException, ParseException {
        this.fileHelper.prepareDownload(lastModifyFile(), file(), contentLength, lastModify);
    }

    /**
     * prepare range download, create necessary files and save last-modify.
     *
     * @throws IOException
     * @throws ParseException
     */
    public void prepareRangeDownload() throws IOException, ParseException {
        this.fileHelper.prepareDownload(lastModifyFile(), tempFile(), file(), contentLength, lastModify);
    }

    /**
     * Read download range from record file.
     *
     * @param index index
     * @return
     * @throws IOException
     */
    public DownloadRange readDownloadRange(int index) throws IOException {
        return this.fileHelper.readDownloadRange(tempFile(), index);
    }

    /**
     * Normal download save.
     *
     * @param e        emitter
     * @param response response
     */
    public void save(FlowableEmitter<DownloadStatus> e, Response<ResponseBody> response) {
        this.fileHelper.saveFile(e, file(), response);
    }

    /**
     * Range download save
     *
     * @param emitter  emitter
     * @param index    download index
     * @param response response
     * @throws IOException
     */
    public void save(FlowableEmitter<DownloadStatus> emitter, int index, ResponseBody response) throws IOException {
        this.fileHelper.saveFile(emitter, index, tempFile(), file(), response);
    }

    /**
     * Normal download request.
     *
     * @return response
     */
    public Flowable<Response<ResponseBody>> download() {
        return this.downloadApi.download(null, this.bean.getUrl());
    }

    /**
     * Range download request
     *
     * @param index download index
     * @return response
     */
    public Flowable<Response<ResponseBody>> rangeDownload(final int index) {
        return Flowable
                .create((FlowableOnSubscribe<DownloadRange>) e -> {
                    DownloadRange range = readDownloadRange(index);
                    if (range.legal()) {
                        e.onNext(range);
                    }
                    e.onComplete();
                }, BackpressureStrategy.ERROR)
                .flatMap((Function<DownloadRange, Publisher<Response<ResponseBody>>>) range -> {
                    Utils.log(RANGE_DOWNLOAD_STARTED, index, range.start, range.end);
                    String rangeStr = "bytes=" + range.start + "-" + range.end;
                    return this.downloadApi.download(rangeStr, this.bean.getUrl());
                });
    }

    public int getMaxRetryCount() {
        return this.maxRetryCount;
    }

    public int getMaxThreads() {
        return this.maxThreads;
    }

    public boolean isSupportRange() {
        return this.rangeSupport;
    }

    public void setRangeSupport(boolean rangeSupport) {
        this.rangeSupport = rangeSupport;
    }

    public boolean isFileChanged() {
        return this.serverFileChanged;
    }

    public void setFileChanged(boolean serverFileChanged) {
        this.serverFileChanged = serverFileChanged;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setLastModify(String lastModify) {
        this.lastModify = lastModify;
    }

    public String getSaveName() {
        return bean.getSaveName();
    }

    public void setSaveName(String saveName) {
        this.bean.setSaveName(saveName);
    }

    public File file() {
        return new File(this.filePath);
    }

    public File tempFile() {
        return new File(this.tempPath);
    }

    public File lastModifyFile() {
        return new File(this.lmfPath);
    }

    public boolean fileComplete() {
        return file().length() == this.contentLength;
    }

    public boolean tempFileDamaged() throws IOException {
        return this.fileHelper.tempFileDamaged(tempFile(), this.contentLength);
    }

    public String readLastModify() throws IOException {
        return this.fileHelper.readLastModify(lastModifyFile());
    }

    public boolean fileNotComplete() throws IOException {
        return this.fileHelper.fileNotComplete(tempFile());
    }

    public File[] getFiles() {
        return new File[]{file(), tempFile(), lastModifyFile()};
    }


    public void start() {
        if (this.dataBaseHelper.recordNotExists(this.bean.getUrl())) {
            this.dataBaseHelper.insertRecord(this.bean, STARTED);
        } else {
            this.dataBaseHelper.updateRecord(this.bean.getUrl(), this.bean.getSaveName(), this.bean.getSavePath(), STARTED);
        }
    }

    public void update(DownloadStatus status) {
        this.dataBaseHelper.updateStatus(this.bean.getUrl(), status);
    }

    public void error() {
        this.dataBaseHelper.updateRecord(this.bean.getUrl(), FAILED);
    }

    public void complete() {
        this.dataBaseHelper.updateRecord(this.bean.getUrl(), COMPLETED);
    }

    public void cancel() {
        this.dataBaseHelper.updateRecord(this.bean.getUrl(), PAUSED);
    }

    public void finish() {
//        dataBaseHelper.closeDataBase();
    }
}
