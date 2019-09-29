package fi.helsinki.mobiilishakki;


import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.LINE_AA;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.HoughLines;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.threshold;

public class ChessboardAnalyzer implements Runnable {

    // Used for logging success or failure messages
    private static final String TAG = "ChessboardAnalyzer";
    // Bitmap of frame
    private Bitmap bitmap;
    // Frame bytes (NV21) encoded
    private byte[] bytes;
    // Camera parameters
    private Camera.Parameters cameraParams;
    // Flag for board detection status
    private boolean boardDetected;
    // Frame width
    private int frameWidth;
    // Frame height
    private int frameHeight;

    @Override
    public void run() {
        Log.i(TAG, "Called run!");

        // Try to detect chessboard
        detectBoard();

    }

    /**
     * Method for detecting chessboard. Returns true or false depending on the detection success.
     */
    private boolean detectBoard() {
        // Create Mat-object from bytes
        Mat frame = fi.helsinki.mobiilishakki.Utils.bytesToMatConversion(bytes, cameraParams);
        frameWidth = frame.cols();
        frameHeight = frame.rows();
        // Mat-object for holding gray frame
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, COLOR_BGR2GRAY);
        // Blurring the image to reduce the amount of "false positives"
        GaussianBlur(grayFrame, grayFrame, new Size(3, 3), BORDER_DEFAULT);
        // Create binary Mat-object
        Mat binaryMat = new Mat(grayFrame.size(), grayFrame.type());
        // Apply thresholding
        threshold(grayFrame, binaryMat, 100, 255, THRESH_BINARY);
        // Mat object for holding the result of edge detection (Canny)
        Mat edges = new Mat();
        Canny(grayFrame, edges, 100, 300, 3, false); // thresholds changed from 50/150
        // Apply line detection
        Mat linesMat = new Mat();
        HoughLines(edges, linesMat, 1, Math.PI / 180, 125); // threshold changed from 150


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
        // if not enough lines --> could not detect board
        //if (lines.size() < 18) {
        //    return false;
        //}

        // Filter lines that are not part of main chessboard grid
        lines = filterRedundantLines(mergedLines);
        // If detection has failed or wrong number of lines were detected
        //if (!boardDetected || lines.size() != 18) {
        //    return false;
        //}
        // Draw lines to frame image
        bitmap = createLineBitmap(lines);

        // Draw intersection points to video feed
//        List<Point> intersectionPoints = findAndDrawIntersectionPoints(lines, frame);
//        List<Point> orderedPoints = intersectionPoints;
//        for (int i = 0; i < orderedPoints.size() - 1; i++) {
//            for (int j = 0 ; j < orderedPoints.size() - i - 1; j++) {
//                if (orderedPoints.get(j).y > orderedPoints.get(j + 1).y) {
//                    Point p = orderedPoints.get(j);
//                    orderedPoints.set(j, orderedPoints.get(j + 1));
//                    orderedPoints.set(j + 1, p);
//                }
//            }
//        }
//        for (int i = 0; i < 9; i++) {
//            circle(frame, orderedPoints.get(i), 12, new Scalar(0, 255, 0));
//        }

