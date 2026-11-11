package com.meownote.bp;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 导出 CSV 给医生看。
 */
public class Exporter {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** 导出目录 */
    public static File dir(Context c) {
        File d = new File(c.getExternalFilesDir(null), "BpNote");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /**
     * 生成 CSV。
     *
     * 带 UTF-8 BOM，否则用 Excel 打开中文会乱码。
     *
     * @param list  要导出的记录（可为全部或某一段）
     * @param title 文件名前缀
     */
    public static File exportCsv(Context c, List<Record> list, String title) {
        StringBuilder sb = new StringBuilder();

        // BOM：Excel 认这个才知道是 UTF-8
        sb.append('\uFEFF');

        // 表头
        sb.append("日期,时间,高压(mmHg),低压(mmHg),心率(次/分),分级,备注\r\n");

        SimpleDateFormat fd = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        SimpleDateFormat ft = new SimpleDateFormat("HH:mm", Locale.CHINA);

        for (Record r : list) {
            sb.append(fd.format(new Date(r.time))).append(',');
            sb.append(ft.format(new Date(r.time))).append(',');
            sb.append(r.sbp).append(',');
            sb.append(r.dbp).append(',');
            sb.append(r.hr > 0 ? String.valueOf(r.hr) : "").append(',');
            sb.append(r.levelText()).append(',');
            sb.append(csvEscape(r.note == null ? "" : r.note));
            sb.append("\r\n");
        }

        // 末尾附一段统计，医生看得更快
        sb.append("\r\n");
        if (!list.isEmpty()) {
            int n = list.size();
            int sSbp = 0, sDbp = 0, sHr = 0, nHr = 0;
            int maxSbp = 0, minSbp = 999, maxDbp = 0, minDbp = 999;
            for (Record r : list) {
                sSbp += r.sbp; sDbp += r.dbp;
                if (r.hr > 0) { sHr += r.hr; nHr++; }
                maxSbp = Math.max(maxSbp, r.sbp);
                minSbp = Math.min(minSbp, r.sbp);
                maxDbp = Math.max(maxDbp, r.dbp);
                minDbp = Math.min(minDbp, r.dbp);
            }
            sb.append("统计,共 ").append(n).append(" 条\r\n");
            sb.append("平均,").append(sSbp / n).append("/").append(sDbp / n).append(" mmHg");
            if (nHr > 0) sb.append(",平均心率 ").append(sHr / nHr).append(" 次/分");
            sb.append("\r\n");
            sb.append("最高,").append(maxSbp).append("/").append(maxDbp).append(" mmHg\r\n");
            sb.append("最低,").append(minSbp).append("/").append(minDbp).append(" mmHg\r\n");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(new Date());
        File f = new File(dir(c), title + "_" + stamp + ".csv");
        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(sb.toString().getBytes(UTF8));
            fo.flush();
            fo.close();
            return f;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
