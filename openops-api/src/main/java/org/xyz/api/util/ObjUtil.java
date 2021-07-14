package org.xyz.api.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ObjUtil {

    public static Map<String, String> ObjToByteMap(Object obj) {
        Map<String, String> map = new HashMap<String, String>();
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field : fields){
            if(field.getName().equalsIgnoreCase("class")){
                continue;
            }
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(obj).toString());
            } catch (IllegalAccessException e) {
                log.error("属性获取异常",e);
            }
        }
        return map;
    }
}
