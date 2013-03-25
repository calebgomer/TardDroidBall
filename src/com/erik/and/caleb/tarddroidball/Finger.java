package com.erik.and.caleb.tarddroidball;

public class Finger {
  public float x;
  public float y;
  public float pressure;

  public Finger(float x, float y, float pressure) {
    this.x = x;
    this.y = y;
    this.pressure = pressure;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }

  public float getPressure() {
    return pressure;
  }

  public void setPressure(float pressure) {
    this.pressure = pressure;
  }
}
