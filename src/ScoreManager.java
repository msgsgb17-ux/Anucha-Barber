public class ScoreManager {

    private int score;
    private int excessCut;
    private int targetCut;

    public void reset() {
        score = 0;
        excessCut = 0;
        targetCut = 0;
    }

    public void add(int delta) {
        score += delta;
        if (delta > 0) {
            excessCut++;
        } else if (delta < 0) {
            targetCut++;
        }
    }

    public int getScore() {
        return score;
    }

    public int getExcessCut() {
        return excessCut;
    }

    public int getTargetCut() {
        return targetCut;
    }

    public int getStars(int excessTotal, double dyeCoverage, double targetKept) {
        if (excessTotal <= 0 || targetKept < 0.7) {
            return 0;
        }
        double ratio = score / (10.0 * excessTotal);
        int stars;
        if (ratio >= 0.8) {
            stars = 3;
        } else if (ratio >= 0.5) {
            stars = 2;
        } else if (ratio >= 0.2) {
            stars = 1;
        } else {
            return 0;
        }
        if (dyeCoverage < 0.5 && stars > 1) {
            stars--;
        }
        return stars;
    }
}
