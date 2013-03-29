package com.erik.and.caleb.tarddroidball;

public class CoolMatrix {

  private float[] matrix;

  public CoolMatrix(float[] matrix) throws ThisIsNotAMatrixException {
    this.matrix = new float[16];
    if (matrix.length == 16)
      System.arraycopy(matrix, 0, this.matrix, 0, 16);
    else
      throw new ThisIsNotAMatrixException();
  }

  public class ThisIsNotAMatrixException extends Exception {
  }

  public float[] getMatrix() {
    return matrix;
  }

  public void setMatrix(float[] matrix) {
    this.matrix = matrix;
  }
}
