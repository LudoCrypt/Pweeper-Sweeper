package net.ludocrypt.pweeper.game;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.ludocrypt.pweeper.PweeperMain;
import net.ludocrypt.pweeper.game.GameState.Int2;

public class GameMouseController extends MouseAdapter {

    private static final int GRACE_PERIOD_MS = 50;

    private GameState gameState;
    private JPanel drawPanel;
    private boolean realGame;

    private Int2 gameOffset;
    private Int2 ratioOffset;
    private Int2 clickRatio;

    private long leftPressTime = 0;
    private long rightPressTime = 0;

    private boolean clicked = false;
    private boolean doubleClick = false;

    public GameMouseController(GameState gameState, JPanel drawPanel, boolean realGame, Int2 gameOffset, Int2 gameRatio, Int2 clickRatio) {
        this.gameState = gameState;
        this.drawPanel = drawPanel;
        this.realGame = realGame;
        this.gameOffset = gameOffset;
        this.ratioOffset = gameRatio;
        this.clickRatio = clickRatio;
    }

    public GameMouseController(GameState gameState, JPanel drawPanel) {
        this(gameState, drawPanel, true, new Int2(10, 70), new Int2(20, 80), new Int2(0, 0));
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
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (clicked) {
            mouseMoved(e);
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
            } else if (SwingUtilities.isMiddleMouseButton(e) || doubleClick) {
                if (GamePermissions.hasPermission(gameState, GamePermissions.AUTO_REVEAL)) {
                    if (gameState.isRevealed(x, y) && !gameState.isPortal(x, y)) {

                        int flagCount = 0;
                        int marksCount = 0;

                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (!(dx == 0 && dy == 0)) {
                                    if (gameState.isFlagged(x + dx, y + dy)) {
                                        flagCount++;
                                    } else if (gameState.isRevealed(x + dx, y + dy) && gameState.isPortal(x + dx, y + dy)) {
                                        int m = gameState.getPortalMarks(x + dx, y + dy);
                                        if (m != 1) {
                                            marksCount += m;
                                        }
                                    }
                                }
                            }
                        }

                        if (flagCount + marksCount == gameState.getSurrounding(x, y)) {
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
            }
        } else {
            pos = getCell(new Int2(e.getX(), e.getY()), new Int2(10 + (gameState.getWidth() % 2 == 0 ? 0 : 10), 75));
            x = pos.x();
            y = pos.y();

            if (GamePermissions.hasPermission(gameState, GamePermissions.RESTART)) {
                if ((x == Math.floor(gameState.getWidth() / 2.0) - 1 || x == Math.floor(gameState.getWidth() / 2.0)) && (y == -2 || y == -3)) {
                    gameState = new GameState(gameState.getWidth(), gameState.getHeight(), gameState.getMines(), gameState.getPortals(), gameState.getPermissions());
                    PweeperMain.gameState = gameState;
                    drawPanel.repaint();
                }
            }
        }

        clicked = false;

        if (leftPressTime == 0 && rightPressTime == 0) {
            doubleClick = false;
        }

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

        gameState.setMouse(x, y);

        gameState.setClicked(clicked);
        drawPanel.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Int2 pos = getCell(new Int2(e.getX(), e.getY()));
        int x = pos.x();
        int y = pos.y();

        if (GamePermissions.hasPermission(gameState, GamePermissions.MARK)) {
            if (!SwingUtilities.isMiddleMouseButton(e)) {
                if (gameState.isInBounds(x, y)) {
                    if (e.getUnitsToScroll() < 0) {
                        gameState.increaseMark(x, y);
                    } else {
                        gameState.decreaseMark(x, y);
                    }
                }
            }
        }

        drawPanel.repaint();
    }

    private Int2 getCell(Int2 p) {
        return getCell(p, gameOffset);
    }

    private Int2 getCell(Int2 p, Int2 o) {
        int panelWidth = realGame ? drawPanel.getWidth() : clickRatio.x();
        int panelHeight = realGame ? drawPanel.getHeight() : clickRatio.y();

        double aspectRatio = (gameState.getWidth() * 20.0 + ratioOffset.x()) / (gameState.getHeight() * 20.0 + ratioOffset.y());

        int scaledWidth, scaledHeight;
        if ((double) panelWidth / panelHeight > aspectRatio) {
            scaledHeight = panelHeight;
            scaledWidth = (int) (scaledHeight * aspectRatio);
        } else {
            scaledWidth = panelWidth;
            scaledHeight = (int) (scaledWidth / aspectRatio);
        }

        int offsetX = (panelWidth - scaledWidth) / 2;
        int offsetY = (panelHeight - scaledHeight) / 2;

        double normalizedX = (p.x() - offsetX) / (double) scaledWidth;
        double normalizedY = (p.y() - offsetY) / (double) scaledHeight;
        int x = (int) Math.floor(((normalizedX * (gameState.getWidth() * 20.0 + ratioOffset.x())) - o.x()) / 20.0);
        int y = (int) Math.floor(((normalizedY * (gameState.getHeight() * 20.0 + ratioOffset.y())) - o.y()) / 20.0);

        return new Int2(x, y);
    }

    public Int2 getGameOffset() {
        return gameOffset;
    }

    public Int2 getRatioOffset() {
        return ratioOffset;
    }

    public Int2 getClickRatio() {
        return clickRatio;
    }

}
