///**
// * APICloud Modules
// * Copyright (c) 2014-2015 by APICloud, Inc. All Rights Reserved.
// * Licensed under the terms of the The MIT License (MIT).
// * Please see the license.html included with this distribution for details.
// */
//package com.jiashizhan.myscanner;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.database.Cursor;
//import android.hardware.Camera;
//import android.provider.MediaStore;
//import android.util.DisplayMetrics;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.widget.RelativeLayout;
//
//import com.google.zxing.Result;
//import com.uzmap.pkg.uzcore.UZCoreUtil;
//import com.uzmap.pkg.uzcore.UZWebView;
//import com.uzmap.pkg.uzcore.uzmodule.UZModule;
//import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
//import com.uzmap.pkg.uzmodules.uzFNScanner.Zxing.CaptureNewActivity;
//import com.uzmap.pkg.uzmodules.uzFNScanner.Zxing.CaptureView;
//import com.uzmap.pkg.uzmodules.uzFNScanner.Zxing.decoding.Utils;
//import com.uzmap.pkg.uzmodules.uzFNScanner.utlis.BeepUtil;
//import com.uzmap.pkg.uzmodules.uzFNScanner.utlis.ScannerDecoder;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.nio.charset.Charset;
//
//@SuppressWarnings("deprecation")
//public class UzFNScanner extends UZModule implements SurfaceHolder.Callback {
//    public static UZModuleContext mModuleContext;
//    public final int OPEN_CODE = 100;
//    public final int DECODE_CODE = 300;
//    private CaptureView mCaptureView;
//    private SurfaceView mSurfaceView;
//    private SurfaceHolder mSurfaceHolder;
//    private Camera mCamera;
//    private BeepUtil mBeepUtil;
//    private String mSelectedImgPath;
//
//    public UzFNScanner(UZWebView webView) {
//        super(webView);
//    }
//
//    public void jsmethod_openScanner(UZModuleContext moduleContext) {
//        mModuleContext = moduleContext;
//        stopCamera();
//        Intent intent = new Intent(getContext(), CaptureNewActivity.class);
//        initIntentParams(moduleContext, intent);
//        startActivityForResult(this, intent, OPEN_CODE);
//        callBack(moduleContext);
//    }
//
//
//    private void callBack(UZModuleContext moduleContext) {
//        JSONObject ret = new JSONObject();
//        try {
//            ret.put("eventType", "show");
//            moduleContext.success(ret, false);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void openDIYScanner(UZModuleContext moduleContext) {
//        stopCamera();
//        removePreView();
//        initCaptureView(moduleContext);
//        initParams(moduleContext);
//        insertCaptureView(moduleContext);
//        mCaptureView.onResume();
//    }
//
//
//    private void initCaptureView(UZModuleContext moduleContext) {
//        mCaptureView = new CaptureView(mContext, this, moduleContext);
//    }
//
//    private void insertCaptureView(UZModuleContext moduleContext) {
//        String fixedOn = moduleContext.optString("fixedOn");
//        boolean fixed = moduleContext.optBoolean("fixed", true);
//        insertViewToCurWindow(mCaptureView, captureViewLayout(moduleContext),
//                fixedOn, fixed);
//    }
//
//    private RelativeLayout.LayoutParams captureViewLayout(
//            UZModuleContext moduleContext) {
//        int x = x(moduleContext);
//        int y = y(moduleContext);
//        int width = w(moduleContext, mContext);
//        int height = h(moduleContext, mContext);
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
//                width, height);
//        params.setMargins(x, y, 0, 0);
//        return params;
//    }
//
//    public int x(UZModuleContext moduleContext) {
//        JSONObject rect = moduleContext.optJSONObject("rect");
//        if (!moduleContext.isNull("rect")) {
//            return rect.optInt("x", 0);
//        }
//        return 0;
//    }
//
//    public int y(UZModuleContext moduleContext) {
//        JSONObject rect = moduleContext.optJSONObject("rect");
//        if (!moduleContext.isNull("rect")) {
//            return rect.optInt("y", 0);
//        }
//        return 0;
//    }
//
//    public int w(UZModuleContext moduleContext, Context context) {
//        int defaultValue = getScreenWidth((Activity) context);
//        JSONObject rect = moduleContext.optJSONObject("rect");
//        if (!moduleContext.isNull("rect")) {
//            return rect.optInt("w", defaultValue);
//        }
//        return defaultValue;
//    }
//
//    public int h(UZModuleContext moduleContext, Context context) {
//        int defaultValue = getScreenHeight((Activity) context);
//        JSONObject rect = moduleContext.optJSONObject("rect");
//        if (!moduleContext.isNull("rect")) {
//            return rect.optInt("h", defaultValue);
//        }
//        return defaultValue;
//    }
//
//    private int getScreenWidth(Activity act) {
//        DisplayMetrics metric = new DisplayMetrics();
//        act.getWindowManager().getDefaultDisplay().getMetrics(metric);
//        return UZCoreUtil.pixToDip(metric.widthPixels);
//    }
//
//    private int getScreenHeight(Activity act) {
//        DisplayMetrics metric = new DisplayMetrics();
//        act.getWindowManager().getDefaultDisplay().getMetrics(metric);
//        return UZCoreUtil.pixToDip(metric.heightPixels);
//    }
//
//
//    private void initIntentParams(UZModuleContext moduleContext, Intent intent) {
//        String sound = moduleContext.optString("sound");
//        sound = makeRealPath(sound);
//        boolean isSaveToAlbum =true;
//        String savePath = "fs://barImage/hh.jpg";
//
//        boolean autorotation = true;
//        savePath = makeRealPath(savePath);
//        int saveW = 400;
//        int saveH = 400;
//
//        intent.putExtra("soundPath", sound);
//        intent.putExtra("isSaveToAlbum", isSaveToAlbum);
//        intent.putExtra("savePath", savePath);
//        intent.putExtra("saveW", saveW);
//        intent.putExtra("saveH", saveH);
//        intent.putExtra("autorotation", autorotation);
//    }
//
//    private void initParams(UZModuleContext moduleContext) {
//        String sound = moduleContext.optString("sound");
//        sound = makeRealPath(sound);
//        boolean isSaveToAlbum = true;
//        String savePath = "";
//        JSONObject saveImg = moduleContext.optJSONObject("saveImg");
//        if (!moduleContext.isNull("saveImg")) {
//            savePath = saveImg.optString("path");
//        }
//        savePath = makeRealPath(savePath);
//        int saveW = 400;
//        int saveH = 400;
//        mCaptureView.initParams(savePath, saveW, saveH, sound, isSaveToAlbum);
//    }
//
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK) {
//            switch (requestCode) {
//                case OPEN_CODE:
//                    onOpenParseResult(data);
//                    break;
//                case DECODE_CODE:
//                    onDecodeParseResult(data);
//                    break;
//            }
//        }
//    }
//
//    private void onDecodeParseResult(Intent data) {
//        initSelectedImgPath(data);
//        parseImg();
//    }
//
//    private void initSelectedImgPath(Intent data) {
//        String[] proj = {MediaStore.Images.Media.DATA};
//        Cursor cursor = mContext.getContentResolver().query(data.getData(),
//                proj, null, null, null);
//        if (cursor.moveToFirst()) {
//            int column_index = cursor
//                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            mSelectedImgPath = cursor.getString(column_index);
//            if (mSelectedImgPath == null) {
//                mSelectedImgPath = Utils.getPath(mContext, data.getData());
//            }
//        }
//        cursor.close();
//    }
//
//    private void parseImg() {
//        new Thread(mParseImgRunable).start();
//    }
//
//    private Runnable mParseImgRunable = new Runnable() {
//        @Override
//        public void run() {
//            Result result = ScannerDecoder.decodeBar(mSelectedImgPath);
//            mBeepUtil.playBeepSoundAndVibrate();
//            if (result == null) {
//                decodeCallBack(false, null);
//            } else {
//                decodeCallBack(true, result.toString());
//            }
//        }
//    };
//
//    private void onOpenParseResult(Intent data) {
//        String GB_Str = "";
//        try {
//            if (data != null) {
//                String stringExtra = data.getStringExtra("result");
//                String savePath = data.getStringExtra("savePath");
//                String albumPath = data.getStringExtra("albumPath");
//                boolean ISO = Charset.forName("ISO-8859-1").newEncoder()
//                        .canEncode(stringExtra);
//                if (ISO) {
//                    GB_Str = new String(stringExtra.getBytes("ISO-8859-1"),
//                            "GB2312");
//                    openCallback(GB_Str, "success", savePath, albumPath);
//                } else {
//                    openCallback(stringExtra, "success", savePath, albumPath);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void openCallback(String result, String eventType, String savePath,
//                              String albumPath) {
//        JSONObject object = new JSONObject();
//        try {
//            object.put("eventType", eventType);
//            if (savePath != null)
//                object.put("imgPath", savePath);
//            if (albumPath != null)
//                object.put("albumPath", albumPath);
//            object.put("content", result);
//            mModuleContext.success(object, false);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void decodeCallBack(boolean status, String content) {
//        JSONObject ret = new JSONObject();
//        try {
//            ret.put("status", status);
//            if (status) {
//                ret.put("content", content);
//            }
//            mModuleContext.success(ret, false);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private void removePreView() {
//        if (mCaptureView != null) {
//            mCaptureView.onPause();
//            mCaptureView.onDestroy();
//            removeViewFromCurWindow(mCaptureView);
//            mCaptureView = null;
//        }
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        try {
//            if (mCamera == null) {
//                mCamera = Camera.open();
//            }
//            mCamera.setPreviewDisplay(mSurfaceHolder);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width,
//                               int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        destroyCamera();
//    }
//
//    private void stopCamera() {
//        if (mSurfaceView != null) {
//            removeViewFromCurWindow(mSurfaceView);
//            mSurfaceView = null;
//        }
//        destroyCamera();
//    }
//
//    protected void onClean() {
//        destroyCamera();
//        removePreView();
//        if (mSurfaceView != null) {
//            removeViewFromCurWindow(mSurfaceView);
//            mSurfaceView = null;
//        }
//        super.onClean();
//    }
//
//    private void destroyCamera() {
//        if (mCamera != null) {
//            mCamera.stopPreview();
//            mCamera.release();
//            mCamera = null;
//        }
//    }
//
//}
