package cc.hicore.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import cc.hicore.Env;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static String getResizePicPath(String sourcePath,int max){
        String hash = DataUtils.getStrMD5(sourcePath);
        String destPath = Env.app_save_path + "Cache/resize/" + hash;
        if (new File(destPath).exists())return destPath;
        new File(Env.app_save_path + "Cache/resize/").mkdirs();
        resizeImage(sourcePath,max,max,destPath);
        return destPath;
    }
    public static void resizeImage(String imagePath, int maxWidth, int maxHeight, String outputFilePath) {
        // 加载原始图片并获取其宽高信息
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        // 计算缩放比例
        float scaleFactor = Math.min((float) maxWidth / imageWidth, (float) maxHeight / imageHeight);

        // 根据缩放比例创建 Matrix 对象
        Matrix matrix = new Matrix();
        matrix.postScale(scaleFactor, scaleFactor);

        // 使用 Matrix 对象进行缩放操作
        Bitmap scaledBitmap = BitmapFactory.decodeFile(imagePath);
        Bitmap resizedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, imageWidth, imageHeight, matrix, true);

        // 输出至文件
        FileOutputStream outputStream = null;
        try {
            File outputFile = new File(outputFilePath);
            outputStream = new FileOutputStream(outputFile);
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
