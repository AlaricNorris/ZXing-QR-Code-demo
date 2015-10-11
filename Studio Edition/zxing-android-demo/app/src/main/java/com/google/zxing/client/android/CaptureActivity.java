/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.history.HistoryActivity;
import com.google.zxing.client.android.history.HistoryItem;
import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.android.result.ResultButtonListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.android.result.supplement.SupplementalInfoRetriever;
import com.google.zxing.client.android.share.ShareActivity;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity
        implements SurfaceHolder.Callback, SensorEventListener {

    public static final int HISTORY_REQUEST_CODE = 0x0000bacc;
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;
    private static final String[] ZXING_URLS =
            {"http://zxing.appspot.com/scan", "zxing://scan/"};
    private static final Collection<ResultMetadataType> DISPLAYABLE_METADATA_TYPES = EnumSet.of(
            ResultMetadataType.ISSUE_NUMBER, ResultMetadataType.SUGGESTED_PRICE,
            ResultMetadataType.ERROR_CORRECTION_LEVEL, ResultMetadataType.POSSIBLE_COUNTRY
    );
    SensorManager mSensorManager;
    BubbleView mBubbleView;
    int k = 45; //灵敏度
    MySensorEventListener listener = new MySensorEventListener();
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private Result savedResultToShow;
    private ViewfinderView viewfinderView;
    private TextView statusView;
    private View resultView;
    private Result lastResult;
    private boolean hasSurface;
    private boolean copyToClipboard;
    private IntentSource source;
    private String sourceUrl;
    private ScanFromWebPageManager scanFromWebPageManager;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private HistoryManager historyManager;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private float currentDegree = 0f;
    private ImageView mImageView_compress_newAPI;
    private ImageView mImageView_compress;
    private TextView mTextView_compress;
    private TextView mTextView_compress_newAPI;
    private TextView mTextView_horizontal;
    double tiltAngle = 0;
    private final SensorListener mSensorLisener = new SensorListener() {

        @Override
        public void onAccuracyChanged(int sensor, int accuracy) {
        }

        public boolean isContain(int x, int y) {//判断点是否在圆内

            int tempx = (int) (x + mBubbleView.zhongBitmap2.getWidth() / 2.0);
            int tempy = (int) (y + mBubbleView.zhongBitmap2.getWidth() / 2.0);
            int ox = (int) (mBubbleView.zhong1_X + mBubbleView.zhongBitmap1.getWidth() / 2.0);
            int oy = (int) (mBubbleView.zhong1_X + mBubbleView.zhongBitmap1.getWidth() / 2.0);
            if (Math.sqrt((tempx - ox) * (tempx - ox) + (tempy - oy) * (tempy - oy)) >
                    (mBubbleView.zhongBitmap1.getWidth() / 2.0 -
                            mBubbleView.zhongBitmap2.getWidth() / 2.0)) {
                //不在圆内
                return false;
            } else {
                //在圆内时
                return true;
            }
        }

        @Override
        public void onSensorChanged(int sensor, float[] values) {
            if (sensor == SensorManager.SENSOR_ORIENTATION) {
                double pitch = values[SensorManager.DATA_Y];
                double roll = values[SensorManager.DATA_Z];
                tiltAngle = roll;
                int x = 0;
                int y = 0;//临时变量，算中间水泡坐标时用
                int tempX = 0;
                int tempY = 0;//下面气泡的临时变量

                mTextView_horizontal.setText(
                        "Y:" + values[SensorManager.DATA_Y] + "\n" + "Z:" +
                                values[SensorManager.DATA_Z] + "\n" +
                                "X:" + values[SensorManager.DATA_X]
                );
                //开始调整x 的值
                if (Math.abs(roll) <= k) {
                    mBubbleView.shang2_X = mBubbleView.shang1_X //上面的
                            + (int) (((mBubbleView.shangBitmap1.getWidth() -
                            mBubbleView.shangBitmap2.getWidth()) / 2.0) -
                            (((mBubbleView.shangBitmap1.getWidth() -
                                    mBubbleView.shangBitmap2.getWidth()) / 2.0) * roll) / k);

                    x = mBubbleView.zhong1_X //中间的
                            + (int) (((mBubbleView.zhongBitmap1.getWidth() -
                            mBubbleView.zhongBitmap2.getWidth()) / 2.0) -
                            (((mBubbleView.zhongBitmap1.getWidth() -
                                    mBubbleView.zhongBitmap2.getWidth()) / 2.0) * roll) / k);
                } else if (roll > k) {
                    mBubbleView.shang2_X = mBubbleView.shang1_X;
                    x = mBubbleView.zhong1_X;
                } else {
                    mBubbleView.shang2_X =
                            mBubbleView.shang1_X + mBubbleView.shangBitmap1.getWidth() -
                                    mBubbleView.shangBitmap2.getWidth();

                    x = mBubbleView.zhong1_X + mBubbleView.zhongBitmap1.getWidth() -
                            mBubbleView.zhongBitmap2.getWidth();
                }

                //开始调整y 的值

                if (Math.abs(pitch) <= k) {
                    mBubbleView.zuo2_Y = mBubbleView.zuo1_Y //左面的
                            + (int) (((mBubbleView.zuoBitmap1.getHeight() -
                            mBubbleView.zuoBitmap2.getHeight()) / 2.0) +
                            (((mBubbleView.zuoBitmap1.getHeight() -
                                    mBubbleView.zuoBitmap2.getHeight()) / 2.0) * pitch) / k);

                    y = mBubbleView.zhong1_Y + //中间的
                            (int) (((mBubbleView.zhongBitmap1.getHeight() -
                                    mBubbleView.zhongBitmap2.getHeight()) / 2.0) +
                                    (((mBubbleView.zhongBitmap1.getHeight() -
                                            mBubbleView.zhongBitmap2.getHeight()) / 2.0) *
                                            pitch) / k);
                } else if (pitch > k) {
                    mBubbleView.zuo2_Y = mBubbleView.zuo1_Y + mBubbleView.zuoBitmap1.getHeight() -
                            mBubbleView.zuoBitmap2.getHeight();

                    y = mBubbleView.zhong1_Y + mBubbleView.zhongBitmap1.getHeight() -
                            mBubbleView.zhongBitmap2.getHeight();
                } else {
                    mBubbleView.zuo2_Y = mBubbleView.zuo1_Y;
                    y = mBubbleView.zhong1_Y;
                }

                //下面的
                tempX = -(int) (((mBubbleView.xiaBitmap1.getWidth() / 2 - 28) * roll +
                        (mBubbleView.xiaBitmap1.getWidth() / 2 - 28) * pitch) / k);

                tempY = -(int) ((-(mBubbleView.xiaBitmap1.getWidth() / 2 - 28) * roll -
                        (mBubbleView.xiaBitmap1.getWidth() / 2 - 28) * pitch) / k);

                //限制下面的气泡范围
                if (tempY > mBubbleView.xiaBitmap1.getHeight() / 2 - 28) {
                    tempY = mBubbleView.xiaBitmap1.getHeight() / 2 - 28;
                }
                if (tempY < -mBubbleView.xiaBitmap1.getHeight() / 2 + 28) {
                    tempY = -mBubbleView.xiaBitmap1.getHeight() / 2 + 28;
                }

                if (tempX > mBubbleView.xiaBitmap1.getWidth() / 2 - 28) {
                    tempX = mBubbleView.xiaBitmap1.getWidth() / 2 - 28;
                }

                if (tempX < -mBubbleView.xiaBitmap1.getWidth() / 2 + 28) {
                    tempX = -mBubbleView.xiaBitmap1.getWidth() / 2 + 28;
                }

                mBubbleView.xia2_X =
                        tempX + mBubbleView.xia1_X + mBubbleView.xiaBitmap1.getWidth() / 2 -
                                mBubbleView.xiaBitmap2.getWidth() / 2;
                mBubbleView.xia2_Y =
                        tempY + mBubbleView.xia1_Y + mBubbleView.xiaBitmap1.getHeight() / 2 -
                                mBubbleView.xiaBitmap2.getWidth() / 2;

                if (isContain(x, y)) {//中间的水泡在圆内才改变坐标
                    mBubbleView.zhong2_X = x;
                    mBubbleView.zhong2_Y = y;
                }
                mBubbleView.postInvalidate();//重绘
            }
        } //传感器监听器类
        //该处省略了部分代码，将在后面进行介绍
    };
    private Sensor accelerometer; // 加速度传感器
    private Sensor magnetic; // 地磁场传感器
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    private static void drawLine(
            Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor
    ) {
        if (a != null && b != null) {
            canvas.drawLine(
                    scaleFactor * a.getX(), scaleFactor * a.getY(), scaleFactor * b.getX(),
                    scaleFactor * b.getY(), paint
            );
        }
    }

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);
        mImageView_compress = (ImageView) findViewById(R.id.image_compress);
        mImageView_compress_newAPI = (ImageView) findViewById(R.id.image_compress_newAPI);
        mTextView_compress = (TextView) findViewById(R.id.text_compress);
        mTextView_compress_newAPI = (TextView) findViewById(R.id.text_compress_newAPI);
        mTextView_horizontal = (TextView) findViewById(R.id.text_horizontal);
        mBubbleView = (BubbleView) findViewById(R.id.bubbleview);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // 初始化加速度传感器
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // 初始化地磁场传感器
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    @Override
    protected void onResume() {
        mSensorManager.registerListener(
                this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST
        );
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);

        if (sensors.size() > 0) {
            Sensor sensor = sensors.get(0);
            //注册SensorManager
            //this->接收sensor的实例
            //接收传感器类型的列表
            //接受的频率
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        // 注册监听
        mSensorManager.registerListener(
                listener, accelerometer, Sensor.TYPE_ACCELEROMETER
        );
        mSensorManager.registerListener(
                listener, magnetic, Sensor.TYPE_MAGNETIC_FIELD
        );
        mSensorManager.registerListener(mSensorLisener, SensorManager.SENSOR_ORIENTATION);

        super.onResume();

        // historyManager must be initialized here to update the history preference
        historyManager = new HistoryManager(this);
        historyManager.trimHistory();

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);

        handler = null;
        lastResult = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION, true)) {
            setRequestedOrientation(getCurrentOrientation());
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        resetStatusView();

        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        Intent intent = getIntent();

        copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true) &&
                (intent == null || intent.getBooleanExtra(Intents.Scan.SAVE_HISTORY, true));

        source = IntentSource.NONE;
        sourceUrl = null;
        scanFromWebPageManager = null;
        decodeFormats = null;
        characterSet = null;

        if (intent != null) {

            String action = intent.getAction();
            String dataString = intent.getDataString();

            if (Intents.Scan.ACTION.equals(action)) {

                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
                decodeHints = DecodeHintManager.parseDecodeHints(intent);

                if (intent.hasExtra(Intents.Scan.WIDTH) &&
                        intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId);
                    }
                }

                String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
                if (customPromptMessage != null) {
                    statusView.setText(customPromptMessage);
                }

            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

            } else if (isZXingURL(dataString)) {

                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK;
                sourceUrl = dataString;
                Uri inputUri = Uri.parse(dataString);
                scanFromWebPageManager = new ScanFromWebPageManager(inputUri);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
                // Allow a sub-set of the hints to be specified by the caller.
                decodeHints = DecodeHintManager.parseDecodeHints(inputUri);

            }

            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        }
        SharedPreferences mSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        boolean my_checkbox_preference =
                mSharedPreferences.getBoolean("preferences_use_front_camera", false);
        if (my_checkbox_preference)
            cameraManager.setManualCameraId(1);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            default:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        mSensorManager.unregisterListener(listener);
        mSensorManager.unregisterListener(mSensorLisener);
        ;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) &&
                        lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        switch (item.getItemId()) {
            case R.id.menu_share:
                intent.setClassName(this, ShareActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_history:
                intent.setClassName(this, HistoryActivity.class.getName());
                startActivityForResult(intent, HISTORY_REQUEST_CODE);
                break;
            case R.id.menu_settings:
                intent.setClassName(this, PreferencesActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_help:
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK && requestCode == HISTORY_REQUEST_CODE &&
                historyManager != null) {
            int itemNumber = intent.getIntExtra(Intents.History.ITEM_NUMBER, -1);
            if (itemNumber >= 0) {
                HistoryItem historyItem = historyManager.buildHistoryItem(itemNumber);
                decodeOrStoreSavedBitmap(null, historyItem.getResult());
            }
        }
    }


    public static final int LIGHTTOP = 260;
    public static final int HUMANTOP = 120;
    public static final int delta = LIGHTTOP - HUMANTOP;

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message =
                        Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        lastResult = rawResult;
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            historyManager.addHistoryItem(rawResult, resultHandler);
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }

        switch (source) {
            case NATIVE_APP_INTENT:
            case PRODUCT_SEARCH_LINK:
                handleDecodeExternally(rawResult, resultHandler, barcode);
                break;
            case ZXING_LINK:
                if (scanFromWebPageManager == null ||
                        !scanFromWebPageManager.isScanFromWebPage()) {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                } else {
                    handleDecodeExternally(rawResult, resultHandler, barcode);
                }
                break;
            case NONE:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (fromLiveScan &&
                        prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)) {
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.msg_bulk_mode_scanned) + " (" +
                                    rawResult.getText() + ')', Toast.LENGTH_SHORT
                    ).show();
                    // Wait a moment or else it will scan the same barcode continuously about 3 times
                    restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
                } else {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                }
                break;
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                    rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(
                                scaleFactor * point.getX(), scaleFactor * point.getY(), paint
                        );
                    }
                }
            }
        }
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(
            Result rawResult, ResultHandler resultHandler, Bitmap barcode
    ) {

        CharSequence displayContents = resultHandler.getDisplayContents();

        if (copyToClipboard && !resultHandler.areContentsSecure()) {
            ClipboardInterface.setText(displayContents, this);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (resultHandler.getDefaultButtonID() != null &&
                prefs.getBoolean(PreferencesActivity.KEY_AUTO_OPEN_WEB, false)) {
            resultHandler.handleButtonPress(resultHandler.getDefaultButtonID());
            return;
        }

        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(
                    BitmapFactory.decodeResource(
                            getResources(), R.drawable.launcher_icon
                    )
            );
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

        TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());
        if (orientation<0)
            orientation+=360;
        formatTextView.setText(":" + orientation);
        TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
        timeTextView.setText(formatter.format(new Date(rawResult.getTimestamp())));

        TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
        View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(View.GONE);
        metaTextViewLabel.setVisibility(View.GONE);
        Map<ResultMetadataType, Object> metadata = rawResult.getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (Map.Entry<ResultMetadataType, Object> entry : metadata.entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(View.VISIBLE);
                metaTextViewLabel.setVisibility(View.VISIBLE);
            }
        }
        metaTextView.setText(":" + calculateDistance(tiltAngle));
        TextView offsetTextView = (TextView) findViewById(R.id.offset_text_view);
        offsetTextView.setText(":" + calculateOffsets(tiltAngle));
        TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
        contentsTextView.setText(displayContents);
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        TextView supplementTextView =
                (TextView) findViewById(R.id.contents_supplement_text_view);
        supplementTextView.setText(""+tiltAngle);
        supplementTextView.setOnClickListener(null);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PreferencesActivity.KEY_SUPPLEMENTAL, true
        )) {
            SupplementalInfoRetriever.maybeInvokeRetrieval(
                    supplementTextView, resultHandler.getResult(), historyManager, this
            );
        }

        int buttonCount = resultHandler.getButtonCount();
        ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
        buttonView.requestFocus();
        for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
            TextView button = (TextView) buttonView.getChildAt(x);
            if (x < buttonCount) {
                button.setVisibility(View.VISIBLE);
                button.setText(resultHandler.getButtonText(x));
                button.setOnClickListener(new ResultButtonListener(resultHandler, x));
            } else {
                button.setVisibility(View.GONE);
            }
        }
    }


    public static double calculateDistance(double tiltAngle) {
        if (tiltAngle > 90 || tiltAngle < 0)
            return 0;
        Log.i("nrs", "tiltAngle" + tiltAngle);
        double result = delta / Math.tan(Math.toRadians(90 - tiltAngle));
        Log.i("nrs", "(90 - tiltAngle)" + (90 - tiltAngle));
        Log.i("nrs", "Math.tan()" + Math.tan(Math.toRadians(90 - tiltAngle)));
        Log.i("nrs", "result" + result);
        return result;
    }

    public static double calculateOffsets(double tiltAngle) {
        Log.i("nrs", "tiltAngle" + tiltAngle);
        double positiveOffset = tiltAngle + 5;
        double negativeOffset = tiltAngle - 5;
        double positive = delta / Math.tan(Math.toRadians(90 - positiveOffset));
        Log.i("nrs", "positiveOffset" + positiveOffset);
        Log.i("nrs", "negativeOffset" + negativeOffset);
        double negative = delta / Math.tan(Math.toRadians(90 - negativeOffset));
        Log.i("nrs", "positive" + positive);
        Log.i("nrs", "negative" + negative);
        double defaultdistance = calculateDistance(tiltAngle);
        if (positiveOffset > 90) {
            return Math.abs(defaultdistance - negative);
        }
        if (negativeOffset < 0)
            return Math.abs(defaultdistance - positive);
        Log.i("nrs", "defaultdistance" + defaultdistance);
        return Math.min(Math.abs(defaultdistance - positive), Math.abs(defaultdistance - negative)) ;
    }

    // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(
            Result rawResult, ResultHandler resultHandler, Bitmap barcode
    ) {

        if (barcode != null) {
            viewfinderView.drawResultBitmap(barcode);
        }

        long resultDurationMS;
        if (getIntent() == null) {
            resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
        } else {
            resultDurationMS = getIntent().getLongExtra(
                    Intents.Scan.RESULT_DISPLAY_DURATION_MS, DEFAULT_INTENT_RESULT_DURATION_MS
            );
        }

        if (resultDurationMS > 0) {
            String rawResultString = String.valueOf(rawResult);
            if (rawResultString.length() > 32) {
                rawResultString = rawResultString.substring(0, 32) + " ...";
            }
            statusView.setText(
                    getString(resultHandler.getDisplayTitle()) + " : " + rawResultString

            );
            statusView.setText("tileAngle" + tiltAngle);
        }

        if (copyToClipboard && !resultHandler.areContentsSecure()) {
            CharSequence text = resultHandler.getDisplayContents();
            ClipboardInterface.setText(text, this);
        }

        if (source == IntentSource.NATIVE_APP_INTENT) {

            // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
            // the deprecated intent is retired.
            Intent intent = new Intent(getIntent().getAction());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
            byte[] rawBytes = rawResult.getRawBytes();
            if (rawBytes != null && rawBytes.length > 0) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
            }
            Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
            if (metadata != null) {
                if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                    intent.putExtra(
                            Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                            metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString()
                    );
                }
                Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
                if (orientation != null) {
                    intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
                }
                String ecLevel =
                        (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
                if (ecLevel != null) {
                    intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
                }
                @SuppressWarnings("unchecked") Iterable<byte[]> byteSegments =
                        (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
                if (byteSegments != null) {
                    int i = 0;
                    for (byte[] byteSegment : byteSegments) {
                        intent.putExtra(
                                Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment
                        );
                        i++;
                    }
                }
            }
            sendReplyMessage(R.id.return_scan_result, intent, resultDurationMS);

        } else if (source == IntentSource.PRODUCT_SEARCH_LINK) {

            // Reformulate the URL which triggered us into a query, so that the request goes to the same
            // TLD as the scan URL.
            int end = sourceUrl.lastIndexOf("/scan");
            String replyURL =
                    sourceUrl.substring(0, end) + "?q=" + resultHandler.getDisplayContents() +
                            "&source=zxing";
            sendReplyMessage(R.id.launch_product_query, replyURL, resultDurationMS);

        } else if (source == IntentSource.ZXING_LINK) {

            if (scanFromWebPageManager != null && scanFromWebPageManager.isScanFromWebPage()) {
                String replyURL = scanFromWebPageManager.buildReplyURL(rawResult, resultHandler);
                scanFromWebPageManager = null;
                sendReplyMessage(R.id.launch_product_query, replyURL, resultDurationMS);
            }

        }
    }

    private void sendReplyMessage(int id, Object arg, long delayMS) {
        if (handler != null) {
            Message message = Message.obtain(handler, id, arg);
            if (delayMS > 0L) {
                handler.sendMessageDelayed(message, delayMS);
            } else {
                handler.sendMessage(message);
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(
                        this, decodeFormats, decodeHints, characterSet, cameraManager
                );
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


    double orientation = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ORIENTATION:
                float degree = event.values[SensorManager.DATA_X];
                degree += 90;
                RotateAnimation ra = new RotateAnimation(
                        currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                ra.setDuration(200);
                //                mImageView_compress.startAnimation( ra );
                mImageView_compress.setRotation(-degree);
                mTextView_compress.setText(
                        "X:" + degree + "\n\n" + "Y:" +
                                event.values[SensorManager.DATA_Y] + "\n" + "Z:" +
                                event.values[SensorManager.DATA_Z]
                );
                currentDegree = -degree;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    // 计算方向
    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(
                R, null, accelerometerValues, magneticFieldValues
        );
        SensorManager.getOrientation(R, values);
        values[0] = (float) Math.toDegrees(values[0]);

        values[0] += 90;
        mImageView_compress_newAPI.setRotation(-values[0]);
        orientation = values[0];
        mTextView_compress_newAPI.setText(
                "X:" + values[0] + "\n\n" + "Y:" + values[1] + "\n" + "Z:" + values[2]
        );
        Log.i(TAG, values[0] + "");
        if (values[0] >= -5 && values[0] < 5) {
        } else if (values[0] >= 5 && values[0] < 85) {
            Log.i(TAG, "东北");
        } else if (values[0] >= 85 && values[0] <= 95) {
            Log.i(TAG, "正东");
        } else if (values[0] >= 95 && values[0] < 175) {
            Log.i(TAG, "东南");
        } else if ((values[0] >= 175 && values[0] <= 180) ||
                (values[0]) >= -180 && values[0] < -175) {
            Log.i(TAG, "正南");
        } else if (values[0] >= -175 && values[0] < -95) {
            Log.i(TAG, "西南");
        } else if (values[0] >= -95 && values[0] < -85) {
            Log.i(TAG, "正西");
        } else if (values[0] >= -85 && values[0] < -5) {
            Log.i(TAG, "西北");
        }
    }

    class MySensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticFieldValues = event.values;
            }
            calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

    }
}
