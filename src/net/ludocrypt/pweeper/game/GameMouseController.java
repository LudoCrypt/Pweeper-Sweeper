package net.ludocrypt.pweeper.game;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import net.ludocrypt.pweeper.PweeperMain;
import net.ludocrypt.pweeper.game.GameState.Int2;

public class GameMouseController extends MouseAdapter {

    private static final int GRACE_PERIOD_MS = 50;

    private GameState gameState;
    protected JPanel drawPanel;
    private boolean realGame;

    private Int2 gameOffset;
    private Int2 clickRatio;

    private long leftPressTime = 0;
    private long rightPressTime = 0;

    private boolean clicked = false;
    private boolean doubleClick = false;
    private boolean around = false;

    private boolean middleClick = false;
    private double lastX, lastY;

    public GameMouseController(GameState gameState, JPanel drawPanel, boolean realGame, Int2 gameOffset, Int2 clickRatio) {
        this.gameState = gameState;
        this.drawPanel = drawPanel;
        this.realGame = realGame;
        this.gameOffset = gameOffset;
        this.clickRatio = clickRatio;
        this.gameState.controller = this;
    }

    public GameMouseController(GameState gameState, JPanel drawPanel) {
        this(gameState, drawPanel, true, new Int2(10, 70), new Int2(0, 0));
    }

    @Override
    public void mousePressed(MouseEvent e) {

        long now = System.currentTimeMillis();

        if (SwingUtilities.isLeftMouseButton(e)) {
            leftPressTime = now;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            rightPressTime = now;
        }

        if (GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL)) {
            if (Math.abs(leftPressTime - rightPressTime) <= GRACE_PERIOD_MS) {
                doubleClick = true;
            }
        }

