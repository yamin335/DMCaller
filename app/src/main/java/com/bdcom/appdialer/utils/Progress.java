package com.bdcom.appdialer.utils;

import android.app.ProgressDialog;
import android.content.Context;

public class Progress extends ProgressDialog {

    public Progress(Context context) {
        super(context);
        setIndeterminate(true);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setMessage("Please Wait...");
    }

    @Override
    public void dismiss() {
        if (isShowing()) {
            super.dismiss();
        }
    }
}
