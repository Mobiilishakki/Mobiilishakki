package fi.helsinki.mobiilishakki;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;



public class DrawView extends View {

    Point[] points = new Point[4];

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    int groupId = -1;
    private ArrayList<ColorBall> colorballs = new ArrayList<ColorBall>();
    // array that holds the balls
    private int balID = 0;
    // variable to know what ball is being dragged
    Paint paint;
    Canvas canvas;

    public DrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(5);

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#AADB1255"));
        paint.setStrokeWidth(2);
/*        canvas.drawRect(
                left + colorballs.get(0).getWidthOfBall() / 2,
                top + colorballs.get(0).getWidthOfBall() / 2,
                right + colorballs.get(2).getWidthOfBall() / 2,
                bottom + colorballs.get(2).getWidthOfBall() / 2, paint);

 */
        //fill the rectangle
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#55DB1255"));
        paint.setStrokeWidth(3);
/*        canvas.drawRect(
                left + colorballs.get(0).getWidthOfBall() / 2,
                top + colorballs.get(0).getWidthOfBall() / 2,
                right + colorballs.get(2).getWidthOfBall() / 2,
                bottom + colorballs.get(2).getWidthOfBall() / 2, paint);
*/

        //draw the corners
       BitmapDrawable bitmap = new BitmapDrawable(getContext().getResources());
        // draw the balls on the canvas
        paint.setColor(Color.BLUE);
        paint.setTextSize(18);
        paint.setStrokeWidth(3);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);

          //  canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(), paint);

            System.out.println("EKA PALLO "+i +"  koordi "+colorballs.get(i).point.toString());

            colorballs.get(i).getDrawable().draw(canvas);

            if(i<colorballs.size()-1){
                drawLine(colorballs.get(i).point,colorballs.get(i+1).point,canvas,paint,colorballs.get(0).getHeightOfBall());
            } else {
                drawLine(colorballs.get(i).point,colorballs.get(0).point,canvas,paint,colorballs.get(0).getHeightOfBall());
            }


           // canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }

    public void drawLine(Point x, Point y, Canvas canvas, Paint paint, int size){

/*
        float startX=0,startY=0,endX=0,endY=0;

        if(x.x < y.x){
            startX=x.x+size/2;
            endX=y.x-size/2;
        } else if(x.x>y.x){
            startX=x.x-size/2;
            endX=y.x+size/2;
        }
        if(x.y < y.y){
            startY=x.y+size/2;
            endY=y.y-size/2;
        } else if(x.y>y.y){
            startY=x.y-size/2;
            endY=y.y+size/2;
        }
*/
        canvas.drawLine(x.x, x.y, y.x, y.y, paint);
        //canvas.drawLine(startX,endX,startY,endY,paint);
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        System.out.println("LEVEYSSSS "+getWidth()+"  KORKEUS "+getHeight());
        switch (eventaction) {

            case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                // a ball
                if (points[0] == null) {
                    //initialize rectangle.
                    points[0] = new Point();
                    points[1] = new Point();
                    points[2] = new Point();
                    points[3] = new Point();

                    points[0].x = X;
                    points[0].y = Y;



                    points[1].x = X;
                    points[3].y = Y;

                    if(Y<(getHeight()-100)) {
                        points[1].y = Y + 50;
                        points[2].y = Y + 50;
                    } else {
                        points[1].y = Y - 50;
                        points[2].y = Y - 50;
                    }
                    if(X<(getWidth()-100)){
                        points[2].x = X + 50;
                        points[3].x = X + 50;
                    } else {
                        points[2].x = X - 50;
                        points[3].x = X - 50;
                    }







                    balID = 2;
                    groupId = 1;
                    // declare each ball with the ColorBall class
                    for (Point pt : points) {

                        colorballs.add(new ColorBall(getContext(), R.drawable.circle, pt));// R.drawable.ic_circle, pt));
                    }

                } else {
                    //resize rectangle
                    balID = -1;
                    groupId = -1;
                    for (int i = colorballs.size()-1; i>=0; i--) {
                        ColorBall ball = colorballs.get(i);
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball


                        int centerX = ball.getX()+ ball.getWidthOfBall();
                        int centerY = ball.getY()+ ball.getHeightOfBall();


                        paint.setColor(Color.CYAN);
                        // calculate the radius from the touch to the center of the
                        // ball
                        double radCircle = Math
                                .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                        * (centerY - Y)));

                        if (radCircle < ball.getWidthOfBall()*1.5) {

                            balID = ball.getID();
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                            } else {
                                groupId = 1;
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball


                if (balID > -1) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);

                    paint.setColor(Color.CYAN);

                    if (groupId == 1) {
                        colorballs.get(1).setX(colorballs.get(0).getX());
                        colorballs.get(1).setY(colorballs.get(2).getY());
                        colorballs.get(3).setX(colorballs.get(2).getX());
                        colorballs.get(3).setY(colorballs.get(0).getY());
                    } else {
                        colorballs.get(0).setX(colorballs.get(1).getX());
                        colorballs.get(0).setY(colorballs.get(3).getY());
                        colorballs.get(2).setX(colorballs.get(3).getX());
                        colorballs.get(2).setY(colorballs.get(1).getY());
                    }

                    invalidate();
                }

                break;

            case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping

                break;
        }
        // redraw the canvas
        invalidate();

        return true;

    }

    public Point topLeft(){

        Point point=colorballs.get(0).point;
        for(int x=1;x<colorballs.size();x++){
            if(colorballs.get(x).getX()<=point.x)
                if(colorballs.get(x).getY()<=point.y)
                    point=colorballs.get(x).point;
        }
        return point;
    }

    public Point topRight(){
        Point point=colorballs.get(0).point;
        for(int x=1;x<colorballs.size();x++){
            if(colorballs.get(x).getX()>=point.x)
                if(colorballs.get(x).getY()<=point.y)
                    point=colorballs.get(x).point;
        }
        return point;
    }

    public Point bottomLeft(){
        Point point=colorballs.get(0).point;
        for(int x=1;x<colorballs.size();x++){
            if(colorballs.get(x).getX()<=point.x)
                if(colorballs.get(x).getY()>=point.y)
                    point=colorballs.get(x).point;
        }
        return point;
    }
    public Point bottomRight(){
        Point point=colorballs.get(0).point;
        for(int x=1;x<colorballs.size();x++){
            if(colorballs.get(x).getX()>=point.x)
                if(colorballs.get(x).getY()>=point.y)
                    point=colorballs.get(x).point;
        }
        return point;
    }



    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id, width, height;
        private Drawable ball;
        int left, top, right, bottom;

        static int count = 0;

        public ColorBall(Context context, int resourceId, Point point) {

            System.out.println("LOKKILOKKI   "+context.toString());
            System.out.println("LOKKILOKKI   "+resourceId);
            System.out.println("LOKKILOKKI   "+point.toString());


            ball=context.getResources().getDrawable(resourceId, context.getTheme());
            width=ball.getMinimumWidth();
            height=ball.getMinimumHeight();

           /* ImageView imgView = (ImageView) findViewById(resourceId);
            imgView.setImageResource(R.drawable.abc_image);
            z.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
*/
            this.id = count++;
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    resourceId);
            mContext = context;
            this.point = point;
            updateBounds();
        }

        public void updateBounds(){
            left=point.x-width/2;
            right=left+width;
            top=point.y-height/2;
            bottom=top+height;
            ball.setBounds(left,top,right,bottom);
        }

        public int getWidthOfBall() {

            return width;
        }

        public Drawable getDrawable(){
            return ball;
        }
        public int getHeightOfBall() {
            return height;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public int getID() {
            return id;
        }

        public void setX(int x) {
            point.x = x;
            updateBounds();
        }

        public void setY(int y) {
            point.y = y;
            updateBounds();
        }
    }
}