package fi.helsinki.mobiilishakki;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
// import android.support.annotation.NonNull;
// import android.support.v4.app.ActivityCompat;
// import android.support.v7.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private Button startAsWhiteButton;
    private Button startAsBlackButton;
    private TextureView textureView;
    private DrawView drawView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private TextToSpeech speaker;
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private int DSI_height=0;
    private int DSI_width=0;

    int cameraPictureWidth = 0;
    int cameraPictureHeight = 0;

    private static boolean blockPolling=false;

    private static boolean isPlayerWhite;


    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;

    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private  ImageView mImageView;

    private Size imageDimension;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();


        textureView = (TextureView) findViewById(R.id.texture);

        drawView=(DrawView) findViewById(R.id.drawView);

        drawView.setZ(100);




        mImageView = (ImageView) findViewById(R.id.board_coord);


        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        startAsWhiteButton = (Button) findViewById(R.id.btn_startAsWhite);
        startAsWhiteButton.setZ(101);
        startAsWhiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlayerWhite=true;
                startGame();
            }
        });
        startAsBlackButton = (Button) findViewById(R.id.btn_startAsBlack);
        startAsBlackButton.setZ(101);
        startAsBlackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlayerWhite=false;
                startGame();
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

    private void setAspectRatioTextureView(int ResolutionWidth , int ResolutionHeight )
    {
        if(ResolutionWidth > ResolutionHeight){
            int newWidth = DSI_width;
            int newHeight = ((DSI_width * ResolutionWidth)/ResolutionHeight);
            updateTextureViewSize(newWidth,newHeight);

        }else {
            int newWidth = DSI_width;
            int newHeight = ((DSI_width * ResolutionHeight)/ResolutionWidth);
            updateTextureViewSize(newWidth,newHeight);
        }

    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d(TAG, "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));

        ViewGroup.LayoutParams params=drawView.getLayoutParams();
        params.height=viewHeight;
        drawView.setLayoutParams(params);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here

            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface cameraPictureWidth and cameraPictureHeight
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

    public void startGame() {

            final Handler handler = new Handler();

             Runnable runnable = new Runnable() {

                public void run() {

                    if(blockPolling==false)
                        pollServer(isPlayerWhite);

                    handler.postDelayed(this, 1000);

                }
            };

        runnable.run();

    }

    public void sendPicture(){

        System.out.println("SEND SEND SEND PICTURE PICTURE");
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

            if (jpegSizes != null && 0 < jpegSizes.length) {
                cameraPictureWidth = jpegSizes[0].getWidth();
                cameraPictureHeight = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(cameraPictureWidth, cameraPictureHeight, ImageFormat.JPEG, 2);
            //ImageReader reader = ImageReader.newInstance(cameraPictureWidth, cameraPictureHeight, ImageFormat.RAW12, 2);

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
                        image = reader.acquireLatestImage();
                        fileToSend = File.createTempFile("Chess",".jpg");


                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];

                        buffer.rewind();
                        buffer.get(bytes);



                        //System.out.println("viides");
                        //byte[] bytes = buffer.array();


                       // buffer.get(bytes);


                       // bufferedWriter = new BufferedWriter(new FileWriter(fileToSend, false));
                       // bufferedWriter.write(buffer.asCharBuffer().array());


                        //Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);





                        output = new FileOutputStream(fileToSend);
                        //bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, output);
                        //cropImage(bitmapImage).compress(Bitmap.CompressFormat.JPEG, 100, output);


                        output.write(bytes);
                        output.flush();
                        output.close();

                        System.out.println("SENDFILE SENDFILE SENDFILE");
                        sendFile(fileToSend);

/*
                        // BitmapFactory.Options options = new BitmapFactory.Options();
                       // options.inJustDecodeBounds = true;
                        Bitmap bitmapImage=BitmapFactory.decodeFile(fileToSend.getAbsolutePath());

                        File fileToSend2 = File.createTempFile("Chess2",".jpg");
                        OutputStream output2=new FileOutputStream(fileToSend2);
                        bitmapImage=cropImage90(bitmapImage);
                        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, output2);

                        image.close();
                        bitmapImage.recycle();
                        output2.flush();
                        output2.close();
                        System.out.println("PERKEL "+fileToSend.getTotalSpace());
                        sendFile(fileToSend2);

*/


                    } catch (Exception e){
                        e.printStackTrace();

                    }

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

            // This should help with dark images on certain smartphones
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());//This line of code is used for adjusting the fps range and fixing the dark preview
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);



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

    private Range<Integer> getRange() {
        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics chars = null;
        try {
            chars = mCameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        Range<Integer> result = null;

        for (Range<Integer> range : ranges) {
            int upper = range.getUpper();

            // 10 - min range upper for my needs
            if (upper >= 20) {
                if (result == null || upper < result.getUpper().intValue()) {
                    result = range;
                }
            }
        }
        return result;
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

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            DSI_height = displayMetrics.heightPixels;
            DSI_width = displayMetrics.widthPixels;
            setAspectRatioTextureView(imageDimension.getHeight(),imageDimension.getWidth());
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

    public void cropImage(Image orig){
        Rect rect=new Rect(drawView.topLeft().x,drawView.topLeft().y,drawView.bottomRight().x,drawView.bottomRight().y);
        System.out.println("WWEWEWWEWEWEWE left "+drawView.topLeft().x+" top "+drawView.topLeft().y+" right "+drawView.bottomRight().x+" bottom "+drawView.bottomRight().y);
        orig.setCropRect(rect);

    }

    // Crops bitmap based on drawView points
    public Bitmap cropImage(Bitmap bitmap){

        if(DSI_height==0|this.DSI_width==0|this.cameraPictureHeight==0|this.cameraPictureWidth==0)
            System.out.println("SOMETHING WENT WRONG");


        float ratioWidth=(float) bitmap.getWidth() /(float)DSI_width;
        float ratioHeight=(float) bitmap.getHeight() /(float)DSI_height;
        float ratio;
        if(ratioHeight<ratioWidth)
            ratio=ratioHeight;
        else
            ratio=ratioWidth;

        float topleftx=ratio * (float)drawView.topLeft().x;
        float toplefty=ratio * (float)drawView.topLeft().y;
        float toprightx=ratio * (float)drawView.topRight().x;
        float toprighty=ratio * (float)drawView.topRight().y;
        float bottomleftx=ratio * (float)drawView.bottomLeft().x;
        float bottomlefty=ratio * (float)drawView.bottomLeft().y;
        float bottomrightx=ratio * (float)drawView.bottomRight().x;
        float bottomrighty=ratio * (float)drawView.bottomRight().y;


        float width = toprightx-topleftx;
        float height= bottomlefty-toplefty;

        return Bitmap.createBitmap(bitmap, (int)topleftx, (int)toplefty, (int)width, (int)height); //, matrix, true);

    }


    // Crops bitmap based on drawView points
    public Bitmap cropImage90s(Bitmap bitmap){

        if(DSI_height==0|this.DSI_width==0|this.cameraPictureHeight==0|this.cameraPictureWidth==0)
            System.out.println("SOMETHING WENT WRONG");


        float ratioWidth=(float) bitmap.getHeight() /(float)DSI_width;
        float ratioHeight=(float) bitmap.getWidth() /(float)DSI_height;

        System.out.println("BITMAP W "+bitmap.getWidth()+" DSI W "+DSI_width+"  BITMAP H "+bitmap.getHeight()+ "  DSI H "+DSI_height);
        System.out.println("RATIO W "+ratioWidth+"  RATIO H "+ratioHeight);

        float ratio;
        if(ratioHeight>ratioWidth)
            ratio=ratioHeight;
        else
            ratio=ratioWidth;

        float topleftx=ratio * (float)drawView.topLeft().x;
        float toplefty=ratio * (float)drawView.topLeft().y;
        float toprightx=ratio * (float)drawView.topRight().x;
        float toprighty=ratio * (float)drawView.topRight().y;
        float bottomleftx=ratio * (float)drawView.bottomLeft().x;
        float bottomlefty=ratio * (float)drawView.bottomLeft().y;
        float bottomrightx=ratio * (float)drawView.bottomRight().x;
        float bottomrighty=ratio * (float)drawView.bottomRight().y;

/*      float rotatedTopLeftX=toprighty;
        float rotatedTopLeftY=bitmap.getHeight()-toprightx;
        float rotatedBottomRightX=bottomlefty;
        float rotatedBottomRightY=bitmap.getHeight()-bottomleftx;

        float rotatedTopRightX=bottomrighty;
        float rotatedTopRightY=bitmap.getHeight()-bottomrightx;
        float rotatedBottomLeftX=toplefty;
        float rotatedBottomLeftY=bitmap.getHeight()-topleftx;
*/

        float rotatedTopLeftX=topleftx; float rotatedTopLeftY=toplefty; float rotatedTopRightX=toprightx;
        float rotatedTopRightY=toprighty; float rotatedBottomRightX=bottomrightx; float rotatedBottomRightY=bottomrighty;
        float rotatedBottomLeftX=bottomleftx; float rotatedBottomLeftY=bottomlefty;

        float width = rotatedBottomRightX-rotatedTopLeftX;
        float height= rotatedBottomRightY-rotatedTopLeftY;


        float[] src = new float[8];
        src[0] = rotatedTopLeftX;
        src[1] = rotatedTopLeftY;
        src[2] = rotatedTopRightX;
        src[3] = rotatedTopRightY;
        src[4] = rotatedBottomRightX;
        src[5] = rotatedBottomRightY;
        src[6] = rotatedBottomLeftX;
        src[7] = rotatedBottomLeftY;

        float dstWidth,dstheight;
        if((rotatedBottomRightX-rotatedBottomLeftX)>(rotatedTopRightX-rotatedTopLeftX))
            dstWidth=(rotatedBottomRightX-rotatedBottomLeftX);
        else
            dstWidth=(rotatedTopRightX-rotatedTopLeftX);

        if((rotatedBottomRightY-rotatedTopRightY)>(rotatedBottomLeftY-rotatedTopLeftY))
            dstheight=(rotatedBottomRightY-rotatedTopRightY);
        else
            dstheight=(rotatedBottomLeftY-rotatedTopLeftY);

        float startx,starty;

        if(rotatedBottomLeftX<rotatedTopLeftX) {
            startx=rotatedBottomLeftX;
            if (rotatedBottomRightX < rotatedTopRightX) {
                width = rotatedTopRightX - rotatedBottomLeftX;}
            else{
                width = rotatedBottomRightX - rotatedBottomLeftX;
            }
        }
        else{
            startx=rotatedTopLeftX;
            if (rotatedBottomRightX < rotatedTopRightX) {
                width = rotatedTopRightX - rotatedTopLeftX;}
            else{
                width = rotatedBottomRightX - rotatedTopLeftX;
            }
        }
        if(rotatedBottomLeftY<rotatedBottomRightY) {

            if (rotatedTopLeftY<rotatedTopRightY) {
                starty=rotatedTopLeftY;
                height = rotatedBottomRightY - rotatedTopLeftY;}
            else{
                starty=rotatedTopRightY;
                height = rotatedBottomRightY - rotatedTopRightY;
            }
        }
        else{

            if (rotatedTopLeftY<rotatedTopRightY) {
                starty=rotatedTopLeftY;
                height = rotatedBottomLeftY - rotatedTopLeftY;}
            else{
                starty=rotatedTopRightY;
                height = rotatedBottomLeftY - rotatedTopRightY;
            }
        }


        float[] dst = new float[8];
        dst[0] = 0;
        dst[1] = 0;
        dst[2] = width;
        dst[3] = 0;
        dst[4] = width;
        dst[5] = height;
        dst[6] = 0;
        dst[7] = height;

        Matrix matrix = new Matrix();
        boolean mapped = matrix.setPolyToPoly(src, 0, dst, 0, 4);

        System.out.println(" TOIMIIKO POLYPOLY "+mapped);

        System.out.println("topLX  " + topleftx+" topLY " + toplefty +" topRX " + toprightx+" topRY " + toprighty);
        System.out.println(" BotLX " + bottomleftx+" botLY " + bottomlefty+" botRX " + bottomrightx+" botRY " + bottomrighty);

        System.out.println("yllä alkup koordinaatit, alla rotatoidut");
        System.out.println("topLX  " + rotatedTopLeftX+" topLY " + rotatedTopLeftY +" topRX " + rotatedTopRightX+" topRY " + rotatedTopRightY);
        System.out.println(" BotLX " + rotatedBottomLeftX+" botLY " + rotatedBottomLeftY+" botRX " + rotatedBottomRightX+" botRY " + rotatedBottomRightY);
        System.out.println(" Width " + width+" Height " + height+" startX " + startx+" startY " + starty);


        Matrix matrix2 = new Matrix();
        matrix2.postRotate(90);

        Bitmap rotatedBitmap=Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix2, true);
        return Bitmap.createBitmap(rotatedBitmap, (int)rotatedTopLeftX,(int)rotatedTopLeftY, (int)width, (int) height, matrix, true);

       // return changePerspective(new Point(topleftx,toplefty), new Point(toprightx,toprighty),new Point(bottomrightx,bottomrighty),new Point(bottomleftx,bottomlefty),rotatedBitmap);

        // return Bitmap.createBitmap((Bitmap.createBitmap(bitmap, (int)startx,(int)starty, (int)width, (int) height, matrix, true)),0,0,(int)width, (int) height, matrix2, true);
        //return Bitmap.createBitmap(bitmap, (int)startx,(int)starty, (int)width, (int) height, matrix, true);
        // return Bitmap.createBitmap(bitmap, 0, 0, (int)dstWidth, (int)dstheight, matrix, true);

        //bitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix2,true);
       // Bitmap dstBitmap = Bitmap.createBitmap((int)width, (int) height, Bitmap.Config.RGB_565);
       // Canvas canvas = new Canvas(dstBitmap);
        //canvas.clipRect(0, 0, (int)width, (int) height);
       // canvas.drawBitmap(bitmap, matrix, null);
       // return dstBitmap;
    }


    // Crops bitmaps that are horizontal, and transforms bitmaps to vertical.
    // Takes coordinates from drawView that are correct on the original vertical picture
    // so it uses them in a way that is correct in horizontal bitmap
    public Bitmap cropImage90(Bitmap bitmap){

        System.out.println("ÄXÄ "+drawView.topLeft().x+"YXY "+drawView.topLeft().y +" TOKA X "+drawView.bottomRight().x+" TOKE Y "+drawView.bottomRight().y);
        float ratio2=(float) cameraPictureWidth  /(float)DSI_height;
        float ratio=(float) cameraPictureHeight/(float)DSI_width;
        System.out.println(" camwidth "+cameraPictureWidth+"  camheight "+cameraPictureHeight +"  DSIwidth "+DSI_width+"  DSIheight "+DSI_height);
        System.out.println(" RATIORATIO "+ ratio2+ "  RATIO width "+ratio);
        float topLeftX,topLeftY,bottomRightX,bottomRightY;
        topLeftX=ratio * (float)drawView.topLeft().x;
        topLeftY=ratio * (float)drawView.topLeft().y;
        bottomRightX=ratio * (float)drawView.bottomRight().x;
        bottomRightY=ratio * (float)drawView.bottomRight().y;


        int rotatedTopLeftX=(int)topLeftY;
        int rotatedTopLeftY=(int)(cameraPictureHeight-bottomRightX);
        int rotatedBottomRightX=(int)bottomRightY;
        int rotatedBottomRightY=(int)(cameraPictureHeight-topLeftX);
        int height=rotatedBottomRightY-rotatedTopLeftY;
        int width=rotatedBottomRightX-rotatedTopLeftX;

        System.out.println(" RTLX "+rotatedTopLeftX+" RTLY "+rotatedTopLeftY+" RBRX "+rotatedBottomRightX
                + " RBRY "+rotatedBottomRightY+" weight "+height+" width "+width);
        System.out.println(" DSL Height "+DSI_height + " DSL WIDTH "+DSI_width + " cameraPictureWidth "+width+" hieght " + height);
        System.out.println("Äksä "+rotatedTopLeftX + " YYYY " + rotatedTopLeftY+ " LEVEYS " +width + " KORKEUS " + height);


        Matrix matrix = new Matrix();

        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, rotatedTopLeftX,rotatedTopLeftY, width, height, matrix, true);

    }

    private static  Bitmap changePerspective(Point topLeft, Point topRight, Point lowRight, Point lowLeft, Bitmap bitmap){

        Mat source=new Mat();
        Mat destination=new Mat();
        //bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        org.opencv.android.Utils.bitmapToMat(bitmap, source);

        MatOfPoint2f sourcePoints=new MatOfPoint2f(topLeft, topRight, lowRight, lowLeft);
        int width;
        if(source.width()>source.height()){
            width=source.height();
        } else {
            width=source.width();
        }
        System.out.println("LEVEYS = "+width);
        MatOfPoint2f goalPoints=new MatOfPoint2f(new Point(0,0), new Point(width-1,0), new Point(width-1, width-1),new Point(0,width-1));

        Mat transform= Imgproc.getPerspectiveTransform(sourcePoints,goalPoints);

        Imgproc.warpPerspective(source, destination, transform, new org.opencv.core.Size(source.width(), source.height()));

        org.opencv.android.Utils.matToBitmap(destination,bitmap);
        return bitmap;
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



    public boolean pollServer(final boolean isWhite){
        RequestParams params = new RequestParams();
        String url ="http://94.237.117.223/snapshot";
        //String url ="http://192.168.43.133/snapshot";
        AsyncHttpClient client = new AsyncHttpClient();
        final boolean[] pollResult=new boolean[1];
        pollResult[0]=false;
        // Toast.makeText(AndroidCameraApi.this, "testin vuoksi", Toast.LENGTH_LONG).show();
        client.setTimeout(333);
        client.setResponseTimeout(333);
        client.get(url,new AsyncHttpResponseHandler(){
            @Override
            public void onSuccess (int statusCode, Header[] headers, byte[] bytes){
                //fileToSend.delete();
                String ko=new String(bytes);
                System.out.println("POLLAUKSESTA TAKAISIN "+ko);
                System.out.println("VÄRI true = white " + isWhite);
                if(isWhite){
                    if(ko.equals("white")){
                        System.out.println("WHITE pollresult muutettu");
                        blockPolling=true;
                        sendPicture();                    }

                }
                else{
                    if(ko.equals("black")){
                        System.out.println("BLACK pollresult muutettu");
                        blockPolling=true;
                        sendPicture();
                    }
                }

            }

            @Override
            public void onFailure (int statusCode, Header[] headers, byte[] bytes, Throwable throwable){

                String ko=Arrays.toString(bytes);
                System.out.println("FAIL POLLAUKSESTA TAKAISIN "+ko);
            }

        });

        return pollResult[0];
    }
    public void sendFile(File fileToSend) {

        RequestParams params = new RequestParams();
        String url ="http://94.237.117.223/upload";
//        String url ="http://192.168.43.133/upload";

        try {
            params.put("file", fileToSend);
        } catch(Exception e){

        }
        System.out.println("JIIPEEGEE " +fileToSend.length() +" LOPPUOSA " + fileToSend.toString());
            // send request
        AsyncHttpClient client = new AsyncHttpClient();
       // Toast.makeText(AndroidCameraApi.this, "testin vuoksi", Toast.LENGTH_LONG).show();
        client.setTimeout(30000);
        client.setResponseTimeout(30000);
        client.post(url,params,new AsyncHttpResponseHandler(){
            @Override
            public void onSuccess (int statusCode, Header[] headers, byte[] bytes){
                //fileToSend.delete();
                System.out.println("MAKKARA");
                String byt=new String(bytes);
                String koo=headers.toString()+"__"+byt;
                //Toast.makeText(AndroidCameraApi.this, koo, Toast.LENGTH_LONG).show();
                blockPolling=false;
            }

            @Override
            public void onFailure (int statusCode, Header[] headers, byte[] bytes, Throwable throwable){
                System.out.println("KALAKALA");
                //drawResult("fucked");
                String byt=new String(bytes);
                String koo=headers.toString()+"__"+byt;
                //Toast.makeText(AndroidCameraApi.this, "Fail "+ koo, Toast.LENGTH_LONG).show();
                blockPolling=false;
            }
        });
    }
}


