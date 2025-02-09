package minesweeper;

import processing.core.PConstants; 
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

public class Tile {

    private boolean revealed;
    private boolean toBeRevealed;
    private boolean mine;
    private int x;
    private int y;
    private int explosionFrame;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.revealed = false;
        this.mine = false;
        this.explosionFrame = 0;
    }

    public void draw(App app, boolean isFlagged, int cellsize, int dynamicTopBar) {
        PImage darkBlueTileImg = app.getSprite("tile1", cellsize);
        PImage emptyTileImg = app.getSprite("tile", cellsize);
        PImage hoveredTileImg = app.getSprite("tile2", cellsize);
    
        // If the tile has been revealed
        if (revealed) {
            if (mine) {
                PImage explosionTileImage = app.getSprite("mine" + explosionFrame / 3, cellsize);
                app.image(explosionTileImage, x * cellsize, y * cellsize + App.TOPBAR);  // Draw the mine tile
                explosionFrame = Math.min(explosionFrame + 1, 27);
            } else {
                app.image(emptyTileImg, x * cellsize, y * cellsize + App.TOPBAR);  // Draw the empty tile
    
                int adjacentMines = countAdjacentMines(app);
                if (adjacentMines > 0) {
                    // Draw the number of adjacent mines
                    app.fill(App.mineCountColour[adjacentMines][0], App.mineCountColour[adjacentMines][1], App.mineCountColour[adjacentMines][2]);
                    float scaledTextSize = cellsize / 2.5f;
                    app.textSize(scaledTextSize);
                    app.textAlign(PConstants.CENTER, PConstants.CENTER);
                    app.text(adjacentMines, x * cellsize + cellsize / 2, y * cellsize + App.TOPBAR + cellsize / 2);
                }
            }
        } 
        // If the tile has NOT been revealed, draw the dark blue tile or hover state
        else {
            app.image(darkBlueTileImg, x * cellsize, y * cellsize + App.TOPBAR);  // Draw the unrevealed tile
    
            // Check for hovering
            if (app.mouseX >= x * cellsize && app.mouseX < (x + 1) * cellsize &&
                app.mouseY >= y * cellsize + App.TOPBAR && app.mouseY < (y + 1) * cellsize + App.TOPBAR) {
                app.image(hoveredTileImg, x * cellsize, y * cellsize + App.TOPBAR);  // Draw the hover tile
            }
        }
    
        // Handle revealing the tile based on game state
        if (this.toBeRevealed && !app.isGameEnded()) {
            this.reveal(app);
        } else if (!this.revealed && this.hasAdjacentEmptyTile(app) && !app.isGameEnded()) {
            this.toBeRevealed = true;
        }
    
        // If the tile is flagged, draw the flag on top
        if (isFlagged) {
            PImage flagImage = app.getSprite("flag", cellsize);
            app.image(flagImage, x * cellsize, y * cellsize + App.TOPBAR);
        }
    }
    

    public boolean hasMine() {
        return mine;
    }

    public void placeMine() {
        this.mine = true;
    }

    public void reveal(App app) {
        // // Allow mine tiles to explode even if the game has ended
        // if (!revealed) {  // Only reveal if the tile is not already revealed
        //     if (!mine || !app.isGameEnded()) {
                this.revealed = true;  // Reveal the tile normally if it's not a mine or if the game hasn't ended
            }
        // }
    // }
    

    public boolean isRevealed() {
        return revealed;
    }

    public boolean hasAdjacentEmptyTile(App app) {
        for (Tile t : getAdjacentTiles(app)) {
            if (t.revealed && t.countAdjacentMines(app) == 0) { //ensure the cell has no adjacent mines here
                return true;
            }
        }
        return false;
    }

    public int countAdjacentMines(App app) {
        int count = 0;
        for (Tile adjacentTile : getAdjacentTiles(app)) {
            if (adjacentTile.hasMine()) {
                count++;
            }
        }
        return count;
    }

    public List<Tile> getAdjacentTiles(App app) {
        List<Tile> adjacentTiles = new ArrayList<>();
        int[][] directions = {{1, 0}, {1, 1}, {1, -1}, {0, 1}, {0, -1}, {-1, 0}, {-1, 1}, {-1, -1}};
        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            if (newX >= 0 && newX < App.BOARD_WIDTH && newY >= 0 && newY < App.BOARD_HEIGHT) {
                adjacentTiles.add(app.getBoard()[newY][newX]);
            }
        }
        return adjacentTiles;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
