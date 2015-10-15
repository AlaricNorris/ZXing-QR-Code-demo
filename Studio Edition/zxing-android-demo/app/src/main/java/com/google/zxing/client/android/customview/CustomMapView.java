/**
 *  CustomMap
 *  com.google.zxing.client.android.customview
 * 	Function: 	${TODO}
 *  date            author
 *  *****************************************************
 *  2015/10/14      AlaricNorris
 *	Copyright (c) 2015, TNT All Rights Reserved.
 */
package com.google.zxing.client.android.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
/**
 *  ClassName:  CustomMap
 *  Function:   ${TODO}  ADD FUNCTION
 *  Reason:     ${TODO}  ADD REASON
 *  @author AlaricNorris
 *  Contact:    Norris.sly@gmail.com
 *  @version Ver 1.0
 *  @since I used to be a programmer like you, then I took an arrow in the knee
 *	************************************************************************************************************************************************************************************************************
 * 	Modified By 	AlaricNorris		 2015/10/1410:50
 *	Modifications:	${TODO}
 *	************************************************************************************************************************************************************************************************************
 */
public class CustomMapView extends View {
    public static final int GRIDS_WIDTH = 375;
    public static final int GRIDS_HEIGHT = 400;

    private int peiceSpace = 2;
    Rect mRect1 = new Rect( 121 * peiceSpace, 67 * peiceSpace, 158 * peiceSpace, 104 * peiceSpace );
    Rect mRect2 = new Rect( 217 * peiceSpace, 67 * peiceSpace, 254 * peiceSpace, 104 * peiceSpace );
    Rect mRect3 =
            new Rect( 121 * peiceSpace, 297 * peiceSpace, 158 * peiceSpace, 334 * peiceSpace );
    Rect mRect4 =
            new Rect( 217 * peiceSpace, 297 * peiceSpace, 254 * peiceSpace, 334 * peiceSpace );
    private Paint mPaint_Floor = new Paint();
    private Paint mPaint_Light = new Paint();
    private Paint mPaint_Window = new Paint();
    private int Coordinate_X = 30 * peiceSpace;
    private int Coordinate_Y = 390 * peiceSpace;
    public CustomMapView ( Context context, AttributeSet attrs, int defStyleAttr ) {
        super( context, attrs, defStyleAttr );
        init();
    }
    public CustomMapView ( Context context, AttributeSet attrs ) {

        this( context, attrs, 0 );
    }
    public CustomMapView ( Context context ) {
        this( context, null );
    }
    private void init () {
        mPaint_Floor = new Paint();
        mPaint_Floor.setColor( Color.GRAY );
        mPaint_Floor.setStrokeJoin( Paint.Join.ROUND );
        mPaint_Floor.setStrokeCap( Paint.Cap.ROUND );
        mPaint_Floor.setStrokeWidth( 1 );
        mPaint_Light = new Paint();
        mPaint_Light.setColor( Color.WHITE );
        mPaint_Light.setStrokeJoin( Paint.Join.ROUND );
        mPaint_Light.setStrokeCap( Paint.Cap.ROUND );
        mPaint_Light.setStrokeWidth( 1 );
        mPaint_Window = new Paint();
        mPaint_Window.setColor( Color.BLUE );

        Log.d( "nrs", mRect1.centerX() + "|" + mRect1.centerY() );
        Log.d( "nrs", mRect2.centerX() + "|" + mRect2.centerY() );
        Log.d( "nrs", mRect3.centerX() + "|" + mRect3.centerY() );
        Log.d( "nrs", mRect4.centerX() + "|" + mRect4.centerY() );
    }
    @Override
    protected void onDraw ( Canvas canvas ) {
        super.onDraw( canvas );
        if ( isInEditMode() )
            return;
        canvas.drawColor( Color.LTGRAY );
        drawMapTile( canvas );
        drawLights( canvas );
        drawDoor( canvas );
        drawWindow( canvas );
        drawLocation( canvas );
        Paint paint = new Paint();
        paint.setColor( Color.RED );
        paint.setStrokeWidth( 4 );
        canvas.drawLine( 0, 700, 750, 700, paint );
        canvas.drawLine( 0, 100, 750, 100, paint );
    }
    private void drawLocation ( Canvas canvas ) {

    }
    private void drawWindow ( Canvas canvas ) {
        canvas.drawRect( new Rect( 150 * peiceSpace, 0, 220 * peiceSpace, 5 ), mPaint_Light );

    }
    private void drawDoor ( Canvas canvas ) {

    }
    private void drawLights ( Canvas canvas ) {
        canvas.drawRect( mRect1, mPaint_Light );
        canvas.drawRect( mRect2, mPaint_Light );
        canvas.drawRect( mRect3, mPaint_Light );
        canvas.drawRect( mRect4, mPaint_Light );
    }
    private void drawMapTile ( Canvas canvas ) {
        // horizontal
        for ( int i = 0 ; i < GRIDS_HEIGHT ; i++ ) {
            canvas.drawLine(
                    0, 0 + i * peiceSpace, GRIDS_WIDTH * peiceSpace, 0 + i * peiceSpace,
                    mPaint_Floor
            );
        }
        // vertical
        for ( int j = 0 ; j < GRIDS_WIDTH ; j++ ) {
            canvas.drawLine(
                    0 + j * peiceSpace, 0, 0 + j * peiceSpace, GRIDS_HEIGHT * peiceSpace,
                    mPaint_Floor
            );
        }
    }
    @Override
    protected void onMeasure ( int widthMeasureSpec, int heightMeasureSpec ) {
        super.onMeasure( widthMeasureSpec, heightMeasureSpec );
    }

    public void updateLocation ( int index, double orientation, double distance, double offset ) {
    }
    public void updateLocation (
            int x, int y, double orientation, double distance, double offset
    ) {
    }
    public void updateLocation (
            int index, int x, int y, double orientation, double distance, double offset
    ) {



    }
}
