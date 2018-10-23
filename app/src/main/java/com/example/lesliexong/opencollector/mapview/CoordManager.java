package com.example.lesliexong.opencollector.mapview;

import android.graphics.PointF;
import android.view.MotionEvent;

public class CoordManager {
    /**
     * x is along the direction of the width
     * y is along the direction of the height
     */
    private float stride;

    private float pixelWidth;
    private float pixelHeight;
    private float widthScale;
    private float heightScale;

    private PointF currentSCoord;  //coordinate on Source picture
    private PointF currentTCoord;  //coordinate on True coordination

    CoordManager(float width, float height, float pixelWidth, float pixelHeight) {
        this.pixelHeight = pixelHeight;
        this.pixelWidth = pixelWidth;

        currentSCoord = new PointF();
        currentTCoord = new PointF();

        widthScale = pixelWidth / width;
        heightScale = pixelHeight / height;

        this.stride = 1;
    }

    void setStride(float stride) {
        this.stride = stride;
    }

    /**
     * @param tapSCoord the tap point
     */
    void moveBySingleTap(PointF tapSCoord) {
        float deltaX = tapSCoord.x - currentSCoord.x;
        float deltaY = tapSCoord.y - currentSCoord.y;

        float moveX = Math.abs(deltaX) >= Math.abs(deltaY) ? (deltaX > 0 ? 1 : -1) * stride * widthScale : 0;
        float moveY = Math.abs(deltaX) >= Math.abs(deltaY) ? 0 : (deltaY > 0 ? 1 : -1) * stride * heightScale;

        moveInsideMapRange(moveX, moveY);
    }

    private void moveInsideMapRange(float moveX, float moveY) {

        float temp = currentSCoord.x + moveX;
        if (temp >= 0 && temp <= pixelWidth)
            currentSCoord.x = temp;

        temp = currentSCoord.y + moveY;
        if (temp >= 0 && temp <= pixelHeight)
            currentSCoord.y = temp;

        this.currentTCoord = sCoordToTCoord(currentSCoord);
    }

    private static final int FLING_MIN_DISTANCE = 200; // 移动最小距离
    private static final int FLING_MIN_VELOCITY = 200; // 移动最小速度

    public PointF moveByFling(MotionEvent e1, MotionEvent e2, float velocityX,
                              float velocityY) {
        float moveX = 0;
        float moveY = 0;

        //up
        if (e1.getY() - e2.getY() > FLING_MIN_DISTANCE
                && Math.abs(velocityY) > FLING_MIN_VELOCITY) {
            moveY = -stride * heightScale;
        }
        //down
        if (e2.getY() - e1.getY() > FLING_MIN_DISTANCE
                && Math.abs(velocityY) > FLING_MIN_VELOCITY) {
            moveY = stride * heightScale;
        }
        //left
        if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            moveX = -stride * widthScale;
        }
        //right
        if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            moveX = stride * widthScale;
        }

        moveInsideMapRange(moveX, moveY);
        return currentSCoord;
    }

    /**
     * Set the current position on the source map.
     *
     * @param currentSCoord current picture source coordinate
     */
    public void setCurrentSCoord(PointF currentSCoord) {
        this.currentSCoord = currentSCoord;
        this.currentTCoord = sCoordToTCoord(currentSCoord);
    }

    void setCurrentTCoord(PointF currentTCoord) {
        this.currentTCoord = currentTCoord;
        this.currentSCoord = tCoordToSCoord(currentTCoord);
    }

    PointF getCurrentTCoord() {
        return currentTCoord;
    }

    PointF getCurrentSCoord() {
        return currentSCoord;
    }

    PointF tCoordToSCoord(PointF tCoord) {
        return new PointF(tCoord.x * widthScale, tCoord.y * heightScale);
    }

    PointF sCoordToTCoord(PointF sCoord) {
        return new PointF(sCoord.x / widthScale, sCoord.y / heightScale);
    }

}
