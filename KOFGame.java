import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

/**
 * KOFGame  ─ 將原有邏輯完整保留，
 *  但把角色顯示從純色方塊升級為手繪圖。
 *  只要把圖檔放進 classpath 的 /img 目錄或專案資料夾下的 img/ 即可。
 *  檔名格式：<name>_idle.png  及 <name>_attack.png  及 <name>_select.png
 *  例如 ken_idle.png、ken_attack.png、ken_select.png。
 *
 *  若圖檔找不到則自動以原本的顏色方塊替代。
 */
public class KOFGame extends JPanel implements ActionListener, KeyListener {

    public static final int W = 1920;      // 畫面寬
    public static final int H = 1080;      // 畫面高
    public static final int HUD_BAR_W = 400;
    public static final int GROUND_Y = 900;    private final SettingsManager settings = new SettingsManager("keybinds.properties");
    private javax.swing.Timer timer = new javax.swing.Timer(16, this);

    /* -------- 角色定義改為含圖片 -------- */
    private final Role[] roles = loadRoles();

    private int sel1 = 0, sel2 = 1;
    private boolean lock1 = false, lock2 = false;

    private Player p1, p2;
    private final java.util.List<Skill> skills = new ArrayList<>();
    private boolean running = false, menu = false, rebinding = false;
    private String rebindingKey = "", winner = "";

    private final Map<String, Rectangle> boxes = new LinkedHashMap<>();

