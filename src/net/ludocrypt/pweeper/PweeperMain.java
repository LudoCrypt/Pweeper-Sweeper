package net.ludocrypt.pweeper;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import net.ludocrypt.pweeper.game.GameMouseController;
import net.ludocrypt.pweeper.game.GamePermissions;
import net.ludocrypt.pweeper.game.GameState;
import net.ludocrypt.pweeper.game.GameState.Int2;
import net.ludocrypt.pweeper.game.MinesOnlyGame;
import net.ludocrypt.pweeper.render.Viewport;

public class PweeperMain {
	private JFrame frame;
	private BufferedImage canvas;
	private JPanel drawPanel;

	public static GameState gameState = new GameState(10, 10, 10, 6);
	public static Viewport viewport;
	public static GameMouseController gameController;

	public static final Map<String, BufferedImage> SPRITES = new HashMap<>();

	public PweeperMain() {
		frame = new JFrame("Pweeper Sweeper");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());

		try {
			InputStream spriteList = ClassLoader.getSystemResourceAsStream("resources/sprites.lst");
			if (spriteList != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(spriteList));
				String line;
				while ((line = reader.readLine()) != null) {
					String name = line.trim().replace(".png", "");
					InputStream imageStream = ClassLoader.getSystemResourceAsStream("resources/" + line.trim());
					if (imageStream != null) {
						SPRITES.put(name, ImageIO.read(imageStream));
					}
				}
				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		drawPanel = new JPanel() {
			private static final long serialVersionUID = -1455072995595976189L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				gameState.draw(canvas, true);

				if (PweeperMain.viewport != null) {
					PweeperMain.viewport.drawTransformedImage(g2d, canvas);
				}

			}
		};

		viewport = new Viewport(drawPanel);

		gameController = new GameMouseController(gameState, drawPanel, viewport);

		drawPanel.addMouseListener(gameController);
		drawPanel.addMouseMotionListener(gameController);
		drawPanel.addMouseWheelListener(gameController);

		frame.add(drawPanel);
		frame.setVisible(true);

		int menuHeight = frame.getJMenuBar() != null ? frame.getJMenuBar().getPreferredSize().height : 0;
		int newWidth = gameState.getWidth() * 20 + 20 + frame.getInsets().left + frame.getInsets().right;
		int newHeight = gameState.getHeight() * 20 + 80 + menuHeight + frame.getInsets().top + frame.getInsets().bottom;
		frame.setSize(newWidth, newHeight);

		frame.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				updateCanvasSize();
			}

		});

		updateCanvasSize();
		centerWindow();

		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> drawPanel.repaint(), 0, 200, TimeUnit.MILLISECONDS);
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu gameMenu = new JMenu("Game");
		JMenuItem easy = new JMenuItem("Easy");
		JMenuItem medium = new JMenuItem("Medium");
		JMenuItem hard = new JMenuItem("Hard");
		JMenuItem custom = new JMenuItem("Custom");
