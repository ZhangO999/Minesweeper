package minesweeper;

import org.checkerframework.checker.units.qual.A;

import processing.core.PApplet;
import processing.core.PImage;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.data.JSONArray;
import processing.data.JSONObject;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.io.*;

public class App extends PApplet {

    // public static final int CELLSIZE = 32;   // 32  --> size of each cell
    public static final int TOPBAR = 64;   // 64
    public static final int WIDTH = 864; // 864   --> width of the window
    public static final int HEIGHT = 640; // 640  --> height of window
    public static int CELLSIZE;

    public static int BOARD_WIDTH;
    public static int BOARD_HEIGHT;
    public static final int FPS = 30;   
    public static Random random = new Random();

    public static int[][] mineCountColour = new int[][] {
        {0, 0, 0},    // Black for 0 adjacent mines (not used in most versions)
        {0, 0, 255},  // Blue for 1 adjacent mine
        {0, 133, 0},  // Green for 2 adjacent mines
        {255, 0, 0},  // Red for 3 adjacent mines
        {0, 0, 132},  // Dark blue for 4 adjacent mines
        {132, 0, 0},  // Dark red for 5 adjacent mines
        {0, 132, 132},// Cyan for 6 adjacent mines
        {132, 0, 132},// Magenta for 7 adjacent mines
        {32, 32, 32}  // Dark gray for 8 adjacent mines
    };

    public String configPath;
    private Tile[][] board;
    private HashMap<String, PImage> sprites = new HashMap<>();
    private int mineCount;
    private boolean gameEnded;
    private int timer;
    private int frameCounter;
    private boolean[][] flaggedTiles;
    private List<Tile> minesToExplode;
    private int explosionFrameCounter = 0;

    private String warningMessage = "";  // To hold the warning message
    private int messageTimer = 0;        // To track how long the message should be shown


    public static String[] passedArgs;

    public App() {
        this.configPath = "config.json";
        this.mineCount = 100;  // Default set to 100 mines upon initialization of a game 
    }

    @Override
    public void settings() {
        size(WIDTH, HEIGHT);
        CELLSIZE = PApplet.min(WIDTH / 10, (HEIGHT - TOPBAR) / 10);  // scaling for a nice display

        // Make sure CELLSIZE is not zero
        if (CELLSIZE <= 0) CELLSIZE = 1;

        // Now calculate BOARD_WIDTH and BOARD_HEIGHT based on CELLSIZE
        BOARD_WIDTH = WIDTH / CELLSIZE;
        BOARD_HEIGHT = (HEIGHT - TOPBAR) / CELLSIZE;

        int totalTiles = BOARD_WIDTH * BOARD_HEIGHT;

        if (passedArgs != null && passedArgs.length > 0) {
            try {
                int requestedMines = Integer.parseInt(passedArgs[0]);
                if (requestedMines >= totalTiles) {
                    this.warningMessage = "Too many mines requested. Defaulting to 100 mines.";
                    messageTimer = 120; // show msg for 120 frames ~4s at 30fps
                } else {
                    this.mineCount = requestedMines;
                }
            } 
            catch (NumberFormatException e) {
                this.mineCount = 100; // default in the case of invalid input
                this.warningMessage = "Invalid input. Defaulting to 100 mines.";
            }
        }
        size(WIDTH, HEIGHT);
    }

    public PImage getSprite(String s, int cellSize) {
        PImage result = sprites.get(s);
        if (result == null) {
            result = loadImage(this.getClass().getResource(s + ".png").getPath().toLowerCase().replace("%20", " "));
            result.resize(cellSize, cellSize); // Resize based on the new cell size
            sprites.put(s, result);
        }
        return result;
    }

    public Tile[][] getBoard() {
        return this.board;
    }

    @Override
    public void setup() {
        frameRate(FPS);
        
        String[] spriteNames = new String[]{"tile1", "tile2", "flag", "tile"};
        for (String sprite : spriteNames) {
            getSprite(sprite, CELLSIZE);  // Load sprites with dynamic scaling
        }
        for (int i = 0; i < 10; i++) {
            getSprite("mine" + i, CELLSIZE);  // Load mine sprites
        }
        resetBoard();
    }
    

