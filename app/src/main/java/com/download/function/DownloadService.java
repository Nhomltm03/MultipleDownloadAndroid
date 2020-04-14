package com.download.function;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.download.db.DataBaseHelper;
import com.download.entity.DownloadEvent;
import com.download.entity.DownloadFlag;
import com.download.entity.DownloadMission;
import com.download.entity.DownloadRecord;
import com.download.entity.DownloadStatus;
import com.download.entity.MultiMission;
import com.download.entity.SingleMission;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.schedulers.Schedulers;

import static com.download.function.Constant.WAITING_FOR_MISSION_COME;
import static com.download.function.DownloadEventFactory.createEvent;
import static com.download.function.DownloadEventFactory.normal;
import static com.download.function.Utils.createProcessor;
import static com.download.function.Utils.deleteFiles;
import static com.download.function.Utils.dispose;
import static com.download.function.Utils.getFiles;
import static com.download.function.Utils.log;

public class DownloadService extends Service {
    public static final String INTENT_KEY = "zlc_season_rxdownload_max_download_number";

    private DownloadBinder mBinder;

    private Semaphore semaphore;
    private BlockingQueue<DownloadMission> downloadQueue;
    private Map<String, DownloadMission> missionMap;
    private Map<String, FlowableProcessor<DownloadEvent>> processorMap;

