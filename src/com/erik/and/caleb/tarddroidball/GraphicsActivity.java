package com.erik.and.caleb.tarddroidball;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GraphicsActivity extends Activity
{
  /** Hold a reference to our GLSurfaceView */
  private GLSurfaceView mGLSurfaceView;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    mGLSurfaceView = new GLSurfaceView(this);

    // Check if the system supports OpenGL ES 2.0.
    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
    final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

    if (supportsEs2)
    {
      // Request an OpenGL ES 2.0 compatible context.
      mGLSurfaceView.setEGLContextClientVersion(2);

      // Set the renderer to our demo renderer, defined below.
      mGLSurfaceView.setRenderer(new GraphicsRenderer(this));
    }
    else
    {
      // This is where you could create an OpenGL ES 1.x compatible
      // renderer if you wanted to support both ES 1 and ES 2.
      return;
    }

    setContentView(mGLSurfaceView);
  }

  @Override
  protected void onResume()
  {
    // The activity must call the GL surface view's onResume() on activity onResume().
    super.onResume();
    mGLSurfaceView.onResume();
  }

  @Override
  protected void onPause()
  {
    // The activity must call the GL surface view's onPause() on activity onPause().
    super.onPause();
    mGLSurfaceView.onPause();
  }
}

//import android.app.Activity;
//import android.content.Context;
//import android.opengl.GLSurfaceView;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.widget.Toast;
//
//public class GraphicsActivity extends Activity {
//
//  private GLSurfaceView mGLView;
//
//  public void onCreate(Bundle savedInstanceState) {
//    super.onCreate(savedInstanceState);
//
//    mGLView = new MyGLSurfaceView(this);
//    setContentView(mGLView);
//  }
//
//  class MyGLSurfaceView extends GLSurfaceView {
//
//    private final GraphicsRenderer mRenderer;
//
//    public MyGLSurfaceView(Context context) {
//      super(context);
//
//      setEGLContextClientVersion(2);
//
//      mRenderer = new GraphicsRenderer();
//      setRenderer(mRenderer);
////      setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//    }
//
//    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
//    private float mPreviousX;
//    private float mPreviousY;
//
//    @Override
//    public boolean onTouchEvent(MotionEvent e) {
//      // MotionEvent reports input details from the touch screen
//      // and other input controls. In this case, you are only
//      // interested in events where the touch position changed.
//
//      float x = e.getX();
//      float y = e.getY();
//      Log.d("Graphics","Pointers: "+e.getPointerCount());
//
//      mRenderer.mFingers.clear();
//      MotionEvent.PointerCoords finger = new MotionEvent.PointerCoords();
//      for (int i = 0; i < e.getPointerCount(); i++) {
//        e.getPointerCoords(i, finger);
//
//        mRenderer.mFingers.add(new Finger(e.getX(), e.getY(), 0));
//
//      }
//
//
//      switch (e.getAction()) {
//        case MotionEvent.ACTION_MOVE:
//
//          float dx = x - mPreviousX;
//          float dy = y - mPreviousY;
//
//          // reverse direction of rotation above the mid-line
//          if (y > getHeight() / 2) {
//            dx = dx * -1 ;
//          }
//
//          // reverse direction of rotation to left of the mid-line
//          if (x < getWidth() / 2) {
//            dy = dy * -1 ;
//          }
//
//          mRenderer.mAngle += (dx + dy) * TOUCH_SCALE_FACTOR;  // = 180.0f / 320
//          requestRender();
//          break;
//        case MotionEvent.ACTION_DOWN:
//          Toast.makeText(getApplicationContext(), "Down", Toast.LENGTH_SHORT).show();
//          break;
//        case MotionEvent.ACTION_UP:
//          Toast.makeText(getApplicationContext(), "Up", Toast.LENGTH_SHORT).show();
//          break;
//        case MotionEvent.ACTION_MASK:
//          Toast.makeText(getApplicationContext(), "Mask", Toast.LENGTH_SHORT).show();
//          break;
//      }
//
//      mPreviousX = x;
//      mPreviousY = y;
//      return true;
//    }
//  }
//}