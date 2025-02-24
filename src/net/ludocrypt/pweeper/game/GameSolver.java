package net.ludocrypt.pweeper.game;

import java.util.ArrayList;
import java.util.List;

import net.ludocrypt.pweeper.game.GameState.Int2;

public class GameSolver {

    public GameState gameState;

    double[][] probabilities;

    public GameSolver(GameState gameState) {
        this.gameState = gameState;
        this.probabilities = new double[gameState.width][gameState.height];
    }

    public void solve() {

    }

    public boolean validate() {
        return getInvalidCells().isEmpty();
    }

    public List<Int2> getInvalidCells() {
        List<Int2> invalid = new ArrayList<Int2>();

        for (int x = 0; x < gameState.width; x++) {
            for (int y = 0; y < gameState.height; y++) {
                if (gameState.isCellKnown(x, y)) {
                    if (!gameState.canAutoReveal(x, y)) {
                        invalid.add(new Int2(x, y));
                    }
                }
            }
        }

        return invalid;
    }

}
