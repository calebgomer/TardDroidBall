package com.erik.and.caleb.tarddroidball;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import com.erik.and.caleb.tarddroidball.learnopengles.RawResourceReader;
import com.erik.and.caleb.tarddroidball.learnopengles.ShaderHelper;
import com.erik.and.caleb.tarddroidball.learnopengles.TextureHelper;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renderers -- the static class GLES20 is used instead.
 */
public class GraphicsRenderer implements GLSurfaceView.Renderer
{
  /** Used for debug logs. */
  private static final String TAG = "GraphicsRenderer";

  private final Context mActivityContext;

  /**
   * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
   * of being located at the center of the universe) to world space.
   */
  private float[] mModelMatrix = new float[16];

  /**
   * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
   * it positions things relative to our eye.
   */
  private float[] mViewMatrix = new float[16];

  /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
  private float[] mProjectionMatrix = new float[16];

  /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
  private float[] mMVPMatrix = new float[16];

  /**
   * Stores a copy of the model matrix specifically for the light position.
   */
  private float[] mLightModelMatrix = new float[16];

  /** Store our model data in a float buffer. */
  private final FloatBuffer mCubePositions;
  private final FloatBuffer mCubeColors;
  private final FloatBuffer mCubeNormals;
  private final FloatBuffer mCubeTextureCoordinates;

  /** Store our model data in a float buffer. */
  // color and normal data is similar to cube, so we'll skip generating it
  private final FloatBuffer mTardisPositions;
  private final FloatBuffer mTardisTextureCoordinates;

  /** Pokeball Parts */
  //POKEBALL PARTS TODO

  /** This will be used to pass in the transformation matrix. */
  private int mMVPMatrixHandle;

  /** This will be used to pass in the modelview matrix. */
  private int mMVMatrixHandle;

  /** This will be used to pass in the light position. */
  private int mLightPosHandle;

  /** This will be used to pass in the texture. */
  private int mTextureUniformHandle;

  /** This will be used to pass in model position information. */
  private int mPositionHandle;

  /** This will be used to pass in model color information. */
  private int mColorHandle;

  /** This will be used to pass in model normal information. */
  private int mNormalHandle;

  /** This will be used to pass in model texture coordinate information. */
  private int mTextureCoordinateHandle;

  /** How many bytes per float. */
  private final int mBytesPerFloat = 4;

  /** Size of the position data in elements. */
  private final int mPositionDataSize = 3;

  /** Size of the color data in elements. */
  private final int mColorDataSize = 4;

  /** Size of the normal data in elements. */
  private final int mNormalDataSize = 3;

  /** Size of the texture coordinate data in elements. */
  private final int mTextureCoordinateDataSize = 2;

  /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
   *  we multiply this by our transformation matrices. */
  private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

  /** Used to hold the current position of the light in world space (after transformation via model matrix). */
  private final float[] mLightPosInWorldSpace = new float[4];

  /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
  private final float[] mLightPosInEyeSpace = new float[4];

  /** This is a handle to our cube shading program. */
  private int mProgramHandle;

  /** This is a handle to our light point program. */
  private int mPointProgramHandle;

  /** This is a handle to our texture data. */
  private int mGrassSideTextureDataHandle;
  private int mGrassTopTextureDataHandle;
  private int mTardisSideTextureHandle;

  /**
   * Initialize the model data.
   */
  public GraphicsRenderer(final Context context) {
    mActivityContext = context;

    // Initialize the buffers.
    float[] cubePositionData = getCubePositionData();
    mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubePositions.put(cubePositionData).position(0);

    float[] cubeColorData = getCubeColorData();
    mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeColors.put(cubeColorData).position(0);

    float[] cubeNormalData = getCubeNormalData();
    mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeNormals.put(cubeNormalData).position(0);

    float[] cubeTextureCoordinateData = getCubeTextureCoordinateData();
    mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);

