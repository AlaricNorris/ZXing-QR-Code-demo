package com.alaric.norris.aars.crashhelper;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;

/**
 *	ClassName:	CrashHandler
 *    @author Norris        Norris.sly@gmail.com
 *	@since Ver 1.0		I used to be a programmer like you, then I took an arrow in the knee
 */
public class CrashHandler implements UncaughtExceptionHandler {

    /**
     * 	String			:		TAG
     */
    private static final String TAG = "tag";
    /**
     * 	String			:		VERSION_NAME
     *    @since Ver 1.0
     */
    private static final String VERSION_NAME = "versionName";
    /**
     * 	String			:		VERSION_CODE
     *    @since Ver 1.0
     */
    private static final String VERSION_CODE = "versionCode";
    /**
     * 	String			:		STACK_TRACE
     *    @since Ver 1.0
     */
    private static final String STACK_TRACE = "STACK_TRACE";
    /**
     * 	String			:		CRASH_REPORTER_EXTENSION
     *    @since Ver 1.0
     */
    private static final String CRASH_REPORTER_EXTENSION = ".cr";
    /**
     * 	CrashHandler			:		mInstance
     */
    private static CrashHandler mInstance;
    /**
     * 	Thread.UncaughtExceptionHandler			:		mDefaultHandler
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    /**
     * 	Properties			:		mDeviceCrashInfo
     *    @since Ver 1.0
     */
    private Properties mDeviceCrashInfo = new Properties();
    /**
     * 	Context			:		mContext
     */
    private Context mContext;

    /**
     * 	Map<String,String>			:		mLogInfo
     */
    private Map< String, String > mDeviceInfo = new HashMap< String, String >();

    /**
     * 	SimpleDateFormat			:		mSimpleDateFormat
     */
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat( "yyyyMMdd_HH-mm-ss" );
    /**
     * 	Vector<OnCrashListener>			:		mCrashListeners
     *    @since Ver 1.0
     */
    private Vector< OnCrashListener > mCrashListeners =
            new Vector< CrashHandler.OnCrashListener >();

