package com.download.entity;

import com.download.RxDownload;
import com.download.db.DataBaseHelper;
import com.download.function.Constant;

import java.util.Map;
import java.util.concurrent.Semaphore;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;

import static com.download.entity.DownloadFlag.WAITING;
import static com.download.function.DownloadEventFactory.completed;
import static com.download.function.DownloadEventFactory.failed;
import static com.download.function.DownloadEventFactory.normal;
import static com.download.function.DownloadEventFactory.paused;
import static com.download.function.DownloadEventFactory.started;
import static com.download.function.DownloadEventFactory.waiting;
import static com.download.function.Utils.createProcessor;
import static com.download.function.Utils.deleteFiles;
import static com.download.function.Utils.dispose;
import static com.download.function.Utils.formatStr;
import static com.download.function.Utils.getFiles;
import static com.download.function.Utils.log;

public class SingleMission extends DownloadMission {
    protected DownloadStatus status;
    protected Disposable disposable;
    private DownloadBean bean;

    private String missionId;
    private Observer<DownloadStatus> observer;

    public SingleMission(RxDownload rxdownload, DownloadBean bean) {
        super(rxdownload);
        this.bean = bean;
    }

    public SingleMission(RxDownload rxDownload, DownloadBean bean, String missionId, Observer<DownloadStatus> observer) {
        super(rxDownload);
        this.bean = bean;
        this.missionId = missionId;
        this.observer = observer;
    }

    public SingleMission(SingleMission other, Observer<DownloadStatus> observer) {
        super(other.rxdownload);
        this.bean = other.getBean();
        this.missionId = other.getMissionId();
        this.observer = observer;
    }

    @Override
    public String getUrl() {
        return bean.getUrl();
    }

    @Override
    public void init(Map<String, DownloadMission> missionMap,
                     Map<String, FlowableProcessor<DownloadEvent>> processorMap) {
        DownloadMission mission = missionMap.get(getUrl());
        if (mission == null) {
            missionMap.put(getUrl(), this);
        } else {
            if (mission.isCanceled()) {
                missionMap.put(getUrl(), this);
            } else {
                throw new IllegalArgumentException(formatStr(Constant.DOWNLOAD_URL_EXISTS, getUrl()));
            }
        }
        this.processor = createProcessor(this.getUrl(), processorMap);
    }

    @Override
    public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
        if (dataBaseHelper.recordNotExists(getUrl())) {
            dataBaseHelper.insertRecord(bean, WAITING, missionId);
        } else {
            dataBaseHelper.updateRecord(getUrl(), WAITING, missionId);
        }
    }

    @Override
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        this.processor.onNext(waiting(dataBaseHelper.readStatus(getUrl())));
    }

    @Override
    public void start(final Semaphore semaphore) throws InterruptedException {
        if (this.isCanceled()) {
            return;
        }

        semaphore.acquire();

        if (this.isCanceled()) {
            semaphore.release();
            return;
        }

        this.disposable = this.rxdownload.download(this.bean)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> {
                    if (this.observer != null) {
                        this.observer.onSubscribe(disposable);
                    }
                })
                .doFinally(() -> {
                    log("finally and release...");
                    this.setCanceled(true);
                    semaphore.release();
                })
                .subscribe(value -> {
                    this.status = value;
                    this.processor.onNext(started(value));
                    if (this.observer != null) {
                        this.observer.onNext(value);
                    }
                }, throwable -> {
                    this.processor.onNext(failed(this.status, throwable));
                    if (this.observer != null) {
                        this.observer.onError(throwable);
                    }
                }, () -> {
                    this.processor.onNext(completed(this.status));
                    this.setCompleted(true);

                    if (this.observer != null) {
                        this.observer.onComplete();
                    }
                });
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        dispose(this.disposable);
        this.setCanceled(true);
        if (this.processor != null && !isCompleted()) {
            this.processor.onNext(paused(dataBaseHelper.readStatus(getUrl())));
        }
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        this.pause(dataBaseHelper);
        if (this.processor != null) {
            this.processor.onNext(normal(null));
        }

        if (deleteFile) {
            DownloadRecord record = dataBaseHelper.readSingleRecord(getUrl());
            if (record != null) {
                deleteFiles(getFiles(record.getSaveName(), record.getSavePath()));
            }
        }

        dataBaseHelper.deleteRecord(getUrl());
    }

    private String getMissionId() {
        return missionId;
    }

    private Observer<DownloadStatus> getObserver() {
        return observer;
    }

    private DownloadBean getBean() {
        return bean;
    }
}
