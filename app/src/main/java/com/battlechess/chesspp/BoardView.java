package com.battlechess.chesspp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import GameEntities.Pieces.Bishop;
import GameEntities.Pieces.King;
import GameEntities.Pieces.Knight;
import GameEntities.Pieces.Pawn;
import GameEntities.Pieces.Piece;
import GameEntities.Pieces.Queen;
import GameEntities.Pieces.Rook;
import GameLogic.GameManager;

/** Draws the 8x8 board. Row index = first board index (i), column = second (j). */
public class BoardView extends View {

    public interface OnCellTapListener {
        void onCellTap(int row, int col);
        /** Finger went down on a piece. Return 0 = don't drag, 1 = newly selected, 2 = already selected. */
        int onPieceGrab(int row, int col);
    }

    public static final int NONE = 0, MOVE = 1, ATTACK = 2, SKILL = 3, SELECTED = 4;

    private final GameManager manager;
    private final int[][] marks = new int[8][8];
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private OnCellTapListener listener;

    // drag state
    private boolean dragging = false;
    private int grabState = 0;
    private int downRow = -1, downCol = -1;
    private float dragX, dragY;

    public BoardView(Context context, GameManager manager) {
        super(context);
        this.manager = manager;
    }

    public void setOnCellTapListener(OnCellTapListener l) { listener = l; }

    public void clearMarks() {
        for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) marks[i][j] = NONE;
    }

    public void mark(int row, int col, int type) { marks[row][col] = type; }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        int size = (MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED) ? w : Math.min(w, h);
        setMeasuredDimension(size, size);
    }

    private Bitmap bitmapFor(Piece p) {
        String name = (p.getColor() == 1 ? "w" : "b");
        if (p instanceof King) name += "k";
        else if (p instanceof Queen) name += "q";
        else if (p instanceof Knight) name += "n";
        else if (p instanceof Rook) name += "r";
        else if (p instanceof Bishop) name += "b";
        else if (p instanceof Pawn) name += "p";
        Bitmap bmp = bitmaps.get(name);
        if (bmp == null) {
            try (InputStream in = getContext().getAssets().open(name + ".png")) {
                bmp = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                return null;
            }
            bitmaps.put(name, bmp);
        }
        return bmp;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cell = getWidth() / 8f;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                rect.set(j * cell, i * cell, (j + 1) * cell, (i + 1) * cell);

                int base = ((i + j) % 2 == 0) ? Color.rgb(170, 170, 170) : Color.rgb(128, 128, 128);
                switch (marks[i][j]) {
                    case MOVE: base = Color.rgb(80, 200, 90); break;
                    case ATTACK: base = Color.rgb(220, 70, 70); break;
                    case SKILL: base = Color.rgb(255, 150, 190); break;
                    case SELECTED: base = Color.rgb(240, 220, 90); break;
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(base);
                canvas.drawRect(rect, paint);

                Piece p = manager.getGameBoard().getPiece(i, j);
                if (p != null && !(dragging && i == downRow && j == downCol)) {
                    Bitmap bmp = bitmapFor(p);
                    if (bmp != null) {
                        float pad = cell * 0.08f;
                        rect.set(j * cell + pad, i * cell + pad, (j + 1) * cell - pad, (i + 1) * cell - pad - cell * 0.1f);
                        canvas.drawBitmap(bmp, null, rect, paint);
                    }
                    drawHpBar(canvas, p, j * cell, i * cell, cell);
                }
            }
        }
        drawDragged(canvas, cell);
    }

    private void drawDragged(Canvas canvas, float cell) {
        if (!dragging) return;
        Piece p = manager.getGameBoard().getPiece(downRow, downCol);
        if (p == null) return;
        Bitmap bmp = bitmapFor(p);
        if (bmp == null) return;
        float half = cell * 0.65f;
        rect.set(dragX - half, dragY - half * 1.3f, dragX + half, dragY + half * 0.7f);
        canvas.drawBitmap(bmp, null, rect, paint);
    }

    private void drawHpBar(Canvas canvas, Piece p, float x, float y, float cell) {
        float barH = cell * 0.08f;
        float left = x + cell * 0.1f, right = x + cell * 0.9f;
        float top = y + cell - barH - cell * 0.04f;
        float frac = Math.max(0f, Math.min(1f, p.getHP() / (float) p.getMaxHP()));
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(left, top, right, top + barH, paint);
        paint.setColor(frac > 0.5f ? Color.rgb(60, 200, 60) : frac > 0.25f ? Color.rgb(230, 190, 40) : Color.rgb(220, 50, 50));
        canvas.drawRect(left, top, left + (right - left) * frac, top + barH, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float cell = getWidth() / 8f;
        int col = (int) Math.floor(e.getX() / cell);
        int row = (int) Math.floor(e.getY() / cell);
        boolean inside = e.getX() >= 0 && e.getY() >= 0 && row >= 0 && row < 8 && col >= 0 && col < 8;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downRow = row;
                downCol = col;
                grabState = 0;
                if (inside && listener != null
                        && manager.getGameBoard().getPiece(row, col) != null) {
                    grabState = listener.onPieceGrab(row, col);
                    if (grabState != 0) {
                        dragging = true;
                        dragX = e.getX();
                        dragY = e.getY();
                    }
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    dragX = e.getX();
                    dragY = e.getY();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                dragging = false;
                invalidate();
                if (inside && listener != null) {
                    boolean sameCell = (row == downRow && col == downCol);
                    if (grabState == 0 || !sameCell || grabState == 2) {
                        listener.onCellTap(row, col);
                    }
                    performClick();
                }
                grabState = 0;
                return true;

            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                grabState = 0;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() { return super.performClick(); }
}
