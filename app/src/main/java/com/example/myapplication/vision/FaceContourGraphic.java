package com.example.myapplication.vision;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/** Graphic instance for rendering face contours graphic overlay view. */
public class FaceContourGraphic extends GraphicOverlay.Graphic {

  public static float globalScaleFactor = 0.5f;
  private static final float FACE_POSITION_RADIUS = 2.0f;
  private static final float BOX_STROKE_WIDTH = 6.8f;
  private static final float FACE_STROKE_WIDTH = 2.0f;

  private static final int[] COLOR_CHOICES = {
          Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.RED, Color.WHITE, Color.YELLOW
  };


  //오일러Y가 -12 ~ 12 : 오른쪽 눈, 왼쪽 눈, 코 밑부분, 왼쪽 볼, 오른쪽 볼, 왼쪽 입, 오른쪽 입, 아래 입
  public static final String[] CONTOUR = { /** 13개 랜드마크 */
          "F_OVAL","LEB_T","LEB_B","REB_T","REB_B","LE","RE","ULT","ULB","LLT","LLB","NBR","NBT"
  };

  private final Paint facePositionPaint; //컨투어 점 용도
  private final Paint facePositionLinePaint; //컨투어 선긋는 용도
  private final Paint boxPaint;
  private volatile Face face;

  public FaceContourGraphic(GraphicOverlay overlay) {
    super(overlay);



    //눈 코 입
    facePositionPaint = new Paint();
    facePositionPaint.setStyle(Paint.Style.STROKE);
    facePositionPaint.setStrokeWidth(FACE_STROKE_WIDTH);


    //눈 코 입
    facePositionLinePaint = new Paint();
    facePositionLinePaint.setStyle(Paint.Style.STROKE);
    facePositionLinePaint.setStrokeWidth(FACE_STROKE_WIDTH/0.85f);

    boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
  }


  /**
   * Updates the face instance from the detection of the most recent frame. Invalidates the relevant
   * portions of the overlay to trigger a redraw.
   */
  public void updateFace(Face face) {
    this.face = face;
    postInvalidate();
  }

  /** Draws the face annotations for position on the supplied canvas. */
  @Override
  public void draw(Canvas canvas) {
    Face face = this.face;
    if (face == null) {
      return;
    }
    //INFO 이렇게 x,y를 구해서 vector 구하자
    //Draws a circle at the position of the detected face
    float x = translateX(face.getBoundingBox().centerX());
    float y = translateY(face.getBoundingBox().centerY());
    canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

    // Draws a bounding box around the face.
    float xOffset = scaleX(face.getBoundingBox().width() / 2.0f);
    float yOffset = scaleY(face.getBoundingBox().height() / 2.0f);
    float left = x - xOffset;
    float top = y - yOffset;
    float right = x + xOffset;
    float bottom = y + yOffset;
    canvas.drawRect(left, top, right, bottom, boxPaint);


    /** Contours */
    for(int i=0; i<CONTOUR.length; i++){
      FaceContour ctr = face.getContour(i);
      if(ctr==null) continue;
      int color = COLOR_CHOICES[i % COLOR_CHOICES.length];
      facePositionPaint.setColor(color);
      facePositionLinePaint.setColor(color);
      int iterCnt = ctr.getPoints().size();
      for(int j=1; j<iterCnt; j++){
        PointF prev, next;
        prev = ctr.getPoints().get(j-1);
        next = ctr.getPoints().get(j);
        float px = translateX(prev.x);
        float py = translateY(prev.y);
        float nx = translateX(next.x);
        float ny = translateY(next.y);
        canvas.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint);
        canvas.drawLine(px,py,nx,ny, facePositionLinePaint);
      }
    }

  }
}

