import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class KOFGame extends JPanel implements ActionListener, KeyListener {

    private final SettingsManager settings = new SettingsManager("keybinds.properties");
    private javax.swing.Timer timer = new javax.swing.Timer(16, this);

    record Role(String name, Color color){}
    private final Role[] roles = {
            new Role("Ken",   Color.BLUE),
            new Role("Ryu",   Color.RED),
            new Role("ChunLi",Color.PINK)
    };
    private int sel1=0, sel2=1;
    private boolean lock1=false, lock2=false;

    private Player p1, p2;
    private final java.util.List<Skill> skills=new ArrayList<>();
    private boolean running=false, menu=false, rebinding=false;
    private String rebindingKey="", winner="";

    private final Map<String, Rectangle> boxes=new LinkedHashMap<>();

    public KOFGame(){
        setPreferredSize(new Dimension(800,500));
        setBackground(Color.BLACK);
        setFocusable(true); addKeyListener(this);

        buildBoxes();
        addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){
                if(menu && !rebinding){
                    for(var en: boxes.entrySet()){
                        if(en.getValue().contains(e.getPoint())){
                            rebindingKey = en.getKey(); rebinding = true; break;
                        }
                    }
                }
            }
        });
        timer.start();
    }

    @Override protected void paintComponent(Graphics g){
        super.paintComponent(g);
        if(!running)                 drawSelect(g);
        else if(menu)                drawMenu(g);
        else if(!winner.isEmpty())  drawWinner(g);
        else                         drawBattle(g);
    }

    private void drawSelect(Graphics g){
        g.setColor(Color.WHITE);
        g.drawString("P1: A/D to select, F to lock", 100,40);
        g.drawString("P2: ←/→ to select, ENTER to lock", 100,60);
        drawRoleBox(g, roles[sel1], 150,120, "P1 "+(lock1?"[OK]":""));
        drawRoleBox(g, roles[sel2], 500,120, "P2 "+(lock2?"[OK]":""));
    }
    private void drawRoleBox(Graphics g, Role r,int x,int y,String label){
        g.setColor(r.color); g.fillRect(x,y,100,100);
        g.setColor(Color.WHITE); g.drawRect(x,y,100,100);
        g.drawString(label,x,y-10); g.drawString(r.name,x+20,y+120);
    }
    private void drawMenu(Graphics g){
        g.setColor(Color.WHITE); g.setFont(new Font("Arial",Font.BOLD,20));
        g.drawString("ESC Menu - Key Bindings", 250,28);
        g.setFont(new Font("Arial",Font.PLAIN,14));
        for(var e: boxes.entrySet()){
            Rectangle b=e.getValue();
            g.setColor(Color.CYAN); g.fillRect(b.x,b.y,b.width,b.height);
            g.setColor(Color.BLACK);
            g.drawString(e.getKey()+" = "+nice(settings.k(e.getKey())), b.x+8,b.y+16);
        }
        g.setColor(rebinding?Color.YELLOW:Color.LIGHT_GRAY);
        g.drawString(rebinding?("Press a key for "+rebindingKey):"ESC to return", 260, 480);
    }
    private void drawBattle(Graphics g){
        p1.move(); p2.move(); p1.draw(g); p2.draw(g);
        drawHUD(g);

        var it=skills.iterator();
        while(it.hasNext()){
            Skill s=it.next(); s.update(); s.draw(g);
            if(s.hit(p1) && s.owner!=p1){ p1.hp-=5; it.remove(); }
            else if(s.hit(p2)&& s.owner!=p2){ p2.hp-=5; it.remove();}
            else if(s.outOf(getWidth()))     it.remove();
        }
        if(p1.hp<=0 || p2.hp<=0) winner = p1.hp<=0 ? "P2" : "P1";

        if(p1.cool>0) p1.cool--;
        if(p2.cool>0) p2.cool--;
        if(p1.atkCool > 0) p1.atkCool--;
        if(p2.atkCool > 0) p2.atkCool--;

        if(p1.attack && p1.atkCool == 0 && p1.bounds().intersects(p2.bounds())){
            p2.hp--; p1.atkCool = 30;
        }
        if(p2.attack && p2.atkCool == 0 && p2.bounds().intersects(p1.bounds())){
            p1.hp--; p2.atkCool = 30;
        }
    }
    private void drawHUD(Graphics g){
        g.setColor(Color.GREEN);
        g.fillRect(50,20, p1.hp*2,15);
        g.fillRect(550,20,p2.hp*2,15);
        g.setColor(Color.WHITE);
        g.drawRect(50,20,200,15); g.drawRect(550,20,200,15);

        g.setColor(Color.CYAN);
        g.fillRect(50,38, 200-p1.cool*200/300,8);
        g.fillRect(550,38,200-p2.cool*200/300,8);
        g.setColor(Color.WHITE);
        g.drawRect(50,38,200,8); g.drawRect(550,38,200,8);
    }
    private void drawWinner(Graphics g){
        g.setColor(Color.YELLOW); g.setFont(new Font("Arial",Font.BOLD,32));
        g.drawString(winner+" Wins!", 300,220);
        g.setFont(new Font("Arial",Font.PLAIN,18));
        g.drawString("Press R to restart", 320,260);
    }

    @Override public void actionPerformed(ActionEvent e){ repaint(); }

        @Override public void keyPressed(KeyEvent e){
        int c = e.getKeyCode();

        if(running && c==KeyEvent.VK_ESCAPE){ menu=!menu; rebinding=false; return; }
        if(menu){
            if(rebinding){ settings.setKey(rebindingKey,c); settings.save(); applyKey(rebindingKey,c); rebinding=false; }
            return;
        }
        if(!running){
            if(!lock1){
                if(c==KeyEvent.VK_A) sel1=(sel1+roles.length-1)%roles.length;
                if(c==KeyEvent.VK_D) sel1=(sel1+1)%roles.length;
                if(c==KeyEvent.VK_F) lock1=true;
            }
            if(!lock2){
                if(c==KeyEvent.VK_LEFT)  sel2=(sel2+roles.length-1)%roles.length;
                if(c==KeyEvent.VK_RIGHT) sel2=(sel2+1)%roles.length;
                if(c==KeyEvent.VK_ENTER) lock2=true;
            }
            if(lock1 && lock2) startGame();
            return;
        }
        if(!winner.isEmpty()){ if(c==KeyEvent.VK_R) reset(); return; }

        // 只針對各自控制鍵做處理，避免同時控制
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

        if(c==p1.skill && p1.cool==0){ skills.add(new Skill(p1)); p1.cool=300; }
        if(c==p2.skill && p2.cool==0){ skills.add(new Skill(p2)); p2.cool=300; }
    }

    @Override public void keyReleased(KeyEvent e){
        int c = e.getKeyCode();
        if (c == p1.l || c == p1.r || c == p1.u || c == p1.d || c == p1.atk)
            p1.handle(c, false);
        if (c == p2.l || c == p2.r || c == p2.u || c == p2.d || c == p2.atk)
            p2.handle(c, false);
    }    @Override public void keyTyped(KeyEvent e){}

    private void buildBoxes(){
        int y=70;
        for(String k: new String[]{"P1_LEFT","P1_RIGHT","P1_UP","P1_DOWN","P1_ATTACK","P1_SKILL"}){
            boxes.put(k,new Rectangle(200,y,300,25)); y+=30;
        }
        y=280;
        for(String k: new String[]{"P2_LEFT","P2_RIGHT","P2_UP","P2_DOWN","P2_ATTACK","P2_SKILL"}){
            boxes.put(k,new Rectangle(200,y,300,25)); y+=30;
        }
    }
    private void startGame(){
        p1 = new Player(100,380, roles[sel1].color, keys("P1_"));
        p2 = new Player(660,380, roles[sel2].color, keys("P2_"));
        running = true;
    }
    private void reset(){ winner=""; running=false; lock1=lock2=false; skills.clear(); }
    private int[] keys(String pre){ return new int[]{ settings.k(pre+"LEFT"), settings.k(pre+"RIGHT"), settings.k(pre+"UP"), settings.k(pre+"DOWN"), settings.k(pre+"ATTACK"), settings.k(pre+"SKILL") }; }
    private void applyKey(String k,int code){ if(p1!=null) p1.apply(k,code); if(p2!=null) p2.apply(k,code); }

    private static String nice(int code){ return switch(code){ case KeyEvent.VK_LEFT->"←"; case KeyEvent.VK_RIGHT->"→"; case KeyEvent.VK_UP->"↑"; case KeyEvent.VK_DOWN->"↓"; default->KeyEvent.getKeyText(code);} ; }
    
    static class Player{
        int x,y,w=40,h=80,hp=100,cool=0;
        Color col; boolean faceR=true;
        int l,r,u,d,atk,skill; boolean L,R,U,D;
        boolean attack = false;
        int atkCool = 0;
        static final int GROUND_Y = 380; // 地面位置

        Player(int x,int y,Color c,int[] k){
            this.x=x; this.y=y; this.col=c;
            l=k[0]; r=k[1]; u=k[2]; d=k[3]; atk=k[4]; skill=k[5];
        }
        void move(){
            int s=5;
            if(L){ x-=s; faceR=false; }
            if(R){ x+=s; faceR=true;  }
            // 僅允許在地面行走，禁止上下移動
            y = GROUND_Y;
        }
        void draw(Graphics g){
            g.setColor(col); g.fillRect(x,y,w,h);
        }
        void handle(int code,boolean press){
            if(code==l) L=press;
            if(code==r) R=press;
            if(code==u) U=press; // 雖然設了 U，但實際上 move() 不會用
            if(code==d) D=press; // 同上
            if(code==atk) attack = press;
        }
        void apply(String key,int c){
            switch(key){
                case "P1_LEFT","P2_LEFT"   -> l=c;
                case "P1_RIGHT","P2_RIGHT" -> r=c;
                case "P1_UP","P2_UP"       -> u=c;
                case "P1_DOWN","P2_DOWN"   -> d=c;
                case "P1_ATTACK","P2_ATTACK"-> atk=c;
                case "P1_SKILL","P2_SKILL" -> skill=c;
            }
        }
        Rectangle bounds(){ return new Rectangle(x,y,w,h); }
    }
    static class Skill{
        int x,y,sz=16,speed=10; Player owner;
        Skill(Player p){
            owner=p;
            x = p.faceR ? p.x+p.w : p.x-sz;
            y = p.y + 30;
        }
        void update(){ x += owner.faceR ? speed : -speed; }
        void draw(Graphics g){ g.setColor(Color.CYAN); g.fillOval(x,y,sz,sz); }
        boolean hit(Player pl){ return bounds().intersects(pl.bounds()); }
        boolean outOf(int W){ return x<-sz || x>W+sz; }
        Rectangle bounds(){ return new Rectangle(x,y,sz,sz); }
    }
    static class SettingsManager{
        private final Properties p=new Properties();
        private final String path;
        SettingsManager(String path){
            this.path=path; load();
        }
        void load(){
            try(FileInputStream f=new FileInputStream(path)){ p.load(f); }
            catch(IOException e){ setDefault(); }
        }
        void setDefault(){
            p.setProperty("P1_LEFT","A");   p.setProperty("P1_RIGHT","D");
            p.setProperty("P1_UP","W");     p.setProperty("P1_DOWN","S");
            p.setProperty("P1_ATTACK","G"); p.setProperty("P1_SKILL","F");
            p.setProperty("P2_LEFT","LEFT");p.setProperty("P2_RIGHT","RIGHT");
            p.setProperty("P2_UP","UP");    p.setProperty("P2_DOWN","DOWN");
            p.setProperty("P2_ATTACK","ENTER"); p.setProperty("P2_SKILL","L");
            save();
        }
        void save(){
            try(FileOutputStream f=new FileOutputStream(path)){ p.store(f,"keybinds"); }
            catch(IOException ignored){}
        }
        int k(String key){
            String v=p.getProperty(key);
            return switch(v){
                case "LEFT"  -> KeyEvent.VK_LEFT;
                case "RIGHT" -> KeyEvent.VK_RIGHT;
                case "UP"    -> KeyEvent.VK_UP;
                case "DOWN"  -> KeyEvent.VK_DOWN;
                case "ENTER" -> KeyEvent.VK_ENTER;
                default      -> KeyEvent.getExtendedKeyCodeForChar(v.charAt(0));
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