package com.example.lesliexong.opencollector.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.example.lesliexong.opencollector.R;
import com.example.lesliexong.opencollector.collectorservice.Fingerprint;

import java.util.LinkedList;
import java.util.List;


public class PinView extends SubsamplingScaleImageView {

    private final Paint paint = new Paint();
    private final PointF vPin = new PointF();

    private PointF currentPoint;
    private List<PointF> fingerprintPoints;

    private Bitmap currentPin;
    private Bitmap completedPin;
    private Bitmap beaconPin;

    private CoordManager coordManager;

    public PinView(Context context) {
        this(context, null);
        initialise();
    }

    public PinView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {
        //todo adaptive size of the icon of position
        float density = getResources().getDisplayMetrics().densityDpi;
        setMaximumDpi((int) density);

        currentPin = BitmapFactory.decodeResource(this.getResources(), R.drawable.location_marker);
        float w = (density / 1500f) * currentPin.getWidth();
        float h = (density / 1500f) * currentPin.getHeight();
        currentPin = Bitmap.createScaledBitmap(currentPin, (int) w, (int) h, true);

        completedPin = BitmapFactory.decodeResource(this.getResources(), R.drawable.completed_point);
        w = (density / 6000) * completedPin.getWidth();
        h = (density / 6000f) * completedPin.getHeight();
        completedPin = Bitmap.createScaledBitmap(completedPin, (int) w, (int) h, true);
    }

    public void addFingerprintPoint(Fingerprint f) {
        if (fingerprintPoints == null)
            fingerprintPoints = new LinkedList<>();

        this.fingerprintPoints.add(coordManager.tCoordToSCoord(new PointF(f.x, f.y)));
        invalidate();
    }

    //Use to sync the collected point retrieved from server
    public void setFingerprintPoints(List<Fingerprint> result) {
        if (fingerprintPoints == null)
            fingerprintPoints = new LinkedList<>();
        else
            fingerprintPoints.clear();

        for (Fingerprint f : result) {
            fingerprintPoints.add(coordManager.tCoordToSCoord(new PointF(f.x, f.y)));
        }

        invalidate();
    }

    public void setCurrentTPosition(PointF p) {
        coordManager.setCurrentTCoord(p);
        this.currentPoint = coordManager.getCurrentSCoord();
        invalidate();
    }

    public PointF getCurrentTCoord() {
        return coordManager.getCurrentTCoord();
    }

    public void initialCoordManager(float width, float height) {
        this.coordManager = new CoordManager(width, height, this.getSWidth(), this.getSHeight());
    }

    public void setStride(float stride) {
        this.coordManager.setStride(stride);
    }

    public void moveBySingleTap(MotionEvent e) {
        PointF sCoord = this.viewToSourceCoord(e.getX(), e.getY());
        coordManager.moveBySingleTap(sCoord);
        this.setCurrentTPosition(coordManager.getCurrentTCoord());
    }

    public PointF getEventPosition(MotionEvent e) {
        PointF sCoord = this.viewToSourceCoord(e.getX(), e.getY());
        return coordManager.sCoordToTCoord(sCoord);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }

        paint.setAntiAlias(true);

        if (currentPoint != null && currentPin != null) {
            sourceToViewCoord(currentPoint, vPin);
            float vX = vPin.x - (currentPin.getWidth() / 2);
            float vY = vPin.y - (currentPin.getHeight() / 2);
            canvas.drawBitmap(currentPin, vX, vY, paint);
        }

        if (fingerprintPoints != null && completedPin != null)
            for (PointF pointF : fingerprintPoints) {
                sourceToViewCoord(pointF, vPin);
                float vX = vPin.x - (completedPin.getWidth() / 2);
                float vY = vPin.y - (completedPin.getHeight() / 2);
                canvas.drawBitmap(completedPin, vX, vY, paint);
            }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
