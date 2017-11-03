/**
 * APICloud Modules
 * Copyright (c) 2014-2015 by APICloud, Inc. All Rights Reserved.
 * Licensed under the terms of the The MIT License (MIT).
 * Please see the license.html included with this distribution for details.
 */
package com.jiashizhan.myscanner.Zxing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.Result;
import com.jiashizhan.myscanner.R;
import com.jiashizhan.myscanner.Zxing.camera.CameraManager;
import com.jiashizhan.myscanner.Zxing.decoding.CaptureActivityHandler;
import com.jiashizhan.myscanner.Zxing.decoding.InactivityTimer;
import com.jiashizhan.myscanner.Zxing.decoding.Utils;
import com.jiashizhan.myscanner.Zxing.view.ViewfinderView;
import com.jiashizhan.myscanner.utlis.BeepUtil;
import com.jiashizhan.myscanner.utlis.ScanUtil;
import com.jiashizhan.myscanner.utlis.ScannerDecoder;


import java.io.File;

public class CaptureNewActivity extends Activity implements Callback,
        OnClickListener {
    private static final int REQUEST_CODE = 234;
    private CaptureActivityHandler mHandler;
    private ViewfinderView mViewfinderView;
    private InactivityTimer mInactivityTimer;
    private String mSelectedImgPath;
    private String mSavePath;
    private int mSaveW;
    private int mSaveH;
    private String mBeepPath;
    private boolean mIsSaveToAlbum;
    private boolean mHasSurface;
    private boolean mSwitchLigthFlag = true;
    private BeepUtil mBeepUtil;
    private int mOrientation = 0;
    private OrientationEventListener mOrientationListener;
    private boolean mIsOrientation = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mo_fnscanner_main);
        init();
        initParams();
        initView();
        initOrientation();
        startOrientationChangeListener();
    }

    private void init() {
        CameraManager.init(getApplication());
        mHasSurface = false;
        mInactivityTimer = new InactivityTimer(this);
    }

    private void initOrientation() {
        int angle = getDisplayRotation();
        switch (angle) {
            case 90:
                mOrientation = 0;
                break;
            case 0:
                mOrientation = 1;
                break;
            case 270:
                mOrientation = 2;
                break;
            case 180:
                mOrientation = 3;
        }
    }

    private void initView() {
        mViewfinderView = (ViewfinderView) findViewById(finderViewId());
        findViewById(backBtnId()).setOnClickListener(this);
        findViewById(selectImgBtnId()).setOnClickListener(this);
        findViewById(switchLightBtnId()).setOnClickListener(this);
    }

    private void initParams() {
        Intent intent = getIntent();
        mBeepPath = intent.getStringExtra("soundPath");
        mBeepUtil = new BeepUtil(this, mBeepPath);
        mIsSaveToAlbum = intent.getBooleanExtra("isSaveToAlbum", false);
        mSavePath = intent.getStringExtra("savePath");
        mSaveW = intent.getIntExtra("saveW", 200);
        mSaveH = intent.getIntExtra("saveH", 200);
        mIsOrientation = intent.getBooleanExtra("autorotation", false);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backBtnId()) {
//			callBack("cancel", null);
            Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show();
            finish();
        } else if (v.getId() == selectImgBtnId()) {
//			callBack("selectImage", null);
            Toast.makeText(this, "selectImage", Toast.LENGTH_SHORT).show();
            selectImg();
        } else if (v.getId() == switchLightBtnId()) {
            switchLight();
        }
    }

    @SuppressLint("InlinedApi")
    private void selectImg() {
        Intent innerIntent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            innerIntent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            innerIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        }
        innerIntent.setType("image/*");
        Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
        startActivityForResult(wrapperIntent, REQUEST_CODE);
    }

    private void switchLight() {
        if (mSwitchLigthFlag == true) {
            mSwitchLigthFlag = false;
            CameraManager.get().openLight();
        } else {
            mSwitchLigthFlag = true;
            CameraManager.get().offLight();
        }
    }

    public void handleDecode(final Result obj, Bitmap barcode) {
        mInactivityTimer.onActivity();
        mBeepUtil.playBeepSoundAndVibrate();
        String savePath = null;
        ScanUtil.scanResult2img(obj.getText(), mSavePath, mSaveW, mSaveH,
                mIsSaveToAlbum, false, this);
        if (!isBlank(mSavePath)) {
            savePath = new File(mSavePath).getAbsolutePath();
        }
        handleDecodeFinish(savePath, ScanUtil.ALBUM_IMG_PATH, obj);
    }

    private void handleDecodeFinish(String savePath, String albumPath,
                                    Result obj) {
        Intent data = new Intent();
        if (savePath != null)
            data.putExtra("savePath", savePath);
        if (albumPath != null)
            data.putExtra("albumPath", albumPath);
        data.putExtra("result", obj.toString());
        setResult(RESULT_OK, data);
        finish();
    }

    private void backPreAct(Result result) {
        Intent data = new Intent();
        data.putExtra("result", result.toString());
        setResult(RESULT_OK, data);
        finish();
    }

    private void initSelectedImgPath(Intent data) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(data.getData(), proj, null,
                null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            mSelectedImgPath = cursor.getString(column_index);
            if (mSelectedImgPath == null) {
                mSelectedImgPath = Utils.getPath(getApplicationContext(),
                        data.getData());
            }
        }
        cursor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE:
                    initSelectedImgPath(data);
                    parseImg();
                    break;
            }
        }
    }

    private void parseImg() {
        new Thread(mParseImgRunable).start();
    }

    private Runnable mParseImgRunable = new Runnable() {
        @Override
        public void run() {
            Result result = ScannerDecoder.decodeBar(mSelectedImgPath);
            if (result == null) {
//                callBack("fail", "非法图片");
                Toast.makeText(CaptureNewActivity.this, "非法图片", Toast.LENGTH_SHORT).show();
            } else {
                backPreAct(result);
            }
        }
    };

    @Override
    protected void onResume() {
        if (!mIsOrientation) {
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        super.onResume();
        initOnResume();

    }

    private void initOnResume() {
        initSurface();
        mBeepUtil.initBeep();
    }

    @SuppressWarnings("deprecation")
    private void initSurface() {
        SurfaceView surfaceView = (SurfaceView) findViewById(preViewId());
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @SuppressWarnings("deprecation")
    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            CameraManager.get().openDriver(surfaceHolder, display.getWidth(),
                    display.getHeight());
            chargeScreenAngle();
        } catch (Exception e) {
            return;
        }
        if (mHandler == null) {
            mHandler = new CaptureActivityHandler(this, null, null);
        }
    }

    private void chargeScreenAngle() {
        int angle = getDisplayRotation();
        CameraManager.get().setScreemOrientation(angle);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.quitSynchronously();
            mHandler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        mInactivityTimer.shutdown();
        super.onDestroy();
    }