    private Disposable disposable;
    private DataBaseHelper dataBaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new DownloadBinder();
        this.downloadQueue = new LinkedBlockingQueue<>();
        this.processorMap = new ConcurrentHashMap<>();
        this.missionMap = new ConcurrentHashMap<>();
        this.dataBaseHelper = DataBaseHelper.getSingleton(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("start Download Service");
        this.dataBaseHelper.repairErrorFlag();
        if (intent != null) {
            int maxDownloadNumber = intent.getIntExtra(INTENT_KEY, 5);
            this.semaphore = new Semaphore(maxDownloadNumber);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("destroy Download Service");
        this.destroy();
        this.dataBaseHelper.closeDataBase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log("bind Download Service");
        this.startDispatch();
        return this.mBinder;
    }

    /**
     * Receive the url download event.
     * <p>
     * Will receive the following event:
     * {@link DownloadFlag#NORMAL}、{@link DownloadFlag#WAITING}、
     * {@link DownloadFlag#STARTED}、{@link DownloadFlag#PAUSED}、
     * {@link DownloadFlag#COMPLETED}、{@link DownloadFlag#FAILED};
     * <p>
     * Every event has {@link DownloadStatus}, you can get it and display it on the interface.
     *
     * @param url url
     * @return DownloadEvent
     */
    public FlowableProcessor<DownloadEvent> receiveDownloadEvent(String url) {
        FlowableProcessor<DownloadEvent> processor = createProcessor(url, this.processorMap);
        DownloadMission mission = this.missionMap.get(url);
        if (mission == null) {  //Not yet add this url mission.
            DownloadRecord record = this.dataBaseHelper.readSingleRecord(url);
            if (record == null) {
                processor.onNext(normal(null));
            } else {
                File file = getFiles(record.getSaveName(), record.getSavePath())[0];
                if (file.exists()) {
                    processor.onNext(createEvent(record.getFlag(), record.getStatus()));
                } else {
                    processor.onNext(normal(null));
                }
            }
        }
        return processor;
    }

    /**
     * Add this mission into download queue.
     *
     * @param mission mission
     * @throws InterruptedException Blocking queue
     */
    public void addDownloadMission(DownloadMission mission) throws InterruptedException {
        mission.init(this.missionMap, this.processorMap);
        mission.insertOrUpdate(this.dataBaseHelper);
        mission.sendWaitingEvent(this.dataBaseHelper);
        this.downloadQueue.put(mission);
    }

    /**
     * Pause download.
     * <p>
     * Pause a url or all tasks belonging to missionId.
     *
     * @param url url or missionId
     */
    public void pauseDownload(String url) {
        DownloadMission mission = this.missionMap.get(url);
        if (mission != null && mission instanceof SingleMission) {
            mission.pause(this.dataBaseHelper);
        }
    }

    /**
     * Delete download.
     * <p>
     * Delete a url or all tasks belonging to missionId.
     *
     * @param url        url or missionId
     * @param deleteFile whether delete file
     */
    public void deleteDownload(String url, boolean deleteFile) {
        DownloadMission mission = this.missionMap.get(url);
        if (mission != null && mission instanceof SingleMission) {
            mission.delete(this.dataBaseHelper, deleteFile);
            this.missionMap.remove(url);
        } else {
            createProcessor(url, processorMap).onNext(normal(null));

            if (deleteFile) {
                DownloadRecord record = this.dataBaseHelper.readSingleRecord(url);
                if (record != null) {
                    deleteFiles(getFiles(record.getSaveName(), record.getSavePath()));
                }
            }
            this.dataBaseHelper.deleteRecord(url);
        }
    }

    /**
     * Start all mission. Not include MultiMission.
     *
     * @throws InterruptedException interrupt
     */
    public void startAll() throws InterruptedException {
        for (DownloadMission each : this.missionMap.values()) {
            if (each.isCompleted()) {
                continue;
            }

            if (each instanceof SingleMission) {
                addDownloadMission(new SingleMission((SingleMission) each, null));
            }

//            if (each instanceof MultiMission) {
//                addDownloadMission(new MultiMission((MultiMission) each));
//            }
        }
    }

    /**
     * Pause all mission.Not include MultiMission.
     */
    public void pauseAll() {
        for (DownloadMission each : this.missionMap.values()) {
            if (each instanceof SingleMission) {
                each.pause(this.dataBaseHelper);
            }
        }
        this.downloadQueue.clear();
    }

    /**
     * Start all mission which associate with missionId.
     *
     * @param missionId missionId
     * @throws InterruptedException interrupt
     */
    public void startAll(String missionId) throws InterruptedException {
        DownloadMission mission = this.missionMap.get(missionId);
        if (mission == null) {
            log("mission not exists");
            return;
        }

        if (mission.isCompleted()) {
            log("mission complete");
            return;
        }

        if (mission instanceof MultiMission) {
            addDownloadMission(new MultiMission((MultiMission) mission));
        }
    }

    /**
     * Pause all mission which associate with missionId
     *
     * @param missionId missionId
     */
    public void pauseAll(String missionId) {
        DownloadMission mission = this.missionMap.get(missionId);
        if (mission == null) {
            log("mission not exists");
            return;
        }

        if (mission.isCompleted()) {
            log("mission complete");
            return;
        }

        if (mission instanceof MultiMission) {
            mission.pause(this.dataBaseHelper);
        }
    }

    /**
     * Delete all mission which associate with missionId.
     *
     * @param missionId  missionId
     * @param deleteFile deleteFile?
     */
    public void deleteAll(String missionId, boolean deleteFile) {
        DownloadMission mission = this.missionMap.get(missionId);
        if (mission != null && mission instanceof MultiMission) {
            mission.delete(this.dataBaseHelper, deleteFile);
            this.missionMap.remove(missionId);
        } else {
            createProcessor(missionId, this.processorMap).onNext(normal(null));

            if (deleteFile) {
                List<DownloadRecord> list = this.dataBaseHelper.readMissionsRecord(missionId);
                for (DownloadRecord each : list) {
                    deleteFiles(getFiles(each.getSaveName(), each.getSavePath()));
                    this.dataBaseHelper.deleteRecord(each.getUrl());
                }
            }
        }
    }

    /**
     * start dispatch download queue.
     */
    private void startDispatch() {
        this.disposable = Observable
                .create((ObservableOnSubscribe<DownloadMission>) emitter -> {
                    DownloadMission mission;
                    while (!emitter.isDisposed()) {
                        try {
                            log(WAITING_FOR_MISSION_COME);
                            mission = downloadQueue.take();
                            log(Constant.MISSION_COMING);
                        } catch (InterruptedException e) {
                            log("Interrupt blocking queue.");
                            continue;
                        }
                        emitter.onNext(mission);
                    }
                    emitter.onComplete();
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(mission -> mission.start(this.semaphore), Utils::log);
    }

    /**
     * Call when service is onDestroy.
     */
    private void destroy() {
        dispose(this.disposable);
        for (DownloadMission each : this.missionMap.values()) {
            each.pause(this.dataBaseHelper);
        }
        this.downloadQueue.clear();
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
