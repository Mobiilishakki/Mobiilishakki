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

    Point[] points = new Point[8];

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
    boolean firstMove=true;
    boolean afterReset=true;
    Point centralPoint;

    public DrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

        ColorBall.canvasHeight=canvas.getHeight();
        ColorBall.canvasWidth=canvas.getWidth();

    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
        ColorBall.canvasHeight=canvas.getHeight();
        ColorBall.canvasWidth=canvas.getWidth();

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

            if(i<3){
                drawLine(colorballs.get(i).point,colorballs.get(i+1).point,canvas,paint,colorballs.get(0).getHeightOfBall());
            } else if (i==3){
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

                    for(int x=0;x<8;x++){
                        points[x]=new Point();
                    }
                 //   points[0] = new Point();
                 //   points[1] = new Point();
                 //   points[2] = new Point();
                 //   points[3] = new Point();

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


                    points[4].x=(points[0].x+points[3].x)/2;
                    points[4].y=(points[0].y+points[3].y)/2;

                    points[5].x=(points[3].x+points[2].x)/2;
                    points[5].y=(points[3].y+points[2].y)/2;

                    points[6].x=(points[2].x+points[1].x)/2;
                    points[6].y=(points[2].y+points[1].y)/2;


                    points[7].x=(points[1].x+points[0].x)/2;
                    points[7].y=(points[1].y+points[0].y)/2;


                    int centerBallY=(points[0].y+points[1].y+points[2].y+points[3].y)/4;
                    int centerBallX=(points[0].x+points[1].x+points[2].x+points[3].x)/4;

                    centralPoint=new Point(centerBallX, centerBallY);


                    balID = 2;
                    groupId = 1;
                    afterReset=false;
                    // declare each ball with the ColorBall class
                    for (int x=0;x<4;x++) {

                        colorballs.add(new ColorBall(getContext(), R.drawable.circle, points[x]));// R.drawable.ic_circle, pt));
                    }
                    for(int x=4;x<points.length;x++){
                        colorballs.add(new ColorBall(getContext(), R.drawable.circle2, points[x]));
                    }
                    colorballs.add(new ColorBall(getContext(), R.drawable.circle2, centralPoint));

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

                        double multiplier=1.6;
                        if(ball.getID()>3){
                            multiplier=2;
                        }
                        if (radCircle < ball.getWidthOfBall()*multiplier) {

                            balID = ball.getID();
                            if(balID==8){
                                reset();
                                invalidate();
                                break;
                            }
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                            } else if (balID == 0 || balID==2){
                                groupId = 1;
                                firstMove=false;
                            } else {
                                groupId=balID;
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball


                if (balID > -1 && !afterReset) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);

                    paint.setColor(Color.CYAN);

                    if(firstMove) {

                        if (groupId == 1) {
                            colorballs.get(1).setX(colorballs.get(0).getX());
                            colorballs.get(1).setY(colorballs.get(2).getY());
                            colorballs.get(3).setX(colorballs.get(2).getX());
                            colorballs.get(3).setY(colorballs.get(0).getY());
                        }

                        if (groupId == 2) {
                            colorballs.get(0).setX(colorballs.get(1).getX());
                            colorballs.get(0).setY(colorballs.get(3).getY());
                            colorballs.get(2).setX(colorballs.get(3).getX());
                            colorballs.get(2).setY(colorballs.get(1).getY());
                        }
                    }

                    if(groupId==4){
                        int differY=(colorballs.get(0).getY()-colorballs.get(3).getY())/2;
                        colorballs.get(0).setY(colorballs.get(4).getY()+differY);
                        colorballs.get(3).setY(colorballs.get(4).getY()-differY);
                    }
                    if(groupId==5){
                        int differX=(colorballs.get(3).getX()-colorballs.get(2).getX())/2;
                        colorballs.get(3).setX(colorballs.get(5).getX()+differX);
                        colorballs.get(2).setX(colorballs.get(5).getX()-differX);
                    }
                    if(groupId==6){
                        int differY=(colorballs.get(1).getY()-colorballs.get(2).getY())/2;
                        colorballs.get(1).setY(colorballs.get(6).getY()+differY);
                        colorballs.get(2).setY(colorballs.get(6).getY()-differY);
                    }
                    if(groupId==7){
                        int differX=(colorballs.get(0).getX()-colorballs.get(1).getX())/2;
                        colorballs.get(0).setX(colorballs.get(7).getX()+differX);
                        colorballs.get(1).setX(colorballs.get(7).getX()-differX);
                    }

                    colorballs.get(4).setX((colorballs.get(0).getX()+colorballs.get(3).getX())/2);
                    colorballs.get(4).setY((colorballs.get(0).getY()+colorballs.get(3).getY())/2);
                    colorballs.get(5).setX((colorballs.get(3).getX()+colorballs.get(2).getX())/2);
                    colorballs.get(5).setY((colorballs.get(3).getY()+colorballs.get(2).getY())/2);
                    colorballs.get(6).setX((colorballs.get(2).getX()+colorballs.get(1).getX())/2);
                    colorballs.get(6).setY((colorballs.get(2).getY()+colorballs.get(1).getY())/2);
                    colorballs.get(7).setX((colorballs.get(1).getX()+colorballs.get(0).getX())/2);
                    colorballs.get(7).setY((colorballs.get(1).getY()+colorballs.get(0).getY())/2);


                    int centerY=0,centerX=0;
                    for(int x=0;x<4;x++){
                        centerX+=colorballs.get(x).getX();
                        centerY+=colorballs.get(x).getY();
                    }
                    colorballs.get(8).setX(centerX/4);
                    colorballs.get(8).setY(centerY/4);
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

    public void reset(){
        for(int x=0;x<points.length;x++)
            points[x]=null;
        this.colorballs.clear();
        this.firstMove=true;
        this.groupId=-1;
        this.balID=-0;
        afterReset=true;
        ColorBall.count=0;
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
      static  public int canvasWidth,canvasHeight;
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

 /*           if(x<0)
                x=0;
            if(x>ColorBall.canvasWidth)
                x=ColorBall.canvasWidth;
 */           point.x = x;
            updateBounds();
        }

        public void setY(int y) {
  /*          if(y<0)
                y=0;
            if(y>ColorBall.canvasHeight)
                y=ColorBall.canvasHeight;
   */         point.y = y;
            updateBounds();
        }
    }
}