import java.awt.Color;

public class HairBlock {

    private boolean present;
    private final boolean target;
    private Color color;

    public HairBlock(boolean present, boolean target, Color color) {
        this.present = present;
        this.target = target;
        this.color = color;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isTarget() {
        return target;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void remove() {
        present = false;
    }
}
