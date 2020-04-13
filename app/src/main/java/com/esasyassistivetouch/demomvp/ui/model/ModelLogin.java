package com.esasyassistivetouch.demomvp.ui.model;

import android.content.Context;

import com.download.RxDownload;
import com.esasyassistivetouch.demomvp.ui.inter.IPresenterListener;

public class ModelLogin {
    private IPresenterListener presenterCallback;

    public ModelLogin(IPresenterListener presenterCallback) {
        this.presenterCallback = presenterCallback;
    }

    public void handlerLogin(String account, String password) {
        if (account.equals("datnt") && password.equals("123")) {
            this.presenterCallback.onLoginSucces();
        } else {
            this.presenterCallback.onLoginFail();
        }
    }

    public void startDownload(String url, Context context) {
        RxDownload.getInstance(context).download(url,"d","dd");
    }

    public void onDownloadProgress() {

    }

    public void onDownloadSucces() {

    }

    public void onDownloadErros() {

    }

    public void onPauseDownload(){

    }

    public void onResumeDownload(){

    }

}
