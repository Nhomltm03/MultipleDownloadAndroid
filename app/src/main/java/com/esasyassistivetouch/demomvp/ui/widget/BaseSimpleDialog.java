package com.esasyassistivetouch.demomvp.ui.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Window;

import java.lang.ref.WeakReference;

public abstract class BaseSimpleDialog {

    private Context mContext;
    private WeakReference<Activity> mActivity;
    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialog;

    public BaseSimpleDialog(Context mContext) {
        this.mContext = mContext;
        this.mActivity = new WeakReference<>((Activity) this.getContext());
        this.mBuilder = new AlertDialog.Builder(this.mContext);
        this.mBuilder.setCancelable(this.isCancelable());
//        this.configDialog();
    }

    public BaseSimpleDialog show() {
        this.mActivity.get().runOnUiThread(() -> {
            if (this.mDialog == null) {
                this.mDialog = this.mBuilder.create();
                this.mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                this.mDialog.setCanceledOnTouchOutside(this.isCancelable());
                this.mDialog.setCancelable(this.isCancelable());
            }

/*            if (this.mDialog != null && this.mDialog.getWindow() != null) {
                this.mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }*/

            if (!this.mActivity.get().isFinishing()) {
                this.mDialog.show();
            }
        });
        return this;
    }

//    private void configDialog() {
//        if (this.mBuilder != null) {
//            this.mBuilder.setTitle(this.getTitle());
//            this.mBuilder.setMessage(this.getMessage());
//        }
//    }

    public BaseSimpleDialog setTitle(CharSequence title) {
        this.mBuilder.setTitle(title);
        return this;
    }

    public BaseSimpleDialog setMessage(CharSequence message) {
        this.mBuilder.setMessage(message);
        return this;
    }

    public BaseSimpleDialog setPositiveAction(int textId, DialogInterface.OnClickListener listener) {
        this.mBuilder.setPositiveButton(textId, listener);
        return this;
    }

    public BaseSimpleDialog setNegativeAction(int textId, DialogInterface.OnClickListener listener) {
        this.mBuilder.setNegativeButton(textId, listener);
        return this;
    }

    public void destroy() {
        this.mActivity.get().runOnUiThread(() -> {
            if (this.mDialog != null) {
                this.mDialog.dismiss();
                this.mDialog = null;
            }
        });
    }

/*    protected abstract CharSequence getTitle();

    protected abstract CharSequence getMessage();*/

    public void dimiss() {
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
    }

    protected abstract boolean isCancelable();

    public Context getContext() {
        return this.mContext;
    }
}
