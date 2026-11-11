package com.meownote.bp;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 一条血压记录。
 *
 * 高压 = 收缩压 SBP，低压 = 舒张压 DBP，心率 = HR。
 */
public class Record {

    public String id;
    /** 测量时间（毫秒） */
    public long time;
    /** 收缩压（高压） */
    public int sbp;
    /** 舒张压（低压） */
    public int dbp;
    /** 心率 */
    public int hr;
    /** 备注，如「早起」「运动后」 */
    public String note = "";

    public Record() {
        id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        time = System.currentTimeMillis();
    }

    // ---------------- 血压分级 ----------------

    /** 正常 */
    public static final int LEVEL_NORMAL = 0;
    /** 正常高值 */
    public static final int LEVEL_HIGH_NORMAL = 1;
    /** 1 级高血压 */
    public static final int LEVEL_1 = 2;
    /** 2 级高血压 */
    public static final int LEVEL_2 = 3;
    /** 3 级高血压 */
    public static final int LEVEL_3 = 4;

    /**
     * 按《中国高血压防治指南》分级（取高压/低压中更严重的那个）。
     */
    public int level() {
        return levelOf(sbp, dbp);
    }

    public static int levelOf(int sbp, int dbp) {
        if (sbp >= 180 || dbp >= 110) return LEVEL_3;
        if (sbp >= 160 || dbp >= 100) return LEVEL_2;
        if (sbp >= 140 || dbp >= 90) return LEVEL_1;
        if (sbp >= 120 || dbp >= 80) return LEVEL_HIGH_NORMAL;
        return LEVEL_NORMAL;
    }

    public String levelText() {
        switch (level()) {
            case LEVEL_NORMAL: return "正常";
            case LEVEL_HIGH_NORMAL: return "正常高值";
            case LEVEL_1: return "1 级高血压";
            case LEVEL_2: return "2 级高血压";
            default: return "3 级高血压";
        }
    }

    /** 分级对应的颜色（文字/标签） */
    public int levelColor() {
        switch (level()) {
            case LEVEL_NORMAL: return 0xFF2E7D32;        // 绿
            case LEVEL_HIGH_NORMAL: return 0xFF1565C0;   // 蓝
            case LEVEL_1: return 0xFFEF6C00;             // 橙
            case LEVEL_2: return 0xFFD84315;             // 深橙
            default: return 0xFFC62828;                  // 红
        }
    }

    /** 分级对应的浅底色 */
    public int levelBgColor() {
        switch (level()) {
            case LEVEL_NORMAL: return 0xFFE8F5E9;
            case LEVEL_HIGH_NORMAL: return 0xFFE3F2FD;
            case LEVEL_1: return 0xFFFFF3E0;
            case LEVEL_2: return 0xFFFBE9E7;
            default: return 0xFFFFEBEE;
        }
    }

    /** 心率是否在正常范围 60~100 */
    public boolean hrNormal() {
        return hr >= 60 && hr <= 100;
    }

    // ---------------- 时间显示 ----------------

    public String dateText() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(time));
    }

    public String timeText() {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(time));
    }

    /** 「今天 08:30」这种友好显示 */
    public String whenText() {
        Calendar c = Calendar.getInstance();
        int today = c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);

        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(time);
        int day = t.get(Calendar.YEAR) * 1000 + t.get(Calendar.DAY_OF_YEAR);

        String hm = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(time));
        if (day == today) return "今天 " + hm;
        if (day == today - 1) return "昨天 " + hm;
        if (t.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
            return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(time));
        }
        return new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(new Date(time));
    }

    // ---------------- JSON ----------------

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("time", time);
        o.put("sbp", sbp);
        o.put("dbp", dbp);
        o.put("hr", hr);
        o.put("note", note == null ? "" : note);
        return o;
    }

    public static Record fromJson(JSONObject o) {
        Record r = new Record();
        r.id = o.optString("id", r.id);
        r.time = o.optLong("time", System.currentTimeMillis());
        r.sbp = o.optInt("sbp", 0);
        r.dbp = o.optInt("dbp", 0);
        r.hr = o.optInt("hr", 0);
        r.note = o.optString("note", "");
        return r;
    }

    public Record copy() {
        Record r = new Record();
        r.id = id;
        r.time = time;
        r.sbp = sbp;
        r.dbp = dbp;
        r.hr = hr;
        r.note = note;
        return r;
    }
}
