import java.awt.*;

import javax.swing.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

import java.awt.event.*;
import java.io.File;

public class GameClient implements KeyListener, MouseMotionListener {

    private static final float DAMPING = 0.1f;

    private static final float SPEED = 3f;

    private static final float MAXSPEED = 4f;

    float mouseXRatio = 0;
    float mouseYRatio = 0;

    private SimpleVector moveRes = new SimpleVector(0, 0, 0);

    private SimpleVector ellipsoid = new SimpleVector(5, 15, 5);

    private JFrame pauseFrame, gameFrame;
    JPanel panel1, panel2, panel3, panel4;
    JTextField messageField;
    JTextArea serverLog, messageArea;

    Object3D player = null;
    Object3D[] map;
    World world;
    FrameBuffer buffer;
    GraphicsDevice device;
    Canvas canvas;
    boolean gameLoop;
    Thread gameThread;

    boolean up = false;
    boolean down = false;
    boolean left = false;
    boolean right = false;

    KeyMapper keyMapper;

    int mouseX = 0;
    int mouseY = 0;

    public GameClient() {

        Font helloHeadline = new Font("", Font.PLAIN, 0);
        try {
            helloHeadline = Font.createFont(Font.TRUETYPE_FONT, new File("resources/HelloHeadline.ttf"))
                    .deriveFont(36f);
        } catch (Exception e) {
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(helloHeadline);

        pauseFrame = new JFrame("Campus Camouflage Paused");
        pauseFrame.setLayout(new GridLayout(1, 2));
        pauseFrame.addKeyListener(this);

        pauseFrame.setUndecorated(true);
        pauseFrame.setResizable(false);

        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        pauseFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pauseFrame.setVisible(true);

        // // Enter full-screen mode
        device.setFullScreenWindow(pauseFrame);

        pauseFrame.setSize(device.getFullScreenWindow().getWidth(), device.getFullScreenWindow().getHeight());

        gameFrame = new JFrame("Campus Camouflage");

        gameFrame.setUndecorated(true);
        gameFrame.setResizable(false);

        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setVisible(true);
        gameFrame.setSize(device.getFullScreenWindow().getWidth(), device.getFullScreenWindow().getHeight());

        int maxWidth = 800;
        int maxHeight = 600;
        for (VideoMode vMode : FrameBuffer.getVideoModes(IRenderer.RENDERER_OPENGL)) {

            if (maxWidth < vMode.width) {
                maxWidth = vMode.width;
                maxHeight = vMode.height;
                System.out.println("" + vMode.width + ", " + vMode.height);
            }

        }

        mouseXRatio = maxWidth / device.getFullScreenWindow().getWidth();
        mouseYRatio = maxHeight / device.getFullScreenWindow().getHeight();

        buffer = new FrameBuffer(maxWidth, maxHeight, FrameBuffer.SAMPLINGMODE_HARDWARE_ONLY);
        buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
        canvas = buffer.enableGLCanvasRenderer();

        gameFrame.add(canvas, BorderLayout.CENTER);
        canvas.requestFocus();
        canvas.addKeyListener(this);
        canvas.addMouseMotionListener(this);
        keyMapper = new KeyMapper(canvas);
        gameLoop = true;

        // TODO improve pauseFrame
        init();

        serverLog = new JTextArea();
        serverLog.setText("      ");
        serverLog.setAutoscrolls(true);
        serverLog.setFont(helloHeadline);
        serverLog.setEditable(false);

        panel1 = new JPanel(new GridLayout(2, 1));

        panel4 = new JPanel(new GridLayout(2, 1));
        JButton resumeButton = new JButton("Resume");
        resumeButton.addActionListener(e -> {
            device.setFullScreenWindow(gameFrame);
            gameFrame.setSize(device.getFullScreenWindow().getWidth(),
                    device.getFullScreenWindow().getHeight());
        });
        JButton closeServer = new JButton("Leave");
        closeServer.addActionListener(e -> {
            if (e.getActionCommand().equals("Leave")) {
                closeServer.setText("Are you Sure?");
                int delay = 0750; // 3 seconds
                Timer timer = new Timer(delay, ae -> {
                    closeServer.setText("Leave");
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                gameLoop = false;
                gameFrame.dispose();
                pauseFrame.dispose();
                new Game();
            }
        });

        panel4.add(resumeButton);
        panel4.add(closeServer);
        panel1.add(panel4);

        pauseFrame.add(serverLog);
        pauseFrame.add(panel1);

        gameThread = new Thread(new GameLoop());
        gameThread.start();
    }

    // TODO Test For mouse listener, remove afterwards

    Object3D mouseCube = Primitives.getPyramide(2f);

    private void init() {
        world = new World();
        mouseCube.build();
        world.addObject(mouseCube);
        loadMap("testMap");
    }

    private void gameLoop() {
        while (gameLoop) {

            mouseCube.clearTranslation();
            mouseCube.translate(Functions.getMouseWorldPosition(buffer, world, mouseX, mouseY));
            moveCamera();
            world.getCamera().align(player);
            world.getCamera().setPosition(player.getTransformedCenter());
            world.getCamera().moveCamera(Camera.CAMERA_MOVEOUT, 100f);
            world.getCamera().moveCamera(Camera.CAMERA_MOVEUP, 20f);
            buffer.clear(java.awt.Color.ORANGE);
            world.renderScene(buffer);
            world.draw(buffer);
            buffer.update();
            buffer.display(canvas.getGraphics());
            canvas.repaint();
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                
            }
        }
    }

    public class Init implements Runnable {

        @Override
        public void run() {
            init();
        }
    }

    public class GameLoop implements Runnable {

        @Override
        public void run() {
            gameLoop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 'p':
                if (!device.getFullScreenWindow().toString().contains("Paused")) {
                    device.setFullScreenWindow(null);
                    device.setFullScreenWindow(pauseFrame);

                    pauseFrame.setSize(device.getFullScreenWindow().getWidth(),
                            device.getFullScreenWindow().getHeight());
                } else {
                    device.setFullScreenWindow(gameFrame);
                    gameFrame.setSize(device.getFullScreenWindow().getWidth(),
                            device.getFullScreenWindow().getHeight());
                }
                break;
            default:
                break;
        }
    }

    public void loadMap(String mapName) {
        // TODO Add props to props array

        for (int i = 0; i < Functions.texturesJPG.length; ++i) {

            TextureManager.getInstance().addTexture(Functions.texturesJPG[i] + ".jpg",
                    new Texture("assets/textures/" + Functions.texturesJPG[i] + ".jpg"));
        }

        for (int i = 0; i < Functions.texturesPNG.length; ++i) {
            TextureManager.getInstance().addTexture(Functions.texturesPNG[i] + ".png",
                    new Texture("assets/textures/" + Functions.texturesPNG[i] + ".png"));
        }

        map = Loader.load3DS("assets/map/" + mapName + ".3ds", 1f);

        for (Object3D object3d : map) {
            object3d.setCenter(SimpleVector.ORIGIN);
            object3d.rotateX((float) -Math.PI / 2);
            object3d.rotateMesh();
            object3d.setRotationMatrix(new Matrix());
            if (object3d.getName().contains("cop")) {
                System.out.println("done");
                player = object3d;
                player.setCollisionMode(Object3D.COLLISION_CHECK_SELF);
                world.addObject(player);
                Camera cam = world.getCamera();
                cam.moveCamera(Camera.CAMERA_MOVEOUT, 100);
                cam.lookAt(player.getTransformedCenter());
                cam.setFovAngle((float) (Math.PI * 120 / 180));
                player.build();
            } else if (object3d.getName().contains("light")) {
                Light light = new Light(world);
                light.setIntensity(140, 120, 120);
                light.setAttenuation(-1);
                light.setPosition(object3d.getTransformedCenter());
            } else {
                object3d.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
                world.addObject(object3d);
            }
            object3d.build();
        }

        world.setAmbientLight(20, 20, 20);
        world.buildAllObjects();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = (int) (e.getX() * mouseXRatio);
        mouseY = (int) (e.getY() * mouseYRatio);
    }

    public void moveCamera() {
        KeyState ks = null;
        while ((ks = keyMapper.poll()) != KeyState.NONE) {
            if (ks.getKeyCode() == KeyEvent.VK_W) {
                up = ks.getState();
            }
            if (ks.getKeyCode() == KeyEvent.VK_S) {
                down = ks.getState();
            }
            if (ks.getKeyCode() == KeyEvent.VK_A) {
                left = ks.getState();
            }
            if (ks.getKeyCode() == KeyEvent.VK_D) {
                right = ks.getState();
            }

            if (ks.getKeyCode() == KeyEvent.VK_ESCAPE) {
                gameLoop = false;
            }
        }

        // move the cube
        if (up) {
            SimpleVector t = player.getZAxis();
            t.scalarMul(SPEED);
            moveRes.add(t);
        }

        if (down) {
            SimpleVector t = player.getZAxis();
            t.scalarMul(-SPEED);
            moveRes.add(t);
        }

        if (left) {
            player.rotateY((float) Math.toRadians(-1));
        }

        if (right) {
            player.rotateY((float) Math.toRadians(1));
        }

        // avoid high speeds
        if (moveRes.length() > MAXSPEED) {
            moveRes.makeEqualLength(new SimpleVector(0, 0, MAXSPEED));
        }

        player.translate(0, -0.02f, 0);

        moveRes = player.checkForCollisionEllipsoid(moveRes, ellipsoid, 8);
        player.translate(moveRes);

        // finally apply the gravity:
        SimpleVector t = new SimpleVector(0, 1, 0);
        t = player.checkForCollisionEllipsoid(t, ellipsoid, 1);
        player.translate(t);

        // damping
        if (moveRes.length() > DAMPING) {
            moveRes.makeEqualLength(new SimpleVector(0, 0, DAMPING));
        } else {
            moveRes = new SimpleVector(0, 0, 0);
        }
    }

}