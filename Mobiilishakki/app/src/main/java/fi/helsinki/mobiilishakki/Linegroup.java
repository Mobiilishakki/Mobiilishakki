package fi.helsinki.mobiilishakki;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing group of similar lines.
 */
public class Linegroup {

    private List<Line> lines;

    /**
     * Create new Linegroup-object.
     */
    public Linegroup() {
        lines = new ArrayList<>();
    }

    /**
     * Add new line to linegroup.
     *
     * @param line new line
     */
    public void addLine(Line line) {
        lines.add(line);
    }

    /**
     * Get list of all lines in linegroup.
     *
     * @return lines
     */
    public List<Line> getLines() {
        return lines;
    }

    /**
     * Get average rho value.
     *
     * @return average rho
     */
    public double getAverageRho() {
        double rho = 0;
        if (lines.isEmpty()) {
            return rho;
        }
        for (Line line : lines) {
            rho += line.getRho();
        }
        return rho / lines.size();
    }

    /**
     * Get average theta value.
     *
     * @return average theta
     */
    public double getAverageTheta() {
        double theta = 0;
        if (lines.isEmpty()) {
            return theta;
        }
        for (Line line : lines) {
            theta += line.getTheta();
        }
        return theta / lines.size();
    }
}