    /* =====================================================
     *                    建構與主迴圈
     * =====================================================*/
    public KOFGame() {
        setPreferredSize(new Dimension(1920, 1080));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        buildBoxes();
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (menu && !rebinding) {
                    for (var en : boxes.entrySet()) {
                        if (en.getValue().contains(e.getPoint())) {
                            rebindingKey = en.getKey();
                            rebinding = true;
                            break;
                        }
                    }
                }
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!running)
            drawSelect(g);
        else if (menu)
            drawMenu(g);
        else if (!winner.isEmpty())
            drawWinner(g);
        else
            drawBattle(g);
    }

    /* =====================================================
     *                    選角畫面
     * =====================================================*/
    private void drawSelect(Graphics g){
    g.setColor(Color.WHITE);
    g.drawString("P1: A/D to select, F to lock", W / 2 - 200, 60);
    g.drawString("P2: ←/→ to select, ENTER to lock", W / 2 - 200, 90);

    int boxY = H / 3;
    drawRoleBox(g, roles[sel1], (int)(W*0.30) - 50, boxY, "P1 " + (lock1 ? "[OK]" : ""));
    drawRoleBox(g, roles[sel2], (int)(W*0.70) - 50, boxY, "P2 " + (lock2 ? "[OK]" : ""));
}
    private void drawRoleBox(Graphics g, Role r,int x,int y,String label){
    if (r.selectImg != null) {                       // 有選角圖
        g.drawImage(r.selectImg, x, y, 100, 100, null);
    } else {                                         // 沒圖 → 方塊
        g.setColor(r.fallbackColor);
        g.fillRect(x, y, 100, 100);
    }
    g.setColor(Color.WHITE);
    g.drawRect(x, y, 100, 100);
    g.drawString(label, x, y - 10);
    g.drawString(r.name, x + 20, y + 120);
}


    /* =====================================================
     *                       ESC 選單
     * =====================================================*/
    private void drawMenu(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("ESC Menu - Key Bindings", 250, 28);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        for (var e : boxes.entrySet()) {
            Rectangle b = e.getValue();
            g.setColor(Color.CYAN);
            g.fillRect(b.x, b.y, b.width, b.height);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey() + " = " + nice(settings.k(e.getKey())), b.x + 8, b.y + 16);
        }
        g.setColor(rebinding ? Color.YELLOW : Color.LIGHT_GRAY);
        g.drawString(rebinding ? ("Press a key for " + rebindingKey) : "ESC to return", 260, 480);
    }

    /* =====================================================
     *                      戰鬥畫面
     * =====================================================*/
    private void drawBattle(Graphics g) {
        p1.move();
        p2.move();
        p1.draw(g);
        p2.draw(g);
        drawHUD(g);

        var it = skills.iterator();
        while (it.hasNext()) {
            Skill s = it.next();
            s.update();
            s.draw(g);
            if (s.hit(p1) && s.owner != p1) {
                p1.hp -= 5;
                it.remove();
            } else if (s.hit(p2) && s.owner != p2) {
                p2.hp -= 5;
                it.remove();
            } else if (s.outOf(getWidth()))
                it.remove();
        }
        if (p1.hp <= 0 || p2.hp <= 0)
            winner = p1.hp <= 0 ? "P2" : "P1";

        if (p1.cool > 0) p1.cool--;
        if (p2.cool > 0) p2.cool--;
        if (p1.atkCool > 0) p1.atkCool--;
        if (p2.atkCool > 0) p2.atkCool--;

        if (p1.attack && p1.atkCool == 0 && p1.bounds().intersects(p2.bounds())) {
            p2.hp--;
            p1.atkCool = 30;
        }
        if (p2.attack && p2.atkCool == 0 && p2.bounds().intersects(p1.bounds())) {
            p1.hp--;
            p2.atkCool = 30;
        }
    }

    private void drawHUD(Graphics g) {
         int barHeight = 20;
    int coolHeight = 10;

    // P1 血條：左上 50, 40
    int p1X = 50;
    int p1Y = 40;

    // P2 血條：右上 (W - 50 - HUD_BAR_W)
    int p2X = W - 50 - HUD_BAR_W;
    int p2Y = 40;
         
         g.setColor(Color.GREEN);
         g.fillRect(p1X, p1Y,  p1.hp * HUD_BAR_W / 100, barHeight);
         g.fillRect(p2X, p2Y,  p2.hp * HUD_BAR_W / 100, barHeight);
         g.setColor(Color.WHITE);
         g.drawRect(p1X, p1Y, HUD_BAR_W, barHeight);
         g.drawRect(p2X, p2Y, HUD_BAR_W, barHeight);

         g.setColor(Color.CYAN);
         g.fillRect(p1X, p1Y + barHeight + 2, HUD_BAR_W - p1.cool * HUD_BAR_W / 300, coolHeight);
         g.fillRect(p2X, p2Y + barHeight + 2, HUD_BAR_W - p2.cool * HUD_BAR_W / 300, coolHeight);
         g.setColor(Color.WHITE);
         g.drawRect(p1X, p1Y + barHeight + 2, HUD_BAR_W, coolHeight);
         g.drawRect(p2X, p2Y + barHeight + 2, HUD_BAR_W, coolHeight);
    }

    private void drawWinner(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString(winner + " Wins!", 300, 220);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Press R to restart", 320, 260);
    }

    /* =====================================================
     *                 既有事件處理邏輯 → 保留
     * =====================================================*/
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();

        if (running && c == KeyEvent.VK_ESCAPE) {
            menu = !menu;
            rebinding = false;
            return;
        }
        if (menu) {
            if (rebinding) {
                settings.setKey(rebindingKey, c);
                settings.save();
                applyKey(rebindingKey, c);
                rebinding = false;
            }
            return;
        }
        if (!running) {
            if (!lock1) {
                if (c == KeyEvent.VK_A) sel1 = (sel1 + roles.length - 1) % roles.length;
                if (c == KeyEvent.VK_D) sel1 = (sel1 + 1) % roles.length;
                if (c == KeyEvent.VK_F) lock1 = true;
            }
            if (!lock2) {
                if (c == KeyEvent.VK_LEFT) sel2 = (sel2 + roles.length - 1) % roles.length;
                if (c == KeyEvent.VK_RIGHT) sel2 = (sel2 + 1) % roles.length;
                if (c == KeyEvent.VK_ENTER) lock2 = true;
            }
            if (lock1 && lock2) startGame();
            return;
        }
        if (!winner.isEmpty()) {
            if (c == KeyEvent.VK_R) reset();
            return;
        }

        // 僅處理各自控制鍵，避免同時控制
        if (c == p1.l || c == p1.r || c == p1.u || c == p1.d || c == p1.atk) {
            p1.handle(c, true);
            if (c == p1.l) p1.faceR = false;
            if (c == p1.r) p1.faceR = true;
        }
        if (c == p2.l || c == p2.r || c == p2.u || c == p2.d || c == p2.atk) {
            p2.handle(c, true);
            if (c == p2.l) p2.faceR = false;
            if (c == p2.r) p2.faceR = true;
        }

        if (c == p1.skill && p1.cool == 0) {
            skills.add(new Skill(p1));
            p1.cool = 300;
        }
        if (c == p2.skill && p2.cool == 0) {
            skills.add(new Skill(p2));
            p2.cool = 300;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();
        if (c == p1.l || c == p1.r || c == p1.u || c == p1.d || c == p1.atk)
            p1.handle(c, false);
        if (c == p2.l || c == p2.r || c == p2.u || c == p2.d || c == p2.atk)
            p2.handle(c, false);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /* =====================================================
     *                     選單與遊戲控制
     * =====================================================*/
    private void buildBoxes() {
        int y = 70;
        for (String k : new String[]{"P1_LEFT", "P1_RIGHT", "P1_UP", "P1_DOWN", "P1_ATTACK", "P1_SKILL"}) {
            boxes.put(k, new Rectangle(200, y, 300, 25));
            y += 30;
        }
        y = 280;
        for (String k : new String[]{"P2_LEFT", "P2_RIGHT", "P2_UP", "P2_DOWN", "P2_ATTACK", "P2_SKILL"}) {
            boxes.put(k, new Rectangle(200, y, 300, 25));
            y += 30;
        }
    }

    private void startGame() {
        // P1: 左側 10% ；P2: 右側 90% - 寬
         p1 = new Player((int)(W * 0.10), GROUND_Y - 80, roles[sel1], keys("P1_"));
         p2 = new Player((int)(W * 0.90) - 40, GROUND_Y - 80, roles[sel2], keys("P2_"));

        running = true;
    }

    private void reset() {
        winner = "";
        running = false;
        lock1 = lock2 = false;
        skills.clear();
    }

    private int[] keys(String pre) {
        return new int[]{settings.k(pre + "LEFT"), settings.k(pre + "RIGHT"), settings.k(pre + "UP"), settings.k(pre + "DOWN"), settings.k(pre + "ATTACK"), settings.k(pre + "SKILL")};
    }

    private void applyKey(String k, int code) {
        if (p1 != null) p1.apply(k, code);
        if (p2 != null) p2.apply(k, code);
    }

    private static String nice(int code) {
        return switch (code) {
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            default -> KeyEvent.getKeyText(code);
        };
    }

    /* =====================================================
     *                  Role 與 Player 物件
     * =====================================================*/
   static class Role {
    final String name;
    final Color fallbackColor;         // 圖片找不到時用的方塊顏色
    // ---- 圖片資源 ----
    BufferedImage idleImg;
    BufferedImage walk1Img;
    BufferedImage walk2Img;
    BufferedImage attackImg;
    BufferedImage selectImg;

    Role(String name, Color fallbackColor) {
        this.name = name;
        this.fallbackColor = fallbackColor;
        loadImages();                  // 建構子一進來就載圖
    }

    /** 從 /img 資料夾載入圖檔；沒找到則保持 null。*/
    private void loadImages() {
        String base = "/img/" + name.toLowerCase();       // ken → /img/ken_idle.png
        idleImg   = img(base + "_idle.png");
        walk1Img  = img(base + "_walk1.png");
        walk2Img  = img(base + "_walk2.png");
        attackImg = img(base + "_attack.png");
        selectImg = img(base + "_select.png");
    }
    private static BufferedImage img(String path) {
        try { return javax.imageio.ImageIO.read(
                  java.util.Objects.requireNonNull(KOFGame.class.getResource(path))); }
        catch (Exception e) { return null; }
    }
}

    private static Role[] loadRoles() {
        return new Role[]{
                createRole("Ken", Color.BLUE),
                createRole("Ryu", Color.RED),
                createRole("ChunLi", Color.PINK)
        };
    }

    private static Role createRole(String name, Color color) {
        Role r = new Role(name, color);
        r.idleImg = loadImage("/img/" + name.toLowerCase() + "_idle.png");
        r.attackImg = loadImage("/img/" + name.toLowerCase() + "_attack.png");
        r.selectImg = loadImage("/img/" + name.toLowerCase() + "_select.png");
        return r;
    }

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(Objects.requireNonNull(KOFGame.class.getResource(path)));
        } catch (Exception e) {
            return null; // 若圖不存在則回傳 null → 由呼叫端 fallback
        }
    }

    static class Player {
        int x, y, w = 40, h = 80, hp = 100, cool = 0;
       static final int GROUND_Y = KOFGame.GROUND_Y;  // 換成全域常數

        final Role role;
        int walkIdx = 0, walkTick = 0;
        boolean faceR = true;
        int l, r, u, d, atk, skill;
        boolean L, R, U, D;
        boolean attack = false;
        int atkCool = 0;
       
        static final int JUMP_STRENGTH = -15;
        static final int GRAVITY = 1;
        int vy = 0; // 垂直速度
        boolean jumping = false;

        Player(int x, int y, Role role, int[] k) {
             this.x = x; this.y = y; this.role = role;
            l = k[0];
            r = k[1];
            u = k[2];
            d = k[3];
            atk = k[4];
            skill = k[5];
            // 若有 idle 圖，以其尺寸設定角色碰撞盒
            if (role.idleImg != null) {
                w = role.idleImg.getWidth();
                h = role.idleImg.getHeight();
            }
        }

        void move(){
    int s = 5;
    boolean moving = false;

    if (L) { x -= s; faceR = false; moving = true; }
    if (R) { x += s; faceR = true;  moving = true; }


    if (moving) {
        if (++walkTick >= 6) {      // 6 可自行調快慢
            walkIdx ^= 1;           // 0↔1 翻轉
            walkTick = 0;
        }
    } else {
        walkIdx = 0;                // 停止時回到第一張 walk 圖
    }

            // 跳躍機制
            if (U && !jumping) { // 按上跳
                vy = JUMP_STRENGTH;
                jumping = true;
            }

            if (jumping) {
                vy += GRAVITY;
                y += vy;
                if (y >= GROUND_Y) {
                    y = GROUND_Y;
                    vy = 0;
                    jumping = false;
                }
            }
        }

        void draw(Graphics g) {
    BufferedImage src = null;

    if (attack && role.attackImg != null) {
        src = role.attackImg;                       // 攻擊
    } else if ((L || R) && role.walk1Img != null) {
        src = (walkIdx == 0 ? role.walk1Img : role.walk2Img); // 走路
    } else if (role.idleImg != null) {
        src = role.idleImg;                         // 待機
    }

    if (src != null) {
        // === 計算縮放後大小：高度貼滿 h，寬度等比例 ===
        int hDst = h;                                                    // h = 80
        int wDst = src.getWidth() * hDst / src.getHeight();              // 等比例

        // === 計算繪製位置：水平置中碰撞盒，底邊貼地 ===
        int drawX = x + (w - wDst) / 2;          // 水平置中 (w = 40)
        int drawY = y + h - hDst;                // Y + h = 腳底 → 上移 hDst

        Graphics2D g2 = (Graphics2D) g;
        if (!faceR) {                            // 面向左→水平翻轉
            g2.drawImage(src,
                         drawX + wDst, drawY,
                         -wDst, hDst, null);
        } else {
            g2.drawImage(src, drawX, drawY, wDst, hDst, null);
        }
    } else {                                     // 沒圖→fallback 方塊
        g.setColor(role.fallbackColor);
        g.fillRect(x, y, w, h);
    }
}

        void handle(int code, boolean press) {
            if (code == l) L = press;
            if (code == r) R = press;
            if (code == u) U = press;
            if (code == d) D = press;
            if (code == atk) attack = press;
        }

        void apply(String key, int c) {
            switch (key) {
                case "P1_LEFT", "P2_LEFT" -> l = c;
                case "P1_RIGHT", "P2_RIGHT" -> r = c;
                case "P1_UP", "P2_UP" -> u = c;
                case "P1_DOWN", "P2_DOWN" -> d = c;
                case "P1_ATTACK", "P2_ATTACK" -> atk = c;
                case "P1_SKILL", "P2_SKILL" -> skill = c;
            }
        }

        Rectangle bounds() {
            return new Rectangle(x, y, w, h);
        }
    }

    /* =====================================================
 *                      投射技能
 * =====================================================*/
