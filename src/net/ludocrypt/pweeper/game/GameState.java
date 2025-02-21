package net.ludocrypt.pweeper.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import net.ludocrypt.pweeper.PweeperMain;

public class GameState {
    public int width, height, mines, portals, permissions;
    public boolean[][] mineGrid, revealed, flagged;
    public int[][] portalMarks;
    public Map<Int2, Int2> portalsMap;

    public long startTime;
    public long endTime;
    public boolean gameLive;
    public boolean officiallyStarted;

    private int mouseX = -10, mouseY = -10;
    private boolean around;
    private boolean clicked;

    public GameState(int width, int height, int mines, int portals) {
        this(width, height, mines, portals, GamePermissions.FULL);
    }

    public GameState(int width, int height, int mines, int portals, int permissions) {
        this.width = width;
        this.height = height;
        this.mines = mines;
        this.portals = portals;
        this.permissions = permissions;
        this.mineGrid = new boolean[width][height];
        this.revealed = new boolean[width][height];
        this.flagged = new boolean[width][height];
        this.portalMarks = new int[width][height];
        this.portalsMap = new HashMap<Int2, Int2>();

        resetGame();
    }

    public void resetGame() {
        this.officiallyStarted = false;
        this.mouseX = -10;
        this.mouseY = -10;
        this.around = false;

        this.mineGrid = new boolean[width][height];
        this.revealed = new boolean[width][height];
        this.flagged = new boolean[width][height];
        this.portalMarks = new int[width][height];
        this.portalsMap.clear();

        placeMinesAndRoots();
        startTime = System.currentTimeMillis();
        gameLive = true;
    }

    private void placeMinesAndRoots() {
        Random random = new Random();

        int minesPlaced = 0;

        while (minesPlaced < mines) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            if (!isMine(x, y)) {
                this.mineGrid[x][y] = true;
                minesPlaced++;
            }
        }

        int portalsPlaced = 0;

