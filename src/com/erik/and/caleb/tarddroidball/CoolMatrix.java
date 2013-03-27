package com.erik.and.caleb.tarddroidball;

import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: calebgomer
 * Date: 3/27/13
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoolMatrix {

  public static final String TAG = "CoolMatrix";
  private float[] matrix;

  public CoolMatrix(float[] matrix) throws ThisIsNotAMatrixException {
    this.matrix = new float[16];
    if (matrix.length == 16)
      System.arraycopy(matrix, 0, this.matrix, 0, 16);
    else
      throw new ThisIsNotAMatrixException();
  }

  public class ThisIsNotAMatrixException extends Exception {

    @Override
    public void printStackTrace() {
      Log.d(TAG, TAG + " Exception: This is not a cool matrix!");
      super.printStackTrace();
    }
  }

  public float[] getMatrix() {
    return matrix;
  }

  public void setMatrix(float[] matrix) {
    this.matrix = matrix;
  }
}
