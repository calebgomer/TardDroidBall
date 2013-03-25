package com.erik.and.caleb.tarddroidball.shapes;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Cube {

  private FloatBuffer cubeVertBuffer,
      cubeColourBuffer;

  private ShortBuffer cubeIndexBuffer;

  private int mProgram,
      maPositionHandle,
      maColourHandle,
      muMVPMatrixHandle;

  private float[] mMVPMatrix = new float[16];
  private float[] mMMatrix = new float[16];
  private float[] mVMatrix = new float[16];
  private float[] mProjMatrix = new float[16];

  private final String vertexShaderCode =
      "uniform mat4 uMVPMatrix;	\n" +
          "attribute vec4 vPosition;	\n" +
          "attribute vec4 vColour;	\n" +
          "varying vec4 fColour;		\n" +
          "void main(){				\n" +
          "	fColour = vColour;		\n" +
          "	gl_Position = uMVPMatrix * vPosition;\n" +
          "}							\n";

  private final String fragmentShaderCode =
      "precision mediump float;	\n" +
          "varying vec4 fColour;		\n" +
          "void main(){				\n" +
          "	gl_FragColor = fColour;	\n" +
          //"	gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); \n" +
          "}							\n";

  static final int COORDS_PER_VERTEX = 3;
  static float cubeCoords[] = {
      //X, Y, Z
      -1, -1, -1,
      1, -1, -1,
      1, 1, -1,
      -1, 1, -1,
      -1, -1, 1,
      1, -1, 1,
      1, 1, 1,
      -1, 1, 1
  };

      float[] cubeColours = {
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 0, 0, 1,
        0, 0, 1, 1,
        0, 0, 1, 1,
        0, 0, 1, 1,
        0, 0, 1, 1
    };

    short[] cubeIndices = {
        0, 4, 5,
        0, 5, 1,
        1, 5, 6,
        1, 6, 2,
        2, 6, 7,
        2, 7, 3,
        3, 7, 4,
        3, 4, 0,
        4, 7, 6,
        4, 6, 5,
        3, 0, 1,
        3, 1, 2
    };

  private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

}
