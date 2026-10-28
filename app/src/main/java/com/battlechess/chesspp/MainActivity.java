package com.battlechess.chesspp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(24, 24, 28));
        int pad = dp(16);
        root.setPadding(pad, dp(32), pad, pad);

        ImageView logo = new ImageView(this);
        logo.setImageBitmap(loadAsset("chess.png"));
        root.addView(logo, new LinearLayout.LayoutParams(dp(110), dp(110)));

        TextView title = new TextView(this);
        title.setText("ChessBattlePort");
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView credit = new TextView(this);
        credit.setText("By KhalilXmodz\u2122");
        credit.setTextSize(18);
        credit.setTextColor(Color.rgb(255, 190, 60));
        credit.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(4), 0, dp(24));
        root.addView(credit, cp);

        TextView pick = new TextView(this);
        pick.setText("Choose a game mode");
        pick.setTextSize(16);
        pick.setTextColor(Color.LTGRAY);
        pick.setGravity(Gravity.CENTER);
        root.addView(pick, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(modeButton("elimination.png", false, "Elimination"), cell());
        row.addView(modeButton("assassination.png", true, "Assassination"), cell());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(12), 0, dp(12));
        root.addView(row, rp);

        TextView hint = new TextView(this);
        hint.setText("Elimination: destroy every enemy piece.\nAssassination: kill the enemy King.\n\nDrag a piece or tap it, then tap where to go.");
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(Color.GRAY);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(24, 24, 28));
        scroll.addView(root);
        setContentView(scroll);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(150), dp(150));
        lp.setMargins(dp(6), 0, dp(6), 0);
        return lp;
    }

    private Bitmap loadAsset(String name) {
        try (InputStream in = getAssets().open(name)) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    private ImageButton modeButton(String asset, boolean assassination, String label) {
        ImageButton b = new ImageButton(this);
        b.setImageBitmap(loadAsset(asset));
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.setBackgroundColor(Color.LTGRAY);
        b.setContentDescription(label);
        b.setOnClickListener(v -> {
            Intent i = new Intent(this, GameActivity.class);
            i.putExtra(GameActivity.EXTRA_MODE, assassination);
            startActivity(i);
        });
        return b;
    }
}
