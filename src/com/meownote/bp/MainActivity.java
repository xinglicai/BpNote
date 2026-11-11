package com.meownote.bp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private List<Record> data = new ArrayList<>();
    /** 列表行：String = 日期分组标题，Record = 记录 */
    private final List<Object> rows = new ArrayList<>();

    private ListView list;
    private View emptyBox;
    private View pageRecord, pageTrend;
    private TextView tvTitle, tvSubtitle;
    private TextView tvAvg, tvCount, tvHr;
    private TextView tvMorning, tvDay, tvEvening, tvNight;
    private TextView tab7, tab30, tabAll;
    private TrendView trend;
    private ImageView icRecord, icTrend;
    private TextView lbRecord, lbTrend;
    private Adapter adapter;

    private int curTab = 0;   // 0=记录 1=趋势

    private static final int COLOR_ON = 0xFFE53935;
    private static final int COLOR_OFF = 0xFF9A9A9E;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.list);
        emptyBox = findViewById(R.id.empty_box);
        pageRecord = findViewById(R.id.page_record);
        pageTrend = findViewById(R.id.page_trend);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        tvAvg = findViewById(R.id.tv_avg);
        tvCount = findViewById(R.id.tv_count);
        tvHr = findViewById(R.id.tv_hr);
        tvMorning = findViewById(R.id.tv_morning);
        tvDay = findViewById(R.id.tv_day);
        tvEvening = findViewById(R.id.tv_evening);
        tvNight = findViewById(R.id.tv_night);
        trend = findViewById(R.id.trend);
        tab7 = findViewById(R.id.tab_7);
        tab30 = findViewById(R.id.tab_30);
        tabAll = findViewById(R.id.tab_all);

        icRecord = findViewById(R.id.ic_record);
        icTrend = findViewById(R.id.ic_trend);
        lbRecord = findViewById(R.id.lb_record);
        lbTrend = findViewById(R.id.lb_trend);

        adapter = new Adapter();
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Record) editRecord((Record) o);
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Record) showActions((Record) o);
                return true;
            }
        });

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editRecord(null); }
        });
        findViewById(R.id.btn_export).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportCsv(); }
        });

        findViewById(R.id.tabbar_record).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(0); }
        });
        findViewById(R.id.tabbar_trend).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchTab(1); }
        });

        tab7.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchDays(7); }
        });
        tab30.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchDays(30); }
        });
        tabAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchDays(0); }
        });

        switchDays(7);
        switchTab(0);
        reload();
    }

    // ---------------- Tab ----------------

    private void switchTab(int t) {
        curTab = t;
        pageRecord.setVisibility(t == 0 ? View.VISIBLE : View.GONE);
        pageTrend.setVisibility(t == 1 ? View.VISIBLE : View.GONE);

        icRecord.setColorFilter(t == 0 ? COLOR_ON : COLOR_OFF);
        lbRecord.setTextColor(t == 0 ? COLOR_ON : COLOR_OFF);
        icTrend.setColorFilter(t == 1 ? COLOR_ON : COLOR_OFF);
        lbTrend.setTextColor(t == 1 ? COLOR_ON : COLOR_OFF);

        tvTitle.setText(t == 0 ? "血压本" : "血压趋势");
        updateSubtitle();
    }

    private void updateSubtitle() {
        if (data.isEmpty()) {
            tvSubtitle.setText(curTab == 0 ? "记录每一次测量" : "暂无数据");
            return;
        }
        if (curTab == 0) {
            tvSubtitle.setText("共 " + data.size() + " 条记录");
        } else {
            tvSubtitle.setText("共 " + data.size() + " 条记录 · 点右上角导出给医生");
        }
    }

    // ---------------- 趋势范围 ----------------

    private void switchDays(int d) {
        trend.setDays(d);
        paintTab(tab7, d == 7);
        paintTab(tab30, d == 30);
        paintTab(tabAll, d == 0);
    }

    private void paintTab(TextView t, boolean on) {
        t.setTextColor(on ? 0xFFFFFFFF : COLOR_OFF);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setColor(on ? COLOR_ON : 0x00000000);
        t.setBackground(g);
    }

    // ---------------- 数据 ----------------

    private void reload() {
        data = Store.load(this);
        buildRows();
        adapter.notifyDataSetChanged();

        boolean has = !data.isEmpty();
        emptyBox.setVisibility(has ? View.GONE : View.VISIBLE);
        list.setVisibility(has ? View.VISIBLE : View.GONE);

        if (has) {
            trend.setData(data);
            updateSummary();
            updatePeriods();
        }
        updateSubtitle();
    }

    /** 把记录切成「日期标题 + 记录」的混合行 */
    private void buildRows() {
        rows.clear();
        String lastKey = null;
        SimpleDateFormat keyFmt = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        for (Record r : data) {
            String key = keyFmt.format(new Date(r.time));
            if (!key.equals(lastKey)) {
                rows.add(dayLabel(r.time));     // 存 String 表示日期标题
                lastKey = key;
            }
            rows.add(r);
        }
    }

    /**
     * 日期标题：
     *   今天 / 昨天 / 前天   副标题留空
     *   其他日期  主标题 M月d日，副标题星期几
     */
    private String dayLabel(long t) {
        Calendar today = Calendar.getInstance();
        Calendar d = Calendar.getInstance();
        d.setTimeInMillis(t);

        int ty = today.get(Calendar.YEAR), td = today.get(Calendar.DAY_OF_YEAR);
        int dy = d.get(Calendar.YEAR), dd = d.get(Calendar.DAY_OF_YEAR);

        if (ty == dy) {
            int diff = td - dd;
            if (diff == 0) return "今天\u0000";
            if (diff == 1) return "昨天\u0000";
            if (diff == 2) return "前天\u0000";
        }
        String[] wk = {"", "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        String main = new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(t));
        return main + "\u0000" + wk[d.get(Calendar.DAY_OF_WEEK)];
    }

    private void updateSummary() {
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
        int n = 0, sSbp = 0, sDbp = 0, sHr = 0, nHr = 0;
        for (Record r : data) {
            if (r.time < weekAgo) continue;
            n++;
            sSbp += r.sbp;
            sDbp += r.dbp;
            if (r.hr > 0) { sHr += r.hr; nHr++; }
        }
        tvAvg.setText(n == 0 ? "—" : (sSbp / n) + "/" + (sDbp / n));
        tvHr.setText(nHr == 0 ? "—" : String.valueOf(sHr / nHr));
        tvCount.setText(String.valueOf(n));
    }

    private void updatePeriods() {
        int[] n = new int[4];
        int[] sbp = new int[4];
        int[] dbp = new int[4];

        Calendar c = Calendar.getInstance();
        for (Record r : data) {
            c.setTimeInMillis(r.time);
            int h = c.get(Calendar.HOUR_OF_DAY);
            int g;
            if (h >= 6 && h < 10) g = 0;
            else if (h >= 10 && h < 18) g = 1;
            else if (h >= 18 && h < 22) g = 2;
            else g = 3;
            n[g]++; sbp[g] += r.sbp; dbp[g] += r.dbp;
        }

        setPeriod(tvMorning, n[0], sbp[0], dbp[0]);
        setPeriod(tvDay, n[1], sbp[1], dbp[1]);
        setPeriod(tvEvening, n[2], sbp[2], dbp[2]);
        setPeriod(tvNight, n[3], sbp[3], dbp[3]);
    }

    private void setPeriod(TextView tv, int n, int sbp, int dbp) {
        if (n == 0) {
            tv.setText("—");
            tv.setTextColor(0xFFBDBDC2);
            return;
        }
        int a = sbp / n, b = dbp / n;
        tv.setText(a + "/" + b);
        int lv = Record.levelOf(a, b);
        tv.setTextColor(lv >= Record.LEVEL_1 ? 0xFFC62828
                : (lv == Record.LEVEL_HIGH_NORMAL ? 0xFF1565C0 : 0xFF2E7D32));
    }

    // ---------------- 导出 CSV ----------------

    private void exportCsv() {
        if (data.isEmpty()) { toast("还没有记录可以导出"); return; }
        final String[] items = {"全部记录", "最近 7 天", "最近 30 天"};
        new AlertDialog.Builder(this)
                .setTitle("导出 CSV（给医生看）")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { doExport(which); }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doExport(int which) {
        List<Record> part = new ArrayList<>();
        long from = 0;
        String title = "血压记录_全部";
        if (which == 1) {
            from = System.currentTimeMillis() - 7L * 24 * 3600 * 1000;
            title = "血压记录_近7天";
        } else if (which == 2) {
            from = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
            title = "血压记录_近30天";
        }
        for (Record r : data) if (r.time >= from) part.add(r);
        if (part.isEmpty()) { toast("这段时间没有记录"); return; }

        File f = Exporter.exportCsv(this, part, title);
        if (f == null) { toast("导出失败"); return; }

        try {
            Uri uri = FileProvider2.forExport(f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/csv");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, f.getName());
            i.putExtra(Intent.EXTRA_TEXT, "血压记录 " + part.size() + " 条，可用 Excel 或 WPS 打开");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "发送 CSV"));
            toast("已生成：" + f.getName());
        } catch (Exception e) {
            toast("分享失败：" + e.getMessage());
        }
    }

    // ---------------- 添加 / 编辑 ----------------

    private void editRecord(final Record origin) {
        final boolean isNew = (origin == null);
        final Record r = isNew ? new Record() : origin.copy();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(6), pad, 0);

        final TextView tvTime = new TextView(this);
        tvTime.setTextSize(15);
        tvTime.setTextColor(0xFF1A1A1A);
        GradientDrawable tbg = new GradientDrawable();
        tbg.setColor(0xFFF5F6F8);
        tbg.setCornerRadius(dp(10));
        tvTime.setBackground(tbg);
        tvTime.setPadding(dp(14), dp(12), dp(14), dp(12));
        tvTime.setText("⏱  " + fmtTime(r.time));
        tvTime.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDateTime(r, tvTime); }
        });
        box.addView(tvTime, lp(0));

        final EditText etSbp = num("高压（收缩压）", r.sbp == 0 ? "" : String.valueOf(r.sbp), box, "mmHg");
        final EditText etDbp = num("低压（舒张压）", r.dbp == 0 ? "" : String.valueOf(r.dbp), box, "mmHg");
        final EditText etHr = num("心率", r.hr == 0 ? "" : String.valueOf(r.hr), box, "次/分");

        final EditText etNote = new EditText(this);
        etNote.setHint("备注（选填，如：早起 / 运动后 / 服药前）");
        etNote.setTextSize(14);
        etNote.setText(r.note);
        etNote.setSingleLine(true);
        box.addView(label("备注"));
        box.addView(etNote, lp(0));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(box);

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(isNew ? "添加记录" : "修改记录")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .setNeutralButton(isNew ? null : "删除", null)
                .create();

        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                int sbp = parseInt(etSbp);
                                int dbp = parseInt(etDbp);
                                int hr = parseInt(etHr);

                                if (sbp < 50 || sbp > 300) { toast("高压请填 50~300"); return; }
                                if (dbp < 30 || dbp > 200) { toast("低压请填 30~200"); return; }
                                if (dbp >= sbp) { toast("高压应该大于低压"); return; }
                                if (hr != 0 && (hr < 30 || hr > 250)) { toast("心率请填 30~250"); return; }

                                r.sbp = sbp; r.dbp = dbp; r.hr = hr;
                                r.note = etNote.getText().toString().trim();

                                if (isNew) {
                                    data.add(0, r);
                                } else {
                                    for (int i = 0; i < data.size(); i++) {
                                        if (data.get(i).id.equals(r.id)) { data.set(i, r); break; }
                                    }
                                }
                                Store.save(MainActivity.this, data);
                                dlg.dismiss();
                                reload();
                                toast(isNew ? "已保存" : "已更新");
                            }
                        });

                if (!isNew) {
                    Button del = ((AlertDialog) d).getButton(AlertDialog.BUTTON_NEUTRAL);
                    if (del != null) {
                        del.setTextColor(0xFFD32F2F);
                        del.setOnClickListener(new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                dlg.dismiss();
                                confirmDelete(origin);
                            }
                        });
                    }
                }
            }
        });

        dlg.show();
    }

    private void showActions(final Record r) {
        String[] items = new String[]{"修改数值", "删除这条"};
        new AlertDialog.Builder(this)
                .setTitle(r.whenText() + "   " + r.sbp + "/" + r.dbp)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) editRecord(r);
                        else confirmDelete(r);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(final Record r) {
        new AlertDialog.Builder(this)
                .setTitle("删除这条记录？")
                .setMessage(r.whenText() + "  " + r.sbp + "/" + r.dbp + " mmHg")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        for (int i = 0; i < data.size(); i++) {
                            if (data.get(i).id.equals(r.id)) { data.remove(i); break; }
                        }
                        Store.save(MainActivity.this, data);
                        reload();
                        toast("已删除");
                    }
                })
                .show();
    }

    // ---------------- 时间选择 ----------------

    private void pickDateTime(final Record r, final TextView show) {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(r.time);

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker dp, int y, int m, int day) {
                c.set(Calendar.YEAR, y);
                c.set(Calendar.MONTH, m);
                c.set(Calendar.DAY_OF_MONTH, day);

                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker tp, int hh, int mm) {
                        c.set(Calendar.HOUR_OF_DAY, hh);
                        c.set(Calendar.MINUTE, mm);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        r.time = c.getTimeInMillis();
                        show.setText("⏱  " + fmtTime(r.time));
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String fmtTime(long t) {
        return new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(new Date(t));
    }

    // ---------------- 视图辅助 ----------------

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams lp(int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = topMargin == 0 ? dp(8) : topMargin;
        return p;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(11);
        t.setTextColor(0xFF8A8A8E);
        t.setPadding(dp(2), dp(12), 0, dp(2));
        return t;
    }

    private EditText num(String title, String val, LinearLayout parent, String unit) {
        parent.addView(label(title + "   " + unit));
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint("请输入数字");
        et.setTextSize(16);
        et.setText(val);
        et.setSingleLine(true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        parent.addView(et, p);
        return et;
    }

    private int parseInt(EditText et) {
        try {
            String s = et.getText().toString().trim();
            if (s.length() == 0) return 0;
            return Integer.parseInt(s);
        } catch (Exception e) { return 0; }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---------------- 适配器：日期标题 + 记录 ----------------


    private class Adapter extends BaseAdapter {
        private static final int TYPE_DATE = 0;
        private static final int TYPE_REC = 1;

        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public int getViewTypeCount() { return 2; }

        @Override public int getItemViewType(int pos) {
            return (rows.get(pos) instanceof String) ? TYPE_DATE : TYPE_REC;
        }

        @Override public boolean isEnabled(int pos) {
            // 日期标题不可点
            return rows.get(pos) instanceof Record;
        }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            Object o = rows.get(pos);

            if (o instanceof String) {
                View v = cv;
                if (v == null || v.findViewById(R.id.tv_date) == null) {
                    v = LayoutInflater.from(MainActivity.this)
                            .inflate(R.layout.item_date, parent, false);
                }
                String[] parts = ((String) o).split("\u0000", -1);
                TextView main = v.findViewById(R.id.tv_date);
                TextView sub = v.findViewById(R.id.tv_date_sub);
                main.setText(parts[0]);
                if (parts.length > 1 && parts[1].length() > 0) {
                    sub.setVisibility(View.VISIBLE);
                    sub.setText(parts[1]);
                } else {
                    sub.setVisibility(View.GONE);
                }
                return v;
            }

            // 记录卡
            final Record r = (Record) o;
            View v = cv;
            if (v == null || v.findViewById(R.id.tv_sbp) == null) {
                v = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_record, parent, false);
            }

            TextView when = v.findViewById(R.id.tv_when);
            TextView level = v.findViewById(R.id.tv_level);
            TextView sbp = v.findViewById(R.id.tv_sbp);
            TextView dbp = v.findViewById(R.id.tv_dbp);
            TextView hr = v.findViewById(R.id.tv_hr);
            TextView note = v.findViewById(R.id.tv_note);

            // 已按日期分组，卡片上只显示时刻
            when.setText(r.timeText());

            level.setText(r.levelText());
            level.setTextColor(r.levelColor());
            GradientDrawable lg = new GradientDrawable();
            lg.setColor(r.levelBgColor());
            lg.setCornerRadius(dp(9));
            level.setBackground(lg);

            sbp.setText(String.valueOf(r.sbp));
            dbp.setText(String.valueOf(r.dbp));
            hr.setText(r.hr > 0 ? ("♥ " + r.hr) : "♥ —");
            hr.setTextColor(r.hrNormal() ? 0xFFD32F2F : 0xFFEF6C00);

            if (r.note != null && r.note.length() > 0) {
                note.setVisibility(View.VISIBLE);
                note.setText(r.note);
            } else {
                note.setVisibility(View.GONE);
            }

            // 左滑：弹出「修改数值 / 删除这条」。
            // 不用 GestureDetector.onFling —— 它依赖抬手瞬间的速度，
            // adb 生成的滑动序列常常判不出来。这里直接按位移判定，
            // 一旦确认左滑就 return true 消费事件，避免又触发点击进编辑。
            v.setOnTouchListener(new View.OnTouchListener() {
                float downX, downY;
                boolean fired;

                @Override
                public boolean onTouch(View view, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = e.getX();
                            downY = e.getY();
                            fired = false;
                            return false;     // 不消费，点击/长按/竖向滚动照常
                        case MotionEvent.ACTION_MOVE:
                            if (!fired) {
                                float dx = e.getX() - downX;
                                float dy = e.getY() - downY;
                                // 向左、且水平位移明显大于垂直（排除竖向滚动）
                                if (dx < -dp(55) && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                                    fired = true;
                                    showActions((Record) rows.get(rows.indexOf(r)));
                                    return true;
                                }
                            }
                            return false;
                        default:
                            return false;
                    }
                }
            });

            return v;
        }
    }
}
