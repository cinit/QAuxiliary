package cc.hicore.Utils;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import io.github.qauxv.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FunProtoData {
    private final HashMap<Integer, List<Object>> values = new HashMap<>();
    public void fromJSON(JSONObject json){
        try {
            Iterator<String> key_it = json.keys();
            while (key_it.hasNext()){
                String key = key_it.next();
                int k = Integer.parseInt(key);
                Object value = json.get(key);
                if (value instanceof JSONObject){
                    FunProtoData newProto = new FunProtoData();
                    newProto.fromJSON((JSONObject) value);
                    putValue(k, newProto);
                }else if (value instanceof JSONArray){
                    JSONArray arr = (JSONArray) value;
                    for (int i=0;i < arr.length(); i++){
                        Object arr_obj = arr.get(i);
                        if (arr_obj instanceof JSONObject){
                            FunProtoData newProto = new FunProtoData();
                            newProto.fromJSON((JSONObject) arr_obj);
                            putValue(k, newProto);
                        }else {
                            putValue(k, arr_obj);
                        }
                    }
                }else {
                    putValue(k, value);
                }
            }
        }catch (Exception ignored){ }
    }
    private void putValue(int key, Object value){
        List<Object> list = values.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
    }
    public void fromBytes(byte[] b) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(b);
        while (in.getBytesUntilLimit() > 0){
            int tag = in.readTag();
            int fieldNumber = tag >>> 3;
            int wireType = tag & 7;
            if (wireType == 4 || wireType == 3 || wireType > 5) throw new IOException("Unexpected wireType: "+wireType);
            switch (wireType){
                case 0:
                    putValue(fieldNumber, in.readInt64());
                    break;
                case 1:
                    putValue(fieldNumber, in.readRawVarint64());
                    break;
                case 2:
                    byte[] subBytes = in.readByteArray();
                    try {
                        FunProtoData sub_data = new FunProtoData();
                        sub_data.fromBytes(subBytes);
                        putValue(fieldNumber, sub_data);
                    }catch (Exception e){
                        putValue(fieldNumber, new String(subBytes));
                    }
                    break;
                case 5:
                    putValue(fieldNumber, in.readFixed32());
                    break;
                default:
                    putValue(fieldNumber,"Unknown wireType: "+wireType);
                    break;
            }
        }
    }
    public JSONObject toJSON()throws Exception{
        JSONObject obj = new JSONObject();
        for (Integer k_index : values.keySet()){
            List<?> list = values.get(k_index);
            if (list.size() > 1){
                JSONArray arr = new JSONArray();
                for (Object o : list){
                    arr.put(valueToText(o));
                }
                obj.put(String.valueOf(k_index), arr);
            }else {
                for (Object o : list){
                    obj.put(String.valueOf(k_index), valueToText(o));
                }
            }
        }
        return obj;
    }
    private Object valueToText(Object value) throws Exception {
        if (value instanceof FunProtoData){
            FunProtoData data = (FunProtoData) value;
            return data.toJSON();
        }else {
            return value;
        }
    }
    public byte[] toBytes(){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bos);
        try {
            for (Integer k_index : values.keySet()){
                List<?> list = values.get(k_index);
                for (Object o : list){
                    if (o instanceof Long){
                        long l = (long) o;
                        out.writeInt64(k_index , l);
                    }else if (o instanceof String){
                        String s = (String) o;
                        out.writeByteArray(k_index , s.getBytes());
                    }else if (o instanceof FunProtoData){
                        FunProtoData data = (FunProtoData) o;
                        byte[] subBytes = data.toBytes();
                        out.writeByteArray(k_index , subBytes);
                    }else if (o instanceof Integer){
                        int i = (int) o;
                        out.writeInt32(k_index, i);
                    }else {
                        Log.w("FunProtoData.toBytes "+ "Unknown type: " + o.getClass().getName());
                    }
                }
            }
            out.flush();
            return bos.toByteArray();
        }catch (Exception e){
            XLog.e("FunProtoData", "toBytes", e);
            return new byte[0];
        }
    }
}
