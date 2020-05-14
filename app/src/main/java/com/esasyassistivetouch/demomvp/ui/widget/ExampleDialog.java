package com.esasyassistivetouch.demomvp.ui.widget;

import android.content.Context;

public class ExampleDialog extends BaseSimpleDialog {

//    private CharSequence title;
//    private CharSequence message;

    public ExampleDialog(Context mContext) {
        super(mContext);
    }

//    @Override
//    protected CharSequence getTitle() {
//        return this.title;
//    }
//
//
//
//    public ExampleDialog setTitle(CharSequence title) {
//        this.title = title;
//        return this;
//    }
//
//    @Override
//    protected CharSequence getMessage() {
//        return this.message;
//    }

//    public ExampleDialog setMessage(CharSequence message) {
//        this.message = message;
//        return this;
//    }

    @Override
    protected boolean isCancelable() {
        return true;
    }
}
