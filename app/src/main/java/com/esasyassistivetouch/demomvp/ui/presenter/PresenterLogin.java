package com.esasyassistivetouch.demomvp.ui.presenter;

import android.content.Context;

import com.esasyassistivetouch.demomvp.ui.inter.IPresenterListener;
import com.esasyassistivetouch.demomvp.ui.inter.IViewListener;
import com.esasyassistivetouch.demomvp.ui.model.ModelLogin;

public class PresenterLogin implements IPresenterListener {
    private ModelLogin modelLogin;

    private IViewListener mCallback;

    public PresenterLogin(IViewListener mCallback) {
        this.mCallback = mCallback;
        this.modelLogin = new ModelLogin(this);

    }

    public void reciveHandleLogin(String accout, String password) {
        this.modelLogin.handlerLogin(accout, password);
    }

    public void reciveHandleDownload(Context context, String url) {
        this.modelLogin.startDownload(context, url);
//        RxDownload.getInstance().download()
    }

    @Override
    public void onLoginSucces() {
        this.mCallback.onLoginSucces();
    }

    @Override
    public void onLoginFail() {
        this.mCallback.onLoginFail();
    }

    @Override
    public void startDownload() {
//        this.modelLogin.startDownload();
    }

    @Override
    public void onDownloadProgress() {

    }

    @Override
    public void onDownloadSucces() {

    }

    @Override
    public void onDownloadErros() {

    }

    @Override
    public void onPauseDownload() {

    }

    @Override
    public void onResumeDownload() {

    }
}