    /**
     */
    private CrashHandler () {
    }
    /**
     *  @return CrashHandler 单例
     *  @since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     */
    public static CrashHandler getInstance () {
        if ( mInstance == null )
            mInstance = new CrashHandler();
        return mInstance;
    }
    /**
     *
     * @param inOnCrashListener 说明
     */
    public void addOnCrashListener ( OnCrashListener inOnCrashListener ) {
        mCrashListeners.add( inOnCrashListener );
    }
    /**
     *  @param  paramContext context
     *  @since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     */
    public void init ( Context paramContext ) {
        mContext = paramContext;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler( this );
    }
    /**
     * 	(non-Javadoc)
     *    @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread , java.lang.Throwable)
     */
    public void uncaughtException ( Thread inThread, Throwable inThrowable ) {
        if ( mCrashListeners != null ) {
            for ( OnCrashListener tempCrashListener : mCrashListeners ) {
                tempCrashListener.onCrash();
            }
        }
        if ( ! handleException( inThrowable ) && mDefaultHandler != null ) {
            mDefaultHandler.uncaughtException( inThread, inThrowable );
        }
        else {
            try {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException e ) {
                e.printStackTrace();
            }
            android.os.Process.killProcess( android.os.Process.myPid() );
            System.exit( 0 );
        }
    }
    /**
     * @param inThrowable    异常
     * @since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     * @return 是否解决
     */
    public boolean handleException ( Throwable inThrowable ) {
        if ( inThrowable == null )
            return false;
        new Thread() {

            public void run () {
                Log.i( "tag", "run" );
                Looper.prepare();
                Toast.makeText( mContext, "Crash!", 0 ).show();
                Looper.loop();
            }
        }.start();
        getDeviceInfo( mContext );
        saveCrashLogToFile( inThrowable );
        //		collectCrashDeviceInfo(mContext) ;
        //		String crashFileName = saveCrashInfoToFile(inThrowable) ;
        //		sendCrashReportsToServer(mContext) ;
        return true;
    }
    /**
     *
     * @param inContext context
     */
    private void sendCrashReportsToServer ( Context inContext ) {
        String[] mFiles = getCrashReportFiles( inContext );
        if ( mFiles != null && mFiles.length > 0 ) {
            TreeSet< String > sortedFiles = new TreeSet< String >();
            sortedFiles.addAll( Arrays.asList( mFiles ) );
            for ( String fileName : sortedFiles ) {
                File mFile = new File( inContext.getFilesDir(), fileName );
                if ( postReport( mFile ) ) {
                    mFile.delete();
                }
            }
        }
    }
    /**
     *
     * @param inContext context
     */
    private String[] getCrashReportFiles ( Context inContext ) {
        File filesDir = inContext.getFilesDir();
        FilenameFilter filter = new FilenameFilter() {

            // accept(File dir, String name)
            public boolean accept ( File dir, String name ) {
                return name.endsWith( CRASH_REPORTER_EXTENSION );
            }
        };
        // list(FilenameFilter filter)
        return filesDir.list( filter );
    }
    private boolean postReport ( File file ) {
        return false;
    }
    /**
     *    @param inContext context
     * 	@since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     */
    public void sendPreviousReportsToServer ( Context inContext ) {
        sendCrashReportsToServer( inContext );
    }
    /**
     *
     * @param ex 异常
     */
    private String saveCrashInfoToFile ( Throwable ex ) {
        Writer info = new StringWriter();
        PrintWriter printWriter = new PrintWriter( info );
        // printStackTrace(PrintWriter s)
        ex.printStackTrace( printWriter );
        Throwable cause = ex.getCause();
        while ( cause != null ) {
            cause.printStackTrace( printWriter );
            cause = cause.getCause();
        }
        String result = info.toString();
        printWriter.close();
        mDeviceCrashInfo.put( STACK_TRACE, result );
        try {
            Log.i( "tag", "" + mDeviceCrashInfo.getProperty( STACK_TRACE ) );
            long timestamp = System.currentTimeMillis();
            String fileName = "crash-" + timestamp + CRASH_REPORTER_EXTENSION;
            FileOutputStream trace = mContext.openFileOutput( fileName, Context.MODE_PRIVATE );
            mDeviceCrashInfo.store( trace, "" );
            trace.flush();
            trace.close();
            return fileName;
        }
        catch ( Exception e ) {
            Log.e( TAG, "an error occured while writing report file...", e );
        }
        return null;
    }
    /**
     *
     * @param ctx context
     */
    public void collectCrashDeviceInfo ( Context ctx ) {
        try {
            // Class for retrieving various kinds of information related to the
            // application packages that are currently installed on the device.
            // You can find this class through getPackageManager().
            PackageManager pm = ctx.getPackageManager();
            // getPackageInfo(String packageName, int flags)
            // Retrieve overall information about an application package that is installed on the system.
            // public static final int GET_ACTIVITIES
            // Since: API Level 1 PackageInfo flag: return information about activities in the package in activities.
            PackageInfo pi =
                    pm.getPackageInfo( ctx.getPackageName(), PackageManager.GET_ACTIVITIES );
            if ( pi != null ) {
                // public String versionName The version name of this package,
                // as specified by the <manifest> tag's versionName attribute.
                mDeviceCrashInfo.put(
                        VERSION_NAME, pi.versionName == null ? "not set" : pi.versionName
                );
                // public int versionCode The version number of this package,
                // as specified by the <manifest> tag's versionCode attribute.
                mDeviceCrashInfo.put( VERSION_CODE, pi.versionCode );
            }
        }
        catch ( NameNotFoundException e ) {
            Log.e( TAG, "Error while collect package info", e );
        }
        Field[] fields = Build.class.getDeclaredFields();
        for ( Field field : fields ) {
            try {
                // setAccessible(boolean flag)
                field.setAccessible( true );
                mDeviceCrashInfo.put( field.getName(), field.get( null ) );
                Log.d( TAG, field.getName() + " : " + field.get( null ) );
            }
            catch ( Exception e ) {
                Log.e( TAG, "Error while collect crash info", e );
            }
        }
    }
    /**
     *    @param        paramContext context
     * 	@since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     */
    public void getDeviceInfo ( Context paramContext ) {
        try {
            PackageManager mPackageManager = paramContext.getPackageManager();
            PackageInfo mPackageInfo = mPackageManager.getPackageInfo(
                    paramContext.getPackageName(), PackageManager.GET_ACTIVITIES
            );
            if ( mPackageInfo != null ) {
                String versionName =
                        mPackageInfo.versionName == null ? "null" : mPackageInfo.versionName;
                String versionCode = mPackageInfo.versionCode + "";
                mDeviceInfo.put( "versionName", versionName );
                mDeviceInfo.put( "versionCode", versionCode );
            }
        }
        catch ( NameNotFoundException e ) {
            e.printStackTrace();
        }
        Field[] mFields = Build.class.getDeclaredFields();
        for ( Field field : mFields ) {
            try {
                field.setAccessible( true );
                mDeviceInfo.put( field.getName(), field.get( "" ).toString() );
                Log.d( TAG, field.getName() + ":" + field.get( "" ) );
            }
            catch ( IllegalArgumentException e ) {
                e.printStackTrace();
            }
            catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }
        }
    }
    /**
     *    @param        inThrowable 异常
     * 	@return FileName
     * 	@since I used to be a programmer like you, then I took an arrow in the kneeVer 1.0
     */
    private String saveCrashLogToFile ( Throwable inThrowable ) {
        //		Writer mmWriter = new StringWriter() ;
        //		PrintWriter mmPrintWriter = new PrintWriter(mmWriter) ;
        //		// printStackTrace(PrintWriter s)
        //		inThrowable.printStackTrace(mmPrintWriter) ;
        //		Throwable cause = inThrowable.getCause() ;
        //		while(cause != null) {
        //			cause.printStackTrace(mmPrintWriter) ;
        //			cause = cause.getCause() ;
        //		}
        //		String result = mmWriter.toString() ;
        //		mmPrintWriter.close() ;
        //		mDeviceCrashInfo.put(STACK_TRACE , result) ;
        StringBuffer mStringBuffer = new StringBuffer();
        for ( Map.Entry< String, String > entry : mDeviceInfo.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            mStringBuffer.append( key + "=" + value + "\r\n" );
        }
        Writer mWriter = new StringWriter();
        PrintWriter mPrintWriter = new PrintWriter( mWriter );
        inThrowable.printStackTrace( mPrintWriter );
        inThrowable.printStackTrace();
        Throwable mThrowable = inThrowable.getCause();
        while ( mThrowable != null ) {
            mThrowable.printStackTrace( mPrintWriter );
            mPrintWriter.append( "\r\n" );
            mThrowable = mThrowable.getCause();
        }
        mPrintWriter.close();
        String mResult = mWriter.toString();
        mStringBuffer.append( mResult );
        String mTime = mSimpleDateFormat.format( new Date() );
        String mFileName = "CrashLog-" + mTime + CRASH_REPORTER_EXTENSION;
        if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) ) {
            try {
                File mDirectory = new File(
                        Environment.getExternalStorageDirectory() + "/CrashInfos"
                );
                Log.i( TAG, mDirectory.toString() );
                if ( ! mDirectory.exists() )
                    mDirectory.mkdir();
                FileOutputStream mFileOutputStream = new FileOutputStream(
                        mDirectory + "/" + mFileName
                );
                mFileOutputStream.write( mStringBuffer.toString().getBytes() );
                mFileOutputStream.close();
                return mFileName;
            }
            catch ( FileNotFoundException e ) {
                e.printStackTrace();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *  ClassName:	OnCrashListener
     *  @author AlaricNorris        Norris.sly@gmail.com
     *  @version CrashHandler
     *	@since Ver 1.0		I used to be a programmer like you, then I took an arrow in the knee
     */
    public interface OnCrashListener {

        public void onCrash ();
    }
}