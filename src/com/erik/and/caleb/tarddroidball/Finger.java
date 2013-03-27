package com.erik.and.caleb.tarddroidball;

public class Finger {
  public float x;
  public float dx;
  public float y;
  public float dy;
  public float rotationAngleX;
  public float rotationAngleY;

  public boolean translate;
  public boolean homeTardis;
  public boolean rotate;

  public Finger(boolean home) {
    x = -1;
    dx = -1;
    y = -1;
    dy = -1;
    homeTardis = home;
  }

  public Finger(float rotationAngleX, float rotationAngleY) {
    this.rotationAngleX = rotationAngleX;
    this.rotationAngleY = rotationAngleY;
    rotate = true;
  }

  public Finger(float x, float dx, float y, float dy) {
    this.x = x;
    this.dx = dx;
    this.y = y;
    this.dy = dy;
    translate = true;
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

  public float getDx() {
    return dx;
  }

  public void setDx(float dx) {
    this.dx = dx;
  }

  public float getDy() {
    return dy;
  }

  public void setDy(float dy) {
    this.dy = dy;
  }

  public boolean isHomeTardis() {
    return homeTardis;
  }

  public void setHomeTardis(boolean homeTardis) {
    this.homeTardis = homeTardis;
  }
}
