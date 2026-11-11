package com.meownote.bp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 数据层：所有记录存在应用私有目录的 records.json 里。
 * 不需要任何存储权限。
 */
public class Store {

    private static final String DIR = "BpNote";
    private static final String FILE = "records.json";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static File file(Context c) {
        File dir = new File(c.getExternalFilesDir(null), DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, FILE);
    }

    /** 读全部记录，按时间倒序 */
    public static List<Record> load(Context c) {
        List<Record> out = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return out;

        try {
            String s = readAll(f);
            if (s == null || s.trim().length() == 0) return out;
            JSONArray arr = new JSONArray(s);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(Record.fromJson(o));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(out, new Comparator<Record>() {
            @Override
            public int compare(Record a, Record b) {
                return Long.compare(b.time, a.time);   // 新的在上
            }
        });
        return out;
    }

    public static boolean save(Context c, List<Record> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Record r : list) arr.put(r.toJson());
            File f = file(c);
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(arr.toString().getBytes(UTF8));
            fo.flush();
            fo.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String readAll(File f) throws IOException {
        InputStream in = new java.io.FileInputStream(f);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return new String(bo.toByteArray(), UTF8);
    }
}
