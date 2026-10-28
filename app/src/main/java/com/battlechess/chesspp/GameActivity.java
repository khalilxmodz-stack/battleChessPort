package com.battlechess.chesspp;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import GameEntities.Abilities.Ability;
import GameEntities.Pieces.Piece;
import GameLogic.GameManager;

public class GameActivity extends Activity {

    public static final String EXTRA_MODE = "mode";

    private GameManager manager;
    private BoardView board;
    private TextView turnText, infoText, descText;
    private Button skillButton;

    private int selX = -1, selY = -1;      // selected piece, -1 = none
    private boolean targeting = false;     // waiting for an ability target tap

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean assassination = getIntent().getBooleanExtra(EXTRA_MODE, true);
        manager = new GameManager(assassination ? GameManager.GameMode.ASSASSINATION : GameManager.GameMode.ELIMINATION);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        int pad = dp(8);
        root.setPadding(pad, pad, pad, pad);

        turnText = new TextView(this);
        turnText.setTextSize(20);
        turnText.setGravity(Gravity.CENTER);
        turnText.setTextColor(Color.BLACK);
        root.addView(turnText, new LinearLayout.LayoutParams(-1, -2));

        board = new BoardView(this, manager);
        board.setOnCellTapListener(new BoardView.OnCellTapListener() {
            @Override public void onCellTap(int row, int col) { GameActivity.this.onCellTap(row, col); }
            @Override public int onPieceGrab(int row, int col) { return GameActivity.this.onPieceGrab(row, col); }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dp(6), 0, dp(6));
        root.addView(board, bp);

        infoText = new TextView(this);
        infoText.setTextSize(16);
        infoText.setTextColor(Color.BLACK);
        root.addView(infoText, new LinearLayout.LayoutParams(-1, -2));

        descText = new TextView(this);
        descText.setTextSize(13);
        descText.setTextColor(Color.DKGRAY);
        root.addView(descText, new LinearLayout.LayoutParams(-1, -2));

        skillButton = new Button(this);
        skillButton.setText("Use Ability");
        skillButton.setVisibility(View.GONE);
        skillButton.setOnClickListener(v -> onSkillButton());
        root.addView(skillButton, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        refresh();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private void onCellTap(int x, int y) {
        Piece tapped = manager.getGameBoard().getPiece(x, y);

        if (targeting) {
            if (x == selX && y == selY) {
                targeting = false; // tapping the caster cancels
            } else if (manager.useAbility(selX, selY, x, y)) {
                targeting = false;
                clearSelection();
            } else {
                // invalid target: stay in targeting mode
            }
            refresh();
            checkEnd();
            return;
        }

        if (selX >= 0) {
            Piece sel = manager.getGameBoard().getPiece(selX, selY);
            boolean sameSquare = (x == selX && y == selY);
            boolean ownPiece = tapped != null && sel != null && tapped.getColor() == sel.getColor();
            if (sameSquare) {
                clearSelection();
            } else if (ownPiece) {
                select(x, y);                 // switch selection to another friendly piece
            } else {
                manager.action(selX, selY, x, y);
                clearSelection();
            }
        } else if (tapped != null) {
            select(x, y);
        }
        refresh();
        checkEnd();
    }

    /** Finger pressed on a piece: select it so it can be dragged. */
    private int onPieceGrab(int x, int y) {
        if (manager.isGameOver() || targeting) return 0;
        Piece p = manager.getGameBoard().getPiece(x, y);
        if (p == null || p.getColor() != manager.getTurn()) return 0;
        if (x == selX && y == selY) return 2;
        select(x, y);
        refresh();
        return 1;
    }

    private void select(int x, int y) { selX = x; selY = y; targeting = false; }

    private void clearSelection() { selX = -1; selY = -1; targeting = false; }

    private void onSkillButton() {
        if (selX < 0) return;
        Piece p = manager.getGameBoard().getPiece(selX, selY);
        if (p == null) return;
        Ability a = p.getAbility();
        if (a == null || a.isPassive() || a.getCooldownLeft() != 0 || p.getColor() != manager.getTurn()) return;
        if (a.isTargeted()) {
            targeting = true;
        } else {
            manager.useAbility(selX, selY);
            clearSelection();
        }
        refresh();
        checkEnd();
    }

    private void refresh() {
        board.clearMarks();
        boolean white = manager.getTurn() == 1;
        turnText.setText(white ? "White's turn" : "Black's turn");
        turnText.setTextColor(white ? Color.GRAY : Color.BLACK);

        Piece p = selX >= 0 ? manager.getGameBoard().getPiece(selX, selY) : null;
        if (p == null) {
            clearSelection();
            infoText.setText("Tap a piece to see its stats and moves.");
            descText.setText("");
            skillButton.setVisibility(View.GONE);
        } else {
            Ability a = p.getAbility();
            String cd = a.isPassive() ? "Passive" : String.valueOf(a.getCooldownLeft());
            infoText.setText(p.getClass().getSimpleName() + "  HP " + p.getHP() + "  AP " + p.getAP() + "  CD " + cd);
            descText.setText(a.getName() + ": " + a.getDesc());
            board.mark(selX, selY, BoardView.SELECTED);

            boolean myPiece = p.getColor() == manager.getTurn();
            boolean usable = myPiece && !a.isPassive() && a.getCooldownLeft() == 0;
            skillButton.setVisibility(usable ? View.VISIBLE : View.GONE);
            skillButton.setText(targeting ? "Tap a target (pink)" : "Use Ability");

            if (myPiece) {
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        if (!p.isValid(selX, selY, i, j)) continue;
                        if (targeting) {
                            board.mark(i, j, BoardView.SKILL);
                            continue;
                        }
                        if (!manager.checkPath(selX, selY, i, j)) continue;
                        Piece t = manager.getGameBoard().getPiece(i, j);
                        if (t == null) board.mark(i, j, BoardView.MOVE);
                        else if (t.getColor() != p.getColor()) board.mark(i, j, BoardView.ATTACK);
                    }
                }
            }
        }
        board.invalidate();
    }

    private void checkEnd() {
        if (!manager.isGameOver()) return;
        String who = manager.getWinner() == 1 ? "White" : "Black";
        new AlertDialog.Builder(this)
                .setTitle("Game over")
                .setMessage(who + " wins!")
                .setCancelable(false)
                .setPositiveButton("Back to menu", (d, w) -> finish())
                .show();
    }
}
