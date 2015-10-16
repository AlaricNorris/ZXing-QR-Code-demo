/**
 *  UserLocation
 *  com.google.zxing.client.android.customview
 * 	Function: 	${TODO}
 *  date            author
 *  *****************************************************
 *  2015/10/16      AlaricNorris
 *	Copyright (c) 2015, TNT All Rights Reserved.
 */
package com.google.zxing.client.android.customview;

/**
 *  ClassName:  UserLocation
 *  Function:   ${TODO}  ADD FUNCTION
 *  Reason:     ${TODO}  ADD REASON
 *  @author AlaricNorris
 *  Contact:    Norris.sly@gmail.com
 *  @version Ver 1.0
 *  @since I used to be a programmer like you, then I took an arrow in the knee
 *	************************************************************************************************************************************************************************************************************
 * 	Modified By 	AlaricNorris		 2015/10/1614:11
 *	Modifications:	${TODO}
 *	************************************************************************************************************************************************************************************************************
 */
public class UserLocation {

    private int X;
    private int Y;
    private int Radius;
    public UserLocation () {
    }
    public UserLocation ( int x, int y, int radius ) {
        X = x;
        Y = y;
        Radius = radius;
    }
    public int getX () {
        return X;
    }
    public void setX ( int x ) {
        X = x;
    }
    public int getY () {
        return Y;
    }
    public void setY ( int y ) {
        Y = y;
    }
    public int getRadius () {
        return Radius;
    }
    public void setRadius ( int radius ) {
        this.Radius = radius;
    }
    @Override
    public String toString () {
        return "UserLocation{" +
                "X=" + X +
                ", Y=" + Y +
                ", Radius=" + Radius +
                '}';
    }
}
