package com.maning.mlkitscanner.demo

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.maning.mlkitscanner.scan.MNScanManager
import com.maning.mlkitscanner.scan.callback.MNCustomViewBindCallback
import com.maning.mlkitscanner.scan.callback.act.MNScanCallback
import com.maning.mlkitscanner.scan.model.MNScanConfig

class MultiScanUtils {

    companion object {
        const val TAG = "MultiScanUtils"
        private const val colorText = "#22CE6B"
        private const val colorLine = "#22CE6B"
        private const val colorBackground = "#22FF0000"
        private const val colorStatusBar = "#00000000"
        private const val colorResultPoint = "#CC22CE6B"
        private const val colorResultPointStroke = "#FFFFFFFF"

        fun startSuperScan(activity: Activity, grid: Boolean, fullScreen: Boolean) {
            //需要判断有没有权限
            val scanConfig = MNScanConfig.Builder()
                //设置完成震动
                .isShowVibrate(true)
                //扫描完成声音
                .isShowBeep(true)
                //显示相册功能
                .isShowPhotoAlbum(false)
                //显示闪光灯
                .isShowLightController(true)
                //打开扫描页面的动画
//                .setActivityOpenAnime(R.anim.setting_right_in)
//            //退出扫描页面动画
//                .setActivityExitAnime(R.anim.setting_right_out)
                //自定义文案
//            .setScanHintText("")
//            .setScanHintTextColor(colorText)
//            .setScanHintTextSize(14)
                //扫描线的颜色
                .setScanColor(colorLine)
                //是否支持手势缩放
//            .setSupportZoom(false)
                //扫描线样式
                .setLaserStyle(if (grid) MNScanConfig.LaserStyle.Grid else MNScanConfig.LaserStyle.Line) //背景颜色
//            .setBgColor(colorBackground)
                //网格扫描线的列数
//            .setGridScanLineColumn(30)
                // 网格高度
//            .setGridScanLineHeight(0)
                //是否全屏扫描,默认全屏
                .setFullScreenScan(fullScreen)
                //单位dp
                .setResultPointConfigs(36, 12, 3, colorResultPointStroke, colorResultPoint)
                //状态栏设置
//            .setStatusBarConfigs(colorStatusBar, mCbStatusDark.isChecked())
                //绘制扫描框
                .setDrawScanBox(false)
                //自定义遮罩
                .setCustomShadeViewLayoutID(
                    R.layout.custom_scan_action_menu,
                    MNCustomViewBindCallback { customView ->
                        if (customView == null) {
                            return@MNCustomViewBindCallback
                        }
                        var scanBoxView = customView.findViewById<ScanBoxView>(R.id.scan_box)
                        var scanTips = customView.findViewById<LinearLayout>(R.id.layout_scan_tips)
                        var back = customView.findViewById<RelativeLayout>(R.id.layout_back)
                        var tvTitle = customView.findViewById<TextView>(R.id.tv_title)
                        var inputNumber = customView.findViewById<ImageView>(R.id.iv_input_number)
                        var ivFlashLight = customView.findViewById<ImageView>(R.id.iv_flash_light)
                        var tv_flash_light = customView.findViewById<TextView>(R.id.tv_flash_light)

                        back.setOnClickListener(View.OnClickListener {
                            //关闭扫描页面
                            MNScanManager.closeScanPage()
                        })

                        ivFlashLight.setOnClickListener(View.OnClickListener {
                            //手电筒
                            if (MNScanManager.isLightOn()) {
                                MNScanManager.closeScanLight()
                            } else {
                                MNScanManager.openScanLight()
                            }
                            ivFlashLight.setSelected(MNScanManager.isLightOn())
                            tv_flash_light.setText(
                                if (MNScanManager.isLightOn()) activity.getString(R.string.flash_light_close) else activity.getString(
                                    R.string.flash_light_open
                                )
                            )
                        })
                        Handler().postDelayed(Runnable {
                            scanTips.setY((scanBoxView.getScanBoxRect().bottom + scanBoxView.marginText).toFloat())
                            scanTips.setVisibility(View.VISIBLE)
                        }, 200)
                    })
                .build()
            MNScanManager.startScan(activity, scanConfig,
                MNScanCallback { resultCode, data -> handlerResult(resultCode, data) })
        }


        /**
         * 默认扫码
         */
        fun startScan(activity: Activity) {
            MNScanManager.startScan(
                activity
            ) { resultCode, data ->
                handlerResult(resultCode, data);
            }
        }

        private fun handlerResult(resultCode: Int, data: Intent?) {
            if (data == null) {
                return
            }
            when (resultCode) {
                MNScanManager.RESULT_SUCCESS -> {
                    val results =
                        data.getStringArrayListExtra(MNScanManager.INTENT_KEY_RESULT_SUCCESS)
                    val resultStr = StringBuilder()
                    var i = 0
                    while (i < results!!.size) {
                        resultStr.append("第" + (i + 1) + "条：")
                        resultStr.append(results[i])
                        resultStr.append("\n")
                        i++
                    }
                }
                MNScanManager.RESULT_FAIL -> {
                    val resultError = data.getStringExtra(MNScanManager.INTENT_KEY_RESULT_ERROR)
                }
                MNScanManager.RESULT_CANCLE -> {
                    Log.d(TAG, "取消扫码")
                }
                else -> {}
            }
        }

    }


}