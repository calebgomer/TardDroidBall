package com.erik.and.caleb.tarddroidball;

/************************************************
 * Caleb Gomer and Erik Kremer
 * Graphics Android Project 1
 * Mashup of Doctor Who and Minecraft on Android
 *
 * We learned some of our techniques from
 * learnopengles.com's Android tutorials
 ***********************************************/

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class GraphicsActivity extends Activity {
  private GLSurfaceView mGLSurfaceView;
  private GraphicsRenderer mRenderer;
  private Button homeTardisButton;
  private float mPreviousX;
  private float mPreviousY;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
    final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

    mGLSurfaceView = new GLSurfaceView(this);

    if (supportsEs2) {
      mGLSurfaceView.setEGLContextClientVersion(2);
      mRenderer = new GraphicsRenderer(this);
      mGLSurfaceView.setRenderer(mRenderer);
    } else {
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
          }
        } else if (e.getPointerCount() == 2) {
          e.getPointerCoords(0, finger);

          float dx = finger.x - mPreviousX;
          float dy = -(finger.y - mPreviousY);
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
        break;

      case MotionEvent.ACTION_UP:
        mRenderer.mFingers.clear();
        break;
    }

    return true;
  }
}