        // Everything ok...
        return true;
    }

    /**
     * Finds the intersection points for lines, then draws them on the Mat object
     */
    private List<Point> findAndDrawIntersectionPoints(List<Line> lines, Mat rgbFrame) {
        // Create list for intersection points
        List<Point> intersectionPoints = new ArrayList<>();

        // Create lists for vertical and horizontal lines
        List<Line> vertical = new ArrayList<>();
        List<Line> horizontal = new ArrayList<>();

        // Iterate through all lines and group them to horizontal and vertical lines
        for (Line line : lines) {
            if (line.getTheta() >= Math.PI/4 && line.getTheta() <= Math.PI/4*3) {
                vertical.add(line);     // line angle closer to vertical
            } else {
                horizontal.add(line);   // line angle closer to horizontal
            }
        }

        for (Line verticalLine : vertical) {
            for (Line horizontalLine : horizontal) {
                if (linesIntersect(verticalLine, horizontalLine)) {
                    Point intersection = findIntersectionPoint(verticalLine, horizontalLine);
                    intersectionPoints.add(intersection);
                    circle(rgbFrame, intersection, 8, new Scalar(255, 0, 0));
                }
            }
        }
        return intersectionPoints;
    }

    /**
     * Filter lines that are not part of the chessboard grid.
     * In other words, get rid of redundant lines.
     */
    private List<Line> filterRedundantLines(List<Line> lines) {
        // Create lists for vertical and horizontal lines
        boardDetected = false;
        List<Line> vertical = new ArrayList<>();
        List<Line> horizontal = new ArrayList<>();
        // Iterate through all lines and group them to horizontal and vertical lines
        for (Line line : lines) {
            if (line.getTheta() >= Math.PI / 4 && line.getTheta() <= Math.PI / 4 * 3) {
                horizontal.add(line);     // line angle closer to vertical
            } else {
                vertical.add(line);   // line angle closer to horizontal
            }
        }


        vertical = removeIntersectingLines(vertical, true);
        horizontal = removeIntersectingLines(horizontal, false);

//        for (int i = 0; i < horizontal.size() - 1; i++) {
//            for (int j = 0; j < horizontal.size() - i - 1; j++) {
//                if (horizontal.get(j).getRho() < horizontal.get(j + 1).getRho()) {
//                    Line l = horizontal.get(j);
//                    horizontal.set(j, horizontal.get(j + 1));
//                    horizontal.set(j + 1, l);
//                }
//            }
//        }
//        for (int i = 0; i < vertical.size() - 1; i++) {
//            for (int j = 0; j < vertical.size() - i - 1; j++) {
//                if (vertical.get(j).getRho() < vertical.get(j + 1).getRho()) {
//                    Line l = vertical.get(j);
//                    vertical.set(j, vertical.get(j + 1));
//                    vertical.set(j + 1, l);
//                }
//            }
//        }
//
//        // Get a vertical line and find out the intersection points of that and the bottom horizontal lines
//        Line line = vertical.get(vertical.size() / 2);
//        Point intersection1 = findIntersectionPoint(line, horizontal.get(0));
//        Point intersection2 = findIntersectionPoint(line, horizontal.get(1));
//        Point intersection3 = findIntersectionPoint(line, horizontal.get(2));
//
//        // Calculate and compare the distances of the intersection points
//        int ydiff1 = (int) Math.abs(intersection1.y - intersection2.y);
//        int ydiff2 = (int) Math.abs(intersection2.y - intersection3.y);
//
//        // If differences too big, redundant bottom line detected
//        if (2*ydiff1 < ydiff2) {
//            horizontal.remove(0);
//        }


        // TODO: remove extra lines.
        //  9 most centered lines should be considered as actual grid lines

        if (vertical.size() == 9 && horizontal.size() == 9) {
            boardDetected = true;
        }

        // Merge horizontal and vertical lines
        lines = vertical;
        lines.addAll(horizontal);
        return lines;
    }

    /**
     * Remove lines that intersect each other in frame area. The line that has higher
     */
    private List<Line> removeIntersectingLines(List<Line> lines, boolean isVertical) {
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
                    if (!isVertical) {
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
    private boolean linesIntersect(Line line1, Line line2) {

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

    /**
     * Finds the intersection point of two lines
     */
    private Point findIntersectionPoint(Line line1, Line line2) {
        // Horizontal line calculation
        double a0 = Math.cos(line1.getTheta()), b0 = Math.sin(line1.getTheta());
        double x0h = a0 * line1.getRho(), y0h = b0 * line1.getRho();
        double x1 = x0h + 3000*(-1 * b0);
        double y1 = y0h + 3000*a0;
        double x2 = x0h - 3000*(-1 * b0);
        double y2 = y0h - 3000*a0;

        // Vertical line calculation
        double a1 = Math.cos(line2.getTheta()), b1 = Math.sin(line2.getTheta());
        double x0v = a1 * line2.getRho(), y0v = b1 * line2.getRho();
        double x3 = x0v + 3000*(-1 * b1);
        double y3 = y0v + 3000*a1;
        double x4 = x0v - 3000*(-1 * b1);
        double y4 = y0v - 3000*a1;

        // Intersection point calculation
        double u = ((x4-x3)*(y1-y3) - (y4-y3)*(x1-x3)) / ((y4-y3)*(x2-x1) - (x4-x3)*(y2-y1));
        int x = (int) (x1 + u * (x2 - x1));
        int y = (int) (y1 + u * (y2 - y1));

        return new Point(x, y);
    }

    /**
     * Get List of lines from Mat-object.
     */
    private List<Line> getLinesList(Mat lines) {
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
    private List<Line> mergeLines(List<Linegroup> linegroups) {
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
    private List<Linegroup> getLinegroups(List<Line> lines) {
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
     * Draw lines to bitmap.
     */
    private Bitmap createLineBitmap(List<Line> lines) {
        // Create black frame
        Mat black = Mat.zeros(frameHeight, frameWidth, CV_8UC4);

        // Draw lines to frame object
        for (int i = 0; i < lines.size(); i++) {
            Point pt1 = lines.get(i).getStartingPoint();
            Point pt2 = lines.get(i).getEndingPoint();
            line(black, pt1, pt2, new Scalar(0, 0, 255, 255), 1, LINE_AA, 0);
        }

        // Make other than line pixels transparent
        for (int i = 0; i < black.rows(); i++) {
            for (int j = 0; j < black.cols(); j++) {
                double[] d = black.get(i, j);
                if (d[0] == 0 && d[1] == 0 && d[2] == 0) {
                    d[3] = 0;
                }
            }
        }

        // Create bitmap of the edited frame
        Bitmap bitmap = Bitmap.createBitmap(black.width(), black.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(black, bitmap);
        return bitmap;
    }

    /**
     * Get frame that contains lines and other openCV stuff."
     * @return bitmap frame
     */
    public Bitmap getFrameInBitmap() {
        return bitmap;
    }

    /**
     * Set camera frame that needs to be processed.
     * Input should be NV21 encoded.
     * @param bytes
     * @param params
     */
    public void setFrameBytes(byte[] bytes, Camera.Parameters params) {
        this.bytes = bytes;
        this.cameraParams = params;
    }
}