package com.maning.mlkitscanner.scan.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.mlkit.vision.barcode.Barcode;
import com.maning.mlkitscanner.R;
import com.maning.mlkitscanner.scan.model.MNScanConfig;
import com.maning.mlkitscanner.scan.utils.CommonUtils;
import com.maning.mlkitscanner.scan.utils.StatusBarUtil;

import java.util.List;

/**
 * @author : maning
 * @date : 2021/1/7
 * @desc : 扫描结果点View展示
 */
public class ScanResultPointView extends FrameLayout {
    private static final String TAG = ScanResultPointView.class.getSimpleName();

    private MNScanConfig scanConfig;
    private List<Barcode> resultPoint;
    private OnResultPointClickListener onResultPointClickListener;

    private int resultPointColor;
    private int resultPointStrokeColor;
    private int resultPointWithdHeight;
    private int resultPointRadiusCorners;
    private int resultPointStrokeWidth;
    private TextView tv_tips;
    private FrameLayout fl_result_point_root;
    private View fakeStatusBar;
    private int statusBarHeight;
    private ImageView iv_show_result,iv_back;
    private Bitmap barcodeBitmap;
    private RelativeLayout rl_tips;
//    private LinearLayout ll_cancel;

    public void setOnResultPointClickListener(OnResultPointClickListener onResultPointClickListener) {
        this.onResultPointClickListener = onResultPointClickListener;
    }

    public interface OnResultPointClickListener {
        void onPointClick(String result);

        void onCancel();
    }

    public ScanResultPointView(Context context) {
        this(context, null);
    }

