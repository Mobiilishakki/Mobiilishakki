package fi.helsinki.mobiilishakki;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
// import android.support.annotation.NonNull;
// import android.support.v4.app.ActivityCompat;
// import android.support.v7.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class AndroidCameraApi extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private TextToSpeech speaker;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private  ImageView mImageView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);


        mImageView = (ImageView) findViewById(R.id.board_coord);


        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });


        speaker =new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i==TextToSpeech.SUCCESS) {
                    speaker.setLanguage(Locale.ENGLISH);
                }
            }
        });
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {

        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 1800;
            int height = 2800;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


            //final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {

                    Image image = null;
                    OutputStream output=null;

                   // ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                   // Bitmap bitmap;
                    //bitmap = new Bitmap();
                   // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//                    file = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
//                    try {
//                        FileOutputStream fo = new FileOutputStream(file);
//                        fo.write(bytes.toByteArray());
//                        fo.flush();
//                        fo.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    ByteArrayOutputStream byteoutput =null;
                    BufferedWriter bufferedWriter=null;
                    File fileToSend =null;
                    try {

                        System.out.println("eka");
                        image = reader.acquireLatestImage();
                        System.out.println("toka");
                        fileToSend = File.createTempFile("Chess",".jpg");
                        System.out.println("kolmas");
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];

                        buffer.rewind();
                        buffer.get(bytes);
                        //System.out.println("viides");
                        //byte[] bytes = buffer.array();


                       // buffer.get(bytes);


                       // bufferedWriter = new BufferedWriter(new FileWriter(fileToSend, false));
                       // bufferedWriter.write(buffer.asCharBuffer().array());





                        output = new FileOutputStream(fileToSend);
                        output.write(bytes);
                        output.flush();
                        output.close();

                        image.close();

                        System.out.println("PERKEL "+fileToSend.getTotalSpace());
                        sendFile(fileToSend);
                    } catch (Exception e){
                        e.printStackTrace();

                    }


                    /*
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }*/
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(AndroidCameraApi.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(AndroidCameraApi.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(AndroidCameraApi.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(AndroidCameraApi.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    public void speak(String line){

        speaker.speak(line, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void sendFile(File fileToSend) {

       // File file = new File(this.getFilesDir(), "tiedosto");
        //file = fileToSend;
      //  System.out.println("KOKOKO "+fileToSend.length);

//       showPicture(fileToSend);


        RequestParams params = new RequestParams();
        String url ="http://94.237.117.223/upload";
//        String url ="http://192.168.42.113/upload";

        try {
            params.put("file", fileToSend);
        } catch(Exception e){

        }
        System.out.println("JIIPEEGEE " +fileToSend.length() +" LOPPUOSA " + fileToSend.toString());
            // send request
        AsyncHttpClient client = new AsyncHttpClient();
       // Toast.makeText(AndroidCameraApi.this, "testin vuoksi", Toast.LENGTH_LONG).show();
        client.setTimeout(50000);
        client.post(url,params,new AsyncHttpResponseHandler(){
            @Override
            public void onSuccess (int statusCode, Header[] headers, byte[] bytes){
                //fileToSend.delete();
                System.out.println("MAKKARA");
                String byt=new String(bytes);
                String koo=headers.toString()+"__"+byt;
                Toast.makeText(AndroidCameraApi.this, koo, Toast.LENGTH_LONG).show();
                //drawResult("success");
            }

            @Override
            public void onFailure (int statusCode, Header[] headers, byte[] bytes, Throwable throwable){
                System.out.println("KALAKALA");
                //drawResult("fucked");
                String byt=new String(bytes);
                String koo=headers.toString()+"__"+byt;
                Toast.makeText(AndroidCameraApi.this, "Fail "+ koo, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void showPicture(File picture){

        /*
        this.textureView.setEnabled(false);
        this.closeCamera();

        //setContentView(R.layout.activity_menu);

       float[] NEGATIVE = {
                -1.0f,     0,     0,    0, 255, // red
                0, -1.0f,     0,    0, 255, // green
                0,     0, -1.0f,    0, 255, // blue
                0,     0,     0, 1.0f,   0  // alpha
        };

        */
      //  File chess=new File(getDataDir()+"/chess.jpg");


        //System.out.println("CHESSCHESS " + chess.exists());


    //    ImageView mImageView;

    //    mImageView = (ImageView) findViewById(R.id.board_coord);
        //mImageView.setImageBitmap(BitmapFactory.decodeFile(chess.getAbsolutePath()));

        AssetManager assetManager = getAssets();
        InputStream is = null;
        try {
            is = assetManager.open("chess.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mImageView.setImageBitmap(BitmapFactory.decodeStream(is));



 //       this.runOnUiThread(java.lang.Runnable{
 //           mImageView.setImageBitmap(BitmapFactory.decodeStream(is))
 //       });



    //mImageView.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
        mImageView.setVisibility(View.VISIBLE);


    }

    public void drawResult(byte[] bytes){
        TextView textView_res = findViewById(R.id.img_result);
        try {
            String res = new String(bytes, "UTF-8");
            textView_res.setText(res);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            textView_res.setText("ERROR");
        }
        textView_res.setVisibility(View.VISIBLE);
    }
    public void drawResult(String text){
        TextView textView_res = findViewById(R.id.img_result);
        textView_res.setText(text);
        textView_res.setVisibility(View.VISIBLE);
    }

}







//import androidx.appcompat.app.AppCompatActivity;
//
//import android.graphics.Bitmap;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.WindowManager;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//
//import org.opencv.android.OpenCVLoader;
//
//public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {
//
//    // Used for logging success or failure messages
//    private static final String TAG = "MainActivity";
//    // Camera object
//    private Camera mCamera;
//    // Preview object
//    private CameraPreview mPreview;
//    // Thread to process frame data
//    private Thread ocvThread;
//    // Chessboard analyzer
//    private ChessboardAnalyzer cbAnalyzer;
//    // ImageView
//    private ImageView imageView;
//
//    // Load openCV before onCreate
//    static {
//        if (!OpenCVLoader.initDebug()) {
//            // Handle initialization error
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "Called onCreate!");
//        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setContentView(R.layout.activity_main);
//
//        // Create an instance of Camera
//        mCamera = getCameraInstance();
//
//        // Set camera to continually auto-focus
//        Camera.Parameters params = mCamera.getParameters();
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        params.setPreviewSize(1280, 960);  // BAD --> USE get supportedPreviewSize
//        mCamera.setParameters(params);
//
//        // Create our preview view and set it as the content of our activity
//        mPreview = new CameraPreview(this, mCamera, this);
//
//        mPreview.g
//        FrameLayout preview = findViewById(R.id.camera_preview);
//        preview.addView(mPreview);
//
//        // Create new chessboard analyzer
//        cbAnalyzer = new ChessboardAnalyzer();
//
//        // Set ImageVIew
//        imageView = findViewById(R.id.imageView);
//
//    }
//
//    /**
//     * Get camera instance. Returns null if instance could not be opened.
//     *
//     * @return camera
//     */
//    public static Camera getCameraInstance() {
//        Camera c = null;
//        try {
//            c = Camera.open();  // attempt to get a Camera instance
//        } catch (Exception e) {
//            // Camera is not available (in use or does not exist)
//        }
//        return c;   // returns null if camera is unavailable
//    }
//
//    @Override
//    public void onPreviewFrame(byte[] bytes, Camera camera) {
//        Log.i(TAG, "Called onPreviewFrame!");
//
//        // If thread does not exist or is dead --> create new one
//        if (ocvThread == null || ocvThread.isAlive() == false) {
//            Bitmap bitmap = cbAnalyzer.getFrameInBitmap();
//            if (bitmap != null) {
//                imageView.setImageBitmap(bitmap);
//            }
//            // Update frame data to chessboard analyzer
//            cbAnalyzer.setFrameBytes(bytes, mCamera.getParameters());
//            // Create new thread and run it
//            ocvThread = new Thread(cbAnalyzer);
//            ocvThread.start();
//        }
//    }
//
//}
//
