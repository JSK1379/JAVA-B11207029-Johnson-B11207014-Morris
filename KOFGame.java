
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class KOFGame extends JPanel implements ActionListener, KeyListener {
    Timer timer;
    Player p1, p2;
    boolean gameOver = false;
    String winner = "";

    public KOFGame() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        p1 = new Player(100, 300, Color.BLUE, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_F, "Player 1");
        p2 = new Player(600, 300, Color.RED, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER, "Player 2");
        timer = new Timer(30, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!gameOver) {
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

    private void drawHealthBars(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(50, 20, p1.hp * 2, 20);
        g.fillRect(700 - p2.hp * 2, 20, p2.hp * 2, 20);
        g.setColor(Color.WHITE);
        g.drawRect(50, 20, 200, 20);
        g.drawRect(500, 20, 200, 20);
        g.drawString("P1", 20, 35);
        g.drawString("P2", 750, 35);
    }

    private void checkVictory() {
        if (p1.hp <= 0) {
            winner = "Player 2";
            gameOver = true;
        } else if (p2.hp <= 0) {
            winner = "Player 1";
            gameOver = true;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            p1.move();
            p2.move();
            repaint();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java 格鬥遊戲 - 完整版");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new KOFGame());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        p1.keyPressed(e);
        p2.keyPressed(e);
    }
    public void keyReleased(KeyEvent e) {
        p1.keyReleased(e);
        p2.keyReleased(e);
    }
    public void keyTyped(KeyEvent e) {}

    class Player {
        int x, y, width = 50, height = 100;
        int speed = 5, hp = 100;
        Color color;
        boolean left, right, up, down, attack;
        int leftKey, rightKey, upKey, downKey, attackKey;
        String name;

        public Player(int x, int y, Color color, int l, int r, int u, int d, int atk, String name) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.leftKey = l;
            this.rightKey = r;
            this.upKey = u;
            this.downKey = d;
            this.attackKey = atk;
            this.name = name;
        }

        public void move() {
            if (left) x -= speed;
            if (right) x += speed;
            if (up) y -= speed;
            if (down) y += speed;
        }

        public void draw(Graphics g) {
            g.setColor(color);
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
