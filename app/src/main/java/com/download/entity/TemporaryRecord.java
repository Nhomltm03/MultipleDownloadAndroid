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
        filePath = paths[0];
        tempPath = paths[1];
        lmfPath = paths[2];
    }


    /**
     * prepare normal download, create files and save last-modify.
     *
     * @throws IOException
     * @throws ParseException
     */
    public void prepareNormalDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(lastModifyFile(), file(), contentLength, lastModify);
    }

    /**
     * prepare range download, create necessary files and save last-modify.
     *
     * @throws IOException
     * @throws ParseException
     */
    public void prepareRangeDownload() throws IOException, ParseException {
        fileHelper.prepareDownload(lastModifyFile(), tempFile(), file(), contentLength, lastModify);
    }

    /**
     * Read download range from record file.
     *
     * @param index index
     * @return
     * @throws IOException
     */
    public DownloadRange readDownloadRange(int index) throws IOException {
        return fileHelper.readDownloadRange(tempFile(), index);
    }

    /**
     * Normal download save.
     *
     * @param e        emitter
     * @param response response
     */
    public void save(FlowableEmitter<DownloadStatus> e, Response<ResponseBody> response) {
        fileHelper.saveFile(e, file(), response);
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
        return downloadApi.download(null, bean.getUrl());
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
                    return this.downloadApi.download(rangeStr, bean.getUrl());
                });
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public boolean isSupportRange() {
        return rangeSupport;
    }

    public void setRangeSupport(boolean rangeSupport) {
        this.rangeSupport = rangeSupport;
    }

    public boolean isFileChanged() {
        return serverFileChanged;
    }

    public void setFileChanged(boolean serverFileChanged) {
        this.serverFileChanged = serverFileChanged;
    }

    public long getContentLength() {
        return contentLength;
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
        bean.setSaveName(saveName);
    }

    public File file() {
        return new File(filePath);
    }

    public File tempFile() {
        return new File(tempPath);
    }

    public File lastModifyFile() {
        return new File(lmfPath);
    }

    public boolean fileComplete() {
        return file().length() == contentLength;
    }

    public boolean tempFileDamaged() throws IOException {
        return fileHelper.tempFileDamaged(tempFile(), contentLength);
    }

    public String readLastModify() throws IOException {
        return fileHelper.readLastModify(lastModifyFile());
    }

    public boolean fileNotComplete() throws IOException {
        return fileHelper.fileNotComplete(tempFile());
    }

    public File[] getFiles() {
        return new File[]{file(), tempFile(), lastModifyFile()};
    }


    public void start() {
        if (dataBaseHelper.recordNotExists(bean.getUrl())) {
            dataBaseHelper.insertRecord(bean, STARTED);
        } else {
            dataBaseHelper.updateRecord(bean.getUrl(), bean.getSaveName(), bean.getSavePath(), STARTED);
        }
    }

    public void update(DownloadStatus status) {
        dataBaseHelper.updateStatus(bean.getUrl(), status);
    }

    public void error() {
        dataBaseHelper.updateRecord(bean.getUrl(), FAILED);
    }

    public void complete() {
        dataBaseHelper.updateRecord(bean.getUrl(), COMPLETED);
    }

    public void cancel() {
        dataBaseHelper.updateRecord(bean.getUrl(), PAUSED);
    }

    public void finish() {
//        dataBaseHelper.closeDataBase();
    }
}