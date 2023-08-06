package cc.hicore.hook.stickerPanel;

import cc.hicore.Env;
import cc.hicore.Utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;


public class LocalDataHelper {
    public static List<LocalPath> readPaths() {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/set.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            List<LocalPath> paths = new ArrayList<>();
            JSONArray pathList = pathJson.getJSONArray("paths");
            for (int i = 0; i < pathList.length(); i++) {
                try {
                    JSONObject path = pathList.getJSONObject(i);
                    LocalPath localPath = new LocalPath();
                    localPath.coverName = path.optString("coverName");
                    localPath.Name = path.getString("Name");
                    localPath.storePath = path.getString("storePath");
                    paths.add(localPath);
                } catch (Exception ignored) { }
            }
            return paths;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public synchronized static List<LocalPicItems> getPicItems(String pathName) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/" + pathName + "/info.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            List<LocalPicItems> items = new ArrayList<>();
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                try {
                    JSONObject path = pathList.getJSONObject(i);
                    LocalPicItems localPath = new LocalPicItems();
                    localPath.MD5 = path.getString("MD5");
                    localPath.fileName = path.optString("fileName");
                    localPath.addTime = path.optLong("addTime");
                    localPath.type = path.optInt("type",1);
                    localPath.url = path.optString("url");
                    localPath.thumbName = path.optString("thumbName");
                    localPath.thumbUrl = path.optString("thumbUrl");
                    localPath.ocr = path.optString("ocr");
                    items.add(localPath);
                } catch (Exception ignored) { }

            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public synchronized static boolean addPath(LocalPath addInfo) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/set.json";
            if (!new File(pathSetDir).exists()) {
                FileUtils.WriteToFile(pathSetDir, "{\"paths\":[]}");
            }
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("paths");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("Name").equals(addInfo.Name)) {
                    return false;
                }
            }
            JSONObject newPath = new JSONObject();
            newPath.put("coverName", addInfo.coverName);
            newPath.put("Name", addInfo.Name);
            newPath.put("storePath", addInfo.storePath);
            pathList.put(newPath);

            new File(Env.app_save_path + "本地表情包/" + addInfo.storePath).mkdirs();
            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized static boolean addPicItem(String pathName, LocalPicItems addInfo) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/" + pathName + "/info.json";
            if (!new File(pathSetDir).exists()) {
                JSONObject pathJson = new JSONObject();
                pathJson.put("items", new JSONArray());
                FileUtils.WriteToFile(pathSetDir, pathJson.toString());
            }
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("MD5").equals(addInfo.MD5)) {
                    return false;
                }
            }
            JSONObject newPath = new JSONObject();
            newPath.put("MD5", addInfo.MD5);
            newPath.put("fileName", addInfo.fileName);
            newPath.put("addTime", addInfo.addTime);
            newPath.put("type", addInfo.type);
            newPath.put("url", addInfo.url);
            newPath.put("thumbName", addInfo.thumbName);
            newPath.put("thumbUrl", addInfo.thumbUrl);


            pathList.put(newPath);

            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized static void deletePath(LocalPath pathInfo) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/set.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("paths");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("Name").equals(pathInfo.Name)) {
                    pathList.remove(i);
                    break;
                }
            }
            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
            FileUtils.deleteFile(new File(Env.app_save_path + "本地表情包/" + pathInfo.storePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void deletePicItem(LocalPath pathInfo, LocalPicItems item) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/" + pathInfo.storePath + "/info.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("MD5").equals(item.MD5)) {
                    pathList.remove(i);
                    break;
                }
            }
            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void setPathCover(LocalPath pathInfo, LocalPicItems coverItem) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/set.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("paths");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("Name").equals(pathInfo.Name)) {
                    if (coverItem.type == 1) {
                        path.put("coverName", coverItem.fileName);
                    } else if (coverItem.type == 2) {
                        path.put("coverName", coverItem.url);
                    }

                    break;
                }
            }
            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized static void updatePicItemInfo(LocalPath pathInfo, LocalPicItems newItemInfo) {
        try {
            String pathSetDir = Env.app_save_path + "本地表情包/" + pathInfo.storePath + "/info.json";
            JSONObject pathJson = new JSONObject(FileUtils.ReadFileString(pathSetDir));
            JSONArray pathList = pathJson.getJSONArray("items");
            for (int i = 0; i < pathList.length(); i++) {
                JSONObject path = pathList.getJSONObject(i);
                if (path.getString("MD5").equals(newItemInfo.MD5)) {
                    path.put("fileName", newItemInfo.fileName);
                    path.put("addTime", newItemInfo.addTime);
                    path.put("type", newItemInfo.type);
                    path.put("url", newItemInfo.url);
                    path.put("thumbName", newItemInfo.thumbName);
                    path.put("thumbUrl", newItemInfo.thumbUrl);
                    break;
                }
            }
            FileUtils.WriteToFile(pathSetDir, pathJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalItemPath(RecentStickerHelper.RecentItemInfo recentItemInfo) {
        return Env.app_save_path + "本地表情包/" + recentItemInfo.pathName + "/" + recentItemInfo.fileName;
    }

    public static String getLocalItemPath(LocalPath pathInfo, LocalPicItems newItemInfo) {
        return Env.app_save_path + "本地表情包/" + pathInfo.storePath + "/" + newItemInfo.fileName;
    }

    public static String getLocalThumbPath(LocalPath pathInfo, LocalPicItems newItemInfo) {
        return Env.app_save_path + "本地表情包/" + pathInfo.storePath + "/" + newItemInfo.fileName + "_thumb";
    }

    public static class LocalPath {
        public String coverName;
        public String Name;
        public String storePath;
    }

    public static class LocalPicItems {
        public String MD5;
        public String fileName;
        public String thumbName;

        public String url;
        public String thumbUrl;


        public long addTime;
        public int type;
        public String ocr;
    }
}
