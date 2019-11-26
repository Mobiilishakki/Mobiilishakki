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
    private Button takePictureButton;
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



        textureView = (TextureView) findViewById(R.id.texture);

        drawView=(DrawView) findViewById(R.id.drawView);

        drawView.setZ(100);




        mImageView = (ImageView) findViewById(R.id.board_coord);


        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        takePictureButton.setZ(101);
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

                        System.out.println("eka");

                        image = reader.acquireLatestImage();
                        System.out.println("toka");
                        fileToSend = File.createTempFile("Chess",".jpg");
                        System.out.println("kolmas");

                        //cropImage(image);                                                   // New rectagle CROP

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

        float rotatedTopLeftX=toprighty;
        float rotatedTopLeftY=bitmap.getHeight()-toprightx;
        float rotatedBottomRightX=bottomlefty;
        float rotatedBottomRightY=bitmap.getHeight()-bottomleftx;

        float rotatedTopRightX=bottomrighty;
        float rotatedTopRightY=bitmap.getHeight()-bottomrightx;
        float rotatedBottomLeftX=toplefty;
        float rotatedBottomLeftY=bitmap.getHeight()-topleftx;

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

        float[] dst = new float[8];
        dst[0] = 0;
        dst[1] = 0;
        dst[2] = dstWidth;
        dst[3] = 0;
        dst[4] = dstWidth;
        dst[5] = dstheight;
        dst[6] = 0;
        dst[7] = dstheight;

        Matrix matrix = new Matrix();
        //boolean mapped = matrix.setPolyToPoly(src, 0, dst, 0, 4);

        matrix.postRotate(90);

        return Bitmap.createBitmap(bitmap, 0, 0, (int)dstWidth, (int)dstheight, matrix, true);

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
//        String url ="http://94.237.117.223/upload";
        String url ="http://192.168.42.155/upload";

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
