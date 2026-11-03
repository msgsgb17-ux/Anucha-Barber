import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {

    public static final int PANEL_WIDTH = 960;
    public static final int PANEL_HEIGHT = 600;

    private static final int BLOCK = 14;
    private static final int GRID_X = 312;
    private static final int GRID_Y = 165;

    private static final Color SKY = new Color(161, 211, 235);
    private static final Color SKIN = new Color(245, 215, 185);
    private static final Color DARK = new Color(60, 60, 60);
    private static final Color GREEN = new Color(80, 170, 90);

    private static final Rectangle TOOLBAR = new Rectangle(820, 130, 90, 270);
    private static final Rectangle DONE_BUTTON = new Rectangle(780, 500, 150, 46);
    private static final Rectangle PLAY_BUTTON = new Rectangle(380, 430, 200, 56);
    private static final Rectangle BACK_BUTTON = new Rectangle(40, 520, 140, 46);
    private static final Rectangle PALETTE_PANEL = new Rectangle(704, 236, 104, 176);
    private static final Ellipse2D BUBBLE = new Ellipse2D.Double(25, 45, 210, 160);

    private enum State { MENU, LEVELS, PLAYING, RESULT }

    private static class Popup {
        String text;
        double x;
        double y;
        int life;
        boolean good;
    }

    private final Random random = new Random();
    private final ScoreManager score = new ScoreManager();
    private final List<Popup> popups = new ArrayList<>();
    private final Timer timer = new Timer(16, this);
    private final Level[] levels = Level.createAll();
    private final int[] bestStars = new int[levels.length];

    private Customer customer = new Customer(random, levels[0]);
    private State state = State.MENU;
    private Tool tool = Tool.SCISSORS;
    private int levelIndex = 0;
    private boolean unlockAll = false;
    private int sprayIndex = 0;
    private boolean paletteOpen = false;
    private boolean working = false;
    private boolean showZoom = false;
    private long endTime = 0;
    private long remaining = levels[0].getSeconds() * 1000L;
    private int stars = 0;
    private Point mouse = new Point(-100, -100);
    private Point lastPoint = new Point(0, 0);

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(SKY);
        setFocusable(true);
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleDrag(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                working = false;
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_U) {
                    unlockAll = true;
                }
            }
        });
        timer.start();
    }

    private boolean isUnlocked(int i) {
        return unlockAll || i == 0 || bestStars[i - 1] >= 2;
    }

    private Rectangle levelCard(int i) {
        return new Rectangle(40 + i * 180, 170, 160, 250);
    }

    private void resetPreview() {
        customer = new Customer(random, levels[0]);
        score.reset();
        stars = 0;
        remaining = levels[0].getSeconds() * 1000L;
        popups.clear();
    }

    void startGame(int index) {
        levelIndex = index;
        Level level = levels[index];
        customer = new Customer(random, level);
        score.reset();
        tool = Tool.SCISSORS;
        sprayIndex = 0;
        paletteOpen = false;
        working = false;
        popups.clear();
        showZoom = false;
        stars = 0;
        long millis = level.getSeconds() * 1000L;
        endTime = System.currentTimeMillis() + millis;
        remaining = millis;
        state = State.PLAYING;
    }

    private void finishGame() {
        state = State.RESULT;
        showZoom = false;
        paletteOpen = false;
        working = false;
        stars = score.getStars(customer.getExcessTotal(), customer.getDyeCoverage(), customer.getTargetKept());
        if (stars > bestStars[levelIndex]) {
            bestStars[levelIndex] = stars;
        }
    }

    private boolean canGoNext() {
        return stars >= 2 && levelIndex < levels.length - 1;
    }

    private int resultButtonCount() {
        return canGoNext() ? 3 : 2;
    }

    private Rectangle resultButton(int i, int count) {
        int w = 190;
        int gap = 20;
        int total = count * w + (count - 1) * gap;
        return new Rectangle(480 - total / 2 + i * (w + gap), 470, w, 56);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) {
            remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0) {
                remaining = 0;
                finishGame();
            }
        }
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            p.life--;
            p.y -= 0.8;
            if (p.life <= 0) {
                it.remove();
            }
        }
        repaint();
    }

    private Rectangle swatch(int i) {
        return new Rectangle(PALETTE_PANEL.x + 8 + (i % 2) * 48, PALETTE_PANEL.y + 8 + (i / 2) * 42, 40, 36);
    }

    private void handlePress(Point p) {
        requestFocusInWindow();
        mouse = p;
        if (state == State.MENU) {
            if (PLAY_BUTTON.contains(p)) {
                state = State.LEVELS;
            }
            return;
        }
        if (state == State.LEVELS) {
            if (BACK_BUTTON.contains(p)) {
                resetPreview();
                state = State.MENU;
                return;
            }
            for (int i = 0; i < levels.length; i++) {
                if (levelCard(i).contains(p) && isUnlocked(i)) {
                    startGame(i);
                    return;
                }
            }
            return;
        }
        if (state == State.RESULT) {
            int count = resultButtonCount();
            for (int i = 0; i < count; i++) {
                if (resultButton(i, count).contains(p)) {
                    if (i == 0) {
                        resetPreview();
                        state = State.LEVELS;
                    } else if (i == 1) {
                        startGame(levelIndex);
                    } else {
                        startGame(levelIndex + 1);
                    }
                    return;
                }
            }
            return;
        }
        if (showZoom) {
            showZoom = false;
            return;
        }
        if (BUBBLE.contains(p)) {
            showZoom = true;
            paletteOpen = false;
            working = false;
            return;
        }
        if (DONE_BUTTON.contains(p)) {
            finishGame();
            return;
        }
        if (paletteOpen) {
            for (int i = 0; i < Customer.PALETTE.length; i++) {
                if (swatch(i).contains(p)) {
                    sprayIndex = i;
                    paletteOpen = false;
                    return;
                }
            }
            if (PALETTE_PANEL.contains(p)) {
                return;
            }
            if (!TOOLBAR.contains(p)) {
                paletteOpen = false;
            }
        }
        if (TOOLBAR.contains(p)) {
            int slot = (p.y - TOOLBAR.y) / 90;
            if (slot == 0) {
                tool = Tool.SCISSORS;
                paletteOpen = false;
            } else if (slot == 1) {
                if (tool == Tool.SPRAY) {
                    paletteOpen = !paletteOpen;
                } else {
                    tool = Tool.SPRAY;
                    paletteOpen = true;
                }
            } else {
                tool = Tool.CLIPPER;
                paletteOpen = false;
            }
            return;
        }
        working = true;
        lastPoint = p;
        applyTool(p, p);
    }

    private void handleDrag(Point p) {
        mouse = p;
        if (state == State.PLAYING && working) {
            applyTool(lastPoint, p);
            lastPoint = p;
        }
    }

    private void applyTool(Point from, Point to) {
        int dist = Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y));
        int steps = Math.max(1, dist / 4);
        int total = 0;
        for (int s = 0; s <= steps; s++) {
            int x = from.x + (to.x - from.x) * s / steps;
            int y = from.y + (to.y - from.y) * s / steps;
            int c = Math.floorDiv(x - GRID_X, BLOCK);
            int r = Math.floorDiv(y - GRID_Y, BLOCK);
            int gained = 0;
            if (tool == Tool.SCISSORS) {
                int d = customer.cut(c, r);
                if (d != 0) {
                    score.add(d);
                    gained += d;
                }
            } else if (tool == Tool.CLIPPER) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int d = customer.cut(c + dc, r + dr);
                        if (d != 0) {
                            score.add(d);
                            gained += d;
                        }
                    }
                }
            } else {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        customer.dye(c + dc, r + dr, Customer.PALETTE[sprayIndex]);
                    }
                }
            }
            total += gained;
        }
        if (total != 0) {
            Popup p = new Popup();
            p.text = (total > 0 ? "+" : "") + total;
            p.x = to.x + 10;
            p.y = to.y - 10;
            p.life = 40;
            p.good = total > 0;
            popups.add(p);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (state == State.LEVELS) {
            drawLevels(g2);
        } else {
            drawScene(g2);
            drawHud(g2);
            drawToolbar(g2);
            if (state == State.PLAYING) {
                drawBrush(g2);
                drawPopups(g2);
                drawButton(g2, DONE_BUTTON, "เสร็จแล้ว", GREEN);
                if (showZoom) {
                    drawZoom(g2);
                }
            }
            if (state == State.MENU) {
                drawMenu(g2);
            } else if (state == State.RESULT) {
                drawResult(g2);
            }
        }
        g2.dispose();
    }

    private void drawCentered(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }

    private void drawButton(Graphics2D g, Rectangle rect, String text, Color color) {
        g.setColor(color);
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20);
        g.setColor(color.darker());
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 22));
        drawCentered(g, text, rect.x + rect.width / 2, rect.y + rect.height / 2 + 8);
    }

    private void drawScene(Graphics2D g) {
        g.setColor(SKY);
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g.setColor(new Color(230, 230, 230));
        g.fillOval(210, 500, 220, 220);
        g.setColor(Color.WHITE);
        g.fillOval(250, 470, 460, 300);

        g.setColor(SKIN);
        g.fillOval(365, 235, 230, 230);
        g.setColor(Color.BLACK);
        g.fillOval(438, 340, 18, 18);
        g.fillOval(504, 340, 18, 18);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (state == State.RESULT && stars == 0) {
            g.drawArc(448, 392, 64, 36, 0, 180);
        } else {
            g.drawArc(448, 368, 64, 36, 180, 180);
        }

        for (int r = 0; r < Customer.ROWS; r++) {
            for (int c = 0; c < Customer.COLS; c++) {
                HairBlock b = customer.getBlock(c, r);
                if (!b.isPresent()) {
                    continue;
                }
                int x = GRID_X + c * BLOCK;
                int y = GRID_Y + r * BLOCK;
                g.setColor(b.getColor());
                g.fillRect(x, y, BLOCK, BLOCK);
                g.setColor(new Color(0, 0, 0, 35));
                g.setStroke(new BasicStroke(1));
                g.drawRect(x, y, BLOCK - 1, BLOCK - 1);
            }
        }

        g.setColor(Color.WHITE);
        g.fillOval(25, 45, 210, 160);
        g.fillOval(222, 190, 24, 24);
        g.fillOval(250, 214, 15, 15);
        g.fillOval(268, 232, 9, 9);

        g.setColor(SKIN);
        g.fillOval(88, 103, 84, 84);
        g.setColor(customer.getWantedColor());
        for (int r = 0; r < Customer.ROWS; r++) {
            for (int c = 0; c < Customer.COLS; c++) {
                if (customer.getBlock(c, r).isTarget()) {
                    g.fillRect(70 + c * 5, 78 + r * 5, 5, 5);
                }
            }
        }
        g.setColor(Color.BLACK);
        g.fillOval(113, 142, 6, 6);
        g.fillOval(141, 142, 6, 6);

        g.setColor(DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 16));
        drawCentered(g, customer.getStyleName() + " / " + customer.getColorName(), 130, 235);
        g.setFont(new Font("Tahoma", Font.PLAIN, 13));
        drawCentered(g, "(คลิกที่ฟองเพื่อขยาย)", 130, 256);
    }

    private void drawZoom(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setColor(Color.WHITE);
        g.fillRoundRect(170, 60, 620, 490, 30, 30);
        g.setColor(new Color(90, 90, 90));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(170, 60, 620, 490, 30, 30);

        g.setColor(DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 26));
        drawCentered(g, "ทรงที่ลูกค้าต้องการ", 480, 105);

        int cx = 480;
        int cy = 345;
        g.setColor(SKIN);
        g.fillOval(cx - 131, cy - 131, 262, 262);
        g.setColor(Color.BLACK);
        g.fillOval(cx - 48, cy - 10, 21, 21);
        g.fillOval(cx + 28, cy - 10, 21, 21);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(cx - 37, cy + 21, 74, 41, 180, 180);

        int size = 16;
        int ox = cx - 192;
        int oy = cy - 211;
        for (int r = 0; r < Customer.ROWS; r++) {
            for (int c = 0; c < Customer.COLS; c++) {
                if (customer.getBlock(c, r).isTarget()) {
                    g.setColor(customer.getWantedColor());
                    g.fillRect(ox + c * size, oy + r * size, size, size);
                    g.setColor(new Color(0, 0, 0, 35));
                    g.setStroke(new BasicStroke(1));
                    g.drawRect(ox + c * size, oy + r * size, size - 1, size - 1);
                }
            }
        }

        g.setColor(DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 24));
        drawCentered(g, customer.getStyleName() + " / " + customer.getColorName(), 480, 505);
        g.setFont(new Font("Tahoma", Font.PLAIN, 15));
        drawCentered(g, "คลิกที่ใดก็ได้เพื่อปิด (เวลายังเดินอยู่)", 480, 531);
    }

    private void drawHud(Graphics2D g) {
        g.setColor(DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 24));
        drawCentered(g, "SCORE: " + score.getScore(), 865, 50);
        int secs = (int) Math.ceil(remaining / 1000.0);
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        g.setColor(state == State.PLAYING && remaining <= 10000 ? new Color(200, 40, 40) : DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 38));
        drawCentered(g, time, 865, 98);
        if (state == State.PLAYING) {
            g.setColor(DARK);
            g.setFont(new Font("Tahoma", Font.BOLD, 22));
            drawCentered(g, "ด่านที่ " + (levelIndex + 1) + " / " + levels.length + " : "
                    + levels[levelIndex].getName(), 480, 40);
        }
    }

    private void drawToolbar(Graphics2D g) {
        RoundRectangle2D box = new RoundRectangle2D.Double(TOOLBAR.x, TOOLBAR.y, TOOLBAR.width, TOOLBAR.height, 28, 28);
        g.setColor(Color.WHITE);
        g.fill(box);

        int selected = tool == Tool.SCISSORS ? 0 : tool == Tool.SPRAY ? 1 : 2;
        g.setColor(new Color(255, 235, 150));
        g.fillRoundRect(TOOLBAR.x + 8, TOOLBAR.y + selected * 90 + 6, 74, 78, 18, 18);

        g.setColor(new Color(90, 90, 90));
        g.setStroke(new BasicStroke(4));
        g.draw(box);

        int cx = TOOLBAR.x + TOOLBAR.width / 2;

        int cy = TOOLBAR.y + 45;
        g.setColor(DARK);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 10, cy - 18, cx + 8, cy + 10);
        g.drawLine(cx + 10, cy - 18, cx - 8, cy + 10);
        g.drawOval(cx - 20, cy + 6, 14, 14);
        g.drawOval(cx + 6, cy + 6, 14, 14);

        cy = TOOLBAR.y + 135;
        g.setColor(DARK);
        g.fillRect(cx - 4, cy - 20, 16, 5);
        g.fillRect(cx - 4, cy - 15, 8, 9);
        g.setColor(Customer.PALETTE[sprayIndex]);
        g.fillRect(cx - 12, cy - 6, 24, 26);

        cy = TOOLBAR.y + 233;
        g.setColor(DARK);
        g.fillRoundRect(cx - 13, cy - 6, 26, 32, 12, 12);
        g.setColor(new Color(150, 150, 150));
        g.fillRect(cx - 16, cy - 16, 32, 10);
        for (int i = 0; i < 7; i++) {
            g.fillRect(cx - 16 + i * 5, cy - 22, 3, 7);
        }
        g.setColor(new Color(220, 220, 220));
        g.fillOval(cx - 4, cy + 6, 8, 8);

        if (paletteOpen && state == State.PLAYING) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(PALETTE_PANEL.x, PALETTE_PANEL.y, PALETTE_PANEL.width, PALETTE_PANEL.height, 20, 20);
            g.setColor(new Color(90, 90, 90));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(PALETTE_PANEL.x, PALETTE_PANEL.y, PALETTE_PANEL.width, PALETTE_PANEL.height, 20, 20);
            for (int i = 0; i < Customer.PALETTE.length; i++) {
                Rectangle s = swatch(i);
                g.setColor(Customer.PALETTE[i]);
                g.fillRect(s.x, s.y, s.width, s.height);
                g.setColor(i == sprayIndex ? Color.BLACK : new Color(0, 0, 0, 70));
                g.setStroke(new BasicStroke(i == sprayIndex ? 4 : 1));
                g.drawRect(s.x, s.y, s.width, s.height);
            }
        }
    }

    private void drawBrush(Graphics2D g) {
        if (TOOLBAR.contains(mouse) || DONE_BUTTON.contains(mouse)) {
            return;
        }
        if (paletteOpen && PALETTE_PANEL.contains(mouse)) {
            return;
        }
        if (mouse.x < GRID_X - 60 || mouse.x > GRID_X + Customer.COLS * BLOCK + 60
                || mouse.y < GRID_Y - 60 || mouse.y > GRID_Y + Customer.ROWS * BLOCK + 60) {
            return;
        }
        int c = Math.floorDiv(mouse.x - GRID_X, BLOCK);
        int r = Math.floorDiv(mouse.y - GRID_Y, BLOCK);
        int half = tool == Tool.SCISSORS ? 0 : 1;
        int x = GRID_X + (c - half) * BLOCK;
        int y = GRID_Y + (r - half) * BLOCK;
        int size = (2 * half + 1) * BLOCK;
        if (tool == Tool.SCISSORS) {
            g.setColor(new Color(220, 40, 40));
        } else if (tool == Tool.SPRAY) {
            g.setColor(Customer.PALETTE[sprayIndex]);
        } else {
            g.setColor(new Color(40, 40, 40));
        }
        g.setStroke(new BasicStroke(3));
        g.drawRect(x, y, size, size);
    }

    private void drawPopups(Graphics2D g) {
        g.setFont(new Font("Tahoma", Font.BOLD, 22));
        for (Popup p : popups) {
            int alpha = Math.min(255, p.life * 10);
            g.setColor(p.good ? new Color(30, 150, 60, alpha) : new Color(210, 40, 40, alpha));
            g.drawString(p.text, (int) p.x, (int) p.y);
        }
    }

    private void drawStar(Graphics2D g, int cx, int cy, int radius, boolean filled) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-90 + i * 36);
            double rad = i % 2 == 0 ? radius : radius * 0.45;
            double px = cx + Math.cos(angle) * rad;
            double py = cy + Math.sin(angle) * rad;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        g.setColor(filled ? new Color(250, 200, 30) : new Color(140, 140, 140));
        g.fill(star);
        g.setColor(filled ? new Color(190, 140, 10) : new Color(90, 90, 90));
        g.setStroke(new BasicStroke(3));
        g.draw(star);
    }

    private void drawLevels(Graphics2D g) {
        g.setColor(SKY);
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setColor(DARK);
        g.setFont(new Font("Tahoma", Font.BOLD, 42));
        drawCentered(g, "เลือกด่าน", 480, 90);
        g.setFont(new Font("Tahoma", Font.PLAIN, 20));
        drawCentered(g, "ผ่านด่านต้องได้อย่างน้อย 2 ดาว จึงจะปลดล็อกด่านถัดไป", 480, 135);

        for (int i = 0; i < levels.length; i++) {
            Level level = levels[i];
            Rectangle card = levelCard(i);
            boolean unlocked = isUnlocked(i);
            int cx = card.x + card.width / 2;

            if (!unlocked) {
                g.setColor(new Color(205, 205, 205));
            } else if (card.contains(mouse)) {
                g.setColor(new Color(255, 245, 200));
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRoundRect(card.x, card.y, card.width, card.height, 26, 26);
            g.setColor(new Color(90, 90, 90));
            g.setStroke(new BasicStroke(4));
            g.drawRoundRect(card.x, card.y, card.width, card.height, 26, 26);

            g.setColor(unlocked ? DARK : new Color(130, 130, 130));
            g.setFont(new Font("Tahoma", Font.BOLD, 54));
            drawCentered(g, String.valueOf(i + 1), cx, card.y + 70);
            g.setFont(new Font("Tahoma", Font.BOLD, 18));
            drawCentered(g, level.getName(), cx, card.y + 108);
            g.setFont(new Font("Tahoma", Font.PLAIN, 15));
            int secs = level.getSeconds();
            drawCentered(g, String.format("เวลา %d:%02d นาที", secs / 60, secs % 60), cx, card.y + 138);
            drawCentered(g, level.isDye() ? "ต้องย้อมสี" : "ไม่ต้องย้อมสี", cx, card.y + 160);

            if (unlocked) {
                for (int k = 0; k < 3; k++) {
                    drawStar(g, cx + (k - 1) * 40, card.y + 208, 15, k < bestStars[i]);
                }
            } else {
                g.setColor(new Color(110, 110, 110));
                g.setStroke(new BasicStroke(5));
                g.drawArc(cx - 10, card.y + 182, 20, 28, 0, 180);
                g.fillRoundRect(cx - 17, card.y + 198, 34, 28, 8, 8);
            }
        }
        drawButton(g, BACK_BUTTON, "กลับ", new Color(110, 110, 110));
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 48));
        drawCentered(g, "ร้านตัดผม 2D", 480, 110);
        g.setFont(new Font("Tahoma", Font.PLAIN, 22));
        drawCentered(g, "2D Hair Salon Simulator", 480, 150);
        g.setFont(new Font("Tahoma", Font.PLAIN, 20));
        drawCentered(g, "ฟองความคิดมุมซ้ายบนบอกทรงผมและสีที่ลูกค้าต้องการ (คลิกที่ฟองเพื่อขยาย)", 480, 215);
        drawCentered(g, "กรรไกร: ตัดทีละ 1 บล็อก ส่วนเกิน +10 โดนทรงเป้าหมาย -20", 480, 255);
        drawCentered(g, "แบตเตอเลี่ยน: ตัดทีละ 3×3 บล็อก เร็วแต่เสี่ยง ส่วนเกิน +10 โดนทรงเป้าหมาย -20", 480, 295);
        drawCentered(g, "สเปรย์: เลือกสีจากจานสี แล้วลากพ่นลงบนผม", 480, 335);
        drawCentered(g, "มีทั้งหมด 5 ด่าน ยิ่งสูงยิ่งเวลาน้อยลงและผมรกขึ้น ผ่านด่านต้องได้อย่างน้อย 2 ดาว", 480, 375);
        drawButton(g, PLAY_BUTTON, "เริ่มเกม", GREEN);
    }

    private void drawResult(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 44));
        drawCentered(g, remaining == 0 ? "หมดเวลา!" : "เสร็จสิ้น!", 480, 95);
        g.setFont(new Font("Tahoma", Font.BOLD, 22));
        drawCentered(g, "ด่านที่ " + (levelIndex + 1) + " : " + levels[levelIndex].getName(), 480, 135);
        g.setFont(new Font("Tahoma", Font.BOLD, 30));
        drawCentered(g, "คะแนนรวม: " + score.getScore(), 480, 185);
        g.setFont(new Font("Tahoma", Font.PLAIN, 20));
        drawCentered(g, "ตัดผมส่วนเกิน " + score.getExcessCut() + " ก้อน (จากทั้งหมด " + customer.getExcessTotal()
                + ")   ตัดพลาด " + score.getTargetCut() + " ก้อน", 480, 225);
        if (levels[levelIndex].isDye()) {
            drawCentered(g, "ย้อมสีถูกต้อง " + Math.round(customer.getDyeCoverage() * 100) + "%", 480, 258);
        }
        for (int i = 0; i < 3; i++) {
            drawStar(g, 480 + (i - 1) * 90, 335, 36, i < stars);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("Tahoma", Font.BOLD, 24));
        String message;
        if (stars >= 2) {
            message = levelIndex == levels.length - 1 ? "ผ่านครบทุกด่านแล้ว ยินดีด้วย!" : "ผ่านด่านแล้ว! ลูกค้าพอใจ";
        } else if (stars == 1) {
            message = "ลูกค้าพอใจเล็กน้อย แต่ยังไม่ผ่าน (ต้องได้ 2 ดาวขึ้นไป)";
        } else {
            message = "ลูกค้าไม่พอใจ... ไม่ผ่านด่าน";
        }
        drawCentered(g, message, 480, 415);

        int count = resultButtonCount();
        String[] labels = {"เลือกด่าน", "เล่นซ้ำ", "ด่านต่อไป"};
        for (int i = 0; i < count; i++) {
            drawButton(g, resultButton(i, count), labels[i], i == count - 1 && count == 3 ? GREEN : new Color(70, 130, 190));
        }
    }
}
