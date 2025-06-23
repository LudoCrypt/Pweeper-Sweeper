package net.ludocrypt.pweeper.game;

public class GamePermissions {
	public static final int REVEAL = 1;
	public static final int AUTO_REVEAL = 2;
	public static final int FLAG = 4;
	public static final int MARK = 8;
	public static final int RESTART = 16;

	public static final int FULL = REVEAL | AUTO_REVEAL | FLAG | MARK | RESTART;

	public static boolean hasPermission(int gameRules, int permission) {
		return (gameRules & permission) != 0;
	}

	public static boolean hasPermission(GameState state, int permission) {
		return (state.getPermissions() & permission) != 0;
	}

	public static int create(int... is) {
		int i = 0;

		for (int p : is) {
			i |= p;
		}

		return i;
	}
}
