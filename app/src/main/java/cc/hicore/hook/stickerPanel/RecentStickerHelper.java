package cc.hicore.hook.stickerPanel;

import cc.hicore.Env;
import cc.hicore.Utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RecentStickerHelper {
    public static List<RecentItemInfo> getAllRecentRecord() {
        try {
            removeUnavailableItem();
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            List<RecentItemInfo> items = new ArrayList<>();
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                RecentItemInfo localPath = new RecentItemInfo();
                localPath.MD5 = path.getString("MD5");
                localPath.fileName = path.getString("fileName");
                localPath.addTime = path.getLong("addTime");
                localPath.url = path.getString("url");
                localPath.pathName = path.getString("pathName");
                localPath.thumbName = path.optString("thumbName");
                localPath.thumbUrl = path.optString("thumbUrl");

                items.add(localPath);
            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static void removeUnavailableItem(){
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                RecentItemInfo localPath = new RecentItemInfo();
                localPath.MD5 = path.getString("MD5");
                localPath.fileName = path.getString("fileName");
                localPath.addTime = path.getLong("addTime");
                localPath.url = path.getString("url");
                localPath.pathName = path.getString("pathName");
                localPath.thumbName = path.optString("thumbName");
                localPath.thumbUrl = path.optString("thumbUrl");

                String localFilePath = LocalDataHelper.getLocalItemPath(localPath);
                if (!new File(localFilePath).exists()) {
                    removeContainOrLast(localPath);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanAllRecentRecord() {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject();
            JSONArray pathList = new JSONArray();
            pathJson.put("items", pathList);
            FileUtils.writeToFile(pathSetDir, pathJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createIfFileNotContain() {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            if (!new File(pathSetDir).exists()) {
                JSONObject pathJson = new JSONObject();
                JSONArray pathList = new JSONArray();
                pathJson.put("items", pathList);
                FileUtils.writeToFile(pathSetDir, pathJson.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addPicItemToRecentRecord(LocalDataHelper.LocalPath bandPath, LocalDataHelper.LocalPicItems item) {
        try {
            createIfFileNotContain();
            removeContainOrLast(item);
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");

            JSONObject newItem = new JSONObject();
            newItem.put("MD5", item.MD5);
            newItem.put("fileName", item.fileName);
            newItem.put("addTime", item.addTime);
            newItem.put("url", item.url);
            newItem.put("pathName", bandPath.storePath);
            newItem.put("thumbName", item.thumbName);
            newItem.put("thumbUrl", item.thumbUrl);
            pathList.put(newItem);

            pathJson.put("items", pathList);
            FileUtils.writeToFile(pathSetDir, pathJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addPicItemToRecentRecord(RecentItemInfo itemInfo) {
        try {
            createIfFileNotContain();
            removeContainOrLast(itemInfo);
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");

            JSONObject newItem = new JSONObject();
            newItem.put("MD5", itemInfo.MD5);
            newItem.put("fileName", itemInfo.fileName);
            newItem.put("addTime", itemInfo.addTime);
            newItem.put("url", itemInfo.url);
            newItem.put("pathName", itemInfo.pathName);
            newItem.put("thumbName", itemInfo.thumbName);
            newItem.put("thumbUrl", itemInfo.thumbUrl);


            pathList.put(newItem);

            pathJson.put("items", pathList);
            FileUtils.writeToFile(pathSetDir, pathJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void removeContainOrLast(LocalDataHelper.LocalPicItems item) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("MD5").equals(item.MD5)) {
                    pathList.remove(i);
                    pathJson.put("items", pathList);
                    FileUtils.writeToFile(pathSetDir, pathJson.toString());
                    return;
                }
            }

            if (pathList.length() >= 100) {
                pathList.remove(0);
                pathJson.put("items", pathList);
                FileUtils.writeToFile(pathSetDir, pathJson.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void removeContainOrLast(RecentItemInfo item) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/recent.json";
            JSONObject pathJson = new JSONObject(FileUtils.readFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("MD5").equals(item.MD5)) {
                    pathList.remove(i);
                    pathJson.put("items", pathList);
                    FileUtils.writeToFile(pathSetDir, pathJson.toString());
                    return;
                }
            }
            if (pathList.length() >= 100) {
                pathList.remove(0);
                pathJson.put("items", pathList);
                FileUtils.writeToFile(pathSetDir, pathJson.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class RecentItemInfo {
        public String MD5;
        public String fileName;
        public String url;
        public String thumbName;
        public String thumbUrl;


        public long addTime;
        public String pathName;
    }
}
