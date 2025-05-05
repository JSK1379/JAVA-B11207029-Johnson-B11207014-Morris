
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class KOFGame extends JPanel implements ActionListener, KeyListener {
    javax.swing.Timer timer;
    Player p1, p2;
    boolean gameStarted = false;
    boolean gameOver = false;
    String winner = "";
    Character[] characters = {
        new Character("Ken", Color.BLUE, 5, "Punch"),
        new Character("Ryu", Color.RED, 4, "Fireball"),
        new Character("ChunLi", Color.PINK, 6, "Kick")
    };
    int selectIndexP1 = 0, selectIndexP2 = 1;

    public KOFGame() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new javax.swing.Timer(30, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gameStarted) {
            drawCharacterSelection(g);
        } else if (!gameOver) {
            p1.draw(g);
            p2.draw(g);
            p1.checkAttack(p2);
            p2.checkAttack(p1);
            drawHealthBars(g);
            checkVictory();
        } else {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString(winner + " Wins!", 300, 200);
        }
    }

    private void drawCharacterSelection(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("P1: Use A/D to choose, F to confirm", 100, 50);
        g.drawString("P2: Use ←/→ to choose, ENTER to confirm", 100, 80);
        drawCharBox(g, characters[selectIndexP1], 150, 150);
        drawCharBox(g, characters[selectIndexP2], 500, 150);
    }

    private void drawCharBox(Graphics g, Character c, int x, int y) {
        g.setColor(c.color);
        g.fillRect(x, y, 100, 100);
        g.setColor(Color.WHITE);
        g.drawString(c.name, x, y - 10);
        g.drawString("Skill: " + c.skillName, x, y + 120);
    }

    private void drawHealthBars(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(50, 20, p1.hp * 2, 20);
        g.fillRect(700 - p2.hp * 2, 20, p2.hp * 2, 20);
        g.setColor(Color.WHITE);
        g.drawRect(50, 20, 200, 20);
        g.drawRect(500, 20, 200, 20);
        g.drawString(p1.character.name, 20, 35);
        g.drawString(p2.character.name, 750, 35);
    }

    private void checkVictory() {
        if (p1.hp <= 0) {
            winner = p2.character.name;
            gameOver = true;
        } else if (p2.hp <= 0) {
            winner = p1.character.name;
            gameOver = true;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (gameStarted && !gameOver) {
            p1.move();
            p2.move();
        }
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java 格鬥遊戲 - 選角+技能版");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new KOFGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        if (!gameStarted) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_A) selectIndexP1 = (selectIndexP1 + characters.length - 1) % characters.length;
            if (code == KeyEvent.VK_D) selectIndexP1 = (selectIndexP1 + 1) % characters.length;
            if (code == KeyEvent.VK_LEFT) selectIndexP2 = (selectIndexP2 + characters.length - 1) % characters.length;
            if (code == KeyEvent.VK_RIGHT) selectIndexP2 = (selectIndexP2 + 1) % characters.length;
            if (code == KeyEvent.VK_F && p1 == null) {
                p1 = new Player(100, 300, characters[selectIndexP1], KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_F);
            }
            if (code == KeyEvent.VK_ENTER && p2 == null) {
                p2 = new Player(600, 300, characters[selectIndexP2], KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER);
            }
            if (p1 != null && p2 != null) gameStarted = true;
        } else {
            p1.keyPressed(e);
            p2.keyPressed(e);
        }
    }

    public void keyReleased(KeyEvent e) {
        if (gameStarted) {
            p1.keyReleased(e);
            p2.keyReleased(e);
        }
    }

    public void keyTyped(KeyEvent e) {}

    class Character {
        String name;
        Color color;
        int speed;
        String skillName;

        public Character(String name, Color color, int speed, String skillName) {
            this.name = name;
            this.color = color;
            this.speed = speed;
            this.skillName = skillName;
        }
    }

    class Player {
        int x, y, width = 50, height = 100;
        int hp = 100;
        Character character;
        boolean left, right, up, down, attack;
        int leftKey, rightKey, upKey, downKey, attackKey;

        public Player(int x, int y, Character c, int l, int r, int u, int d, int atk) {
            this.x = x;
            this.y = y;
            this.character = c;
            this.leftKey = l;
            this.rightKey = r;
            this.upKey = u;
            this.downKey = d;
            this.attackKey = atk;
        }

        public void move() {
            if (left) x -= character.speed;
            if (right) x += character.speed;
            if (up) y -= character.speed;
            if (down) y += character.speed;
        }

        public void draw(Graphics g) {
            g.setColor(character.color);
            g.fillRect(x, y, width, height);
        }

        public void checkAttack(Player opponent) {
            if (attack) {
                Rectangle atkBox = new Rectangle(x - 10, y, width + 20, height);
                Rectangle oppBox = new Rectangle(opponent.x, opponent.y, opponent.width, opponent.height);
                if (atkBox.intersects(oppBox)) {
                    opponent.hp -= 1;
                    if (opponent.hp < 0) opponent.hp = 0;
                }
            }
        }

        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == leftKey) left = true;
            if (code == rightKey) right = true;
            if (code == upKey) up = true;
            if (code == downKey) down = true;
            if (code == attackKey) attack = true;
        }

        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == leftKey) left = false;
            if (code == rightKey) right = false;
            if (code == upKey) up = false;
            if (code == downKey) down = false;
            if (code == attackKey) attack = false;
        }
    }
}
