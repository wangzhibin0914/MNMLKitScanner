package com.maning.mlkitscanner.scan.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.maning.mlkitscanner.R;
import com.maning.mlkitscanner.scan.MNScanManager;
import com.maning.mlkitscanner.scan.callback.BarcodeAnalyserResultCallback;
import com.maning.mlkitscanner.scan.callback.OnCameraAnalyserCallback;
import com.maning.mlkitscanner.scan.camera.CameraManager;
import com.maning.mlkitscanner.scan.model.MNScanConfig;
import com.maning.mlkitscanner.scan.utils.ImageUtils;
import com.maning.mlkitscanner.scan.utils.StatusBarUtil;
import com.maning.mlkitscanner.scan.view.ScanActionMenuView;
import com.maning.mlkitscanner.scan.view.ScanResultPointView;
import com.maning.mlkitscanner.scan.view.ViewfinderView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScanPreviewActivity extends AppCompatActivity {
    private static final String TAG = ScanPreviewActivity.class.getSimpleName();

    //用来保存当前Activity
    private static WeakReference<ScanPreviewActivity> sActivityRef;
    private static final int REQUEST_CODE_PICK_IMAGE = 10010;
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 10011;
    private static final int REQUEST_CODE_PERMISSION_STORAGE = 10012;
    private Context mContext;
    //闪光灯是否打开
    private boolean is_light_on = false;
    private MNScanConfig mScanConfig;

    private CameraManager cameraManager;
    private View fakeStatusBar;
    private PreviewView mPreviewView;
    private ViewfinderView viewfinderView;
    private ScanResultPointView result_point_view;
    private ScanActionMenuView action_menu_view;
    private RelativeLayout rl_act_root;
    private ImageView iv_show_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mn_scan_activity_scan_preview);
        mContext = this;
        sActivityRef = new WeakReference<>(this);
        initConfig();
        initViews();
        initCamera();
        initStatusBar();
        initPermission();
    }


    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            StatusBarUtil.setTransparentForWindow(this);
            int statusBarHeight = StatusBarUtil.getStatusBarHeight(mContext);
            Log.d(TAG, "statusBarHeight--" + statusBarHeight);
            ViewGroup.LayoutParams fakeStatusBarLayoutParams = fakeStatusBar.getLayoutParams();
            fakeStatusBarLayoutParams.height = statusBarHeight;
            fakeStatusBar.setLayoutParams(fakeStatusBarLayoutParams);
            //状态栏文字颜色
            if (mScanConfig.isStatusBarDarkMode()) {
                StatusBarUtil.setDarkMode(this);
            }
            //状态栏颜色
            String statusBarColor = mScanConfig.getStatusBarColor();
            fakeStatusBar.setBackgroundColor(Color.parseColor(statusBarColor));
        } else {
            ViewGroup.LayoutParams fakeStatusBarLayoutParams = fakeStatusBar.getLayoutParams();
            fakeStatusBarLayoutParams.height = 0;
            fakeStatusBar.setLayoutParams(fakeStatusBarLayoutParams);
        }
    }

    private void initPermission() {
        //检查相机权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //没有相机权限
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
            } else {
                startCamera();
            }
        } else {
            startCamera();
        }
    }

    private void initCamera() {
        cameraManager = CameraManager.getInstance(sActivityRef.get(), mPreviewView);
        cameraManager.setScanConfig(mScanConfig);
        cameraManager.setOnCameraAnalyserCallback(new OnCameraAnalyserCallback() {
            @Override
            public void onSuccess(Bitmap bitmap, List<Barcode> barcodes) {
                result_point_view.setDatas(barcodes, bitmap, getTipsY());
                result_point_view.setVisibility(View.VISIBLE);
                iv_show_result.setImageBitmap(bitmap);
                iv_show_result.setVisibility(View.VISIBLE);
                stopCameraOnMultiScan();
                resetZoomConfig();
                if (barcodes.size() == 1) {
                    finishSuccess(barcodes.get(0).getRawValue());
                }
            }
        });

        cameraManager.setBarcodeAnalyserResultCallback(new BarcodeAnalyserResultCallback() {
            @Override
            public void barcodeTooSmall() {
                cameraManager.zoomTo(zoomOut());
            }

            @Override
            public void barcodeOutOfBound(Barcode barcode) {

            }
        });
    }

    /**
     * 自动缩放扫码   *************************************************
     */
    private static final float originalZoomRatio = 1.0f;
    private static final float originalZoomStep = 1.1f;
    private float currentZoomRatio = originalZoomRatio;
    private float currentZoomStep = originalZoomStep;
    private boolean haveShown = false;
    private int count = 0;

    private float zoomOut() {
        if (currentZoomRatio >= 2f) {
//            if (!haveShown && ++count >= 5) {
//                runOnUiThread(() -> {
//                    haveShown = true;
//                    Toast.makeText(mContext, getString(R.string.place_code_closer), Toast.LENGTH_LONG).show();
//                });
//            }
            return currentZoomRatio;
        }
        if (currentZoomStep > 1.3f) {
            currentZoomStep -= 0.01f;
        } else {
            currentZoomStep += 0.01f;
        }
        currentZoomRatio *= currentZoomStep;
        return currentZoomRatio;
    }

    private void resetZoomConfig() {
        currentZoomRatio = originalZoomRatio;//还原放大比例
        currentZoomStep = originalZoomStep;
        haveShown = false;
        count = 0;
    }
    // *************************************************

    /**
     * 子类重写
     */
    protected float getTipsY() {
        return 0f;
    }

    protected void startCamera() {
        cameraManager.startCamera();
    }

    protected void setScanRect(Rect rect){
        int offsetY = StatusBarUtil.getStatusBarHeight(this);//状态栏修正
        Rect rect1 = new Rect(rect);//不影响原数据
        rect1.top += offsetY;
        rect1.bottom += offsetY;
        cameraManager.setAnalyzeRect(rect1);
    }

    boolean stopped = false;
    protected void stopCameraOnMultiScan(){
        cameraManager.stopCamera();
        stopped = true;
    }

    private void initConfig() {
        mScanConfig = (MNScanConfig) getIntent().getSerializableExtra(MNScanManager.INTENT_KEY_CONFIG_MODEL);
        if (mScanConfig == null) {
            mScanConfig = new MNScanConfig.Builder().build();
        }
    }

    protected void initViews() {
        rl_act_root = (RelativeLayout) findViewById(R.id.rl_act_root);
        mPreviewView = (PreviewView) findViewById(R.id.previewView);
        mPreviewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        fakeStatusBar = (View) findViewById(R.id.fakeStatusBar);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinderView);
        action_menu_view = (ScanActionMenuView) findViewById(R.id.action_menu_view);
        result_point_view = (ScanResultPointView) findViewById(R.id.result_point_view);
        iv_show_result = (ImageView) findViewById(R.id.iv_show_result);

        action_menu_view.setOnScanActionMenuListener(new ScanActionMenuView.OnScanActionMenuListener() {
            @Override
            public void onClose() {
                finishCancel();
            }

            @Override
            public void onLight() {
                if (is_light_on) {
                    closeLight();
                } else {
                    openLight();
                }
            }

            @Override
            public void onPhoto() {
                getImageFromAlbum();
            }
        });

        result_point_view.setOnResultPointClickListener(new ScanResultPointView.OnResultPointClickListener() {
            @Override
            public void onPointClick(String result) {
                finishSuccess(result);
            }

            @Override
            public void onCancel() {
                restartScan();
            }
        });

        if (mScanConfig.drawScanBox()) {
            viewfinderView.setScanConfig(mScanConfig);
        } else {
            viewfinderView.setVisibility(View.GONE);
            viewfinderView.destroyView();
            rl_act_root.removeView(viewfinderView);
        }
        result_point_view.setScanConfig(mScanConfig);
        action_menu_view.setScanConfig(mScanConfig, MNScanConfig.mCustomViewBindCallback);
        result_point_view.setMultiScanTips(mScanConfig.getMultiResultTips());
    }

    protected void restartScan() {
        if (stopped) {
            startCamera();
        }
        result_point_view.removeAllPoints();
        result_point_view.setVisibility(View.GONE);
        iv_show_result.setImageBitmap(null);
        iv_show_result.setVisibility(View.GONE);
    }

    private void openLight() {
        if (!is_light_on) {
            is_light_on = true;
            action_menu_view.openLight();
            cameraManager.openLight();
        }
    }

    private void closeLight() {
        if (is_light_on) {
            is_light_on = false;
            action_menu_view.closeLight();
            cameraManager.closeLight();
        }
    }

    /**
     * 获取相册中的图片
     */
    public void getImageFromAlbum() {
        if (checkStoragePermission()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
    }

    private boolean checkStoragePermission() {
        //判断权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted 授予权限
                    //用户同意了权限申请
                    startCamera();
                } else {
                    // Permission Denied 权限被拒绝
                    String permissionDenied = "Camera permission denied.";
                    Toast.makeText(mContext, permissionDenied, Toast.LENGTH_SHORT).show();
                    finishFailed(permissionDenied);
                }
                break;
            case REQUEST_CODE_PERMISSION_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户同意使用write
                    getImageFromAlbum();
                } else {
                    //缺少权限
                    String permissionDenied = "Read storage permission denied.";
                    Toast.makeText(mContext, permissionDenied, Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //去相册选择图片
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data == null) {
                return;
            }
            Bitmap decodeAbleBitmap = ImageUtils.getBitmap(mContext, data.getData());
            if (decodeAbleBitmap == null) {
                Log.w(TAG, "decodeAbleBitmap == null");
                return;
            }
            cameraManager.setAnalyze(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //分析这个图片
                    InputImage inputImage = InputImage.fromBitmap(decodeAbleBitmap, 0);
                    cameraManager.getBarcodeAnalyser().getBarcodeScanner().process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                                @Override
                                public void onSuccess(@NonNull List<Barcode> barcodes) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
//                                            Log.d(TAG, "barcodes.size():" + barcodes.size());
                                            if (barcodes.size() == 0) {
                                                cameraManager.setAnalyze(true);
                                                Toast.makeText(mContext, "未找到二维码或者条形码", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                            ArrayList<String> results = new ArrayList<>();
                                            for (Barcode barcode : barcodes) {
                                                String value = barcode.getRawValue();
//                                                Log.d(TAG, "value:" + value);
                                                results.add(value);
                                            }

                                            finishSuccess(results);
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "onFailure---:" + e.toString());
                                }
                            });
                }
            }).start();
        }
    }


    @Override
    public void onBackPressed() {
        if (result_point_view.getVisibility() == View.VISIBLE) {
            restartScan();
            return;
        }
        //取消扫码
        finishCancel();
    }

    @Override
    protected void onDestroy() {
        cameraManager.release();
        super.onDestroy();
    }

    private void finishCancel() {
        setResult(MNScanManager.RESULT_CANCLE, new Intent());
        finishFinal();
    }

    private void finishFailed(String errorMsg) {
        Intent intent = new Intent();
        intent.putExtra(MNScanManager.INTENT_KEY_RESULT_ERROR, errorMsg);
        setResult(MNScanManager.RESULT_FAIL, intent);
        finishFinal();
    }

    /**
     * 直接返回单个结果  可重写
     * @param result
     */
    protected void finishSuccess(String result) {
        ArrayList<String> results = new ArrayList<>();
        results.add(result);
        finishSuccess(results);
    }

    /**
     * 返回多个结果，  可重写
     * @param results
     */
    protected void finishSuccess(ArrayList<String> results) {
        Log.i(TAG,"finishSuccess results.get(0) = "+results.get(0));
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MNScanManager.INTENT_KEY_RESULT_SUCCESS, results);
        setResult(MNScanManager.RESULT_SUCCESS, intent);
        finishFinal();
    }

    protected void finishFinal() {
        closeLight();
        MNScanConfig.mCustomViewBindCallback = null;
        sActivityRef = null;
        viewfinderView.destroyView();
        cameraManager.release();
        rl_act_root.removeView(viewfinderView);
        rl_act_root.removeView(mPreviewView);
        rl_act_root.removeView(action_menu_view);
        finish();
//        overridePendingTransition(0, mScanConfig.getActivityExitAnime() == 0 ? R.anim.mn_scan_activity_bottom_out : mScanConfig.getActivityExitAnime());
    }

    //---------对外提供方法----------

    /**
     * 关闭当前Activity
     */
    public static void closeScanPage() {
        if (sActivityRef != null && sActivityRef.get() != null) {
            sActivityRef.get().finishCancel();
        }
    }

    /**
     * 打开相册扫描图片
     */
    public static void openAlbumPage() {
        if (sActivityRef != null && sActivityRef.get() != null) {
            sActivityRef.get().getImageFromAlbum();
        }
    }

    /**
     * 打开手电筒
     */
    public static void openScanLight() {
        if (sActivityRef != null && sActivityRef.get() != null) {
            sActivityRef.get().openLight();
        }
    }

    /**
     * 关闭手电筒
     */
    public static void closeScanLight() {
        if (sActivityRef != null && sActivityRef.get() != null) {
            sActivityRef.get().closeLight();
        }
    }

    /**
     * 是否开启手电筒
     */
    public static boolean isLightOn() {
        if (sActivityRef != null && sActivityRef.get() != null) {
            return sActivityRef.get().is_light_on;
        }
        return false;
    }
}