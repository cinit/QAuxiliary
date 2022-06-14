package com.hicore.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    public static String ReadFileString(File f) {
        try {
            FileInputStream fInp = new FileInputStream(f);
            String Content = new String(readAllBytes(fInp), StandardCharsets.UTF_8);
            fInp.close();
            return Content;
        } catch (Exception e) {
            return null;
        }
    }
    public static String ReadFileString(String f) {
        return ReadFileString(new File(f));
    }
    public static void copy(String source, String dest) {

        try {

            File f = new File(dest);
            f = f.getParentFile();
            if (!f.exists()) {
                f.mkdirs();
            }

            File aaa = new File(dest);
            if (aaa.exists()) {
                aaa.delete();
            }

            InputStream in = new FileInputStream(new File(source));
            OutputStream out = new FileOutputStream(new File(dest));
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
        } finally {
        }
    }

    public static byte[] readAllBytes(InputStream inp) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inp.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
