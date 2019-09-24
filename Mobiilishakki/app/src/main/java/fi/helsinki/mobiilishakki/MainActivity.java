package fi.helsinki.mobiilishakki;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.LINE_AA;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughLines;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.threshold;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static int frameWidth = 0;
    private static int frameHeight = 0;

    // Latest frame that was processed
    private Mat lastFrame;
    // Used for logging success or failure messages
    private static final String TAG = "OpenCVTest::Activity";
    // Loads camera view of OpenCV to use.
    private CameraBridgeViewBase cameraBridgeViewBase;
    // OpenCV manager to help our app communicate with android phone to make OpenCV work
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.cameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.enableFpsMeter();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


        // Mat object for holding rgb frame
        Mat rgbFrame = inputFrame.rgba();

        frameHeight = rgbFrame.rows();
        frameWidth = rgbFrame.cols();


        rgbFrame = recogniseBoardGrid(rgbFrame);

        this.lastFrame = rgbFrame;
        return rgbFrame;
    }


    /**
     * Try to recognise grid from frame and write grid lines to picture.
     */
    private static Mat recogniseBoardGrid(Mat rgbFrame) {
        // Mat object for holding gray frame
        Mat grayFrame = new Mat();
        cvtColor(rgbFrame, grayFrame, COLOR_BGR2GRAY);

        // Blurring the image to reduce the amount of "false positives"
        GaussianBlur(grayFrame, grayFrame, new Size(3, 3), BORDER_DEFAULT);

        Mat binaryMat = new Mat(grayFrame.size(), grayFrame.type());

        // Apply thresholding
        threshold(grayFrame, binaryMat, 100, 255, THRESH_BINARY);

        // Mat object for holding the result of edge detection (Canny)
        Mat edges = new Mat();
        Canny(grayFrame, edges, 50, 150, 3, false);

        // Apply line detection
        Mat linesMat = new Mat();
        HoughLines(edges, linesMat, 1, Math.PI / 180, 150);

        // Skip if no lines found
        if (linesMat.empty()) {
            return rgbFrame;
        }

        // Get lines in a list
        List<Line> lines = getLinesList(linesMat);

        // Get linegroups
        List<Linegroup> linegroups = getLinegroups(lines);

        // Get merged lines
        List<Line> mergedLines = mergeLines(linegroups);

        // Keep merging lines until stable situation
        while (lines.size() != mergedLines.size()) {
            lines = mergedLines;
            linegroups = getLinegroups(lines);
            mergedLines = mergeLines(linegroups);
        }

        // Filter lines that are not part of main chessboard grid
        lines = filterRedundantLines(mergedLines);

        // Check if we have right amount of lines to form chessboard
        if (lines.size() == 18) {
            System.out.println("Could be a board!!!");
        }

        // Draw lines to video feed
        drawLinesToMat(lines, rgbFrame);

        // Draw intersection points to video feed
        findAndDrawIntersectionPoints(lines, rgbFrame);

        return rgbFrame;
    }

    /**
     * Draw lines to Mat-object.
     */
    private static Mat drawLinesToMat(List<Line> mergedLines, Mat frame) {
        for (Line line : mergedLines) {
            Point pt1 = line.getStartingPoint();
            Point pt2 = line.getEndingPoint();
            line(frame, pt1, pt2, new Scalar(0, 0, 255), 1, LINE_AA, 0);
        }
        return frame;
    }

    /**
     * Get List of lines from Mat-object.
     */
    private static List<Line> getLinesList(Mat lines) {
        List<Line> lineList = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double rho = lines.get(i, 0)[0];
            double theta = lines.get(i, 0)[1];
            Line line = new Line(rho, theta);
            lineList.add(line);
        }
        return lineList;
    }

    /**
     * Get List of merged lines.
     */
    private static List<Line> mergeLines(List<Linegroup> linegroups) {
        // Create list to contain information about all merged lines
        List<Line> mLines = new ArrayList<>();
        // Iterate through all linegroups and combine groups
        for (Linegroup linegroup : linegroups) {
            double rho = linegroup.getAverageRho();
            double theta = linegroup.getAverageTheta();
            Line line = new Line(rho, theta);
            mLines.add(line);
        }
        return mLines;
    }

    /**
     * Get List of linegroups.
     */
    private static List<Linegroup> getLinegroups(List<Line> lines) {
        // Define thresholds for rho and theta
        double rhoThreshold = 30;
        double thetaThreshold = 0.1;

        // HashMap to keep track of group for each line <line index, group number>
        HashMap<Integer, Integer> groupMap = new HashMap<>();
        // List of all linegroups
        List<Linegroup> linegroups = new ArrayList<>();

        // Compare every line to every other line and find the linegroup of the most similar line.
        // Most similar line is considered to be the one with closest rho value. However, the difference
        // between theta values must be under threshold value, so that the lines can be considered the same line.
        int linegroupNumber = 0;    // next available linegroup number
        for (int i = 0; i < lines.size(); i++) {
            int msIndx = -1;
            double rhoDif = Double.MAX_VALUE;
            double rhoI = lines.get(i).getRho(), thetaI = lines.get(i).getTheta();
            for (int j = 0; j < lines.size(); j++) {
                if (i == j) {
                    continue;   // Do not compare line to itself
                }
                // Get rho and theta from each lines
                double rhoJ = lines.get(j).getRho(), thetaJ = lines.get(j).getTheta();
                // Check if lines are similar enough to be considered the same line
                double difR = Math.abs(rhoI - rhoJ);
                double difT = Math.abs(thetaI - thetaJ);
                if (difR < rhoThreshold && difT < thetaThreshold) {
                    if (difR < rhoDif) {
                        rhoDif = difR;
                        msIndx = j;
                    }
                }
            }

            // Set group for i:th line to be the same as the group of the most similar line.
            // If there is no most similar line, then create new linegroup.
            if (msIndx == -1 || groupMap.get(msIndx) == null) {
                groupMap.put(i, linegroupNumber);
                groupMap.put(msIndx, linegroupNumber);
                linegroupNumber++;
                linegroups.add(new Linegroup());
                linegroups.get(linegroups.size() - 1).addLine(lines.get(i));
            } else {
                int groupNum = groupMap.get(msIndx);
                groupMap.put(i, groupNum);
                linegroups.get(groupNum).addLine(lines.get(i));
            }
        }
        // Return linegroups
        return linegroups;
    }

    /**
     * Finds the intersection points for lines, then draws them on the Mat object
     */
    private static void findAndDrawIntersectionPoints(List<Line> lines, Mat rgbFrame) {
        // Create lists for vertical and horizontal lines
        List<Line> vertical = new ArrayList<>();
        List<Line> horizontal = new ArrayList<>();
        // Iterate through all lines and group them to horizontal and vertical lines
        for(Line line : lines) {
            if(line.getTheta() >= Math.PI/4 && line.getTheta() <= Math.PI/4*3) {
                vertical.add(line);     // line angle closer to vertical
            }else {
                horizontal.add(line);   // line angle closer to horizontal
            }
        }

        for (Line verticalLine : vertical) {
            for (Line horizontalLine : horizontal) {
                if (linesIntersect(verticalLine, horizontalLine)) {
                    // Horizontal line calculation
                    double a0 = Math.cos(horizontalLine.getTheta()), b0 = Math.sin(horizontalLine.getTheta());
                    double x0h = a0 * horizontalLine.getRho(), y0h = b0 * horizontalLine.getRho();
                    double x1 = x0h + 3000*(-1 * b0);
                    double y1 = y0h + 3000*a0;
                    double x2 = x0h - 3000*(-1 * b0);
                    double y2 = y0h - 3000*a0;

                    // Vertical line calculation
                    double a1 = Math.cos(verticalLine.getTheta()), b1 = Math.sin(verticalLine.getTheta());
                    double x0v = a1 * verticalLine.getRho(), y0v = b1 * verticalLine.getRho();
                    double x3 = x0v + 3000*(-1 * b1);
                    double y3 = y0v + 3000*a1;
                    double x4 = x0v - 3000*(-1 * b1);
                    double y4 = y0v - 3000*a1;

                    // Intersection point calculation
                    double u = ((x4-x3)*(y1-y3) - (y4-y3)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
                    int x = (int) (x1 + u * (x2 - x1));
                    int y = (int) (y1 + u * (y2 - y1));
                    Point intersection = new Point(x, y);


                    circle(rgbFrame, intersection, 8, new Scalar(255, 0, 0));
                }
            }
        }
    }

    /**
     * Filter lines that are not part of the chessboard grid.
     * In other words, get rid of redundant lines.
     */
    private static List<Line> filterRedundantLines(List<Line> lines) {
        // Create lists for vertical and horizontal lines
        List<Line> vertical = new ArrayList<>();
        List<Line> horizontal = new ArrayList<>();
        // Iterate through all lines and group them to horizontal and vertical lines
        for (Line line : lines) {
            if (line.getTheta() >= Math.PI / 4 && line.getTheta() <= Math.PI / 4 * 3) {
                vertical.add(line);     // line angle closer to vertical
            } else {
                horizontal.add(line);   // line angle closer to horizontal
            }
        }

        
        vertical = removeIntersectingLines(vertical, true);
        horizontal = removeIntersectingLines(horizontal, false);

        // TODO: remove extra lines.
        //  9 most centered lines should be considered as actual grid lines

        // Merge horizontal and vertical lines
        lines = vertical;
        lines.addAll(horizontal);
        return lines;
    }

    /**
     * Remove lines that intersect each other in frame area. The line that has higher
     */
    private static List<Line> removeIntersectingLines(List<Line> lines, boolean isVertical) {
        // Array where deleted lines are marked
        boolean[] linesToRemove = new boolean[lines.size()];
        // Iterate through all combinations
        for (int i = 0; i < lines.size(); i++) {
            if (linesToRemove[i] == true) {
                continue;
            }
            for (int j = 0; j < lines.size(); j++) {
                if (i == j || linesToRemove[j] == true || linesToRemove[i] == true) {
                    continue;
                }
                // Check if lines intersect
                Line lineI = lines.get(i);
                Line lineJ = lines.get(j);
                if (linesIntersect(lineI, lineJ)) {
                    double deltaI, deltaJ;
                    if (isVertical) {
                        deltaI = Math.abs(Math.PI / 2 - lineI.getTheta());
                        deltaJ = Math.abs(Math.PI / 2 - lineJ.getTheta());
                    } else {
                        if (lineI.getTheta() <= Math.PI / 4) {
                            deltaI = Math.abs(Math.PI / 4 - lineI.getTheta());
                        } else {
                            deltaI = Math.abs(Math.PI - lineI.getTheta());
                        }
                        if (lineJ.getTheta() <= Math.PI / 4) {
                            deltaJ = Math.abs(Math.PI / 4 - lineJ.getTheta());
                        } else {
                            deltaJ = Math.abs(Math.PI - lineJ.getTheta());
                        }
                    }
                    if (deltaI > deltaJ) {
                        linesToRemove[i] = true;
                    } else {
                        linesToRemove[j] = true;
                    }
                }
            }
        }
        // Return lines that do not overlap each other in frame area
        return lines;
    }

    /**
     * Check if lines intersect in frame area.
     */
    private static boolean linesIntersect(Line line1, Line line2) {

        // Get start and end points for lines
        Point p1 = line1.getStartingPoint();
        Point p2 = line1.getEndingPoint();
        Point p3 = line2.getStartingPoint();
        Point p4 = line2.getEndingPoint();

        double a1 = p2.y - p1.y;
        double b1 = p1.x - p2.x;
        double c1 = a1 * p1.x + b1 * p1.y;

        double a2 = p4.y - p3.y;
        double b2 = p3.x - p4.x;
        double c2 = a2 * p3.x + b2 * p3.y;

        double determinant = a1 * b2 - a2 * b1;

        // if determinant is zero --> lines do not intersect
        if (determinant == 0) {
            return false;
        }

        double x = (b2 * c1 - b1 * c2) / determinant;
        double y = (a1 * c2 - a2 * c1) / determinant;

        if (x >= 0 && x <= frameWidth && y >= 0 && y <= frameHeight) {
            return true;
        }
        
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

}
