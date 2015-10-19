/**
 *  LightLocation
 *  com.google.zxing.client.android
 * 	Function: 	${TODO}
 *  date            author
 *  *****************************************************
 *  2015/10/19      AlaricNorris
 *	Copyright (c) 2015, TNT All Rights Reserved.
 */
package com.google.zxing.client.android;

import java.io.Serializable;
/**
 *  ClassName:  LightLocation
 *  Function:   ${TODO}  ADD FUNCTION
 *  Reason:     ${TODO}  ADD REASON
 *  @author AlaricNorris
 *  Contact:    Norris.sly@gmail.com
 *  @version Ver 1.0
 *  @since I used to be a programmer like you, then I took an arrow in the knee
 *	************************************************************************************************************************************************************************************************************
 * 	Modified By 	AlaricNorris		 2015/10/199:30
 *	Modifications:	${TODO}
 *	************************************************************************************************************************************************************************************************************
 */
public class LightLocation implements Serializable {
    private int ID;
    private int X;
    private int Y;
    public LightLocation ( int ID, int x, int y ) {
        this.ID = ID;
        X = x;
        Y = y;
    }

    public int getID () {
        return ID;
    }
    public void setID ( int ID ) {
        this.ID = ID;
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
    @Override
    public String toString () {
        return "LightLocation{" +
                "ID=" + ID +
                ", X=" + X +
                ", Y=" + Y +
                '}';
    }
}