        while (portalsPlaced < portals) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);

            if (!isPortal(x, y) && !isMine(x, y) && getSurroundingMines(x, y) != 1 && !isPortal(x2, y2) && !isMine(x2, y2) && getSurroundingMines(x2, y2) != 1 && x != x2 && y != y2) {
                this.portalsMap.put(new Int2(x, y), new Int2(x2, y2));
                portalsPlaced++;
            }
        }

    }

    public boolean isMine(int x, int y) {
        return isInBounds(x, y) ? mineGrid[x][y] : false;
    }

    public boolean isPortal(int x, int y) {
        return this.portalsMap.containsKey(new Int2(x, y)) || this.portalsMap.containsValue(new Int2(x, y));
    }

    public boolean portalState(int x, int y) {
        return this.portalsMap.containsKey(new Int2(x, y));
    }

    public boolean isRevealed(int x, int y) {
        return isInBounds(x, y) ? revealed[x][y] : false;
    }

    public boolean isFlagged(int x, int y) {
        return isInBounds(x, y) ? flagged[x][y] : false;
    }

    public int getPortalMarks(int x, int y) {
        return isInBounds(x, y) ? portalMarks[x][y] : 0;
    }

    public void reveal(int x, int y) {

        if (!officiallyStarted) {
            if (isInBounds(x, y)) {
                while (isMine(x, y) || getSurrounding(x, y) != 0 || isPortal(x, y)) {
                    resetGame();
                }
                officiallyStarted = true;
                startTime = System.currentTimeMillis();
            }
        }

        if (!isInBounds(x, y) || !gameLive || revealed[x][y] || flagged[x][y]) {
            return;
        }

        revealed[x][y] = true;

        if (isMine(x, y)) {
            gameLive = false;
        } else {
            if (getSurrounding(x, y) == 0) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (!isRevealed(x + dx, y + dy)) {
                            reveal(x + dx, y + dy);
                        }
                    }
                }

            }
        }

    }

    public void toggleFlag(int x, int y) {
        if (!gameLive || revealed[x][y]) {
            return;
        }
        flagged[x][y] = !flagged[x][y];
    }

    public void increaseMark(int x, int y) {
        if (!gameLive || !revealed[x][y] || !isPortal(x, y)) {
            return;
        }
        portalMarks[x][y] = Math.min(portalMarks[x][y] + 1, 18);
    }

    public void decreaseMark(int x, int y) {
        if (!gameLive || !revealed[x][y] || !isPortal(x, y)) {
            return;
        }
        portalMarks[x][y] = Math.max(portalMarks[x][y] - 1, 0);
    }

    public long getElapsedTime() {
        return officiallyStarted ? (endTime - startTime) / 1000 : 0;
    }

    public void drawCells(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (isRevealed(x, y) || (clicked && x == mouseX && y == mouseY) || (clicked && around && isAroundMouse(x, y))) {
                    g.drawImage(PweeperMain.SPRITES.get("CellRevealed"), dx, dy, null);
                } else {
                    g.drawImage(PweeperMain.SPRITES.get("Cell"), dx, dy, null);
                }
            }
        }
    }

    public void drawPortalCells(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (x == mouseX && y == mouseY) {
                    if (isPortal(x, y)) {
                        if (portalState(x, y)) {
                            if (isRevealed(x, y)) {
                                g.drawImage(PweeperMain.SPRITES.get("CellA"), dx, dy, null);
                            } else if (!gameLive) {
                                g.drawImage(PweeperMain.SPRITES.get("CellRevealedA"), dx, dy, null);
                            }

                            Int2 portal = this.portalsMap.get(new Int2(x, y));

                            int ddx = portal.x * 20 + (realGame ? 10 : 0);
                            int ddy = portal.y * 20 + (realGame ? 70 : 0);

                            if (gameLive) {
                                if (isRevealed(portal.x, portal.y) && isRevealed(x, y)) {
                                    g.drawImage(PweeperMain.SPRITES.get("CellB"), ddx, ddy, null);
                                }
                            } else {
                                if (isRevealed(portal.x, portal.y)) {
                                    g.drawImage(PweeperMain.SPRITES.get("CellB"), ddx, ddy, null);
                                } else {
                                    g.drawImage(PweeperMain.SPRITES.get("CellRevealedB"), ddx, ddy, null);
                                }
                            }
                        } else {
                            if (isRevealed(x, y)) {
                                g.drawImage(PweeperMain.SPRITES.get("CellB"), dx, dy, null);
                            } else if (!gameLive) {
                                g.drawImage(PweeperMain.SPRITES.get("CellRevealedB"), dx, dy, null);
                            }

                            Int2 portal = inv(this.portalsMap).get(new Int2(x, y));

                            int ddx = portal.x * 20 + (realGame ? 10 : 0);
                            int ddy = portal.y * 20 + (realGame ? 70 : 0);

                            if (gameLive) {
                                if (isRevealed(portal.x, portal.y) && isRevealed(x, y)) {
                                    g.drawImage(PweeperMain.SPRITES.get("CellA"), ddx, ddy, null);
                                }
                            } else {
                                if (isRevealed(portal.x, portal.y)) {
                                    g.drawImage(PweeperMain.SPRITES.get("CellA"), ddx, ddy, null);
                                } else {
                                    g.drawImage(PweeperMain.SPRITES.get("CellRevealedA"), ddx, ddy, null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void drawPortals(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (isRevealed(x, y) || !gameLive) {
                    if (isPortal(x, y)) {
                        if (portalState(x, y)) {
                            g.drawImage(PweeperMain.SPRITES.get("PortalA"), dx, dy, null);
                        } else {
                            g.drawImage(PweeperMain.SPRITES.get("PortalB"), dx, dy, null);
                        }
                    }
                }
            }
        }
    }

    public void drawFlags(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (!isRevealed(x, y) && isFlagged(x, y)) {
                    if ((!gameLive && !isPortal(x, y)) || gameLive) {
                        g.drawImage(PweeperMain.SPRITES.get("Flag"), dx, dy, null);
                    }

                    if (!gameLive && !isMine(x, y)) {
                        g.drawImage(PweeperMain.SPRITES.get("CellWrong"), dx, dy, null);
                    }
                }
            }
        }
    }

    public void drawEndScreen(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (!gameLive) {
                    if (isMine(x, y)) {
                        if (isRevealed(x, y)) {
                            g.drawImage(PweeperMain.SPRITES.get("MineRevealed"), dx, dy, null);
                        } else {
                            if (isFlagged(x, y)) {
                                g.drawImage(PweeperMain.SPRITES.get("MineCorrect"), dx, dy, null);
                            } else {
                                g.drawImage(PweeperMain.SPRITES.get("Mine"), dx, dy, null);
                            }
                        }
                    }
                }
            }
        }
    }

    public void drawMineCount(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (isRevealed(x, y) && !isPortal(x, y) && !isMine(x, y)) {
                    int c = getSurrounding(x, y);

                    if (c > 0 && c <= 18) {
                        g.drawImage(PweeperMain.SPRITES.get("" + c), dx, dy, null);
                    }
                }

                if (isRevealed(x, y) && isPortal(x, y) && !isMine(x, y)) {
                    int m = getPortalMarks(x, y);

                    if (m > 0 && m <= 18) {
                        if (m == 1) {
                            m = 0;
                        }

                        g.drawImage(PweeperMain.SPRITES.get("" + m), dx, dy, null);
                    }
                }
            }
        }
    }

    public void drawGame(Graphics2D g, boolean won) {
        g.setColor(new Color(206, 206, 206));
        g.fillRect(10, 10, getWidth() * 20, 50);

        g.setColor(new Color(0, 0, 0));
        g.fillRect(12, 13, 25 * 3 + 1, 44);

        g.setColor(new Color(0, 0, 0));
        g.fillRect(getWidth() * 20 - 69, 13, 25 * 3 + 2, 44);

        int flags = getTotalFlags();

        String flagStr = String.format("%03d", mines - flags);

        int i = 0;
        for (char c : flagStr.toCharArray()) {

            if (c == '-') {
                c = 'M';
            }

            if (3 - Integer.toString(mines - flags).length() <= i || mines - flags < 0) {
                g.drawImage(PweeperMain.SPRITES.get("Clock" + c), 12 + 25 * i, 12, null);
            } else {
                g.drawImage(PweeperMain.SPRITES.get("ClockNot"), 12 + 25 * i, 12, null);
            }

            i++;
        }

        String timeStr = String.format("%03d", officiallyStarted ? getElapsedTime() : 0);

        i = 0;
        for (char c : timeStr.toCharArray()) {

            if (3 - Long.toString(getElapsedTime()).length() <= i) {
                g.drawImage(PweeperMain.SPRITES.get("Clock" + c), getWidth() * 20 - 68 + 25 * i, 12, null);
            } else {
                g.drawImage(PweeperMain.SPRITES.get("ClockNot"), getWidth() * 20 - 68 + 25 * i, 12, null);
            }

            i++;
        }

        g.setColor(new Color(60, 60, 60));
        g.drawRect(11, 12, 25 * 3 + 2, 45);

        g.setColor(new Color(60, 60, 60));
        g.drawRect(getWidth() * 20 - 69, 12, 25 * 3 + 2, 45);

        for (int x = 0; x < getWidth(); x++) {
            int dx = x * 20 + 10;

            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx, 0, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx + 10, 0, null);

            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx, 60, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx + 10, 60, null);

            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx, getHeight() * 20 + 70, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderTB"), dx + 10, getHeight() * 20 + 70, null);
        }

        for (int y = 0; y < getHeight(); y++) {
            int dy = y * 20 + 70;

            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), 0, dy, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), 0, dy + 10, null);

            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), getWidth() * 20 + 10, dy, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), getWidth() * 20 + 10, dy + 10, null);
        }

        for (int y = 1; y < 6; y++) {
            int dy = y * 10;

            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), 0, dy, null);
            g.drawImage(PweeperMain.SPRITES.get("BoarderLR"), getWidth() * 20 + 10, dy, null);
        }

        g.drawImage(PweeperMain.SPRITES.get("BoarderTL"), 0, 0, null);
        g.drawImage(PweeperMain.SPRITES.get("BoarderTR"), getWidth() * 20 + 10, 0, null);
        g.drawImage(PweeperMain.SPRITES.get("BoarderBL"), 0, getHeight() * 20 + 70, null);
        g.drawImage(PweeperMain.SPRITES.get("BoarderBR"), getWidth() * 20 + 10, getHeight() * 20 + 70, null);

        g.drawImage(PweeperMain.SPRITES.get("BoarderBLR"), 0, 60, null);
        g.drawImage(PweeperMain.SPRITES.get("BoarderTRL"), getWidth() * 20 + 10, 60, null);

        if (clicked && (mouseX == Math.floor(width / 2.0) - 1 || mouseX == Math.floor(width / 2.0)) && (mouseY == -2 || mouseY == -3)) {
            g.drawImage(PweeperMain.SPRITES.get("FaceClicked"), getWidth() * 10 - 10, 15, null);
        } else {
            g.drawImage(PweeperMain.SPRITES.get("FaceUnclicked"), getWidth() * 10 - 10, 15, null);
        }

        if (gameLive) {
            if (clicked || around) {
                g.drawImage(PweeperMain.SPRITES.get("Face2"), getWidth() * 10 - 10, 15, null);
            } else {
                g.drawImage(PweeperMain.SPRITES.get("Face1"), getWidth() * 10 - 10, 15, null);
            }
            endTime = System.currentTimeMillis();
        } else {
            if (won) {
                g.drawImage(PweeperMain.SPRITES.get("Face4"), getWidth() * 10 - 10, 15, null);
            } else {
                g.drawImage(PweeperMain.SPRITES.get("Face3"), getWidth() * 10 - 10, 15, null);
            }
        }

        if (gameLive) {
            if (mines - getTotalFlags() == 0 && getTotalRevealed() == width * height - mines) {
                gameLive = false;
            }
        }
    }

    public void draw(BufferedImage canvas, boolean realGame) {
        Graphics2D g = canvas.createGraphics();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawCells(g, realGame);
        drawPortalCells(g, realGame);
        drawFlags(g, realGame);
        drawEndScreen(g, realGame);
        drawMineCount(g, realGame);
        drawPortals(g, realGame);

        boolean won = true;

        m: for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (!gameLive) {
                    if (isMine(x, y) && !isRevealed(x, y)) {
                        if (!isFlagged(x, y)) {
                            won = false;
                            break m;
                        }
                    }
                }
            }
        }

        if (realGame) {
            drawGame(g, won);
        }

        g.dispose();
    }

    public void setMouse(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public void setAround(boolean around) {
        this.around = around;
    }

    public boolean isAroundMouse(int x, int y) {
        return Math.abs(x - mouseX) <= 1 && Math.abs(y - mouseY) <= 1;
    }

    public int getSurroundingMines(int x, int y) {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!(dx == 0 && dy == 0)) {
                    if (isMine(x + dx, y + dy)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public int getSurroundingFlags(int x, int y) {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!(dx == 0 && dy == 0)) {
                    if (isFlagged(x + dx, y + dy)) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public int getSurrounding(int x, int y) {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!(dx == 0 && dy == 0)) {
                    if (isMine(x + dx, y + dy)) {
                        count++;
                    } else if (isPortal(x + dx, y + dy)) {
                        if (portalState(x + dx, y + dy)) {
                            count += getSurroundingMines(this.portalsMap.get(new Int2(x + dx, y + dy)).x, this.portalsMap.get(new Int2(x + dx, y + dy)).y);
                        } else {
                            count += getSurroundingMines(inv(this.portalsMap).get(new Int2(x + dx, y + dy)).x, inv(this.portalsMap).get(new Int2(x + dx, y + dy)).y);
                        }
                    }
                }
            }
        }

        return count;
    }

    public int getTotalFlags() {
        int flags = 0;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (isFlagged(x, y)) {
                    flags++;
                }
            }
        }
        return flags;
    }

    public int getTotalRevealed() {
        int revealed = 0;

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                if (isRevealed(x, y)) {
                    revealed++;
                }
            }
        }
        return revealed;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMines() {
        return mines;
    }

    public int getPortals() {
        return portals;
    }

    public int getPermissions() {
        return permissions;
    }

    public static record Int2(int x, int y) {

    }

    public static <K, V> Map<V, K> inv(Map<K, V> map) {
        return Map.copyOf(map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
    }

}
