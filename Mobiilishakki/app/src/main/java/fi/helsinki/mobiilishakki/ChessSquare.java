package fi.helsinki.mobiilishakki;

import org.opencv.core.Point;

import java.util.List;

/**
 * Class representing a square on a chess board
 */
public class ChessSquare {

    private List<Point> points;

    public ChessSquare(List<Point> points) {
        this.points = points;
    }

    public List<Point> getPoints() {
        return this.points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

}
