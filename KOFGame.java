import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class KOFGame extends JPanel implements ActionListener, KeyListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameStarted && p1 != null && p2 != null) {
            p1.keyReleased(e.getKeyCode());
            p2.keyReleased(e.getKeyCode());
        }
    }

    javax.swing.Timer timer;
    SettingsManager settings;
    Player p1, p2;
    boolean gameStarted = false, gameOver = false, inMenu = false, rebinding = false;
    String rebindingPlayer = "", rebindingAction = "", winner = "";
    ArrayList<SkillEffect> effects = new ArrayList<>();

    int selectIndexP1 = 0, selectIndexP2 = 1;
    boolean p1Locked = false, p2Locked = false;
    Character[] characters = {
        new Character("Ken", Color.BLUE),
        new Character("Ryu", Color.RED),
        new Character("ChunLi", Color.PINK)
    };

    public KOFGame() {
        settings = new SettingsManager("keybinds.properties");
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new javax.swing.Timer(30, this);
        timer.start();
    }

    class Character {
        String name; Color color;
        Character(String name, Color color) {
            this.name = name; this.color = color;
        }
    }

    class Player {
    int x, y, width = 50, height = 100, hp = 100, skillCD = 0;
    Color color;
    boolean left, right, up, down, attack;
    int leftKey, rightKey, upKey, downKey, attackKey, skillKey;

    public Player(int x, int y, Color color, int l, int r, int u, int d, int atk, int skill) {
        this.x = x; this.y = y; this.color = color;
        this.leftKey = l; this.rightKey = r; this.upKey = u; this.downKey = d;
        this.attackKey = atk; this.skillKey = skill;
    }

    void move() {
        if (left) x -= 5;
        if (right) x += 5;
        if (up) y -= 5;
        if (down) y += 5;
    }

    void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    void keyPressed(int code) {
        if (code == leftKey) left = true;
        if (code == rightKey) right = true;
        if (code == upKey) up = true;
        if (code == downKey) down = true;
        if (code == attackKey) attack = true;
    }

    void keyReleased(int code) {
        if (code == leftKey) left = false;
        if (code == rightKey) right = false;
        if (code == upKey) up = false;
        if (code == downKey) down = false;
        if (code == attackKey) attack = false;
    }

    Rectangle bounds() {
        return new Rectangle(x, y, width, height);
    }
}

class SkillEffect {
    int x, y, speed = 10, size = 20;
    Player owner;

    SkillEffect(Player p) {
        this.owner = p;
        this.x = p == p1 ? p.x + p.width : p.x - size;
        this.y = p.y + 40;
    }

    void update() {
        x += owner == p1 ? speed : -speed;
    }

    void draw(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillOval(x, y, size, size);
    }

    Rectangle bounds() {
        return new Rectangle(x, y, size, size);
    }
}

