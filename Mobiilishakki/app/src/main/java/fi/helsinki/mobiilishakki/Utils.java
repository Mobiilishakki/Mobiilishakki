package fi.helsinki.mobiilishakki;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;

import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

public class Utils {

    private static final String TAG = "Utils";    // Debug tag string

    /**
     * Convert byte array to openCV Mat object.
     * The data should be NV21 encoded (android default for rear camera).
     * @param data
     * @param params
     * @return Mat
     */
    public static Mat bytesToMatConversion(byte[] data, Camera.Parameters params) {
        Log.i(TAG, "Called bytesToMatConversion!");

        // Convert first to YuvImage
        int width = params.getPreviewSize().width;
        int height = params.getPreviewSize().height;
        YuvImage yuv = new YuvImage(data, params.getPreviewFormat(), width, height, null);

        // Convert YuvImage to BitMap
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 70, out);
        Bitmap bmp = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());

        // Convert Bitmap to Mat; note the bitmap config ARGB_8888 conversion that
        // allows you to use other image processing methods and still save at the end
        Mat orig = new Mat();
        bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        org.opencv.android.Utils.bitmapToMat(bmp, orig);
        return orig;
    }

}
