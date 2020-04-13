package com.download.entity;

import com.download.RxDownload;
import com.download.db.DataBaseHelper;

import java.util.Map;
import java.util.concurrent.Semaphore;

import io.reactivex.processors.FlowableProcessor;

public abstract class DownloadMission {

    protected RxDownload rxdownload;
    FlowableProcessor<DownloadEvent> processor;
    private boolean canceled = false;
    private boolean completed = false;

    DownloadMission(RxDownload rxdownload) {
        this.rxdownload = rxdownload;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public abstract String getUrl();

    public abstract void init(Map<String, DownloadMission> missionMap, Map<String, FlowableProcessor<DownloadEvent>> processorMap);

    public abstract void insertOrUpdate(DataBaseHelper dataBaseHelper);

    public abstract void start(final Semaphore semaphore) throws InterruptedException;

    public abstract void pause(DataBaseHelper dataBaseHelper);

    public abstract void delete(DataBaseHelper dataBaseHelper, boolean deleteFile);

    public abstract void sendWaitingEvent(DataBaseHelper dataBaseHelper);
}
