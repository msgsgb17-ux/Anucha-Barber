public class Level {

    private final String name;
    private final int seconds;
    private final int[] styles;
    private final boolean dye;
    private final double rx;
    private final double ry;
    private final double spikeChance;

    public Level(String name, int seconds, int[] styles, boolean dye, double rx, double ry, double spikeChance) {
        this.name = name;
        this.seconds = seconds;
        this.styles = styles;
        this.dye = dye;
        this.rx = rx;
        this.ry = ry;
        this.spikeChance = spikeChance;
    }

    public String getName() {
        return name;
    }

    public int getSeconds() {
        return seconds;
    }

    public int[] getStyles() {
        return styles;
    }

    public boolean isDye() {
        return dye;
    }

    public double getRx() {
        return rx;
    }

    public double getRy() {
        return ry;
    }

    public double getSpikeChance() {
        return spikeChance;
    }

    public static Level[] createAll() {
        return new Level[] {
            new Level("ฝึกตัดผม", 120, new int[] {1}, false, 8.6, 4.3, 0.12),
            new Level("ทรงกะลาครอบ", 105, new int[] {0}, true, 11.2, 5.3, 0.2),
            new Level("ทรงกลม", 90, new int[] {3}, true, 11.8, 5.6, 0.30),
            new Level("ทรงโมฮอก", 75, new int[] {2}, true, 11.8, 5.6, 0.3),
            new Level("ทรงสุ่ม", 60, new int[] {0, 1, 2, 3}, true, 12.0, 6.0, 0.4)
        };
    }
}