    public void resetBoard() {
        this.board = new Tile[BOARD_HEIGHT][BOARD_WIDTH];
        this.flaggedTiles = new boolean[BOARD_HEIGHT][BOARD_WIDTH];
        this.minesToExplode = new ArrayList<>();
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                this.board[y][x] = new Tile(x, y);
                flaggedTiles[y][x] = false;
            }
        }

        for (int i = 0; i < mineCount; i++) {
            int x, y;
            do {
                x = random.nextInt(BOARD_WIDTH);
                y = random.nextInt(BOARD_HEIGHT);
            } while (board[y][x].hasMine());
            board[y][x].placeMine();
        }
        this.gameEnded = false;
        this.timer = 0;
        this.frameCounter = 0;
        this.explosionFrameCounter = 0;
    }

    @Override
    public void draw() {
        background(200, 200, 200);
        int nonRevealedNonMines = 0;

        // Dynamically scale the TOPBAR
        int dynamicTopBar = getDynamicTopBarHeight(); 

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                Tile tile = board[y][x];
                tile.draw(this, flaggedTiles[y][x], CELLSIZE, dynamicTopBar);  // Pass scaled size to tile draw
                if (tile.isRevealed() && tile.countAdjacentMines(this) == 0) {
                    revealAdjacentTiles(x, y);
                }
                if (!tile.isRevealed() && !tile.hasMine()) {
                    nonRevealedNonMines++;
                }
            }
        }

        // EXPLOSION HANDLING:
        if (gameEnded && !minesToExplode.isEmpty()) {  // if "gaming-ending" sequence is still running:
            if (explosionFrameCounter % 3 == 0) {
                Tile mineToExplode = minesToExplode.remove(0);
                mineToExplode.reveal(this);
            }
            explosionFrameCounter++;
        }

        // Permanent message in top-left corner
        textSize(CELLSIZE / 3);  // Dynamically scale
        fill(0);
        textAlign(LEFT, TOP);
        text("Tiles: " + (BOARD_WIDTH * BOARD_HEIGHT) + "\nMines added: " + mineCount, 10, 10);  // Display tiles and mines

        // Calculate dynamically scaled text size and position for win/lose messages
        int scaledTextSize = CELLSIZE/2;  // Text size based on cell size
        textSize(scaledTextSize);
        fill(0);
        textAlign(CENTER, CENTER);  // Center the text

        // Display the win/lose msg
        if (nonRevealedNonMines == 0) {
            text("You win!", WIDTH / 2, dynamicTopBar / 3);  // Dynamically position the win message
            gameEnded = true;
        } else if (gameEnded) {
            text("You lose!", WIDTH / 2, dynamicTopBar / 3);  // Dynamically position the lose message
        }

        // Display the warning messages
        if (messageTimer > 0) {
            textSize(CELLSIZE / 2);  // Smaller warning text
            fill(255, 0, 0);  // Red color for warning messages
            textAlign(LEFT, CENTER);
            text(this.warningMessage, 10, dynamicTopBar - 30);  // Adjust based on TOPBAR
            messageTimer--;  // Decrease timer every sec to eventually disappear
        }

        // Display the timer on the top right
        fill(0);
        textSize(CELLSIZE / 3);
        textAlign(RIGHT, CENTER);
        text("Time: " + timer, WIDTH - 10, dynamicTopBar / 3);

        if (frameCounter % FPS == 0 && !gameEnded) {
            timer++;
        }
        frameCounter++;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (gameEnded) return;
        int mouseX = e.getX();
        int mouseY = e.getY() - App.TOPBAR;
        if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT - TOPBAR) {
            Tile t = board[mouseY / App.CELLSIZE][mouseX / App.CELLSIZE];  // the CLICKED TILE

            if (e.getButton() == LEFT) {
                if (!flaggedTiles[mouseY / App.CELLSIZE][mouseX / App.CELLSIZE]) {  // if the left-clicked tile is not flagged: 
                    t.reveal(this);
                    if (t.hasMine()) {
                        triggerMineExplosion();
                    } 
                    
                    // else if (t.countAdjacentMines(this) == 0) {
                    //     revealAdjacentTiles(mouseX / App.CELLSIZE, mouseY / App.CELLSIZE);
                    // }
                }
            } else if (e.getButton() == RIGHT) {
                if (!t.isRevealed()) {
                    flaggedTiles[mouseY / App.CELLSIZE][mouseX / App.CELLSIZE] = !flaggedTiles[mouseY / App.CELLSIZE][mouseX / App.CELLSIZE];
                }
            }
        }
    }

    public void triggerMineExplosion() {
        // Add all mine tiles to the list of mines to explode
        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                Tile tile = board[y][x];
                if (tile.hasMine()) {
                    minesToExplode.add(tile);
                }
                flaggedTiles[y][x] = false; // Unflag tiles at the end of the game
            }
        }
        gameEnded = true;  // End the game, BUT allow mines to keep exploding
    }


    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getKey() == 'r' || event.getKey() == 'R') {
            resetBoard();
        }
    }

    public void revealAdjacentTiles(int x, int y) {
        Tile tile = board[y][x];
        if (!tile.isRevealed()) {
            tile.reveal(this);
            if (tile.countAdjacentMines(this) == 0) {
                for (Tile adjacentTile : tile.getAdjacentTiles(this)) {
                    if (!adjacentTile.isRevealed() && !adjacentTile.hasMine()) {
                        revealAdjacentTiles(adjacentTile.getX(), adjacentTile.getY());
                    }
                }
            }
        }
    }

    public boolean isGameEnded() {
        return gameEnded;
    }
    
    // Dynamically scaled TOPBAR
    public int getDynamicTopBarHeight() {
    return PApplet.max(CELLSIZE * 2, 64);  // Set minimum height for the TOPBAR (e.g., 64) but scale it dynamically
    }   


    public static void main(String[] args) {
        passedArgs = args;
        PApplet.main("minesweeper.App");
    }
}
