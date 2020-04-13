package com.download.entity;

import com.download.RxDownload;
import com.download.db.DataBaseHelper;
import com.download.function.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;

import static com.download.function.DownloadEventFactory.completed;
import static com.download.function.DownloadEventFactory.failed;
import static com.download.function.DownloadEventFactory.normal;
import static com.download.function.DownloadEventFactory.paused;
import static com.download.function.DownloadEventFactory.started;
import static com.download.function.DownloadEventFactory.waiting;
import static com.download.function.Utils.createProcessor;
import static com.download.function.Utils.formatStr;
import static com.download.function.Utils.log;

public class MultiMission extends DownloadMission {
    private AtomicInteger completeNumber;
    private AtomicInteger failedNumber;
    private List<SingleMission> missions;

    private String missionId;
    private Observer<DownloadStatus> observer;

    public MultiMission(MultiMission other) {
        super(other.rxdownload);
        this.missionId = other.getUrl();
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);

        this.observer = new SingleMissionObserver(this);

        for (SingleMission each : other.getMissions()) {
            this.missions.add(new SingleMission(each, observer));
        }
    }


    public MultiMission(RxDownload rxDownload, String missionId, List<DownloadBean> missions) {
        super(rxDownload);
        this.missionId = missionId;
        this.missions = new ArrayList<>();
        this.completeNumber = new AtomicInteger(0);
        this.failedNumber = new AtomicInteger(0);
        this.observer = new SingleMissionObserver(this);

        for (DownloadBean each : missions) {
            this.missions.add(new SingleMission(rxDownload, each, missionId, observer));
        }
    }

    public String getUrl() {
        return missionId;
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

        this.processor = createProcessor(getUrl(), processorMap);

        for (SingleMission each : missions) {
            each.init(missionMap, processorMap);
        }
    }

    @Override
    public void insertOrUpdate(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.insertOrUpdate(dataBaseHelper);
        }
    }

    @Override
    public void sendWaitingEvent(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : missions) {
            each.sendWaitingEvent(dataBaseHelper);
        }
        processor.onNext(waiting(null));
    }

    @Override
    public void start(Semaphore semaphore) throws InterruptedException {
        for (SingleMission each : this.missions) {
            each.start(semaphore);
        }
    }

    @Override
    public void pause(DataBaseHelper dataBaseHelper) {
        for (SingleMission each : this.missions) {
            each.pause(dataBaseHelper);
        }
        this.setCanceled(true);
        this.completeNumber.set(0);
        this.failedNumber.set(0);
        this.processor.onNext(paused(null));
    }

    @Override
    public void delete(DataBaseHelper dataBaseHelper, boolean deleteFile) {
        for (SingleMission each : this.missions) {
            each.delete(dataBaseHelper, deleteFile);
        }
        this.setCanceled(true);
        this.completeNumber.set(0);
        this.failedNumber.set(0);
        this.processor.onNext(normal(null));
    }

    private List<SingleMission> getMissions() {
        return missions;
    }

    private class SingleMissionObserver implements Observer<DownloadStatus> {

        MultiMission real;

        public SingleMissionObserver(MultiMission multiMission) {
            this.real = multiMission;
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.real.processor.onNext(started(null));
        }

        @Override
        public void onNext(DownloadStatus value) {

        }

        @Override
        public void onError(Throwable e) {
            log("onerror");
            int temp = this.real.failedNumber.incrementAndGet();
            log("temp: " + temp);
            log("size: " + this.real.missions.size());
            if ((temp + this.real.completeNumber.intValue()) == this.real.missions.size()) {
                this.real.processor.onNext(failed(null, new Throwable("download failed")));
                this.real.setCanceled(true);
                log("set error cancel");
            }
        }

        @Override
        public void onComplete() {
            int temp = this.real.completeNumber.incrementAndGet();
            if (temp == this.real.missions.size()) {
                this.real.processor.onNext(completed(null));
                this.real.setCompleted(true);
                this.real.setCanceled(true);
            } else if ((temp + this.real.failedNumber.intValue()) == this.real.missions.size()) {
                this.real.processor.onNext(failed(null, new Throwable("download failed")));
                this.real.setCanceled(true);
            }
        }
    }
}
