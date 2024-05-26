import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.*;

public class Minesweeper {
    private class MineTile extends JButton {
        int r;
        int c;
        boolean isMiniBomb;
        boolean isMine;
        boolean isBlast;

        public MineTile(int r, int c) {
            this.r = r;
            this.c = c;
            this.isBlast = false;
            this.isMine = false;
            this.isMiniBomb = false;
        }

    }

    public enum Difficulty {
        EASY(8, 14, 1, 32), // 6 each of mines
        MEDIUM(12, 26, 1.5, 72), // 10 each of mines
        HARD(16, 38, 2.0, 128), // 14 each
        DIFFICULT(18, 54, 2.5, 162); // 18 each

        final int size;
        final int mines;
        final double flagModifier;
        final int movesAllowed;

        Difficulty(int size, int mines, double flagModifier, int movesAllowed) {
            this.size = size;
            this.mines = mines;
            this.flagModifier = flagModifier;
            this.movesAllowed = movesAllowed;
        }
    }

    int tileSize = 70;
    int numRows = 8;
    int tilesClicked = 0;
    int numCols = numRows;
    int totalMinesCount = 14;
    int boardWidth = numCols * tileSize;
    int boardHeight = numRows * tileSize + 50; // Added extra space for controls
    boolean powerUpActive = false;
    int flagCount = totalMinesCount;
    int moveCount = 8 * 8 / 2;
    int miniBombCount;
    int mineCount;
    int blastCount;

    JFrame frame = new JFrame("Minesweeper");
    JLabel movesLabel = new JLabel("Moves: ");
    JLabel textLabel = new JLabel("", JLabel.CENTER);
    JPanel textPanel = new JPanel();
    JPanel boardPanel = new JPanel();
    JPanel powerUpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    // JPanel powerUpPanel = new JPanel();
    JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    int powerUpsRemaining = 3; // Initialize the number of power-ups the player starts with

    MineTile[][] board = new MineTile[numRows][numCols];
    ArrayList<MineTile> mineList;
    LinkedList<MineTile> flaggedTiles = new LinkedList<>();
    Random random = new Random();

    boolean gameOver = false;

    JLabel minesLabel = new JLabel("Mines: ");
    JTextField minesField = new JTextField("14", 5);
    JButton startButton = new JButton("Start Game");
    JLabel restartLabel = new JLabel("↻", JLabel.CENTER);
    JComboBox<Difficulty> levelSelector = new JComboBox<>(Difficulty.values());

