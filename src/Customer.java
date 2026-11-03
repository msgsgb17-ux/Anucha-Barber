import java.awt.Color;
import java.util.Random;

public class Customer {

    public static final int COLS = 24;
    public static final int ROWS = 11;

    public static final Color BASE_COLOR = new Color(230, 115, 0);

    public static final Color[] PALETTE = {
        new Color(136, 60, 160),
        new Color(215, 50, 50),
        new Color(50, 100, 225),
        new Color(50, 170, 90),
        new Color(245, 205, 40),
        new Color(240, 120, 180),
        BASE_COLOR
    };

    public static final String[] COLOR_NAMES = {
        "สีม่วง", "สีแดง", "สีน้ำเงิน", "สีเขียว", "สีเหลือง", "สีชมพู", "สีส้ม"
    };

    public static final String[] STYLE_NAMES = {
        "ทรงกะลาครอบ", "ทรงสั้น", "ทรงโมฮอก", "ทรงกลม"
    };

    private final HairBlock[][] grid = new HairBlock[ROWS][COLS];
    private final int styleIndex;
    private final int colorIndex;
    private int excessTotal;
    private int targetTotal;

    public Customer(Random random, Level level) {
        int[] styles = level.getStyles();
        styleIndex = styles[random.nextInt(styles.length)];
        colorIndex = level.isDye() ? random.nextInt(PALETTE.length - 1) : PALETTE.length - 1;
        build(random, level);
    }

    private void build(Random random, Level level) {
        boolean[][] target = new boolean[ROWS][COLS];
        boolean[][] present = new boolean[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                target[r][c] = inStyle(styleIndex, c, r);
                double dx = (c - 11.5) / level.getRx();
                double dy = (r - 5.0) / level.getRy();
                present[r][c] = target[r][c] || dx * dx + dy * dy <= 1.0;
            }
        }

        boolean[][] spikes = new boolean[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!present[r][c] && hasNeighbor(present, c, r) && random.nextDouble() < level.getSpikeChance()) {
                    spikes[r][c] = true;
                }
            }
        }

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean p = present[r][c] || spikes[r][c];
                grid[r][c] = new HairBlock(p, target[r][c], BASE_COLOR);
                if (target[r][c]) {
                    targetTotal++;
                } else if (p) {
                    excessTotal++;
                }
            }
        }
    }

    private static boolean hasNeighbor(boolean[][] present, int c, int r) {
        return (r > 0 && present[r - 1][c])
                || (r < ROWS - 1 && present[r + 1][c])
                || (c > 0 && present[r][c - 1])
                || (c < COLS - 1 && present[r][c + 1]);
    }

    private static boolean inStyle(int style, int c, int r) {
        switch (style) {
            case 0: {
                if (r < 1 || r > 9) {
                    return false;
                }
                int[] left = {7, 5, 4, 3, 3, 3, 3, 3, 3};
                int[] right = {16, 18, 19, 20, 20, 20, 20, 20, 20};
                return c >= left[r - 1] && c <= right[r - 1];
            }
            case 1: {
                if (r < 2 || r > 7) {
                    return false;
                }
                int[] left = {8, 6, 5, 5, 5, 5};
                int[] right = {15, 17, 18, 18, 18, 18};
                return c >= left[r - 2] && c <= right[r - 2];
            }
            case 2:
                return c >= 10 && c <= 13 && r >= 0 && r <= 8;
            default: {
                double dx = (c - 11.5) / 9.0;
                double dy = (r - 5.0) / 4.6;
                return dx * dx + dy * dy <= 1.0;
            }
        }
    }

    public boolean inBounds(int c, int r) {
        return c >= 0 && c < COLS && r >= 0 && r < ROWS;
    }

    public HairBlock getBlock(int c, int r) {
        return grid[r][c];
    }

    private boolean isPresent(int c, int r) {
        return inBounds(c, r) && grid[r][c].isPresent();
    }

    public int cut(int c, int r) {
        if (!isPresent(c, r)) {
            return 0;
        }
        HairBlock b = grid[r][c];
        b.remove();
        return b.isTarget() ? -20 : 10;
    }

    public boolean dye(int c, int r, Color color) {
        if (!isPresent(c, r)) {
            return false;
        }
        HairBlock b = grid[r][c];
        if (b.getColor().equals(color)) {
            return false;
        }
        b.setColor(color);
        return true;
    }

    public double getDyeCoverage() {
        if (targetTotal == 0) {
            return 0;
        }
        Color wanted = getWantedColor();
        int dyed = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                HairBlock b = grid[r][c];
                if (b.isTarget() && b.isPresent() && b.getColor().equals(wanted)) {
                    dyed++;
                }
            }
        }
        return (double) dyed / targetTotal;
    }

    public double getTargetKept() {
        if (targetTotal == 0) {
            return 0;
        }
        int kept = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                HairBlock b = grid[r][c];
                if (b.isTarget() && b.isPresent()) {
                    kept++;
                }
            }
        }
        return (double) kept / targetTotal;
    }

    public Color getWantedColor() {
        return PALETTE[colorIndex];
    }

    public String getStyleName() {
        return STYLE_NAMES[styleIndex];
    }

    public String getColorName() {
        if (colorIndex == PALETTE.length - 1) {
            return "สีเดิม (ไม่ย้อม)";
        }
        return COLOR_NAMES[colorIndex];
    }

    public int getExcessTotal() {
        return excessTotal;
    }

    public int getTargetTotal() {
        return targetTotal;
    }
}
