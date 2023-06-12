package com.bdcom.appdialer.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utils {

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean checkPermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context, Manifest.permission.CAMERA)) {
                    /*AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("External storage permission is necessary");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) context,
                                    new String[]{Manifest.permission.CAMERA,
                                            Manifest.permission.READ_EXTERNAL_STORAGE},
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();*/
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[] {Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                } else {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[] {Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    // compress large image size
    public static Bitmap compressImageSize(
            Context context, Bitmap selectedImageBitmap, int sizeOfImageView) {
        Bitmap resizeBitmap = null;

        if (selectedImageBitmap.getHeight() >= selectedImageBitmap.getWidth()) {

            resizeBitmap =
                    Bitmap.createScaledBitmap(
                            selectedImageBitmap,
                            (int)
                                    (selectedImageBitmap.getWidth()
                                            / (selectedImageBitmap.getHeight()
                                                    / (context.getResources()
                                                                    .getDisplayMetrics()
                                                                    .density
                                                            * sizeOfImageView))),
                            (int) context.getResources().getDisplayMetrics().density
                                    * sizeOfImageView,
                            false);

        } else {

            resizeBitmap =
                    Bitmap.createScaledBitmap(
                            selectedImageBitmap,
                            (int) context.getResources().getDisplayMetrics().density
                                    * sizeOfImageView,
                            (int)
                                    (selectedImageBitmap.getHeight()
                                            / (selectedImageBitmap.getWidth()
                                                    / (context.getResources()
                                                                    .getDisplayMetrics()
                                                                    .density
                                                            * sizeOfImageView))),
                            false);
        }
        return resizeBitmap;
    }
}
