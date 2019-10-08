package fi.helsinki.mobiilishakki;

import org.opencv.core.Point;

/**
 * Class representing line.
 */
public class Line {

    private double rho;
    private double theta;

    /**
     * Constructor for Line-object.
     * @param rho
     * @param theta
     */
    public Line(double rho, double theta) {
        this.rho = rho;
        this.theta = theta;
    }

    /**
     * Get rho value from polar coordinate system.
     * r = x*cos(t) + y*sin(t)
     * @return rho
     */
    public double getRho() {
        return rho;
    }

    /**
     * Get theta value from polar coordinate system.
     * @return theta
     */
    public double getTheta() {
        return theta;
    }

    /**
     * Get line starting point.
     * @return point
     */
    public Point getStartingPoint() {
        double a = Math.cos(theta), b = Math.sin(theta);
        double x0 = a * rho, y0 = b * rho;
        return new Point(Math.round(x0 + 10000 * (-b)), Math.round(y0 + 10000 * (a)));
    }

    /**
     * Get line ending point.
     * @return ending point
     */
    public Point getEndingPoint() {
        double a = Math.cos(theta), b = Math.sin(theta);
        double x0 = a * rho, y0 = b * rho;
        return new Point(Math.round(x0 - 10000 * (-b)), Math.round(y0 - 10000 * (a)));
    }
}
