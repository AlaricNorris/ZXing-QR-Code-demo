/**
 *  C
 *  com.alaric.norris.aars.crashhelper
 *  date            author
 *  2015/9/29      AlaricNorris
 *	Copyright (c) 2015, TNT All Rights Reserved.
 */
package com.alaric.norris.aars.crashhelper;

/**
 *  ClassName:  C
 *  Function:   ${TODO}  ADD FUNCTION
 *  Reason:     ${TODO}  ADD REASON
 *  @author AlaricNorris
 *  @contact Norris.sly@gmail.com
 *  @version Ver 1.0
 *  @since I used to be a programmer like you, then I took an arrow in the knee
 *  @Date 2015     2015/9/29     14:07
 *  @see        ${TAGS}
 *	@Fields
 *	@Methods ${ENCLOSING_TYPE}
 * 	Modified By 	AlaricNorris		 2015/9/2914:07
 *	Modifications:	${TODO}
 */
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author xiaanming
 *
 */
public class CustomCrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "Activity";
    private static final String SDCARD_ROOT = Environment.getExternalStorageDirectory().toString();
    private static CustomCrashHandler mInstance = new CustomCrashHandler();
    private Context mContext;

    private CustomCrashHandler () {
    }
    /**
     * @return 单例
     */
    public static CustomCrashHandler getInstance () {
        return mInstance;
    }

    /**
     * exception
     */
    @Override
    public void uncaughtException ( Thread thread, Throwable ex ) {
        savaInfoToSD( mContext, ex );

        showToast( mContext, "Crash!" );
        try {
            thread.sleep( 2000 );
        }
        catch ( InterruptedException e ) {
            e.printStackTrace();
        }
        android.os.Process.killProcess( android.os.Process.myPid() );
        System.exit( 1 );

        // TODO
        //        ExitAppUtils.getInstance().exit();

    }

    /**
     * @param context    context
     */
    public void setCustomCrashHanler ( Context context ) {
        mContext = context;
        Thread.setDefaultUncaughtExceptionHandler( this );
    }

    /**
     * @param context context
     * @param msg
     */
    private void showToast ( final Context context, final String msg ) {
        new Thread(
                new Runnable() {

                    @Override
                    public void run () {
                        Looper.prepare();
                        Toast.makeText( context, msg, Toast.LENGTH_LONG ).show();
                        Looper.loop();
                    }
                }
        ).start();
    }

    /**
     * @param context context
     * @return
     */
    private HashMap< String, String > obtainSimpleInfo ( Context context ) {
        HashMap< String, String > map = new HashMap< String, String >();
        PackageManager mPackageManager = context.getPackageManager();
        PackageInfo mPackageInfo = null;
        try {
            mPackageInfo = mPackageManager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES
            );
        }
        catch ( NameNotFoundException e ) {
            e.printStackTrace();
        }

        map.put( "versionName", mPackageInfo.versionName );
        map.put( "versionCode", "" + mPackageInfo.versionCode );

        map.put( "MODEL", "" + Build.MODEL );
        map.put( "SDK_INT", "" + Build.VERSION.SDK_INT );
        map.put( "PRODUCT", "" + Build.PRODUCT );

        return map;
    }

    /**
     * @param throwable 异常
     * @return
     */
    private String obtainExceptionInfo ( Throwable throwable ) {
        StringWriter mStringWriter = new StringWriter();
        PrintWriter mPrintWriter = new PrintWriter( mStringWriter );
        throwable.printStackTrace( mPrintWriter );
        mPrintWriter.close();

        Log.e( TAG, mStringWriter.toString() );
        return mStringWriter.toString();
    }

    /**
     * @param context context
     * @param ex
     * @return
     */
    private String savaInfoToSD ( Context context, Throwable ex ) {
        String fileName = null;
        StringBuffer sb = new StringBuffer();

        for ( Map.Entry< String, String > entry : obtainSimpleInfo( context ).entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append( key ).append( " = " ).append( value ).append( "\n" );
        }

        sb.append( obtainExceptionInfo( ex ) );

        if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) ) {
            File dir = new File( SDCARD_ROOT + File.separator + "crash" + File.separator );
            if ( ! dir.exists() ) {
                dir.mkdir();
            }

            try {
                fileName =
                        dir.toString() + File.separator + paserTime( System.currentTimeMillis() ) +
                                ".log";
                FileOutputStream fos = new FileOutputStream( fileName );
                fos.write( sb.toString().getBytes() );
                fos.flush();
                fos.close();
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }

        }

        return fileName;

    }

    /**
     * @param milliseconds 毫秒
     * @return
     */
    private String paserTime ( long milliseconds ) {
        System.setProperty( "user.timezone", "Asia/Shanghai" );
        TimeZone tz = TimeZone.getTimeZone( "Asia/Shanghai" );
        TimeZone.setDefault( tz );
        SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
        String times = format.format( new Date( milliseconds ) );

        return times;
    }
}