    float[] tardisPositionData = getTardisPositionData();
    mTardisPositions = ByteBuffer.allocateDirect(tardisPositionData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mTardisPositions.put(tardisPositionData).position(0);

    float[] tardisTextureCoordinateData = getTardisTextureCoordinateData();
    mTardisTextureCoordinates = ByteBuffer.allocateDirect(tardisTextureCoordinateData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mTardisTextureCoordinates.put(tardisTextureCoordinateData).position(0);
  }



  protected String getVertexShader()
  {
    return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
  }

  protected String getFragmentShader()
  {
    return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
  }

  @Override
  public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
  {
    // Set the background clear color to black.
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

    // Use culling to remove back faces.
    GLES20.glEnable(GLES20.GL_CULL_FACE);

    // Enable depth testing
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);

    // The below glEnable() call is a holdover from OpenGL ES 1, and is not needed in OpenGL ES 2.
    // Enable texture mapping
    // GLES20.glEnable(GLES20.GL_TEXTURE_2D);

    // Position the eye in front of the origin.
    final float eyeX = 0.0f;
    final float eyeY = 0.0f;
    final float eyeZ = 2.0f;

    // We are looking toward the distance
    final float lookX = 0.0f;
    final float lookY = 0.0f;
    final float lookZ = -5.0f;

    // Set our up vector. This is where our head would be pointing were we holding the camera.
    final float upX = 0.0f;
    final float upY = 1.0f;
    final float upZ = 0.0f;

    // Set the view matrix. This matrix can be said to represent the camera position.
    // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
    // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
    Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

    final String vertexShader = getVertexShader();
    final String fragmentShader = getFragmentShader();

    final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
    final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

    mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
        new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});

    // Define a simple shader program for our point.
    final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
    final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

    final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
    final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
    mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
        new String[] {"a_Position"});

    // Load the grass side texture
    mGrassSideTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.grass_side);

    // Load the grass top texture
    mGrassTopTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.grass_top);

    // Load the tardis' side texture
    mTardisSideTextureHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.tardis_side_crop);
  }

  @Override
  public void onSurfaceChanged(GL10 glUnused, int width, int height)
  {
    // Set the OpenGL viewport to the same size as the surface.
    GLES20.glViewport(0, 0, width, height);

    // Create a new perspective projection matrix. The height will stay the same
    // while the width will vary as per aspect ratio.
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
  public void onDrawFrame(GL10 glUnused)
  {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    // Make a pokeball
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 0.0f, 0, -7.0f);
    Matrix.scaleM(mModelMatrix, 0, 5.0f, 5.0f, 5.0f);
    //POKEBALLDRAWING TODO

    // Do a complete rotation every 10 seconds.
    long time = SystemClock.uptimeMillis() % 10000L;
    float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

    // Set our per-vertex lighting program.
    GLES20.glUseProgram(mProgramHandle);

    // Set program handles for cube drawing and set the active texture unit to texture unit 0.
    startDrawingCubes();

    // Make side of the grass
    setupCubeWithTexture(mGrassSideTextureDataHandle);
    setupCubeLightingWithAngle(angleInDegrees);
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 0.0f, -4.0f, -7.0f);
    drawCube();

    // Make the top of the grass
    setupCubeWithTexture(mGrassTopTextureDataHandle);
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 0.0f, -3.99f, -7.0f);
    Matrix.scaleM(mModelMatrix, 0, 1.0f, 1.01f, 1.0f);
    drawCube();

    // Make a tardis
    setupCubeWithTexture(mTardisSideTextureHandle);
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 0.0f, 1.0f, -7.0f);
    Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
    drawTardis();

    // Make 2nd tardis
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, -5.0f, 1.0f, -7.0f);
    Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
    drawTardis();

    // Make 3rd tardis
    Matrix.setIdentityM(mModelMatrix, 0);
    Matrix.translateM(mModelMatrix, 0, 5.0f, 1.0f, -7.0f);
    Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
    drawTardis();

    // Draw a point to indicate the light.
    GLES20.glUseProgram(mPointProgramHandle);
    drawLight();
  }

  private void startDrawingCubes() {
    // Set program handles for cube drawing.
    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
    mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
    mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
    mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
    mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
    mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
    mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
    mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

    // Set the active texture unit to texture unit 0.
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
  }

  private void setupCubeWithTexture(int textureId) {
    // Bind the texture to this unit.
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

    // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
    GLES20.glUniform1i(mTextureUniformHandle, 0);
  }

  private void setupCubeLightingWithAngle(float angleInDegrees) {

    // Calculate position of the light. Rotate and then push into the distance.
    Matrix.setIdentityM(mLightModelMatrix, 0);
    Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);
    Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
    Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);

    Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
    Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);
  }

  /**
   * Draws a cube.
   */
  private void drawCube()
  {
    // Pass in the position information
    mCubePositions.position(0);
    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        0, mCubePositions);

    GLES20.glEnableVertexAttribArray(mPositionHandle);

    // Pass in the color information
    mCubeColors.position(0);
    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        0, mCubeColors);

    GLES20.glEnableVertexAttribArray(mColorHandle);

    // Pass in the normal information
    mCubeNormals.position(0);
    GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
        0, mCubeNormals);

    GLES20.glEnableVertexAttribArray(mNormalHandle);

    // Pass in the texture coordinate information
    mCubeTextureCoordinates.position(0);
    GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
        0, mCubeTextureCoordinates);

    GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

    // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
    // (which currently contains model * view).
    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

    // Pass in the modelview matrix.
    GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

    // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
    // (which now contains model * view * projection).
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

    // Pass in the combined matrix.
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    // Pass in the light position in eye space.
    GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

    // Draw the cube.
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
  }

  private void drawTardis()
  {
    // Pass in the position information
    mTardisPositions.position(0);
    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        0, mTardisPositions);

    GLES20.glEnableVertexAttribArray(mPositionHandle);

    // Pass in the color information
    mCubeColors.position(0);
    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        0, mCubeColors);

    GLES20.glEnableVertexAttribArray(mColorHandle);

    // Pass in the normal information
    mCubeNormals.position(0);
    GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false,
        0, mCubeNormals);

    GLES20.glEnableVertexAttribArray(mNormalHandle);

    // Pass in the texture coordinate information
    mTardisTextureCoordinates.position(0);
    GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false,
        0, mTardisTextureCoordinates);

    GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);

    // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
    // (which currently contains model * view).
    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

    // Pass in the modelview matrix.
    GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

    // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
    // (which now contains model * view * projection).
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

    // Pass in the combined matrix.
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    // Pass in the light position in eye space.
    GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

    // Draw the tardis.
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
  }

  /**
   * Draws a point representing the position of the light.
   */
  private void drawLight()
  {
    final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
    final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

    // Pass in the position.
    GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

    // Since we are not using a buffer object, disable vertex arrays for this attribute.
    GLES20.glDisableVertexAttribArray(pointPositionHandle);

    // Pass in the transformation matrix.
    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
    GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    // Draw the point.
    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
  }

  private float[] getCubePositionData() {
    // Define points for a cube.
    float cx = 10.0f; // cube X length
    float cy = 1.25f; // cube Y length
    float cz = 2.0f; // cube Z length

    // X, Y, Z
    final float[] cubePositionData =
        {
            // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
            // if the points are counter-clockwise we are looking at the "front". If not we are looking at
            // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
            // usually represent the backside of an object and aren't visible anyways.

            // Front face
            -cx,  cy,  cz,
            -cx, -cy,  cz,
            cx,  cy,  cz,
            -cx, -cy,  cz,
            cx, -cy,  cz,
            cx,  cy,  cz,

            // Right face
            cx,  cy,  cz,
            cx, -cy,  cz,
            cx,  cy, -cz,
            cx, -cy,  cz,
            cx, -cy, -cz,
            cx,  cy, -cz,

            // Back face
            cx,  cy, -cz,
            cx, -cy, -cz,
            -cx,  cy, -cz,
            cx, -cy, -cz,
            -cx, -cy, -cz,
            -cx,  cy, -cz,

            // Left face
            -cx,  cy, -cz,
            -cx, -cy, -cz,
            -cx,  cy,  cz,
            -cx, -cy, -cz,
            -cx, -cy,  cz,
            -cx,  cy,  cz,

            // Top face
            -cx,  cy, -cz,
            -cx,  cy,  cz,
            cx,  cy, -cz,
            -cx,  cy,  cz,
            cx,  cy,  cz,
            cx,  cy, -cz,

            // Bottom face
            cx, -cy, -cz,
            cx, -cy,  cz,
            -cx, -cy, -cz,
            cx, -cy,  cz,
            -cx, -cy,  cz,
            -cx, -cy, -cz,
        };

    return cubePositionData;
  }

  private float[] getCubeColorData() {

    // make all the sides white
    final float[] cubeColorData = new float[6*6*4];
    Arrays.fill(cubeColorData, 1.0f);

    return cubeColorData;
  }

  private float[] getCubeNormalData() {

    // X, Y, Z
    // The normal is used in light calculations and is a vector which points
    // orthogonal to the plane of the surface. For a cube model, the normals
    // should be orthogonal to the points of each face.
    final float[] cubeNormalData =
        {
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

    return cubeNormalData;
  }

  private float[] getCubeTextureCoordinateData() {

    float tx = 6.0f; //texture density
    float ty = 1.0f;
    float tz = 0.0f; //zero texture thing, because it looks nice

    // S, T (or X, Y)
    // Texture coordinate data.
    // Because images have a Y axis pointing downward (values increase as you move down the image) while
    // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
    // What's more is that the texture coordinates are the same for every face.
    final float[] cubeTextureCoordinateData =
        {
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

    return cubeTextureCoordinateData;
  }

  private float[] getTardisPositionData() {
    // Define points for a cube.
    float cx = 1.0f; // cube X length
    float cy = 2.0f; // cube Y length
    float cz = 1.0f; // cube Z length

    // X, Y, Z
    final float[] cubePositionData =
        {
            // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
            // if the points are counter-clockwise we are looking at the "front". If not we are looking at
            // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
            // usually represent the backside of an object and aren't visible anyways.

            // Front face
            -cx,  cy,  cz,
            -cx, -cy,  cz,
            cx,  cy,  cz,
            -cx, -cy,  cz,
            cx, -cy,  cz,
            cx,  cy,  cz,

            // Right face
            cx,  cy,  cz,
            cx, -cy,  cz,
            cx,  cy, -cz,
            cx, -cy,  cz,
            cx, -cy, -cz,
            cx,  cy, -cz,

            // Back face
            cx,  cy, -cz,
            cx, -cy, -cz,
            -cx,  cy, -cz,
            cx, -cy, -cz,
            -cx, -cy, -cz,
            -cx,  cy, -cz,

            // Left face
            -cx,  cy, -cz,
            -cx, -cy, -cz,
            -cx,  cy,  cz,
            -cx, -cy, -cz,
            -cx, -cy,  cz,
            -cx,  cy,  cz,

            // Top face
            -cx,  cy, -cz,
            -cx,  cy,  cz,
            cx,  cy, -cz,
            -cx,  cy,  cz,
            cx,  cy,  cz,
            cx,  cy, -cz,

            // Bottom face
            cx, -cy, -cz,
            cx, -cy,  cz,
            -cx, -cy, -cz,
            cx, -cy,  cz,
            -cx, -cy,  cz,
            -cx, -cy, -cz,
        };

    return cubePositionData;
  }

  private float[] getTardisTextureCoordinateData() {

    float tx = 1.0f; //texture density
    float ty = 1.0f;
    float tz = 0.0f; //zero texture thing, because it looks nice

    // S, T (or X, Y)
    // Texture coordinate data.
    // Because images have a Y axis pointing downward (values increase as you move down the image) while
    // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
    // What's more is that the texture coordinates are the same for every face.
    final float[] cubeTextureCoordinateData =
        {
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

    return cubeTextureCoordinateData;
  }
}


//
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.opengl.Matrix;
//import android.util.Log;
//import com.erik.and.caleb.tarddroidball.shapes.Square;
//import com.erik.and.caleb.tarddroidball.shapes.Triangle;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//import java.util.ArrayList;
//
//public class GraphicsRenderer implements GLSurfaceView.Renderer {
//
//  public static String TAG = "TARDDROIDBALL-RENERER";
//
//  private Triangle mTriangle;
//  private Square mSquare;
//
//  private final float[] mMVPMatrix = new float[16];
//  private final float[] mProjMatrix = new float[16];
//  private final float[] mVMatrix = new float[16];
//  private final float[] mRotationMatrix = new float[16];
//
//  // Declare as volatile because we are updating it from another thread
//  public volatile float mAngle;
//
//  public volatile ArrayList<Finger> mFingers;
//
//  @Override
//  public void onSurfaceCreated(GL10 unused, EGLConfig config) {
//
//    // Set the background frame color
//    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//
//    mTriangle = new Triangle();
//    mSquare = new Square();
//
//    mFingers = new ArrayList<Finger>();
//  }
//
//  @Override
//  public void onDrawFrame(GL10 unused) {
//
//    // Draw background color
//    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//
//    // Set the camera position (View matrix)
//    Matrix.setLookAtM(mVMatrix, 0, 1, 1, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
//
//    // Calculate the projection and view transformation
//    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);
//
//    // Draw square
//    mSquare.draw(mMVPMatrix);
//
//    // Create a rotation for the triangle
////    long time = SystemClock.uptimeMillis() % 4000L;
////    float angle = 0.090f * ((int) time);
//    Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
//
//    // Combine the rotation matrix with the projection and camera view
//    Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, mMVPMatrix, 0);
//
//    // Draw triangle
//    mTriangle.draw(mMVPMatrix);
//  }
//
//  @Override
//  public void onSurfaceChanged(GL10 unused, int width, int height) {
//    // Adjust the viewport based on geometry changes,
//    // such as screen rotation
//    GLES20.glViewport(0, 0, width, height);
//
//    float ratio = (float) width / height;
//
//    // this projection matrix is applied to object coordinates
//    // in the onDrawFrame() method
//    Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
//
//  }
//
//  public static int loadShader(int type, String shaderCode) {
//
//    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
//    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
//    int shader = GLES20.glCreateShader(type);
//
//    // add the source code to the shader and compile it
//    GLES20.glShaderSource(shader, shaderCode);
//    GLES20.glCompileShader(shader);
//
//    return shader;
//  }
//
//  /**
//   * Utility method for debugging OpenGL calls. Provide the name of the call
//   * just after making it:
//   * <p/>
//   * <pre>
//   * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
//   * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
//   *
//   * If the operation is not successful, the check throws an error.
//   *
//   * @param glOperation - Name of the OpenGL call to check.
//   */
//  public static void checkGlError(String glOperation) {
//    int error;
//    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
//      Log.e(TAG, glOperation + ": glError " + error);
//      throw new RuntimeException(glOperation + ": glError " + error);
//    }
//  }
//}
//
////  private FloatBuffer cubeVertBuffer,
////      cubeColourBuffer;
////
////  private ShortBuffer cubeIndexBuffer;
////
////  private int	mProgram,
////      maPositionHandle,
////      maColourHandle,
////      muMVPMatrixHandle;
////
////  private float[] mMVPMatrix = new float[16];
////  private float[] mMMatrix = new float[16];
////  private float[] mVMatrix = new float[16];
////  private float[] mProjMatrix = new float[16];
////
////  private final String vertexShaderCode =
////      "uniform mat4 uMVPMatrix;	\n" +
////          "attribute vec4 vPosition;	\n" +
////          "attribute vec4 vColour;	\n" +
////          "varying vec4 fColour;		\n" +
////          "void main(){				\n" +
////          "	fColour = vColour;		\n" +
////          "	gl_Position = uMVPMatrix * vPosition;\n" +
////          "}							\n";
////
////  private final String fragmentShaderCode =
////      "precision mediump float;	\n" +
////          "varying vec4 fColour;		\n" +
////          "void main(){				\n" +
////          "	gl_FragColor = fColour;	\n" +
////          //"	gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); \n" +
////          "}							\n";
////
////  private int loadShader(int type, String shaderCode){
////    //Create a vertex shader type
////    //or a fragment shader type
////    int shader = GLES20.glCreateShader(type);
////
////    //add the source code to the shader and compile it
////    GLES20.glShaderSource(shader, shaderCode);
////    GLES20.glCompileShader(shader);
////
////    return shader;
////  }
////
////  private void initShapes(){
////
////    float[] cubeCoords = {
////        //X, Y, Z
////        -1, -1, -1,
////        1, -1, -1,
////        1,  1, -1,
////        -1,  1, -1,
////        -1, -1,  1,
////        1, -1,  1,
////        1,  1,  1,
////        -1,  1,  1
////    };
////
////    float[] cubeColours = {
////        0, 0, 0, 1,
////        0, 0, 0, 1,
////        0, 0, 0, 1,
////        0, 0, 0, 1,
////        0, 0, 1, 1,
////        0, 0, 1, 1,
////        0, 0, 1, 1,
////        0, 0, 1, 1
////    };
////
////    short[] cubeIndices = {
////        0, 4, 5,
////        0, 5, 1,
////        1, 5, 6,
////        1, 6, 2,
////        2, 6, 7,
////        2, 7, 3,
////        3, 7, 4,
////        3, 4, 0,
////        4, 7, 6,
////        4, 6, 5,
////        3, 0, 1,
////        3, 1, 2
////    };
////
////    cubeVertBuffer = ByteBuffer.allocateDirect(cubeCoords.length * 4)
////        .order(ByteOrder.nativeOrder()).asFloatBuffer();
////    cubeVertBuffer.put(cubeCoords).position(0);
////
////    cubeColourBuffer = ByteBuffer.allocateDirect(cubeColours.length * 4)
////        .order(ByteOrder.nativeOrder()).asFloatBuffer();
////    cubeColourBuffer.put(cubeColours).position(0);
////
////    cubeIndexBuffer = ByteBuffer.allocateDirect(cubeIndices.length * 4)
////        .order(ByteOrder.nativeOrder()).asShortBuffer();
////    cubeIndexBuffer.put(cubeIndices).position(0);
////  }
////
////  public void onSurfaceCreated(GL10 unused, EGLConfig config) {
////
////    //Set the background frame color
////    GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1);
////    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
////    GLES20.glDepthFunc(GLES20.GL_LEQUAL);
////    initShapes();
////
////    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
////    int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
////
////    mProgram = GLES20.glCreateProgram();
////    GLES20.glAttachShader(mProgram, vertexShader);
////    GLES20.glAttachShader(mProgram, fragmentShader);
////    GLES20.glLinkProgram(mProgram);
////
////    //get handle to the vertex shaders vPos member
////    maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
////    maColourHandle = GLES20.glGetAttribLocation(mProgram, "vColour");
////    muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
////
////    Matrix.setIdentityM(mMMatrix, 0);
////    Matrix.rotateM(mMMatrix, 0, -40, 1, -1, 0);
////
////  }
////
////  public void onSurfaceChanged(GL10 unused, int width, int height) {
////    GLES20.glViewport(0, 0, width, height);
////
////    float ratio = (float) width / height;
////
////    Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
////    Matrix.setLookAtM(mVMatrix, 0, 0.0f, 0.0f, -5.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
////
////  }
////
////  public void onDrawFrame(GL10 unused) {
////
////    //Redraw background color
////    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
////
////    //Add program
////    GLES20.glUseProgram(mProgram);
////
////    //Prepare the cube data
////    GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertBuffer);
////    GLES20.glEnableVertexAttribArray(maPositionHandle);
////    GLES20.glVertexAttribPointer(maColourHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColourBuffer);
////    GLES20.glEnableVertexAttribArray(maColourHandle);
////
////    //Rotate cube
////    Matrix.rotateM(mMMatrix, 0, 3f, 6f, 2.7f, 3.5f);
////
////    //Set up MVP
////    Matrix.setIdentityM(mMVPMatrix, 0);
////    Matrix.multiplyMM(mMVPMatrix, 0, mMMatrix, 0, mMVPMatrix, 0);
////    Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMVPMatrix, 0);
////    Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
////
////    GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
////
////    //Draw the cube
////    GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_SHORT, cubeIndexBuffer);
////  }