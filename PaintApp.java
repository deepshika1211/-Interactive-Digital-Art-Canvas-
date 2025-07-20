import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*; // For File
// This is my Paint App
public class PaintApp extends JPanel implements MouseListener, MouseMotionListener {
    // These are the tools and brush types
    enum Tool { PEN, ERASER, TEXT, RECT, OVAL, LINE, BUCKET }
    enum BrushType { ROUND, SQUARE, DASHED, DOUBLE }

    // Some variables for tool, brush, size, color
    Tool tool = Tool.PEN;
    BrushType brush = BrushType.ROUND;
    int size = 4;
    Color penColor = Color.BLACK;

    // Stacks for undo and redo, and list for all strokes
    Stack<ArrayList<MyStroke>> undo = new Stack<>();
    Stack<ArrayList<MyStroke>> redo = new Stack<>();
    ArrayList<MyStroke> allStrokes = new ArrayList<>();
    MyStroke tempStroke = null;
    boolean isDrawing = false;

    // Constructor for the PaintApp
    public PaintApp() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    // Setters for tool, brush, size, color
    public void setTool(Tool t) { tool = t; }
    public void setBrush(BrushType b) { brush = b; }
    public void setSize(int s) { size = s; }
    public void setPenColor(Color c) { penColor = c; }

    // This clears everything
    public void clearAll() {
        if (!allStrokes.isEmpty()) {
            undo.push(new ArrayList<>(allStrokes));
            allStrokes.clear();
            redo.clear();
            repaint();
        }
    }

    // This does undo
    public void undoDraw() {
        if (!allStrokes.isEmpty()) {
            undo.push(new ArrayList<>(allStrokes));
            allStrokes.remove(allStrokes.size() - 1);
            redo.clear();
            repaint();
        }
    }

    // This does redo
    public void redoDraw() {
        if (!undo.isEmpty()) {
            allStrokes = new ArrayList<>(undo.pop());
            repaint();
        }
    }

