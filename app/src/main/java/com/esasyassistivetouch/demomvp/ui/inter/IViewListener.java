package com.esasyassistivetouch.demomvp.ui.inter;

public interface IViewListener {
    void onLoginSucces();

    void onLoginFail();

    void startDownload();

    void onDownloadProgress();

    void onDownloadSucces();

    void onDownloadErros();

    void onPauseDownload();

    void onResumeDownload();
}