//        custom.setEnabled(false);

		easy.addActionListener(e -> setGameSize(10, 10, 10, 6));
		medium.addActionListener(e -> setGameSize(16, 16, 40, 20));
		hard.addActionListener(e -> setGameSize(24, 24, 99, 50));
		custom.addActionListener(e -> openCustomDialog());

		gameMenu.add(easy);
		gameMenu.add(medium);
		gameMenu.add(hard);
		gameMenu.add(custom);

		JMenu helpMenu = new JMenu("Help");

		JMenuItem howto = new JMenuItem("How to play");
		howto.addActionListener(e -> onClickHelpButton());

		helpMenu.add(howto);

		menuBar.add(gameMenu);
		menuBar.add(helpMenu);

		return menuBar;
	}

	private void setGameSize(int width, int height, int mines, int portals) {
		gameState = new GameState(width, height, mines, portals);

		drawPanel.removeMouseListener(gameController);
		drawPanel.removeMouseMotionListener(gameController);
		drawPanel.removeMouseWheelListener(gameController);

		gameController = new GameMouseController(gameState, drawPanel, viewport);

		drawPanel.addMouseListener(gameController);
		drawPanel.addMouseMotionListener(gameController);
		drawPanel.addMouseWheelListener(gameController);

		updateCanvasSize();
	}

	private void updateCanvasSize() {
		canvas = new BufferedImage(gameState.getWidth() * 20 + 20, gameState.getHeight() * 20 + 80, BufferedImage.TYPE_INT_ARGB);
		frame.repaint();
	}

	private void centerWindow() {
		frame.setLocationRelativeTo(null);
	}

	private void openCustomDialog() {
		JTextField widthField = new JTextField(String.valueOf(gameState.getWidth()));
		JTextField heightField = new JTextField(String.valueOf(gameState.getHeight()));

		JPanel panel = new JPanel(new GridLayout(4, 2));

		panel.add(new JLabel("Width:"));
		panel.add(widthField);
		panel.add(new JLabel("Height:"));
		panel.add(heightField);

		DocumentFilter numberFilter = new DocumentFilter() {
			@Override
			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
				if (string != null && string.matches("\\d+")) {
					super.insertString(fb, offset, string, attr);
				}
			}

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
				if (text != null && text.matches("\\d+")) {
					super.replace(fb, offset, length, text, attrs);
				}
			}
		};

		((AbstractDocument) widthField.getDocument()).setDocumentFilter(numberFilter);
		((AbstractDocument) heightField.getDocument()).setDocumentFilter(numberFilter);

		int result = JOptionPane.showConfirmDialog(frame, panel, "Custom Game", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			int w = Integer.parseInt(widthField.getText());
			int h = Integer.parseInt(heightField.getText());

			// Constants found by Regression
			double mineDensity = 0.188662 - 8.81325 / (w * h);
			double portalDensity = 0.091585 - 3.18523 / (w * h);

			setGameSize(w, h, (int) (mineDensity * (double) w * (double) h), (int) (portalDensity * (double) w * (double) h));
		}
	}

	private void onClickHelpButton() {
		JFrame helpFrame = new JFrame("How to Play");
		helpFrame.setSize(300, 165);
		helpFrame.setLocationRelativeTo(null);
		helpFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel buttonPanel = new JPanel();

		BufferedImage[] slideImages = { PweeperMain.SPRITES.get("Slide1"), PweeperMain.SPRITES.get("Slide2"), PweeperMain.SPRITES.get("Slide3"), PweeperMain.SPRITES.get("Slide4"), PweeperMain.SPRITES.get("Slide5"), PweeperMain.SPRITES.get("Slide6"), PweeperMain.SPRITES.get("Slide7") };

		final int[] currentSlide = { 0 };

		final GameMouseController[] adapters = { null, null };
		final BufferedImage[] canvi = { null, null };
		final GameState[] games = { null, null };

		JPanel contentPanel = new JPanel(new BorderLayout()) {
			private static final long serialVersionUID = -4501660782375750337L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;

				if (games[0] != null) {
					games[0].draw(canvi[0], false);
				}

				if (games[1] != null) {
					games[1].draw(canvi[1], false);
				}

				g2d.drawImage(slideImages[currentSlide[0]], 0, 0, slideImages[currentSlide[0]].getWidth(), slideImages[currentSlide[0]].getHeight(), null);
				if (canvi[0] != null) {
					g2d.drawImage(canvi[0], adapters[0].getGameOffset().x(), adapters[0].getGameOffset().y(), canvi[0].getWidth(), canvi[0].getHeight(), null);
				}
				if (canvi[1] != null) {
					g2d.drawImage(canvi[1], adapters[1].getGameOffset().x(), adapters[1].getGameOffset().y(), canvi[1].getWidth(), canvi[1].getHeight(), null);
				}
			}

		};

		JButton prevButton = new JButton("<- Prev");
		JButton nextButton = new JButton("Next ->");

		try (InputStream is = ClassLoader.getSystemResourceAsStream("resources/typewriter.ttf")) {
			if (is != null) {
				Font customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(18f);
				prevButton.setFont(customFont);
				nextButton.setFont(customFont);
			} else {
				System.err.println("Font file not found!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		prevButton.setEnabled(false);

		Runnable updatePanels = () -> {
			int slide = currentSlide[0];

			if (adapters[0] != null) {
				contentPanel.removeMouseListener(adapters[0]);
				contentPanel.removeMouseMotionListener(adapters[0]);
				contentPanel.removeMouseWheelListener(adapters[0]);
			}

			if (adapters[1] != null) {
				contentPanel.removeMouseListener(adapters[1]);
				contentPanel.removeMouseMotionListener(adapters[1]);
				contentPanel.removeMouseWheelListener(adapters[1]);
			}

			games[0] = null;
			games[1] = null;

			adapters[0] = null;
			adapters[1] = null;

			canvi[0] = null;
			canvi[1] = null;

			if (slide == 1) {
				games[0] = new MinesOnlyGame(7, 3);
				games[1] = new MinesOnlyGame(3, 3);

				games[0].mineGrid[2][0] = true;
				games[0].mineGrid[3][0] = true;
				games[0].mineGrid[4][0] = true;

				games[1].mineGrid[0][0] = true;
				games[1].mineGrid[0][1] = true;
				games[1].mineGrid[0][2] = true;
				games[1].mineGrid[1][0] = true;
				games[1].mineGrid[1][2] = true;
				games[1].mineGrid[2][0] = true;
				games[1].mineGrid[2][1] = true;
				games[1].mineGrid[2][2] = true;

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(7, 98), new Int2(7 * 20, 3 * 20), new Viewport(drawPanel, true));
				adapters[1] = new GameMouseController(games[1], contentPanel, false, new Int2(167, 10), new Int2(3 * 20, 3 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(7 * 20, 3 * 20, BufferedImage.TYPE_INT_ARGB);
				canvi[1] = new BufferedImage(3 * 20, 3 * 20, BufferedImage.TYPE_INT_ARGB);
			} else if (slide == 2) {
				games[0] = new GameState(6, 4, 0, 0, GamePermissions.create(GamePermissions.FLAG));

				games[0].mineGrid[2][1] = true;
				games[0].mineGrid[4][2] = true;
				games[0].mineGrid[2][3] = true;

				games[0].flagged[2][1] = true;

				games[0].revealed[0][0] = true;
				games[0].revealed[1][0] = true;
				games[0].revealed[2][0] = true;
				games[0].revealed[3][0] = true;
				games[0].revealed[4][0] = true;
				games[0].revealed[5][0] = true;
				games[0].revealed[0][1] = true;
				games[0].revealed[1][1] = true;
				games[0].revealed[3][1] = true;
				games[0].revealed[4][1] = true;
				games[0].revealed[5][1] = true;
				games[0].revealed[0][2] = true;
				games[0].revealed[1][2] = true;
				games[0].revealed[0][3] = true;
				games[0].revealed[1][3] = true;

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(8, 77), new Int2(6 * 20, 4 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(6 * 20, 4 * 20, BufferedImage.TYPE_INT_ARGB);
			} else if (slide == 3) {
				games[0] = new GameState(3, 3, 0, 0, GamePermissions.create(GamePermissions.AUTO_REVEAL));

				games[0].officiallyStarted = true;

				games[0].mineGrid[0][0] = true;

				games[0].flagged[0][0] = true;

				games[0].revealed[0][1] = true;
				games[0].revealed[1][1] = true;
				games[0].revealed[2][1] = true;
				games[0].revealed[0][2] = true;
				games[0].revealed[1][2] = true;
				games[0].revealed[2][2] = true;

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(8, 98), new Int2(3 * 20, 3 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(3 * 20, 3 * 20, BufferedImage.TYPE_INT_ARGB);
			} else if (slide == 4) {
				games[0] = new GameState(5, 4, 0, 0, GamePermissions.create(GamePermissions.REVEAL)) {

					public void drawCells(Graphics2D g, boolean realGame) {
						for (int x = 0; x < getWidth(); x++) {
							for (int y = 0; y < getHeight(); y++) {
								int dx = x * 20 + (realGame ? 10 : 0);
								int dy = y * 20 + (realGame ? 70 : 0);

								if (isRevealed(x, y)) {
									g.drawImage(PweeperMain.SPRITES.get("CellRevealed"), dx, dy, null);
								} else {
									g.drawImage(PweeperMain.SPRITES.get("Cell"), dx, dy, null);

									if (gameLive) {
										if (x == 1 && y == 2) {
											g.drawImage(PweeperMain.SPRITES.get("Question"), dx, dy, null);
										}
									}
								}
							}
						}
					}

				};

				games[0].officiallyStarted = true;

				games[0].mineGrid[0][2] = true;
				games[0].mineGrid[1][3] = true;
				games[0].mineGrid[3][3] = true;
				games[0].mineGrid[4][3] = true;

				games[0].revealed[0][0] = true;
				games[0].revealed[1][0] = true;
				games[0].revealed[2][0] = true;
				games[0].revealed[3][0] = true;
				games[0].revealed[4][0] = true;
				games[0].revealed[0][1] = true;
				games[0].revealed[1][1] = true;
				games[0].revealed[2][1] = true;
				games[0].revealed[3][1] = true;
				games[0].revealed[4][1] = true;
				games[0].revealed[2][2] = true;
				games[0].revealed[3][2] = true;
				games[0].revealed[4][2] = true;

				games[0].portalsMap.put(new Int2(1, 2), new Int2(0, 3));

				games[1] = new GameState(3, 6, 0, 0, GamePermissions.create(GamePermissions.REVEAL, GamePermissions.FLAG, GamePermissions.AUTO_REVEAL));

				games[1].officiallyStarted = true;

				games[1].mineGrid[0][1] = true;
				games[1].mineGrid[0][4] = true;
				games[1].mineGrid[1][4] = true;

				games[1].flagged[0][1] = true;
				games[1].flagged[0][4] = true;
				games[1].flagged[1][4] = true;

				games[1].revealed[0][0] = true;
				games[1].revealed[1][0] = true;
				games[1].revealed[2][0] = true;
				games[1].revealed[1][1] = true;
				games[1].revealed[2][1] = true;
				games[1].revealed[0][2] = true;
				games[1].revealed[1][2] = true;
				games[1].revealed[2][2] = true;
				games[1].revealed[0][3] = true;
				games[1].revealed[1][3] = true;
				games[1].revealed[2][3] = true;
				games[1].revealed[0][5] = true;
				games[1].revealed[1][5] = true;
				games[1].revealed[2][5] = true;

				games[1].portalsMap.put(new Int2(2, 1), new Int2(0, 3));

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(8, 77), new Int2(5 * 20, 4 * 20), new Viewport(drawPanel, true));
				adapters[1] = new GameMouseController(games[1], contentPanel, false, new Int2(233, 7), new Int2(3 * 20, 6 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(5 * 20, 4 * 20, BufferedImage.TYPE_INT_ARGB);
				canvi[1] = new BufferedImage(3 * 20, 6 * 20, BufferedImage.TYPE_INT_ARGB);
			} else if (slide == 5) {
				games[0] = new GameState(4, 4, 0, 0, GamePermissions.create(GamePermissions.MARK));

				games[0].officiallyStarted = true;

				games[0].mineGrid[0][0] = true;
				games[0].mineGrid[0][1] = true;

				games[0].flagged[0][0] = true;
				games[0].flagged[0][1] = true;

				games[0].revealed[2][0] = true;
				games[0].revealed[1][1] = true;
				games[0].revealed[2][1] = true;
				games[0].revealed[3][1] = true;
				games[0].revealed[0][2] = true;
				games[0].revealed[1][2] = true;
				games[0].revealed[2][2] = true;
				games[0].revealed[3][2] = true;
				games[0].revealed[0][3] = true;
				games[0].revealed[1][3] = true;
				games[0].revealed[2][3] = true;
				games[0].revealed[3][3] = true;

				games[0].portalsMap.put(new Int2(1, 0), new Int2(1, 3));

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(7, 78), new Int2(4 * 20, 4 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(4 * 20, 4 * 20, BufferedImage.TYPE_INT_ARGB);
			} else if (slide == 6) {
				games[0] = new GameState(5, 3, 0, 0, GamePermissions.create(GamePermissions.FLAG, GamePermissions.REVEAL, GamePermissions.AUTO_REVEAL));

				games[0].officiallyStarted = true;

				games[0].mineGrid[1][0] = true;
				games[0].mineGrid[1][1] = true;
				games[0].mineGrid[3][1] = true;
				games[0].mineGrid[4][1] = true;

				games[0].flagged[1][0] = true;
				games[0].flagged[1][1] = true;
				games[0].flagged[3][1] = true;
				games[0].flagged[4][1] = true;

				games[0].revealed[0][0] = true;
				games[0].revealed[2][0] = true;
				games[0].revealed[3][0] = true;
				games[0].revealed[4][0] = true;
				games[0].revealed[0][1] = true;
				games[0].revealed[2][1] = true;
				games[0].revealed[0][2] = true;
				games[0].revealed[1][2] = true;
				games[0].revealed[2][2] = true;
				games[0].revealed[3][2] = true;
				games[0].revealed[4][2] = true;

				adapters[0] = new GameMouseController(games[0], contentPanel, false, new Int2(8, 97), new Int2(5 * 20, 3 * 20), new Viewport(drawPanel, true));

				canvi[0] = new BufferedImage(5 * 20, 3 * 20, BufferedImage.TYPE_INT_ARGB);
			}

			if (adapters[0] != null) {
				contentPanel.addMouseListener(adapters[0]);
				contentPanel.addMouseMotionListener(adapters[0]);
				contentPanel.addMouseWheelListener(adapters[0]);
			}

			if (adapters[1] != null) {
				contentPanel.addMouseListener(adapters[1]);
				contentPanel.addMouseMotionListener(adapters[1]);
				contentPanel.addMouseWheelListener(adapters[1]);
			}

		};

		nextButton.addActionListener(e -> {
			if (currentSlide[0] < slideImages.length - 1) {
				currentSlide[0]++;
				prevButton.setEnabled(true);
			}

			if (currentSlide[0] == slideImages.length - 1) {
				nextButton.setText("Proceed");
			}

			updatePanels.run();
			contentPanel.repaint();
		});

		prevButton.addActionListener(e -> {
			if (currentSlide[0] > 0) {
				currentSlide[0]--;
				nextButton.setText("Next ->");
			}

			if (currentSlide[0] == 0) {
				prevButton.setEnabled(false);
			}

			updatePanels.run();
			contentPanel.repaint();
		});

		nextButton.addActionListener(e -> {
			if (currentSlide[0] == slideImages.length - 1) {
				helpFrame.dispose();
			}
		});

		buttonPanel.add(prevButton);
		buttonPanel.add(nextButton);

		contentPanel.add(buttonPanel, BorderLayout.SOUTH);

		helpFrame.add(contentPanel);
		helpFrame.setVisible(true);

		int menuHeight = buttonPanel != null ? buttonPanel.getPreferredSize().height : 0;
		int newWidth = 300 + helpFrame.getInsets().left + helpFrame.getInsets().right;
		int newHeight = 165 + menuHeight + helpFrame.getInsets().top + helpFrame.getInsets().bottom;

		helpFrame.setSize(newWidth, newHeight);
		helpFrame.setResizable(false);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(PweeperMain::new);
	}
}
