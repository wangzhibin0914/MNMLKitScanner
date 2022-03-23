package com.maning.mlkitscanner.scan.callback;

import android.view.View;

import com.google.mlkit.vision.barcode.Barcode;

public interface BarcodeAnalyserResultCallback {
    void barcodeTooSmall();//太小
    void barcodeOutOfBound(Barcode barcode);//超出扫码框
}
