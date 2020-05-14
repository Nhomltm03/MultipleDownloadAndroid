package com.esasyassistivetouch.demomvp.ui.model;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

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

    public void startDownload(Context context, String url) {
        RxDownload.getInstance(context).download(url, "jarrrr", context.getExternalFilesDir(null).getPath());
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Some descrition");
        request.setTitle("Some title");
// in order for this if to run, you must use the android 3.2 to compile your app
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "name-of-the-file.ext");

// get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
        }
    }

    public void onDownloadProgress() {

    }

    public void onDownloadSucces() {

    }

    public void onDownloadErros() {

    }

    public void onPauseDownload() {

    }

    public void onResumeDownload() {

    }

}