static class Skill {
    // === 新增：所有技能共用一張圖片，可自行換檔名 ===
    private static final BufferedImage IMG = loadSkillImage("/img/skill_fireball.png");

    int x, y, sz = 32, speed = 10;
    Player owner;

    Skill(Player p) {
        owner = p;
        // 起始位置改成圖片寬度
        x = p.faceR ? p.x + p.w : p.x - sz;
        y = p.y + p.h / 2 - sz / 2;
    }

    void update() {
        x += owner.faceR ? speed : -speed;
    }

    void draw(Graphics g) {
        if (IMG != null) {
            // 若要左右翻轉（面向左）：
            if (!owner.faceR) {
                Graphics2D g2 = (Graphics2D) g;
                g2.drawImage(IMG, x + IMG.getWidth(), y, -IMG.getWidth(), IMG.getHeight(), null);
            } else {
                g.drawImage(IMG, x, y, null);
            }
        } else {               // 找不到圖就fallback
            g.setColor(Color.CYAN);
            g.fillOval(x, y, sz, sz);
        }
    }

    boolean hit(Player pl) { return bounds().intersects(pl.bounds()); }
    boolean outOf(int W)   { return x < -sz || x > W + sz; }
    Rectangle bounds()     { return new Rectangle(x, y, sz, sz); }

