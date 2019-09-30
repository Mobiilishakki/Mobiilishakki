package fi.helsinki.mobiilishakki;


import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {

    // Used for logging success or failure messages
    private static final String TAG = "MainActivity";
    // Camera object
    private Camera mCamera;
    // Preview object
    private CameraPreview mPreview;
    // Thread to process frame data
    private Thread ocvThread;
    // Chessboard analyzer
    private ChessboardAnalyzer cbAnalyzer;
    // ImageView
    private ImageView imageView;

    // Load openCV before onCreate
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Called onCreate!");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Set camera to continually auto-focus
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        params.setPreviewSize(1280, 960);  // BAD --> USE get supportedPreviewSize
        mCamera.setParameters(params);

        // Create our preview view and set it as the content of our activity
        mPreview = new CameraPreview(this, mCamera, this);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Create new chessboard analyzer
        cbAnalyzer = new ChessboardAnalyzer();

        // Set ImageVIew
        imageView = findViewById(R.id.imageView);

    }

    /**
     * Get camera instance. Returns null if instance could not be opened.
     *
     * @return camera
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();  // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c;   // returns null if camera is unavailable
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        Log.i(TAG, "Called onPreviewFrame!");

        // If thread does not exist or is dead --> create new one
        if (ocvThread == null || ocvThread.isAlive() == false) {
            Bitmap bitmap = cbAnalyzer.getFrameInBitmap();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            // Update frame data to chessboard analyzer
            cbAnalyzer.setFrameBytes(bytes, mCamera.getParameters());
            // Create new thread and run it
            ocvThread = new Thread(cbAnalyzer);
            ocvThread.start();
        }
    }

}

