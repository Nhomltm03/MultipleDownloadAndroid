package com.download.entity;

public class DownloadEvent {
    private int flag = DownloadFlag.NORMAL;
    private DownloadStatus downloadStatus = new DownloadStatus();
    private Throwable mError;

    public int getFlag() {
        return this.flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public DownloadStatus getDownloadStatus() {
        return this.downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public Throwable getError() {
        return this.mError;
    }

    public void setError(Throwable error) {
        this.mError = error;
    }
}
