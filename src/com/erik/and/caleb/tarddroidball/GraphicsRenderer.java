package com.erik.and.caleb.tarddroidball;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import com.learnopengles.android.RawResourceReader;
import com.learnopengles.android.ShaderHelper;
import com.learnopengles.android.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class GraphicsRenderer implements GLSurfaceView.Renderer {

  public volatile CopyOnWriteArrayList<Finger> mFingers;
  private ArrayList<CoolMatrix> mObjectMatrices;

  private final Context mActivityContext;

  private float[] mProjectionMatrix = new float[16];
  private float[] mViewMatrix = new float[16];
  private float[] mMVPMatrix = new float[16];
  private float[] mLightModelMatrix = new float[16];

  private final FloatBuffer mCubePositions;
  private final FloatBuffer mCubeColors;
  private final FloatBuffer mCubeNormals;
  private final FloatBuffer mCubeTextureCoordinates;
  private final FloatBuffer mTardisPositions;
  private final FloatBuffer mTardisTextureCoordinates;

  private int mMVPMatrixHandle;
  private int mMVMatrixHandle;
  private int mLightPosHandle;
  private int mTextureUniformHandle;
  private int mPositionHandle;
  private int mColorHandle;
  private int mNormalHandle;
  private int mTextureCoordinateHandle;

  private final int mBytesPerFloat = 4;
  private final int mPositionDataSize = 3;
  private final int mColorDataSize = 4;
  private final int mNormalDataSize = 3;
  private final int mTextureCoordinateDataSize = 2;

  private int red = 0, green = 1, blue = 2;
  private int posInModelSpace = 0, posInWorldSpace = 1, posInEyeSpace = 2;
  private final float[][][] mLightData = new float[3][3][4];

  private int mProgramHandle;
  private int mPointProgramHandle;
  private int mGrassSideTextureDataHandle;
  private int mGrassTopTextureDataHandle;
  private int mTardisSideTextureHandle;

  private float TARDIS_HEIGHT_MAX = 10;
  private float TARDIS_HEIGHT_MIN = -0.75f;
  private float redTardisY = TARDIS_HEIGHT_MIN;
  private float redTardisDirection = 1;
  private float redTardisSpeed = 0.1f;

  private float greenTardisY = TARDIS_HEIGHT_MIN;
  private float greenTardisX = 5.0f;
  private float greenTardisRotateX;
  private float greenTardisRotateY;

  float mLastAngle;

  public GraphicsRenderer(final Context context) {
    mActivityContext = context;

    mLightData[red][posInModelSpace][3] = 1.0f;
    mLightData[green][posInModelSpace][3] = 1.0f;
    mLightData[blue][posInModelSpace][3] = 1.0f;

    float[] groundPositionData = getGroundPositionData();
    mCubePositions = ByteBuffer.allocateDirect(groundPositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubePositions.put(groundPositionData).position(0);

    float[] groundColorData = getSquareColorData();
    mCubeColors = ByteBuffer.allocateDirect(groundColorData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeColors.put(groundColorData).position(0);

    float[] groundNormalData = getSquareNormalData();
    mCubeNormals = ByteBuffer.allocateDirect(groundNormalData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeNormals.put(groundNormalData).position(0);

    float[] groundTextureCoordinateData = getSquareTextureCoordinateData();
    mCubeTextureCoordinates = ByteBuffer.allocateDirect(groundTextureCoordinateData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeTextureCoordinates.put(groundTextureCoordinateData).position(0);

    float[] tardisPositionData = getTardisPositionData();
    mTardisPositions = ByteBuffer.allocateDirect(tardisPositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mTardisPositions.put(tardisPositionData).position(0);

    float[] tardisTextureCoordinateData = getTardisTextureCoordinateData();
    mTardisTextureCoordinates = ByteBuffer.allocateDirect(tardisTextureCoordinateData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
    mTardisTextureCoordinates.put(tardisTextureCoordinateData).position(0);
  }


  protected String getVertexShader() {
    return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
  }

  protected String getFragmentShader() {
    return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
  }

  @Override
  public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
    mFingers = new CopyOnWriteArrayList<Finger>();
    mObjectMatrices = new ArrayList<CoolMatrix>();

    GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
    GLES20.glEnable(GLES20.GL_CULL_FACE);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

    final float eyeX = 0.0f;
    final float eyeY = 0.0f;
    final float eyeZ = 2.0f;

    final float lookX = 0.0f;
    final float lookY = 0.0f;
    final float lookZ = -5.0f;

    final float upX = 0.0f;
    final float upY = 1.0f;
    final float upZ = 0.0f;

    Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

    final String vertexShader = getVertexShader();
    final String fragmentShader = getFragmentShader();

    final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
    final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

    mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[]{"a_Position", "a_Color", "a_Normal", "a_TexCoordinate"});

    final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
    final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

    final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
    final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
    mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, new String[]{"a_Position"});

    // Load in all of our png textures
    mGrassSideTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.grass_side);
    mGrassTopTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.grass_top);
    mTardisSideTextureHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.tard_tex);
  }

  @Override
  public void onSurfaceChanged(GL10 glUnused, int width, int height) {
    GLES20.glViewport(0, 0, width, height);
    final float ratio = (float) width / height;
    final float left = -ratio;
    final float right = ratio;
    final float bottom = -1.0f;
    final float top = 1.0f;
    final float near = 1.0f;
    final float far = 10.0f;
    Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
  }

  @Override
  public void onDrawFrame(GL10 glUnused) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    // animate the screen based on the system clock
    long time = SystemClock.uptimeMillis() % 10000L;
    float thisAngle = (360.0f / 10000.0f) * ((int) time);
    float angleInDegrees = thisAngle - mLastAngle;
    mLastAngle = thisAngle;

    moveRedTardis();

    moveGreenTardis();

    setupDrawingHandles();

    setupLighting();

    drawAllTheGroundWithLightAngle(angleInDegrees);

    drawWhiteTardisWithLightAngle(angleInDegrees);

    drawRedTardisWithLightingAngle(angleInDegrees);

    drawGreenTardis();

    letThereBeLight();
  }

  private void moveRedTardis() {
    if (redTardisY >= TARDIS_HEIGHT_MAX)
      redTardisDirection = -1;
    else if (redTardisY <= TARDIS_HEIGHT_MIN)
      redTardisDirection = 1;
    redTardisY += redTardisSpeed * redTardisDirection;
  }

  private void moveGreenTardis() {
    synchronized (mFingers) {
      Iterator it = mFingers.iterator();
      if (it.hasNext() && mObjectMatrices.size() >= 5) {
        Finger finger = (Finger) it.next();
        greenTardisRotateX = 0;
        greenTardisRotateY = 0;
        greenTardisX = 0;
        greenTardisY = 0;
        if (finger.homeTardis) {
          greenTardisX = 5.0f;
          greenTardisY = TARDIS_HEIGHT_MIN;
          Matrix.setIdentityM(mObjectMatrices.get(4).getMatrix(), 0);
          Matrix.translateM(mObjectMatrices.get(4).getMatrix(), 0, greenTardisX, greenTardisY, -7.0f);
          Matrix.rotateM(mObjectMatrices.get(4).getMatrix(), 0, -greenTardisRotateY, 1.0f, 0.0f, 0.0f);
          Matrix.rotateM(mObjectMatrices.get(4).getMatrix(), 0, greenTardisRotateX, 0.0f, 1.0f, 0.0f);
          mFingers.remove(finger);
        } else if (finger.translate) {
          Matrix.translateM(mObjectMatrices.get(4).getMatrix(), 0, finger.dx, finger.dy, 0.0f);
        } else if (finger.rotate) {
          Matrix.rotateM(mObjectMatrices.get(4).getMatrix(), 0, finger.rotationAngleX, 0.0f, 1.0f, 0.0f);
          Matrix.rotateM(mObjectMatrices.get(4).getMatrix(), 0, -finger.rotationAngleY, 1.0f, 0.0f, 0.0f);
        }
      }
    }
  }

  private void drawAllTheGroundWithLightAngle(float angleInDegrees) {
    // Make side of the grass
    float[] modelMatrix = new float[16];
    setupSquareWithTexture(mGrassSideTextureDataHandle);
    if (mObjectMatrices.size() < 1) {
      Matrix.setIdentityM(modelMatrix, 0);
      Matrix.translateM(modelMatrix, 0, 0.0f, -4.0f, -7.0f);
      try {
        mObjectMatrices.add(new CoolMatrix(modelMatrix));
      } catch (CoolMatrix.ThisIsNotAMatrixException tiname) {
        tiname.printStackTrace();
      }
    }
    drawGround(mObjectMatrices.get(0).getMatrix());

    // Make the top of the grass
    setupSquareWithTexture(mGrassTopTextureDataHandle);
    if (mObjectMatrices.size() < 2) {
      Matrix.setIdentityM(modelMatrix, 0);
      Matrix.translateM(modelMatrix, 0, 0.0f, -3.99f, -7.0f);
      Matrix.scaleM(modelMatrix, 0, 1.0f, 1.01f, 1.0f);
      try {
        mObjectMatrices.add(new CoolMatrix(modelMatrix));
      } catch (CoolMatrix.ThisIsNotAMatrixException tiname) {
        tiname.printStackTrace();
      }
    }
    drawGround(mObjectMatrices.get(1).getMatrix());
  }

  private void drawWhiteTardisWithLightAngle(float angleInDegrees) {
    float[] modelMatrix = new float[16];
    setupSquareWithTexture(mTardisSideTextureHandle);
    if (mObjectMatrices.size() < 3) {
      Matrix.setIdentityM(modelMatrix, 0);
      Matrix.translateM(modelMatrix, 0, 0.0f, 1.0f, -7.0f);
      Matrix.rotateM(modelMatrix, 0, -angleInDegrees, 0.0f, 1.0f, 0.0f);
      try {
        mObjectMatrices.add(new CoolMatrix(modelMatrix));
      } catch (CoolMatrix.ThisIsNotAMatrixException tiname) {
        tiname.printStackTrace();
      }
    }
    Matrix.rotateM(mObjectMatrices.get(2).getMatrix(), 0, -angleInDegrees, 0.0f, 1.0f, 0.0f);
    drawTardis(mObjectMatrices.get(2).getMatrix());
  }

  private void drawRedTardisWithLightingAngle(float angleInDegrees) {
    float[] modelMatrix = new float[16];
    if (mObjectMatrices.size() < 4) {
      Matrix.setIdentityM(modelMatrix, 0);
      Matrix.translateM(modelMatrix, 0, -5.0f, redTardisY, -7.0f);
      Matrix.rotateM(modelMatrix, 0, angleInDegrees * 2, 0.0f, 1.0f, 0.0f);
      try {
        mObjectMatrices.add(new CoolMatrix(modelMatrix));
      } catch (CoolMatrix.ThisIsNotAMatrixException tiname) {
        tiname.printStackTrace();
      }
    }
    Matrix.translateM(mObjectMatrices.get(3).getMatrix(), 0, 0.0f, redTardisSpeed * redTardisDirection, 0.0f);
    Matrix.rotateM(mObjectMatrices.get(3).getMatrix(), 0, -angleInDegrees, 0.0f, 1.0f, 0.0f);
    drawTardis(mObjectMatrices.get(3).getMatrix());
  }

  private void drawGreenTardis() {
    float[] modelMatrix = new float[16];
    if (mObjectMatrices.size() < 5) {
      Matrix.setIdentityM(modelMatrix, 0);
      Matrix.translateM(modelMatrix, 0, greenTardisX, greenTardisY, -7.0f);
      Matrix.rotateM(modelMatrix, 0, -greenTardisRotateY, 1.0f, 0.0f, 0.0f);
      Matrix.rotateM(modelMatrix, 0, greenTardisRotateX, 0.0f, 1.0f, 0.0f);
      try {
        mObjectMatrices.add(new CoolMatrix(modelMatrix));
      } catch (CoolMatrix.ThisIsNotAMatrixException tiname) {
        tiname.printStackTrace();
      }
    }
    drawTardis(mObjectMatrices.get(4).getMatrix());
  }

  private void setupDrawingHandles() {
    GLES20.glUseProgram(mProgramHandle);
    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
    mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
    mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
    mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
    mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
    mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
    mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
    mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
  }

  private void setupSquareWithTexture(int textureId) {
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
    GLES20.glUniform1i(mTextureUniformHandle, 0);
  }

  private void setupLighting() {
    Matrix.setIdentityM(mLightModelMatrix, 0);
    Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
    Matrix.rotateM(mLightModelMatrix, 0, mLastAngle, 0.0f, 1.0f, 0.0f);
    Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 3.0f);

    Matrix.multiplyMV(mLightData[red][posInWorldSpace], 0, mLightModelMatrix, 0, mLightData[red][posInModelSpace], 0);
    Matrix.multiplyMV(mLightData[red][posInEyeSpace], 0, mViewMatrix, 0, mLightData[red][posInWorldSpace], 0);
  }

  private void drawGround(float[] modelMatrix) {
    mCubePositions.position(0);
    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mCubePositions);
    GLES20.glEnableVertexAttribArray(mPositionHandle);

    mCubeColors.position(0);
    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, 0, mCubeColors);

    GLES20.glEnableVertexAttribArray(mColorHandle);

    mCubeNormals.position(0);
    GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, mCubeNormals);
    GLES20.glEnableVertexAttribArray(mNormalHandle);

    mCubeTextureCoordinates.position(0);
    GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
    GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
    GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    GLES20.glUniform3f(mLightPosHandle, mLightData[red][posInEyeSpace][0], mLightData[red][posInEyeSpace][1], mLightData[red][posInEyeSpace][2]);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6 * 6);
  }

  private void drawTardis(float[] modelMatrix) {
    mTardisPositions.position(0);
    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mTardisPositions);
    GLES20.glEnableVertexAttribArray(mPositionHandle);

    mCubeColors.position(0);
    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, 0, mCubeColors);
    GLES20.glEnableVertexAttribArray(mColorHandle);

    mCubeNormals.position(0);
    GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 0, mCubeNormals);
    GLES20.glEnableVertexAttribArray(mNormalHandle);

    mTardisTextureCoordinates.position(0);
    GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mTardisTextureCoordinates);
    GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, modelMatrix, 0);
    GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    GLES20.glUniform3f(mLightPosHandle, mLightData[red][posInEyeSpace][0], mLightData[red][posInEyeSpace][1], mLightData[red][posInEyeSpace][2]);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
  }

  private void letThereBeLight() {
    GLES20.glUseProgram(mPointProgramHandle);
    final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
    final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");
    GLES20.glVertexAttrib3f(pointPositionHandle, mLightData[red][posInModelSpace][0], mLightData[red][posInModelSpace][1], mLightData[red][posInModelSpace][2]);
    GLES20.glDisableVertexAttribArray(pointPositionHandle);
    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);
    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
  }

  private float[] getGroundPositionData() {
    float cx = 10.0f; // cube X length
    float cy = 1.25f; // cube Y length
    float cz = 2.0f; // cube Z length

    return new float[]{
        // Front face
        -cx, cy, cz,
        -cx, -cy, cz,
        cx, cy, cz,
        -cx, -cy, cz,
        cx, -cy, cz,
        cx, cy, cz,

        // Right face
        cx, cy, cz,
        cx, -cy, cz,
        cx, cy, -cz,
        cx, -cy, cz,
        cx, -cy, -cz,
        cx, cy, -cz,

        // Back face
        cx, cy, -cz,
        cx, -cy, -cz,
        -cx, cy, -cz,
        cx, -cy, -cz,
        -cx, -cy, -cz,
        -cx, cy, -cz,

        // Left face
        -cx, cy, -cz,
        -cx, -cy, -cz,
        -cx, cy, cz,
        -cx, -cy, -cz,
        -cx, -cy, cz,
        -cx, cy, cz,

        // Top face
        -cx, cy, -cz,
        -cx, cy, cz,
        cx, cy, -cz,
        -cx, cy, cz,
        cx, cy, cz,
        cx, cy, -cz,

        // Bottom face
        cx, -cy, -cz,
        cx, -cy, cz,
        -cx, -cy, -cz,
        cx, -cy, cz,
        -cx, -cy, cz,
        -cx, -cy, -cz,
    };
  }

  private float[] getSquareColorData() {

    // make all the sides white
    final float[] colorData = new float[6 * 6 * 4];
    Arrays.fill(colorData, 1.0f);

    return colorData;
  }

  private float[] getSquareNormalData() {

    return new float[]{
        // Front face
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,

        // Right face
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,

        // Back face
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,
        0.0f, 0.0f, -1.0f,

        // Left face
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,
        -1.0f, 0.0f, 0.0f,

        // Top face
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,

        // Bottom face
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f,
        0.0f, -1.0f, 0.0f
    };
  }

  private float[] getSquareTextureCoordinateData() {

    float tx = 6.0f; //texture density
    float ty = 1.0f;
    float tz = 0.0f; //zero texture thing, because it looks nicer that 0.0f

    return new float[]{
        // Front face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz,

        // Right face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz,

        // Back face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz,

        // Left face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz,

        // Top face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz,

        // Bottom face
        tz, tz,
        tz, ty,
        tx, tz,
        tz, ty,
        tx, ty,
        tx, tz
    };
  }

  private float[] getTardisPositionData() {
    float cx = 1.0f;
    float cy = 2.0f;
    float cz = 1.0f;

    return new float[]{
        // Front face
        -cx, cy, cz,
        -cx, -cy, cz,
        cx, cy, cz,
        -cx, -cy, cz,
        cx, -cy, cz,
        cx, cy, cz,

        // Right face
        cx, cy, cz,
        cx, -cy, cz,
        cx, cy, -cz,
        cx, -cy, cz,
        cx, -cy, -cz,
        cx, cy, -cz,

        // Back face
        cx, cy, -cz,
        cx, -cy, -cz,
        -cx, cy, -cz,
        cx, -cy, -cz,
        -cx, -cy, -cz,
        -cx, cy, -cz,

        // Left face
        -cx, cy, -cz,
        -cx, -cy, -cz,
        -cx, cy, cz,
        -cx, -cy, -cz,
        -cx, -cy, cz,
        -cx, cy, cz,

        // Top face
        -cx, cy, -cz,
        -cx, cy, cz,
        cx, cy, -cz,
        -cx, cy, cz,
        cx, cy, cz,
        cx, cy, -cz,

        // Bottom face
        cx, -cy, -cz,
        cx, -cy, cz,
        -cx, -cy, -cz,
        cx, -cy, cz,
        -cx, -cy, cz,
        -cx, -cy, -cz,
    };
  }

  private float[] getTardisTextureCoordinateData() {

    float tx = 0.5f; //texture density
    float ty = 0.5f;
    float tz = 0.0f; //zero texture thing, because it looks nice

    return new float[]{
        // Front face
        0.0f, 0.5f,
        0.0f, 1.0f,
        0.5f, 0.5f,
        0.0f, 1.0f,
        0.5f, 1.0f,
        0.5f, 0.5f,

        // Right face
        0.5f, 0.5f,
        0.5f, 1.0f,
        1.0f, 0.5f,
        0.5f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.5f,

        // Back face
        0.5f, 0.5f,
        0.5f, 1.0f,
        1.0f, 0.5f,
        0.5f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.5f,

        // Left face
        0.5f, 0.5f,
        0.5f, 1.0f,
        1.0f, 0.5f,
        0.5f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.5f,

        // Top face
        0.5f, 0.0f,
        0.5f, 0.5f,
        1.0f, 0.0f,
        0.5f, 0.5f,
        1.0f, 0.5f,
        1.0f, 0.0f,

        // Bottom face
        0.0f, 0.0f,
        0.0f, 0.5f,
        0.5f, 0.0f,
        0.0f, 0.5f,
        0.5f, 0.5f,
        0.5f, 0.0f
    };
  }
}