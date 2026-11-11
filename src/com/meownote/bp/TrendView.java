package com.meownote.bp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 血压趋势折线图（自绘，零依赖）。
 *
 * 高压红线 + 低压蓝线 + 140/90 参考虚线，X 轴按真实时间比例分布。
 */
public class TrendView extends View {

    /** 0 表示全部 */
    private int days = 7;
    private List<Record> all = new ArrayList<>();
    /** 按时间正序（画图从左到右） */
    private final List<Record> shown = new ArrayList<>();

    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRef = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int COLOR_SBP = 0xFFE53935;
    private static final int COLOR_DBP = 0xFF1E88E5;
    private static final int Y_MIN = 50;
    private static final int Y_MAX = 200;

    public TrendView(Context c) { super(c); init(); }
    public TrendView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        pLine.setStyle(Paint.Style.STROKE);
        pLine.setStrokeWidth(dp(2.2f));
        pLine.setStrokeCap(Paint.Cap.ROUND);
        pLine.setStrokeJoin(Paint.Join.ROUND);

        pDot.setStyle(Paint.Style.FILL);

        pRef.setStyle(Paint.Style.STROKE);
        pRef.setStrokeWidth(dp(1f));
        pRef.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(4)}, 0));

        pGrid.setStyle(Paint.Style.STROKE);
        pGrid.setStrokeWidth(dp(0.8f));
        pGrid.setColor(0xFFEEEEEE);

        pText.setColor(0xFF9A9A9E);
        pText.setTextSize(dp(9));
        pText.setAntiAlias(true);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setDays(int d) {
        days = d;
        rebuild();
        invalidate();
    }

    public int getDays() { return days; }

    public void setData(List<Record> list) {
        all = list == null ? new ArrayList<Record>() : list;
        rebuild();
        invalidate();
    }

    private void rebuild() {
        shown.clear();
        long now = System.currentTimeMillis();
        long from = (days <= 0) ? 0 : now - days * 24L * 3600 * 1000;

        for (Record r : all) {
            if (r.time >= from && r.sbp > 0) shown.add(r);
        }
        Collections.sort(shown, new Comparator<Record>() {
            @Override public int compare(Record a, Record b) {
                return Long.compare(a.time, b.time);
            }
        });
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        float w = getWidth(), h = getHeight();
        float padL = dp(30), padR = dp(10), padT = dp(12), padB = dp(22);
        float plotW = w - padL - padR;
        float plotH = h - padT - padB;
        if (plotW <= 0 || plotH <= 0) return;

        // ---- Y 轴刻度 + 网格 ----
        int[] ticks = {60, 90, 120, 140, 180};
        for (int tv : ticks) {
            float y = yOf(tv, padT, plotH);
            pGrid.setColor(0xFFF0F0F0);
            c.drawLine(padL, y, padL + plotW, y, pGrid);
            c.drawText(String.valueOf(tv), dp(4), y + dp(3), pText);
        }

        // ---- 参考线 140 / 90 ----
        pRef.setColor(0x66E53935);
        float y140 = yOf(140, padT, plotH);
        c.drawLine(padL, y140, padL + plotW, y140, pRef);
        pRef.setColor(0x661E88E5);
        float y90 = yOf(90, padT, plotH);
        c.drawLine(padL, y90, padL + plotW, y90, pRef);

        // ---- 参考线标签 ----
        pText.setColor(0x99E53935);
        c.drawText("140", padL + plotW - dp(22), y140 - dp(3), pText);
        pText.setColor(0x991E88E5);
        c.drawText("90", padL + plotW - dp(18), y90 - dp(3), pText);
        pText.setColor(0xFF9A9A9E);

        if (shown.isEmpty()) {
            pText.setTextSize(dp(12));
            pText.setColor(0xFFBDBDC2);
            c.drawText("还没有数据", w / 2 - dp(30), h / 2, pText);
            pText.setTextSize(dp(9));
            pText.setColor(0xFF9A9A9E);
            return;
        }

        // ---- X 坐标 ----
        long t0 = shown.get(0).time;
        long t1 = shown.get(shown.size() - 1).time;
        // 单点时给个最小跨度，避免除零
        long span = Math.max(t1 - t0, 3600_000L);

        float[] xs = new float[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            if (shown.size() == 1) {
                xs[i] = padL + plotW / 2;
            } else {
                xs[i] = padL + plotW * (shown.get(i).time - t0) / (float) span;
            }
        }

        // ---- 高压线 ----
        drawSeries(c, xs, plotH, padT, true);
        // ---- 低压线 ----
        drawSeries(c, xs, plotH, padT, false);

        // ---- X 轴日期（最多 4 个）----
        int labelCount = Math.min(4, shown.size());
        SimpleDateFormat f = new SimpleDateFormat("M/d", Locale.CHINA);
        for (int k = 0; k < labelCount; k++) {
            int idx = (labelCount == 1) ? 0
                    : (int) Math.round(k * (shown.size() - 1) / (double) (labelCount - 1));
            String s = f.format(new Date(shown.get(idx).time));
            float tw = pText.measureText(s);
            float tx = xs[idx] - tw / 2;
            tx = Math.max(padL, Math.min(tx, padL + plotW - tw));
            c.drawText(s, tx, h - dp(6), pText);
        }
    }

    private void drawSeries(Canvas c, float[] xs, float plotH, float padT, boolean sbp) {
        pLine.setColor(sbp ? COLOR_SBP : COLOR_DBP);
        pDot.setColor(sbp ? COLOR_SBP : COLOR_DBP);

        if (shown.size() == 1) {
            float y = yOf(sbp ? shown.get(0).sbp : shown.get(0).dbp, padT, plotH);
            c.drawCircle(xs[0], y, dp(3.5f), pDot);
            return;
        }

        Path path = new Path();
        for (int i = 0; i < shown.size(); i++) {
            float y = yOf(sbp ? shown.get(i).sbp : shown.get(i).dbp, padT, plotH);
            if (i == 0) path.moveTo(xs[i], y);
            else path.lineTo(xs[i], y);
        }
        c.drawPath(path, pLine);

        // 数据点：点少时全画，点多只画首尾
        int step = shown.size() <= 14 ? 1 : Math.max(1, shown.size() / 10);
        for (int i = 0; i < shown.size(); i++) {
            if (i % step != 0 && i != shown.size() - 1) continue;
            float y = yOf(sbp ? shown.get(i).sbp : shown.get(i).dbp, padT, plotH);
            c.drawCircle(xs[i], y, dp(2.6f), pDot);
        }
    }

    /** 把血压值换算成屏幕 Y */
    private float yOf(int value, float padT, float plotH) {
        float v = Math.max(Y_MIN, Math.min(Y_MAX, value));
        return padT + plotH * (1f - (v - Y_MIN) / (float) (Y_MAX - Y_MIN));
    }
}
