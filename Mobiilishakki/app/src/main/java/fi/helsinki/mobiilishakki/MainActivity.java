package fi.helsinki.mobiilishakki;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.LINE_8;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.arcLength;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.isContourConvex;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Latest frame that was processed
    private Mat lastFrame;
    // Used for logging success or failure messages
    private static final String TAG = "OpenCVTest::Activity";
    // Loads camera view of OpenCV to use.
    private CameraBridgeViewBase cameraBridgeViewBase;
    // OpenCV manager to help our app communicate with android phone to make OpenCV work
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    private int skipCounter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.cameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.enableFpsMeter();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // performance boost --> skip frames to cut the load
        if(this.lastFrame != null){
            skipCounter++;
            if(skipCounter > 15) {
                skipCounter = 0;
                return lastFrame;
            }
        }

        // Mat object for holding rgb frame
        Mat rgbFrame = inputFrame.rgba();

        // Mat object for holding gray frame
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(rgbFrame, grayFrame, COLOR_BGR2GRAY);
        // Mat object for holding the result of edge detection (Canny)
        Mat edges = new Mat();
        Imgproc.Canny(grayFrame, edges, 50, 150, 3, false);

        // Find contours from the frame
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        // performance boost --> if small amount of contours --> cant be chessboard
        if(contours.size() < 40) {
            return rgbFrame;
        }

        // for each contour found
        for (int i = 0; i < contours.size(); i++){

            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double approxDistance = arcLength(contour2f, true) * 0.02;

            if(approxDistance > 1){
                approxPolyDP(contour2f, approxCurve, approxDistance, true);
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                // Rectangle checks
                if (points.total() == 4 && Math.abs(contourArea(points)) > 1000 && isContourConvex(points)){
                    double cos = 0;
                    double mcos = 0;
                    for(int sc = 2; sc < 5; sc++) {
                        if(cos > mcos) {
                            mcos = cos;
                        }
                    }
                    if(mcos < 0.3) {

                        drawContours(rgbFrame, contours, i, new Scalar(255, 0, 0), 2, LINE_8, hierarchy, 0, new Point());
                    }
                }
            }
        }
        this.lastFrame = rgbFrame;
        return rgbFrame;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }else{
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

}