    Minesweeper() {
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 22));
        textLabel.setOpaque(true);
        textPanel.setLayout(new BorderLayout());
        textPanel.add(textLabel, BorderLayout.CENTER);

        initializeUIComponents();
        initializePowerUps();
        initializeBoard();
        showSetupState();
        frame.setVisible(true);
    }

    private void initializeUIComponents() {
        minesField.setHorizontalAlignment(JTextField.CENTER);
        startButton.addActionListener(e -> handleStartButton());
        restartLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 25));
        restartLabel.setForeground(Color.BLACK);
        restartLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        restartLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                textLabel.setText(" ");
                textPanel.add(textLabel, BorderLayout.CENTER);
                showSetupState();
                moveCount = numRows * numCols / 2;
            }
        });
        restartLabel.setToolTipText("Restart Game");

        levelSelector.addActionListener(e -> {
            Difficulty selectedLevel = (Difficulty) levelSelector.getSelectedItem();
            if (selectedLevel != null) {
                numRows = selectedLevel.size;
                numCols = selectedLevel.size;
                totalMinesCount = selectedLevel.mines;
                moveCount = selectedLevel.size * selectedLevel.size / 2;
                flagCount = (int) Math.ceil(totalMinesCount / selectedLevel.flagModifier);
                // flagCount = totalMinesCount;
                minesField.setText(Integer.toString(totalMinesCount));
                flaggedTiles.clear();
                resetGame();
            }
        });

        textPanel.add(levelSelector, BorderLayout.WEST);
        centerPanel.add(minesLabel);
        centerPanel.add(minesField);
        centerPanel.add(startButton);
        textPanel.add(centerPanel, BorderLayout.CENTER);
        frame.add(textPanel, BorderLayout.NORTH);
    }

    private void handleStartButton() {
        String input = minesField.getText();
        try {
            int mines = Integer.parseInt(input);
            if (mines > 2 && mines < numRows * numCols) {
                totalMinesCount = mines;
                tilesClicked = 0;
                moveCount = numRows * numCols / 2;
                miniBombCount = 0;
                mineCount = 0;
                blastCount = 0;
                resetGame();
                updateDisplayText();
                showGameState();
            } else {
                JOptionPane.showMessageDialog(frame, "Enter a valid number between 3 and " + (numRows * numCols - 1));
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Please enter a valid number.");
        }
    }

    private void initializePowerUps() {
        powerUpPanel.removeAll();
        JButton[] powerUps = new JButton[3];
        powerUpsRemaining = 3;
        for (int i = 0; i < powerUps.length; i++) {
            powerUps[i] = new JButton("💡");
            powerUps[i].setToolTipText("Use a safe zone power-up");
            powerUps[i].addActionListener(e -> usePowerUp((JButton) e.getSource()));
            powerUpPanel.add(powerUps[i]);
        }
        textPanel.add(powerUpPanel, BorderLayout.EAST);
        powerUpPanel.setVisible(false); // Initially hide the panel

    }

    private void usePowerUp(JButton powerUpButton) {
        if (powerUpsRemaining > 0 && !powerUpActive) {
            powerUpActive = true;
            powerUpButton.setEnabled(false); // Disabling the power-up button after use
            powerUpsRemaining--;
        }
    }

    private void initializeBoard() {
        boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(numRows, numCols));
        frame.add(boardPanel, BorderLayout.CENTER);
        board = new MineTile[numRows][numCols];

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                MineTile tile = new MineTile(r, c);
                board[r][c] = tile;
                tile.setFocusable(false);
                tile.setMargin(new Insets(0, 0, 0, 0));
                tile.setEnabled(true);
                tile.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
                addTileListener(tile);
                boardPanel.add(tile);
            }
        }

        showSetupState();
        frame.validate();
        frame.repaint();
    }

    private void addTileListener(MineTile tile) {
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameOver) {
                    return;
                }
                MineTile clickedTile = (MineTile) e.getSource();
                handleTileClick(clickedTile, e);
            }
        });
    }

    private void handleTileClick(MineTile tile, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (tile.getText().equals("")) {
                moveCount--; // Decrement move count on every click

                if (mineList.contains(tile)) {
                    if (powerUpActive) {
                        if (tile.isMiniBomb) {
                            miniBombCount--;
                            tile.setText("💣");
                        }
                        if (tile.isMine) {
                            mineCount--;
                            tile.setText("➖");
                        }
                        if (tile.isBlast) {
                            blastCount--;
                            tile.setText("💥");

                            totalMinesCount--;
                        }

                        // checkMine(tile.r, tile.c);
                        mineList.remove(tile);
                        tilesClicked += 1;
                        tile.setEnabled(false);
                        updateDisplayText();
                        // powerUpActive = false;
                    } else if (tile.isBlast) {
                        revealMines();
                        gameOver = true;
                    } else if (tile.isMiniBomb) {
                        tile.setText("💣");
                        miniBombCount--;
                        tilesClicked += 1;
                        mineList.remove(tile);
                        tile.setEnabled(false);
                        moveCount = Math.max(0, moveCount - 4); // Ensure move count doesn't go negative
                        updateDisplayText();
                    } else if (tile.isMine) {
                        tile.setText("➖");
                        mineCount--;
                        tilesClicked += 1;
                        mineList.remove(tile);
                        tile.setEnabled(false);
                        moveCount = Math.max(0, moveCount - 6); // Ensure move count doesn't go negative
                        updateDisplayText();
                    }

                } else {
                    checkMine(tile.r, tile.c); // Existing check mine logic
                }

                if (moveCount <= 0 && !gameOver) {
                    gameOver = true;
                    revealMines();
                }

            }

            powerUpActive = false; // Reset power-up state
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (tile.getText().equals("") && tile.isEnabled()) {
                if (flaggedTiles.size() < flagCount) {
                    tile.setText("🚩");
                    flaggedTiles.add(tile);
                } else {
                    MineTile firstFlagged = flaggedTiles.poll();
                    firstFlagged.setText("");
                    tile.setText("🚩");
                    flaggedTiles.add(tile);
                    if (!mineList.contains(firstFlagged)) {
                        checkMine(firstFlagged.r, firstFlagged.c);
                    } else {
                        revealMines();
                    }
                }
                updateDisplayText();
            } else if (tile.getText().equals("🚩")) {
                tile.setText("");
                flaggedTiles.remove(tile);
                updateDisplayText();
            }
        }
        // System.out.println(tilesClicked);
    }

    void setMines() {
        mineList = new ArrayList<>();
        int totalMines = totalMinesCount; // Total mines based on the current difficulty setting
        int minesPerType = totalMines / 3; // Equal number of each type of mine

        // Set each type of mine (mini bomb, mine, and blast)
        setSpecificMines(minesPerType, true, false, false); // Set mini bombs
        setSpecificMines(minesPerType, false, true, false); // Set mines
        setSpecificMines(minesPerType, false, false, true); // Set blasts

        // If total mines not exactly divisible by 3, handle the remainder
        int remainder = totalMines % 3;
        if (remainder != 0) {
            setSpecificMines(remainder, true, false, false); // Add remaining mines as mini bombs (or choose another
                                                             // type if preferred)
        }
    }

    void setSpecificMines(int count, boolean isMiniBomb, boolean isMine, boolean isBlast) {
        while (count > 0) {
            int r = random.nextInt(numRows);
            int c = random.nextInt(numCols);
            MineTile tile = board[r][c];
            if (!mineList.contains(tile)) {
                mineList.add(tile);
                if (isMiniBomb) {
                    tile.isMiniBomb = true;
                    miniBombCount++;
                }
                if (isMine) {
                    tile.isMine = true;
                    mineCount++;
                }
                if (isBlast) {
                    tile.isBlast = true;
                    blastCount++;
                }
                count--;
            }
        }
    }

    void revealMines() {
        for (MineTile tile : mineList) {
            if (tile.isMiniBomb)
                tile.setText("💣");
            if (tile.isMine)
                tile.setText("➖");
            if (tile.isBlast)
                tile.setText("💥");
        }
        gameOver = true;
        textLabel.setText("👎");
        textPanel.add(textLabel, BorderLayout.CENTER);
        // updateDisplayText();
    }

    void updateDisplayText() {
        SwingUtilities.invokeLater(() -> {
            textLabel.setText(
                    "💣 " + miniBombCount + " ➖ " + mineCount + " 💥 " + blastCount + "   🚩 " + flaggedTiles.size()
                            + " Moves: " + moveCount);
            textPanel.add(textLabel, BorderLayout.CENTER);
            textPanel.revalidate();
            // textPanel.repaint();
        });
    }

    void checkMine(int r, int c) {
        if (r < 0 || r >= numRows || c < 0 || c >= numCols) {
            return;
        }

        MineTile tile = board[r][c];
        if (!tile.isEnabled()) {
            return;
        }
        tile.setEnabled(false);
        tilesClicked += 1;

        int minesFound = 0;

        minesFound += countMine(r - 1, c - 1); // top left
        minesFound += countMine(r - 1, c); // top
        minesFound += countMine(r - 1, c + 1); // top right

        minesFound += countMine(r, c - 1); // left
        minesFound += countMine(r, c + 1); // right

        minesFound += countMine(r + 1, c - 1); // bottom left
        minesFound += countMine(r + 1, c); // bottom
        minesFound += countMine(r + 1, c + 1); // bottom right

        if (minesFound > 0) {
            tile.setText(Integer.toString(minesFound));
        } else {
            tile.setText("");

            checkMine(r - 1, c - 1); // top left
            checkMine(r - 1, c); // top
            checkMine(r - 1, c + 1); // top right

            checkMine(r, c - 1); // left
            checkMine(r, c + 1); // right

            checkMine(r + 1, c - 1); // bottom left
            checkMine(r + 1, c); // bottom
            checkMine(r + 1, c + 1); // bottom right
        }

        if (tilesClicked == numRows * numCols - mineList.size()) {
            gameOver = true;
            textLabel.setText("👏");
            textPanel.add(textLabel, BorderLayout.CENTER);
        } else
            updateDisplayText();
    }

    int countMine(int r, int c) {
        if (r < 0 || r >= numRows || c < 0 || c >= numCols) {
            return 0;
        }
        if (mineList.contains(board[r][c])) {
            return 1;
        }
        return 0;
    }

    private void resetGame() {

        initializePowerUps();
        initializeBoard();
        flaggedTiles.clear();
        gameOver = false;
        setMines();
        // updateDisplayText();
    }

    private void showSetupState() {
        levelSelector.setVisible(true); // Showing level selector
        restartLabel.setVisible(false);
        minesLabel.setVisible(true);
        minesField.setVisible(true);
        startButton.setVisible(true);
        powerUpPanel.setVisible(false); // Ensuring power-up panel is hidden in setup state
        // textLabel.setVisible(false);
        textPanel.revalidate();
        // textPanel.repaint();
    }

    private void showGameState() {
        levelSelector.setVisible(false); // Hiding level selector

        restartLabel.setVisible(true);
        textPanel.add(restartLabel, BorderLayout.WEST);

        minesLabel.setVisible(false);
        minesField.setVisible(false);
        startButton.setVisible(false);
        // textLabel.setVisible(true);
        powerUpPanel.setVisible(true); // Show power-ups only in game state
        textPanel.revalidate();

    }

    public static void main(String[] args) {
        new Minesweeper();
    }
}