    // This draws everything on the panel
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        for (MyStroke s : allStrokes) s.draw((Graphics2D) g);
        if (tempStroke != null) tempStroke.draw((Graphics2D) g);
    }

    // When mouse is pressed
    public void mousePressed(MouseEvent e) {
        if (tool == Tool.BUCKET) {
            // For bucket tool (fill)
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            super.paintComponent(g2);
            for (MyStroke s : allStrokes) s.draw(g2);
            g2.dispose();
            Color oldColor = new Color(img.getRGB(e.getX(), e.getY()));
            fillColor(e.getX(), e.getY(), oldColor, penColor);
            return;
        }
        if (tool == Tool.TEXT) {
            // For text tool
            String txt = JOptionPane.showInputDialog(this, "Enter text:");
            if (txt != null) {
                MyStroke s = new MyStroke(Tool.TEXT, size, penColor, brush);
                s.addPoint(e.getX(), e.getY(), txt);
                allStrokes.add(s);
                undo.push(new ArrayList<>(allStrokes));
                redo.clear();
                repaint();
            }
        } else if (tool == Tool.PEN || tool == Tool.ERASER) {
            // For pen and eraser
            Color c = tool == Tool.ERASER ? Color.WHITE : penColor;
            tempStroke = new MyStroke(tool, size, c, brush);
            tempStroke.addPoint(e.getX(), e.getY());
            isDrawing = true;
        } else if (tool == Tool.RECT || tool == Tool.OVAL || tool == Tool.LINE) {
            // For shapes
            tempStroke = new MyStroke(tool, size, penColor, brush);
            tempStroke.addPoint(e.getX(), e.getY());
            tempStroke.addPoint(e.getX(), e.getY());
            isDrawing = true;
        }
    }

    // When mouse is released
    public void mouseReleased(MouseEvent e) {
        if (isDrawing && tempStroke != null) {
            if (tool == Tool.RECT || tool == Tool.OVAL || tool == Tool.LINE) {
                tempStroke.points.set(1, new Point(e.getX(), e.getY()));
            } else {
                tempStroke.addPoint(e.getX(), e.getY());
            }
            allStrokes.add(tempStroke);
            undo.push(new ArrayList<>(allStrokes));
            redo.clear();
            tempStroke = null;
            isDrawing = false;
            repaint();
        }
    }

    // When mouse is dragged
    public void mouseDragged(MouseEvent e) {
        if (!isDrawing || tool == Tool.TEXT) return;
        if (tool == Tool.PEN || tool == Tool.ERASER) {
            tempStroke.addPoint(e.getX(), e.getY());
        } else if (tool == Tool.RECT || tool == Tool.OVAL || tool == Tool.LINE) {
            tempStroke.points.set(1, new Point(e.getX(), e.getY()));
        }
        repaint();
    }
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    // This is for bucket fill tool
    private void fillColor(int x, int y, Color oldColor, Color newColor) {
        if (oldColor.equals(newColor)) return;
        BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        super.paintComponent(g2);
        for (MyStroke s : allStrokes) s.draw(g2);
        g2.dispose();

        int w = img.getWidth(), h = img.getHeight();
        int oldRGB = img.getRGB(x, y), newRGB = newColor.getRGB();
        if (oldRGB == newRGB) return;

        Queue<Point> q = new LinkedList<>();
        q.add(new Point(x, y));
        while (!q.isEmpty()) {
            Point p = q.remove();
            int px = p.x, py = p.y;
            if (px < 0 || px >= w || py < 0 || py >= h) continue;
            if (img.getRGB(px, py) != oldRGB) continue;
            img.setRGB(px, py, newRGB);
            q.add(new Point(px + 1, py));
            q.add(new Point(px - 1, py));
            q.add(new Point(px, py + 1));
            q.add(new Point(px, py - 1));
        }
        allStrokes.add(new MyImageStroke(img));
        undo.push(new ArrayList<>(allStrokes));
        redo.clear();
        repaint();
    }

    // This is for filled area (bucket)
    static class MyImageStroke extends MyStroke {
        BufferedImage img;
        MyImageStroke(BufferedImage img) {
            super(Tool.BUCKET, 1, Color.BLACK, BrushType.ROUND);
            this.img = img;
        }
        @Override
        void draw(Graphics2D g2d) {
            g2d.drawImage(img, 0, 0, null);
        }
    }

    // This is for pen, eraser, shapes, text
    static class MyStroke {
        Tool tool;
        int width;
        Color color;
        BrushType brush;
        ArrayList<Point> points = new ArrayList<>();
        String text = null;

        MyStroke(Tool tool, int width, Color color, BrushType brush) {
            this.tool = tool; this.width = width; this.color = color; this.brush = brush;
        }
        void addPoint(int x, int y) { points.add(new Point(x, y)); }
        void addPoint(int x, int y, String text) { points.add(new Point(x, y)); this.text = text; }
        void draw(Graphics2D g2d) {
            MyStrokeStyle.setStyle(g2d, brush, width);
            g2d.setColor(color);
            if (tool == Tool.TEXT && text != null && !points.isEmpty()) {
                Point p = points.get(0);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                g2d.drawString(text, p.x, p.y);
            } else if ((tool == Tool.RECT || tool == Tool.OVAL || tool == Tool.LINE) && points.size() >= 2) {
                Point p1 = points.get(0), p2 = points.get(1);
                int x = Math.min(p1.x, p2.x), y = Math.min(p1.y, p2.y);
                int w = Math.abs(p1.x - p2.x), h = Math.abs(p1.y - p2.y);
                if (tool == Tool.RECT) g2d.drawRect(x, y, w, h);
                else if (tool == Tool.OVAL) g2d.drawOval(x, y, w, h);
                else if (tool == Tool.LINE) g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            } else if (points.size() > 1) {
                for (int i = 1; i < points.size(); i++) {
                    Point p1 = points.get(i - 1), p2 = points.get(i);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    if (brush == BrushType.SQUARE)
                        g2d.fillRect(p2.x - width / 2, p2.y - width / 2, width, width);
                    else if (brush == BrushType.DOUBLE)
                        g2d.drawLine(p1.x + width, p1.y + width, p2.x + width, p2.y + width);
                }
            }
        }
    }

    // This sets the brush style
    static class MyStrokeStyle {
        static void setStyle(Graphics2D g2d, BrushType brush, int width) {
            switch (brush) {
                case ROUND:
                    g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    break;
                case SQUARE:
                    g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                    break;
                case DASHED:
                    float[] dash = {10.0f, 10.0f};
                    g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
                    break;
                case DOUBLE:
                    g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    break;
            }
        }
    }

    // This highlights the selected tool button
    static void highlight(JButton selected, ArrayList<JButton> buttons) {
        for (JButton btn : buttons) btn.setBackground(null);
        selected.setBackground(Color.LIGHT_GRAY);
    }

    // This saves the drawing as an image
    public void saveImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Image");
        int userSelection = chooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                for (MyStroke s : allStrokes) s.draw(g2);
                g2.dispose();
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                javax.imageio.ImageIO.write(img, "png", file);
                JOptionPane.showMessageDialog(this, "Image saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage());
            }
        }
    }

    // This is the main function
    public static void main(String[] args) {
        JFrame win = new JFrame("PaintApp");
        PaintApp app = new PaintApp();
        JPanel top = new JPanel();

        // Tool buttons
        JButton penBtn = new JButton("Pen");
        JButton eraserBtn = new JButton("Eraser");
        JButton textBtn = new JButton("Text");
        JButton rectBtn = new JButton("Rect");
        JButton ovalBtn = new JButton("Oval");
        JButton lineBtn = new JButton("Line");
        JButton bucketBtn = new JButton("Bucket");

        ArrayList<JButton> toolBtns = new ArrayList<>(Arrays.asList(
            penBtn, eraserBtn, textBtn, rectBtn, ovalBtn, lineBtn, bucketBtn
        ));

        // Pen brush menu
        JPopupMenu penMenu = new JPopupMenu();
        JMenuItem roundBrush = new JMenuItem("Round");
        roundBrush.addActionListener(e -> { app.setTool(Tool.PEN); app.setBrush(BrushType.ROUND); highlight(penBtn, toolBtns); });
        JMenuItem squareBrush = new JMenuItem("Square");
        squareBrush.addActionListener(e -> { app.setTool(Tool.PEN); app.setBrush(BrushType.SQUARE); highlight(penBtn, toolBtns); });
        JMenuItem dashedBrush = new JMenuItem("Dashed");
        dashedBrush.addActionListener(e -> { app.setTool(Tool.PEN); app.setBrush(BrushType.DASHED); highlight(penBtn, toolBtns); });
        JMenuItem doubleBrush = new JMenuItem("Double");
        doubleBrush.addActionListener(e -> { app.setTool(Tool.PEN); app.setBrush(BrushType.DOUBLE); highlight(penBtn, toolBtns); });
        penMenu.add(roundBrush); penMenu.add(squareBrush); penMenu.add(dashedBrush); penMenu.add(doubleBrush);

        penBtn.addActionListener(e -> penMenu.show(penBtn, penBtn.getWidth()/2, penBtn.getHeight()/2));
        eraserBtn.addActionListener(e -> { app.setTool(Tool.ERASER); highlight(eraserBtn, toolBtns); });
        textBtn.addActionListener(e -> { app.setTool(Tool.TEXT); highlight(textBtn, toolBtns); });
        rectBtn.addActionListener(e -> { app.setTool(Tool.RECT); highlight(rectBtn, toolBtns); });
        ovalBtn.addActionListener(e -> { app.setTool(Tool.OVAL); highlight(ovalBtn, toolBtns); });
        lineBtn.addActionListener(e -> { app.setTool(Tool.LINE); highlight(lineBtn, toolBtns); });
        bucketBtn.addActionListener(e -> { app.setTool(Tool.BUCKET); highlight(bucketBtn, toolBtns); });

        // Color buttons
        JPanel colorPanel = new JPanel();
        Color[] colorArr = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.PINK};
        for (Color c : colorArr) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(c);
            colorBtn.setPreferredSize(new Dimension(20, 20));
            colorBtn.addActionListener(e -> app.setPenColor(c));
            colorPanel.add(colorBtn);
        }
         JButton customColorBtn = new JButton("Custom Color");
         customColorBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(app, "Choose Color", app.penColor);
            if (chosen != null) app.setPenColor(chosen);
        });

        // Size box
        Integer[] sizes = {2, 4, 8, 16};
        JComboBox<Integer> sizeBox = new JComboBox<>(sizes);
        sizeBox.setSelectedItem(4);
        sizeBox.addActionListener(e -> app.setSize((Integer) sizeBox.getSelectedItem()));

        // Undo, redo, clear buttons
        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> app.undoDraw());
        JButton redoBtn = new JButton("Redo");
        redoBtn.addActionListener(e -> app.redoDraw());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> app.clearAll());

        // Save button
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> app.saveImage());

        // Add everything to the top panel
        top.add(penBtn); top.add(eraserBtn); top.add(textBtn);
        top.add(rectBtn); top.add(ovalBtn); top.add(lineBtn); top.add(bucketBtn);
        top.add(colorPanel); top.add(customColorBtn);
        top.add(new JLabel("Stroke:")); top.add(sizeBox);
        top.add(undoBtn); top.add(redoBtn); top.add(clearBtn);
        top.add(saveBtn);

        // Add panels to the window
        win.setLayout(new BorderLayout());
        win.add(top, BorderLayout.NORTH);
        win.add(app, BorderLayout.CENTER);
        win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        win.pack();
        win.setVisible(true);
    }
}