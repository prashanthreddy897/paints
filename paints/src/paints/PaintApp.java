package paints;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.imageio.ImageIO;

public class PaintApp extends JFrame {
    private JPanel drawingPanel;
    private int startX, startY, endX, endY;
    private List<Shape> shapes;
    private Stack<Shape> undoStack;
    private Stack<Shape> redoStack;
    private Color currentColor;
    private int currentBrushSize;

    private ShapeType selectedShape;

    private boolean isSketchMode;

    public PaintApp() {
        setTitle("Paint Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create "File" menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newMenuItem = new JMenuItem("New");
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem saveMenuItem = new JMenuItem("Save");

        newMenuItem.addActionListener(e -> newDrawingPanel());
        openMenuItem.addActionListener(e -> loadDrawing());
        saveMenuItem.addActionListener(e -> saveDrawing());

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawShapes(g);
            }
        };

        // Set the background color of the drawing panel to white
        drawingPanel.setBackground(Color.WHITE);

        shapes = new ArrayList<>();
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        currentColor = Color.BLACK;
        currentBrushSize = 5;
        isSketchMode = false;

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endX = e.getX();
                endY = e.getY();
                if (isSketchMode) {
                    shapes.add(new Shape(startX, startY, endX, endY, currentColor, currentBrushSize, ShapeType.SKETCH));
                } else if (selectedShape == ShapeType.TEXT) {
                    addRandomText();
                } else {
                    shapes.add(new Shape(startX, startY, endX, endY, currentColor, currentBrushSize, selectedShape));
                }
                undoStack.push(shapes.get(shapes.size() - 1));
                redoStack.clear();
                repaint();
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isSketchMode) {
                    endX = e.getX();
                    endY = e.getY();
                    shapes.add(new Shape(startX, startY, endX, endY, currentColor, currentBrushSize, ShapeType.SKETCH));
                    startX = endX;
                    startY = endY;
                    repaint();
                }
            }
        });

        String[] shapeOptions = {"Line", "Rectangle", "Circle", "Ellipse", "Triangle", "Sketch", "Text"};
        JComboBox<String> shapeComboBox = new JComboBox<>(shapeOptions);
        shapeComboBox.addActionListener(e -> selectedShape(shapeComboBox.getSelectedIndex()));

        JButton colorButton = new JButton("Select Color");
        colorButton.addActionListener(e -> chooseColor());

        JSpinner brushSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        brushSizeSpinner.addChangeListener(e -> currentBrushSize = (int) brushSizeSpinner.getValue());

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> undo());

        JButton redoButton = new JButton("Redo");
        redoButton.addActionListener(e -> redo());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveDrawing());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(new JLabel("Select Shape:"));
        buttonPanel.add(shapeComboBox);
        buttonPanel.add(colorButton);
        buttonPanel.add(new JLabel("Brush Size:"));
        buttonPanel.add(brushSizeSpinner);
        buttonPanel.add(undoButton);
        buttonPanel.add(redoButton);
        buttonPanel.add(saveButton);

        setLayout(new BorderLayout());
        add(drawingPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.NORTH);
    }

    private void selectedShape(int shapeIndex) {
        isSketchMode = (shapeIndex == 5); // 5 is the index of "Sketch" in the combo box
        selectedShape = ShapeType.values()[shapeIndex];
    }

    private void chooseColor() {
        currentColor = JColorChooser.showDialog(this, "Select Color", currentColor);
    }

    private void drawShapes(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        for (Shape shape : shapes) {
            g2d.setColor(shape.getColor());
            g2d.setStroke(new BasicStroke(shape.getBrushSize()));

            int x1 = shape.getStartX();
            int y1 = shape.getStartY();
            int x2 = shape.getEndX();
            int y2 = shape.getEndY();

            switch (shape.getShapeType()) {
                case LINE:
                    g2d.drawLine(x1, y1, x2, y2);
                    break;
                case RECTANGLE:
                    g2d.drawRect(Math.min(x1, x2), Math.min(y1, y2),
                            Math.abs(x2 - x1), Math.abs(y2 - y1));
                    break;
                case CIRCLE:
                    g2d.drawOval(Math.min(x1, x2), Math.min(y1, y2),
                            Math.abs(x2 - x1), Math.abs(y2 - y1));
                    break;
                case ELLIPSE:
                    g2d.drawOval(Math.min(x1, x2), Math.min(y1, y2),
                            Math.abs(x2 - x1), Math.abs(y2 - y1));
                    break;
                case TRIANGLE:
                    int[] xPoints = {x1, x2, x1 - (x2 - x1)};
                    int[] yPoints = {y1, y2, y2};
                    g2d.drawPolygon(xPoints, yPoints, 3);
                    break;
                case SKETCH:
                    g2d.drawLine(x1, y1, x2, y2);
                    break;
                case TEXT:
                    g2d.setFont(new Font("Arial", Font.PLAIN, shape.getBrushSize() * 5));
                    g2d.drawString(shape.getText(), shape.getStartX(), shape.getStartY());
                    break;
            }
        }
    }

    private void newDrawingPanel() {
        shapes.clear();
        repaint();
    }

    private void loadDrawing() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Paint Files (*.png; *.jpg)", "png", "jpg"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage image = ImageIO.read(selectedFile);
                shapes.clear();
                shapes.add(new Shape(0, 0, image.getWidth(), image.getHeight(), Color.BLACK, 1, ShapeType.IMAGE));
                undoStack.push(shapes.get(0));
                redoStack.clear();
                repaint();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveDrawing() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JPG Image (*.jpg)", "jpg"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String extension = ((FileNameExtensionFilter) fileChooser.getFileFilter()).getExtensions()[0];

            try {
                BufferedImage image = new BufferedImage(drawingPanel.getWidth(), drawingPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, drawingPanel.getWidth(), drawingPanel.getHeight());
                drawShapes(g2d);
                g2d.dispose();

                ImageIO.write(image, extension, selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addRandomText() {
        String randomText = JOptionPane.showInputDialog(this, "Enter random text:");
        if (randomText != null && !randomText.isEmpty()) {
            shapes.add(new Shape(startX, startY, currentColor, currentBrushSize, ShapeType.TEXT, randomText));
            undoStack.push(shapes.get(shapes.size() - 1));
            redoStack.clear();
            repaint();
        }
    }

    private void undo() {
        if (!shapes.isEmpty()) {
            redoStack.push(shapes.remove(shapes.size() - 1));
            repaint();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            shapes.add(redoStack.pop());
            repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PaintApp paintApp = new PaintApp();
            paintApp.setVisible(true);
        });
    }
}

class Shape {
    private int startX, startY, endX, endY, brushSize;
    private Color color;
    private ShapeType shapeType;
    private String text;

    public Shape(int startX, int startY, int endX, int endY, Color color, int brushSize, ShapeType shapeType) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = color;
        this.brushSize = brushSize;
        this.shapeType = shapeType;
    }

    public Shape(int startX, int startY, Color color, int brushSize, ShapeType shapeType, String text) {
        this.startX = startX;
        this.startY = startY;
        this.color = color;
        this.brushSize = brushSize;
        this.shapeType = shapeType;
        this.text = text;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public Color getColor() {
        return color;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public ShapeType getShapeType() {
        return shapeType;
    }

    public String getText() {
        return text;
    }
}

enum ShapeType {
    LINE, RECTANGLE, CIRCLE, ELLIPSE, TRIANGLE, SKETCH, TEXT, IMAGE
}