    public ScanResultPointView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanResultPointView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.mn_scan_result_point_view, this);
        fakeStatusBar = view.findViewById(R.id.fakeStatusBar2);
        iv_show_result = view.findViewById(R.id.iv_show_result);
        iv_back = view.findViewById(R.id.iv_back);
        tv_tips = view.findViewById(R.id.tv_tips);
        rl_tips = view.findViewById(R.id.rl_tips);
        fl_result_point_root = view.findViewById(R.id.fl_result_point_root);

        statusBarHeight = StatusBarUtil.getStatusBarHeight(getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.LayoutParams fakeStatusBarLayoutParams = fakeStatusBar.getLayoutParams();
            fakeStatusBarLayoutParams.height = statusBarHeight;
            fakeStatusBar.setLayoutParams(fakeStatusBarLayoutParams);
        }

        iv_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //隐藏View
                if (onResultPointClickListener != null) {
                    onResultPointClickListener.onCancel();
                }
                removeAllPoints();
            }
        });
        iv_show_result.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //拦截点击事件
            }
        });
    }

    public void setScanConfig(MNScanConfig config) {
        scanConfig = config;
        initResultPointConfigs();
    }

    private void initResultPointConfigs() {
        if (scanConfig == null) {
            return;
        }
        resultPointRadiusCorners = CommonUtils.dip2px(getContext(), scanConfig.getResultPointCorners());
        resultPointWithdHeight = CommonUtils.dip2px(getContext(), scanConfig.getResultPointWithdHeight());
        resultPointStrokeWidth = CommonUtils.dip2px(getContext(), scanConfig.getResultPointStrokeWidth());
        String resultPointColorStr = scanConfig.getResultPointColor();
        String resultPointStrokeColorStr = scanConfig.getResultPointStrokeColor();
        if (resultPointWithdHeight == 0) {
            resultPointWithdHeight = CommonUtils.dip2px(getContext(), 36);
        }
        if (resultPointRadiusCorners == 0) {
            resultPointRadiusCorners = CommonUtils.dip2px(getContext(), 36);
        }
        if (resultPointStrokeWidth == 0) {
            resultPointStrokeWidth = CommonUtils.dip2px(getContext(), 3);
        }
        if (!TextUtils.isEmpty(resultPointColorStr)) {
            resultPointColor = Color.parseColor(resultPointColorStr);
        } else {
            resultPointColor = getContext().getResources().getColor(R.color.mn_scan_viewfinder_laser_result_point);
        }
        if (!TextUtils.isEmpty(resultPointStrokeColorStr)) {
            resultPointStrokeColor = Color.parseColor(resultPointStrokeColorStr);
        } else {
            resultPointStrokeColor = getContext().getResources().getColor(R.color.mn_scan_viewfinder_laser_result_point_border);
        }
    }

    public void setDatas(List<Barcode> results, Bitmap barcode,float tipsY) {
        this.resultPoint = results;
        this.barcodeBitmap = barcode;
        drawableResultPoint(tipsY);
    }

    public void removeAllPoints() {
        fl_result_point_root.removeAllViews();
    }

    private void drawableResultPoint(float tipsYPoint) {
        Log.d(TAG, "drawableResultPoint---start");
//        iv_show_result.setImageBitmap(barcodeBitmap);
        removeAllPoints();
        if (resultPoint == null || resultPoint.size() == 0) {
            if (onResultPointClickListener != null) {
                onResultPointClickListener.onCancel();
            }
            return;
        }
        if (scanConfig == null) {
            scanConfig = new MNScanConfig.Builder().build();
        }

        if (resultPoint.size() == 1){
            rl_tips.setVisibility(GONE);
        } else {
            rl_tips.setVisibility(VISIBLE);
        }

        float textY = 0f;
        for (int j = 0; j < resultPoint.size(); j++) {
            Barcode barcode = resultPoint.get(j);
            Rect boundingBox = barcode.getBoundingBox();
            int centerX = boundingBox.centerX();
            int centerY = boundingBox.centerY();

            View inflate = LayoutInflater.from(getContext()).inflate(R.layout.mn_scan_result_point_item_view, null);
            RelativeLayout rl_root = inflate.findViewById(R.id.rl_root);
//            ImageView iv_point_bg = inflate.findViewById(R.id.iv_point_bg);
//            ImageView iv_point_arrow = inflate.findViewById(R.id.iv_point_arrow);

            //位置
            RelativeLayout.LayoutParams lpRoot = new RelativeLayout.LayoutParams(resultPointWithdHeight, resultPointWithdHeight);
            rl_root.setLayoutParams(lpRoot);
            rl_root.setX(centerX - resultPointWithdHeight / 2.0f);
            rl_root.setY(centerY - resultPointWithdHeight / 2.0f);

            textY = Math.max(rl_root.getY(),textY);
//
//            GradientDrawable gradientDrawable = new GradientDrawable();
//            gradientDrawable.setCornerRadius(resultPointRadiusCorners);
//            gradientDrawable.setShape(RECTANGLE);
//            gradientDrawable.setStroke(resultPointStrokeWidth, resultPointStrokeColor);
//            gradientDrawable.setColor(resultPointColor);

//            iv_point_bg.setImageDrawable(gradientDrawable);
//
//            //点的大小
//            ViewGroup.LayoutParams lpPoint = iv_point_bg.getLayoutParams();
//            lpPoint.width = resultPointWithdHeight;
//            lpPoint.height = resultPointWithdHeight;
//            iv_point_bg.setLayoutParams(lpPoint);

//            //箭头大小
//            if (resultPoint.size() > 1) {
//                ViewGroup.LayoutParams lpArrow = iv_point_arrow.getLayoutParams();
//                lpArrow.width = resultPointWithdHeight / 2;
//                lpArrow.height = resultPointWithdHeight / 2;
//                iv_point_arrow.setLayoutParams(lpArrow);
//                iv_point_arrow.setVisibility(View.VISIBLE);
//            } else {
//                //一个不需要箭头
//                iv_point_arrow.setVisibility(View.GONE);
//            }

            rl_root.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onResultPointClickListener != null) {
                        onResultPointClickListener.onPointClick(barcode.getRawValue());
                    }
                }
            });

            fl_result_point_root.addView(inflate);
        }

        RelativeLayout.LayoutParams lpRoot = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rl_tips.setLayoutParams(lpRoot);
        rl_tips.setX(0);
        rl_tips.setY(tipsYPoint + statusBarHeight);
//        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        if (tipsYPoint == 0f) {
//            tipsYPoint = Math.min(textY + 300, displayMetrics.heightPixels - 200);
//        }

        int childCount = fl_result_point_root.getChildCount();
//        Log.d(TAG, "fl_result_point_root---childCount：" + childCount);
        if (childCount <= 0) {
            //关闭页面
            if (onResultPointClickListener != null) {
                onResultPointClickListener.onCancel();
            }
        }
//        Log.d(TAG, "drawableResultPoint---end");
    }

    public void setMultiScanTips(String tips) {
        if (tv_tips != null) {
            tv_tips.setText(tips);
        }
    }

}