        if ((SwingUtilities.isLeftMouseButton(e) && (GamePermissions.hasPermission(gameState, GamePermissions.REVEAL) || GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL))) || (SwingUtilities.isRightMouseButton(e) && GamePermissions.hasPermission(gameState, GamePermissions.FLAG)) || (SwingUtilities.isMiddleMouseButton(e) && GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL))) {
            clicked = true;
            gameState.setClicked(clicked);
        }

        mouseDragged(e);

        Int2 pos = getCell(new Int2(e.getX(), e.getY()));
        int x = pos.x();
        int y = pos.y();

        if (gameState.isInBounds(x, y)) {
            if (SwingUtilities.isMiddleMouseButton(e) || doubleClick) {
                if (GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL)) {
                    gameState.setAround(true);
                    around = true;
                }
            } else if (SwingUtilities.isRightMouseButton(e)) {
                if (GamePermissions.hasPermission(gameState, GamePermissions.FLAG)) {
                    gameState.toggleFlag(x, y);
                    clicked = false;
                    gameState.setClicked(clicked);
                    mouseDragged(e);
                }
            }
        }

        if (SwingUtilities.isMiddleMouseButton(e)) {
            middleClick = true;
            lastX = ((double) e.getX() / (double) getScalingRatio().x());
            lastY = ((double) e.getY() / (double) getScalingRatio().y());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (clicked) {
            mouseMoved(e);
        }

        if (middleClick) {
            double dx = ((double) e.getX() / (double) getScalingRatio().x()) - lastX;
            double dy = ((double) e.getY() / (double) getScalingRatio().y()) - lastY;
            PweeperMain.viewport.setMouse(dx, dy);

            if (dx * dx + dy * dy > 0.00001) {
                clicked = false;
                around = false;
                doubleClick = false;
                gameState.setClicked(clicked);
                gameState.setAround(false);
                drawPanel.repaint();
            }
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (SwingUtilities.isLeftMouseButton(e)) {
            leftPressTime = 0;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            rightPressTime = 0;
        }

        Int2 pos = getCell(new Int2(e.getX(), e.getY()));
        int x = pos.x();
        int y = pos.y();

        if (gameState.isInBounds(x, y)) {
            if (SwingUtilities.isLeftMouseButton(e) && !doubleClick) {
                if (GamePermissions.hasPermission(gameState, GamePermissions.REVEAL)) {
                    gameState.reveal(x, y);
                }
            } else if ((SwingUtilities.isMiddleMouseButton(e) && around) || doubleClick) {
                if (GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL)) {
                    if (gameState.canAutoReveal(x, y)) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (!(dx == 0 && dy == 0)) {
                                    if (!gameState.isFlagged(x + dx, y + dy)) {
                                        gameState.reveal(x + dx, y + dy);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            pos = getCell(new Int2(e.getX(), e.getY()), new Int2(10 + (gameState.getWidth() % 2 == 0 ? 0 : 10), 75));
            x = pos.x();
            y = pos.y();

            if (GamePermissions.hasPermission(gameState, GamePermissions.RESTART)) {
                if ((x == Math.floor(gameState.getWidth() / 2.0) - 1 || x == Math.floor(gameState.getWidth() / 2.0)) && (y == -2 || y == -3)) {
                    gameState = new GameState(gameState.getWidth(), gameState.getHeight(), gameState.getMines(), gameState.getPortals(), gameState.getPermissions());
                    gameState.controller = this;
                    PweeperMain.gameState = gameState;
                    drawPanel.repaint();
                }
            }
        }

        clicked = false;

        if (leftPressTime == 0 && rightPressTime == 0) {
            doubleClick = false;
        }

        if (SwingUtilities.isMiddleMouseButton(e)) {
            middleClick = false;
            PweeperMain.viewport.pushMouse();
        }

        around = false;
        gameState.setClicked(clicked);
        gameState.setAround(false);
        drawPanel.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Int2 pos = getCell(new Int2(e.getX(), e.getY()));
        int x = pos.x();
        int y = pos.y();

        if (GamePermissions.hasPermission(gameState, GamePermissions.RESTART)) {
            if (!gameState.isInBounds(x, y)) {
                pos = getCell(new Int2(e.getX(), e.getY()), new Int2(10 + (gameState.getWidth() % 2 == 0 ? 0 : 10), 75));
                x = pos.x();
                y = pos.y();
            }
        }

//        if (x != gameState.getMouseX() || y != gameState.getMouseY()) {
//            if (gameState.isInBounds(x, y)) {
//                if (clicked) {
//                    PweeperMain.playSound("click");
//                }
//            }
//        }

        gameState.setMouse(x, y);

        gameState.setClicked(clicked);
        drawPanel.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Int2 pos = getCell(new Int2(e.getX(), e.getY()));
        int x = pos.x();
        int y = pos.y();

        boolean rolled = false;
        if (GamePermissions.hasPermission(gameState, GamePermissions.MARK)) {
            if (!SwingUtilities.isMiddleMouseButton(e)) {
                if (gameState.isInBounds(x, y)) {
                    if (e.getUnitsToScroll() < 0) {
                        rolled = gameState.increaseMark(x, y);
                    } else {
                        rolled = gameState.decreaseMark(x, y);
                    }
                }
            }
        }

        if (!rolled) {
            if (PweeperMain.viewport != null) {
                PweeperMain.viewport.pushMouse();

                lastX = ((double) e.getX() / (double) getScalingRatio().x());
                lastY = ((double) e.getY() / (double) getScalingRatio().y());

                PweeperMain.viewport.zoom(lastX, lastY, 1.1, (int) (e.getUnitsToScroll() / 2.0));
            }
        }

        drawPanel.repaint();
    }

    public Int2 getCell(Int2 p) {
        return getCell(p, gameOffset);
    }

    public Int2 getCell(Int2 p, Int2 o) {
        Vector2f worldSpace = getWorldSpace(p, o);

        int x = (int) Math.floor(worldSpace.x / 20.0);
        int y = (int) Math.floor(worldSpace.y / 20.0);

        return new Int2(x, y);
    }

    public Vector2f getWorldSpace(Int2 p) {
        return getWorldSpace(p, new Int2(0, 0));
    }

    public Vector2f getWorldSpace(Int2 p, Int2 o) {
        Matrix4f mat = PweeperMain.viewport.composeMat();

        Vector3f scale = new Vector3f();
        scale = mat.getScale(scale);

        Vector3f translation = new Vector3f();
        translation = mat.getTranslation(translation);

        return new Vector2f((((p.x() - translation.x * getScalingRatio().x()) / scale.x) - o.x()), (((p.y() - translation.y * getScalingRatio().y()) / scale.y) - o.y()));
    }

    public Int2 getGameOffset() {
        return gameOffset;
    }

    public Int2 getClickRatio() {
        return clickRatio;
    }

    public Int2 getScalingRatio() {
        int panelWidth = realGame ? drawPanel.getWidth() : clickRatio.x();
        int panelHeight = realGame ? drawPanel.getHeight() : clickRatio.y();

        return new Int2(panelWidth, panelHeight);
    }

}
