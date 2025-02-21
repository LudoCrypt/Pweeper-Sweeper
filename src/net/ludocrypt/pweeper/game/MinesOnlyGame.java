package net.ludocrypt.pweeper.game;

import java.awt.Graphics2D;

import net.ludocrypt.pweeper.PweeperMain;

public class MinesOnlyGame extends GameState {

    public MinesOnlyGame(int width, int height) {
        super(width, height, 0, 0, GamePermissions.create(GamePermissions.REVEAL));
    }

    @Override
    public void reveal(int x, int y) {
        if (isInBounds(x, y)) {
            mineGrid[x][y] = !mineGrid[x][y];
        }
    }

    @Override
    public boolean isRevealed(int x, int y) {
        if (isInBounds(x, y)) {
            return !mineGrid[x][y];
        }
        return true;
    }

    @Override
    public void drawCells(Graphics2D g, boolean realGame) {
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int dx = x * 20 + (realGame ? 10 : 0);
                int dy = y * 20 + (realGame ? 70 : 0);

                if (isRevealed(x, y)) {
                    g.drawImage(PweeperMain.SPRITES.get("CellRevealed"), dx, dy, null);
                } else {
                    g.drawImage(PweeperMain.SPRITES.get("Mine"), dx, dy, null);
                }

            }
        }
    }

}