public static void main(String[] args) {
        JFrame frame = new JFrame("KOF Game 完整版");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new KOFGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    //（以下重寫 keyPressed 與 drawHUD, drawMenu 等相關部分）

public void keyPressed(KeyEvent e) {
    int code = e.getKeyCode();
    if (!gameStarted) {
        if (!p1Locked) {
            if (code == KeyEvent.VK_A) selectIndexP1 = (selectIndexP1 + characters.length - 1) % characters.length;
            if (code == KeyEvent.VK_D) selectIndexP1 = (selectIndexP1 + 1) % characters.length;
            if (code == KeyEvent.VK_F) p1Locked = true;
        }
        if (!p2Locked) {
            if (code == KeyEvent.VK_LEFT) selectIndexP2 = (selectIndexP2 + characters.length - 1) % characters.length;
            if (code == KeyEvent.VK_RIGHT) selectIndexP2 = (selectIndexP2 + 1) % characters.length;
            if (code == KeyEvent.VK_ENTER) p2Locked = true;
        }
        return;
    }
    if (gameOver && code == KeyEvent.VK_R) {
        p1 = null; p2 = null;
        p1Locked = false; p2Locked = false;
        gameStarted = false; gameOver = false;
        winner = "";
        effects.clear();
        return;
    }
    if (code == KeyEvent.VK_ESCAPE) {
        inMenu = !inMenu;
        rebinding = false;
        return;
    }
    if (inMenu) {
        if (rebinding) {
            settings.setKey(rebindingPlayer + "_" + rebindingAction, code);
            settings.save();
            rebinding = false;
        } else {
            if (code == KeyEvent.VK_A) {
                rebindingPlayer = "P1";
                rebindingAction = "SKILL";
                rebinding = true;
            } else if (code == KeyEvent.VK_B) {
                rebindingPlayer = "P2";
                rebindingAction = "SKILL";
                rebinding = true;
            }
        }
        return;
    }
    if (gameStarted && p1 != null && p2 != null) {
        p1.keyPressed(code);
        p2.keyPressed(code);
        if (code == p1.skillKey && p1.skillCD == 0) {
            effects.add(new SkillEffect(p1));
            p1.skillCD = 300;
        }
        if (code == p2.skillKey && p2.skillCD == 0) {
            effects.add(new SkillEffect(p2));
            p2.skillCD = 300;
        }
    }
}

void drawMenu(Graphics g) {
    g.setColor(Color.WHITE);
    g.setFont(new Font("Arial", Font.BOLD, 20));
    g.drawString("ESC 選單 - 鍵位設定", 250, 50);
    int y = 100;
    for (String key : settings.getKeys()) {
        g.drawString(key + " = " + KeyEvent.getKeyText(settings.getKeyCode(key)), 200, y);
        y += 25;
    }
    if (rebinding) {
        g.setColor(Color.YELLOW);
        g.drawString("請按鍵設為 " + rebindingPlayer + " 的 " + rebindingAction, 200, y);
    } else {
        g.setColor(Color.CYAN);
        g.drawString("A 設定 P1 技能鍵, B 設定 P2 技能鍵", 200, y);
    }
    g.setColor(Color.LIGHT_GRAY);
    g.drawString("ESC 返回遊戲", 300, y + 40);
}

void drawHUD(Graphics g) {
    g.setColor(Color.GREEN);
    g.fillRect(50, 20, p1.hp * 2, 20);
    g.fillRect(500 + (100 - p2.hp) * 2, 20, p2.hp * 2, 20);
    g.setColor(Color.WHITE);
    g.drawRect(50, 20, 200, 20);
    g.drawRect(500, 20, 200, 20);
    g.drawString("P1 HP", 20, 15);
    g.drawString("P2 HP", 720, 15);

    g.setColor(Color.CYAN);
    g.fillRect(50, 45, 200 - p1.skillCD * 200 / 300, 8);
    g.fillRect(500, 45, 200 - p2.skillCD * 200 / 300, 8);
    g.setColor(Color.WHITE);
    g.drawRect(50, 45, 200, 8);
    g.drawRect(500, 45, 200, 8);
}



    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gameStarted) {
            drawSelection(g);
        } else if (inMenu) {
            drawMenu(g);
        } else if (gameOver) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString(winner + " Wins!", 300, 200);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Press R to Restart", 320, 250);
        } else {
            p1.move();
            p2.move();
            p1.draw(g);
            p2.draw(g);
            drawHUD(g);
            updateAndDrawEffects(g);
        }
    }

    void updateAndDrawEffects(Graphics g) {
        Iterator<SkillEffect> it = effects.iterator();
        while (it.hasNext()) {
            SkillEffect e = it.next();
            e.update();
            e.draw(g);
            if (e.bounds().intersects(p1.bounds()) && e.owner != p1) {
                p1.hp -= 5;
                it.remove();
            } else if (e.bounds().intersects(p2.bounds()) && e.owner != p2) {
                p2.hp -= 5;
                it.remove();
            } else if (e.x < 0 || e.x > getWidth()) {
                it.remove();
            }
        }
        if (p1.hp <= 0 || p2.hp <= 0) {
            winner = p1.hp <= 0 ? "P2" : "P1";
            gameOver = true;
        }
        if (p1.skillCD > 0) p1.skillCD--;
        if (p2.skillCD > 0) p2.skillCD--;
    }

    void drawSelection(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("P1: A/D to select, F to lock", 100, 50);
        g.drawString("P2: ←/→ to select, ENTER to lock", 100, 70);
        drawCharBox(g, characters[selectIndexP1], 150, 150, "P1 " + (p1Locked ? "[Locked]" : ""));
        drawCharBox(g, characters[selectIndexP2], 500, 150, "P2 " + (p2Locked ? "[Locked]" : ""));
        if (p1Locked && p2Locked && p1 == null && p2 == null) {
            p1 = new Player(100, 300, characters[selectIndexP1].color,
                    settings.getKeyCode("P1_LEFT"), settings.getKeyCode("P1_RIGHT"),
                    settings.getKeyCode("P1_UP"), settings.getKeyCode("P1_DOWN"),
                    settings.getKeyCode("P1_ATTACK"), settings.getKeyCode("P1_SKILL"));
            p2 = new Player(600, 300, characters[selectIndexP2].color,
                    settings.getKeyCode("P2_LEFT"), settings.getKeyCode("P2_RIGHT"),
                    settings.getKeyCode("P2_UP"), settings.getKeyCode("P2_DOWN"),
                    settings.getKeyCode("P2_ATTACK"), settings.getKeyCode("P2_SKILL"));
            gameStarted = true;
        }
    }

    void drawCharBox(Graphics g, Character c, int x, int y, String label) {
        g.setColor(c.color);
        g.fillRect(x, y, 100, 100);
        g.setColor(Color.WHITE);
        g.drawString(label, x, y - 10);
        g.drawString(c.name, x + 20, y + 120);
    }
}
