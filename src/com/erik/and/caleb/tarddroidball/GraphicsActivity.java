package com.erik.and.caleb.tarddroidball;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class GraphicsActivity extends Activity {
  /**
   * Hold a reference to our GLSurfaceView
   */
  private GLSurfaceView mGLSurfaceView;
  private Button homeTardisButton;
  private GraphicsRenderer mRenderer;
  private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
  private float mPreviousX;
  private float mPreviousY;
  private boolean homeTardis;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mGLSurfaceView = new GLSurfaceView(this);


    // Check if the system supports OpenGL ES 2.0.
    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
    final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

    if (supportsEs2) {
      // Request an OpenGL ES 2.0 compatible context.
      mGLSurfaceView.setEGLContextClientVersion(2);

      // Set the renderer to our demo renderer, defined below.
      mRenderer = new GraphicsRenderer(this);
      mGLSurfaceView.setRenderer(mRenderer);
    } else {
      // This is where you could create an OpenGL ES 1.x compatible
      // renderer if you wanted to support both ES 1 and ES 2.
      return;
    }

    setContentView(R.layout.tardball);
    ((RelativeLayout) findViewById(R.id.lyt_tardball)).addView(mGLSurfaceView, 0);

    homeTardisButton = (Button) findViewById(R.id.btn_home_tardis);
    homeTardisButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (mRenderer.mFingers.size() > 0)
          mRenderer.mFingers.get(0).setHomeTardis(true);
        else
          mRenderer.mFingers.add(0, new Finger(true));
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    mGLSurfaceView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mGLSurfaceView.onPause();
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    // MotionEvent reports input details from the touch screen
    // and other input controls. In this case, you are only
    // interested in events where the touch position changed.

//    float x = e.getX();
//    float y = e.getY();
    //Log.d("Graphics", "Pointers: " + e.getPointerCount());

    switch (e.getAction()) {
      case MotionEvent.ACTION_MOVE:

        mRenderer.mFingers.clear();
        MotionEvent.PointerCoords finger = new MotionEvent.PointerCoords();
        if (e.getPointerCount() == 1) {
          for (int i = 0; i < e.getPointerCount(); i++) {
            e.getPointerCoords(i, finger);

            float fx = finger.x;
            float fy = finger.y;
            float dx = fx - mPreviousX;
            float dy = -(fy - mPreviousY);
            mPreviousX = fx;
            mPreviousY = fy;
            mRenderer.mFingers.add(new Finger(fx, dx * 0.01f, fy, dy * 0.01f));
            Log.d("Graphics", String.format("DX: %f DY: %f", dx, dy));
          }
        } else if (e.getPointerCount() == 2) {
          e.getPointerCoords(0, finger);

          float dx = finger.x - mPreviousX;
          float dy = mPreviousX - finger.x;
          mPreviousX = finger.x;
          mPreviousY = finger.y;

          mRenderer.mFingers.add(new Finger(dx, dy));
        }
        break;

      case MotionEvent.ACTION_DOWN:
        MotionEvent.PointerCoords finger0 = new MotionEvent.PointerCoords();
        if (e.getPointerCount() > 0) {
          e.getPointerCoords(0, finger0);
          mPreviousX = finger0.x;
          mPreviousY = finger0.y;
        }
//        Toast.makeText(getApplicationContext(), "Down", Toast.LENGTH_SHORT).show();
        break;

      case MotionEvent.ACTION_UP:
        mRenderer.mFingers.clear();
//        Toast.makeText(getApplicationContext(), "Up", Toast.LENGTH_SHORT).show();
        break;

      case MotionEvent.ACTION_MASK:
        Toast.makeText(getApplicationContext(), "Mask", Toast.LENGTH_SHORT).show();
        break;
    }

    return true;
  }
}