    // === 私有靜態工具：載入圖檔，失敗回傳 null ===
    private static BufferedImage loadSkillImage(String path) {
        try { return ImageIO.read(Objects.requireNonNull(KOFGame.class.getResource(path))); }
        catch (Exception e) { return null; }
    }
}

    /* =====================================================
     *                   Key 設定管理
     * =====================================================*/
    static class SettingsManager {
        private final Properties p = new Properties();
        private final String path;

        SettingsManager(String path) {
            this.path = path;
            load();
        }

        void load() {
            try (FileInputStream f = new FileInputStream(path)) {
                p.load(f);
            } catch (IOException e) {
                setDefault();
            }
        }

        void setDefault() {
            p.setProperty("P1_LEFT", "A");
            p.setProperty("P1_RIGHT", "D");
            p.setProperty("P1_UP", "W");
            p.setProperty("P1_DOWN", "S");
            p.setProperty("P1_ATTACK", "G");
            p.setProperty("P1_SKILL", "F");
            p.setProperty("P2_LEFT", "LEFT");
            p.setProperty("P2_RIGHT", "RIGHT");
            p.setProperty("P2_UP", "UP");
            p.setProperty("P2_DOWN", "DOWN");
            p.setProperty("P2_ATTACK", "ENTER");
            p.setProperty("P2_SKILL", "L");
            save();
        }

        void save() {
            try (FileOutputStream f = new FileOutputStream(path)) {
                p.store(f, "keybinds");
            } catch (IOException ignored) {
            }
        }

        int k(String key) {
            String v = p.getProperty(key);
            return switch (v) {
                case "LEFT" -> KeyEvent.VK_LEFT;
                case "RIGHT" -> KeyEvent.VK_RIGHT;
                case "UP" -> KeyEvent.VK_UP;
                case "DOWN" -> KeyEvent.VK_DOWN;
                case "ENTER" -> KeyEvent.VK_ENTER;
                default -> KeyEvent.getExtendedKeyCodeForChar(v.charAt
(0));
            };
        }
        void setKey(String key,int code){ p.setProperty(key,KeyEvent.getKeyText(code)); }
        Set<String> getKeys(){ return p.stringPropertyNames(); }
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            JFrame f=new JFrame("KOF Game");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(new KOFGame());
            f.pack(); f.setLocationRelativeTo(null); f.setVisible(true);
        });
    }
}