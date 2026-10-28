package GameLogic;

import GameEntities.Pieces.King;
import GameEntities.Pieces.Knight;
import GameEntities.Pieces.Pawn;
import GameEntities.Pieces.Piece;

/**
 * Android port of the original GameManager. Rules are unchanged; the Swing
 * GameFrame dependency was removed and replaced with isGameOver()/getWinner().
 */
public class GameManager
{
    private GameMode gameMode;
    private GameBoard gameBoard;
    private int turn;
    private boolean gameOver = false;
    private int winner = -1; // 0 = black, 1 = white, -1 = none

    public enum GameMode {
        ELIMINATION, ASSASSINATION
    }

    public GameManager(GameMode gameMode)
    {
        this.gameMode = gameMode;
        gameBoard = new GameBoard();
        turn = 1;
    }

    private void checkGameOver()
    {
        boolean whiteAlive = false;
        boolean blackAlive = false;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = gameBoard.getPiece(i, j);
                if (p == null) continue;
                if (gameMode == GameMode.ASSASSINATION && !(p instanceof King)) continue;
                if (p.getColor() == 0) blackAlive = true;
                if (p.getColor() == 1) whiteAlive = true;
            }
        }
        if (!whiteAlive || !blackAlive) {
            gameOver = true;
            winner = whiteAlive ? 1 : 0;
        }
    }

    public boolean isGameOver() { return gameOver; }

    public int getWinner() { return winner; }

    public GameMode getGameMode() { return gameMode; }

    /** Moves if the target is empty, attacks otherwise. Returns true if a turn was used. */
    public boolean action(int curX, int curY, int tarX, int tarY)
    {
        if (gameOver) return false;
        if (gameBoard.getPiece(tarX, tarY) == null) {
            return move(curX, curY, tarX, tarY);
        }
        return attack(curX, curY, tarX, tarY);
    }

    public boolean useAbility(int... coordinates) {
        if (gameOver) return false;
        Piece p = gameBoard.getPiece(coordinates[0], coordinates[1]);
        if (p == null || p.getColor() != getTurn()) {
            return false;
        }
        if (p.getAbility().useAbility(gameBoard, coordinates)) {
            endTurn();
            return true;
        }
        return false;
    }

    private boolean move(int curX, int curY, int tarX, int tarY)
    {
        Piece selectedPiece = gameBoard.getPiece(curX, curY);

        if (selectedPiece == null) return false;
        if (!selectedPiece.isValid(curX, curY, tarX, tarY)) return false;
        if (getTurn() != selectedPiece.getColor()) return false;
        if (!checkPath(curX, curY, tarX, tarY)) return false;

        gameBoard.movePiece(curX, curY, tarX, tarY);
        // Pawn promotion
        Piece moved = gameBoard.getPiece(tarX, tarY);
        if (moved instanceof Pawn) {
            if ((moved.getColor() == 1 && tarX == 0) || (moved.getColor() == 0 && tarX == 7)) {
                gameBoard.promotePawn(tarX, tarY);
            }
        }
        endTurn();
        return true;
    }

    private boolean attack(int curX, int curY, int tarX, int tarY)
    {
        Piece selectedPiece = gameBoard.getPiece(curX, curY);
        Piece enemyPiece = gameBoard.getPiece(tarX, tarY);

        if (selectedPiece == null || enemyPiece == null) return false;
        if (!selectedPiece.isValid(curX, curY, tarX, tarY)) return false;
        if (selectedPiece.getColor() == enemyPiece.getColor()) return false;
        if (getTurn() != selectedPiece.getColor()) return false;
        if (!checkPath(curX, curY, tarX, tarY)) return false;

        enemyPiece.changeHP(-(selectedPiece.getAP()));
        endTurn();
        return true;
    }

    private void endTurn()
    {
        gameBoard.cleanBoard();
        checkGameOver();
        turn++;
    }

    public boolean checkPath(int curX, int curY, int tarX, int tarY)
    {
        if (gameBoard.getPiece(curX, curY) instanceof Knight)
            return true;

        boolean pathClear = true;
        int i = curX;
        int j = curY;

        if (tarX > curX) i = curX + 1;
        if (tarX < curX) i = curX - 1;
        if (tarY > curY) j = curY + 1;
        if (tarY < curY) j = curY - 1;

        while (i != tarX || j != tarY)
        {
            if (gameBoard.isOccupied(i, j))
            {
                pathClear = false;
            }
            if (tarX > curX) i++;
            if (tarX < curX) i--;
            if (tarY > curY) j++;
            if (tarY < curY) j--;
        }
        return pathClear;
    }

    public int getTurn()
    {
        return turn % 2;
    }

    public GameBoard getGameBoard()
    {
        return gameBoard;
    }
}
