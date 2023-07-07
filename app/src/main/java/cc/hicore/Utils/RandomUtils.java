package cc.hicore.Utils;

import java.util.Random;

public class RandomUtils {
    public static String GetRandomName() {
        return getRandomString(16);
    }

    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomString0(int length) {
        String str = "abcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length() - 1);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomString2(int length) {
        String str = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length() - 1);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }


    public static String GetRandomPackageName() {
        Random random = new Random();
        if (Math.random() > 0.5) {
            return getRandomString0(random.nextInt(5) + 3) + "." + getRandomString0(random.nextInt(5) + 3);
        } else {
            return getRandomString0(random.nextInt(5) + 3) + "." + getRandomString0(random.nextInt(5) + 3) + "." + getRandomString0(random.nextInt(5) + 3);
        }
    }
}