//	private void callBack(String eventType, String msg) {
//		JSONObject ret = new JSONObject();
//		try {
//			ret.put("eventType", eventType);
//			if (msg != null)
//				ret.put("content", msg);
//			UzFNScanner.mModuleContext.success(ret, false);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//	}

//    private int getLayoutId() {
//        return R.layout.mo_fnscanner_main;
////		return UZResourcesIDFinder.getResLayoutID("mo_fnscanner_main");
//    }

    private int finderViewId() {
        return R.id.mo_fnscanner_viewfinder_view;
//		return UZResourcesIDFinder.getResIdID("mo_fnscanner_viewfinder_view");
    }

    private int backBtnId() {
//		return UZResourcesIDFinder.getResIdID("mo_fnscanner_back");
        return R.id.mo_fnscanner_back;
    }

    private int selectImgBtnId() {
//		return UZResourcesIDFinder.getResIdID("mo_fnscanner_photo");
        return R.id.mo_fnscanner_photo;
    }

    private int switchLightBtnId() {
//		return UZResourcesIDFinder.getResIdID("mo_fnscanner_light");
        return R.id.mo_fnscanner_light;
    }

    private int preViewId() {
//		return UZResourcesIDFinder.getResIdID("mo_fnscanner_preview_view");
        return R.id.mo_fnscanner_preview_view;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public boolean isBlank(CharSequence cs) {
        int strLen;
        if ((cs == null) || ((strLen = cs.length()) == 0))
            return true;
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        callBack("cancel", null);
        Toast.makeText(CaptureNewActivity.this, "cancel", Toast.LENGTH_SHORT).show();
    }

    public int getDisplayRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:// 上
                return 90;
            case Surface.ROTATION_90:// 左
                return 0;
            case Surface.ROTATION_180:// 下
                return 270;
            case Surface.ROTATION_270:// 右
                return 180;
        }
        return 0;
    }

    private final void startOrientationChangeListener() {
        this.mOrientationListener = new OrientationEventListener(this) {
            public void onOrientationChanged(int rotation) {
                synchronized (mOrientationListener) {
                    int angle = getDisplayRotation();
                    if (angle == 0) {
                        if (mOrientation == 3) {
                            chargeScreenAngle();
                        }
                        mOrientation = 1;
                    } else if (angle == 180) {
                        if (mOrientation == 1) {
                            chargeScreenAngle();
                        }
                        mOrientation = 3;
                    } else if (angle == 90) {
                        if (mOrientation == 2) {
                            chargeScreenAngle();
                        }
                        mOrientation = 0;
                    } else if (angle == 270) {
                        if (mOrientation == 0) {
                            chargeScreenAngle();
                        }
                        mOrientation = 2;
                    }
                }
            }
        };
        this.mOrientationListener.enable();
    }
}