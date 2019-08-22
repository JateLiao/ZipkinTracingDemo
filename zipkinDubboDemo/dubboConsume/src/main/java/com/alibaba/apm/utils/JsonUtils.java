package com.alibaba.apm.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
public class JsonUtils {
    
    private final static ObjectMapper objectMapper = new ObjectMapper();
   private final static ObjectMapper objectMapperYMDHms = new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static String toJsonWithJackson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "Convert string error, message=" + e.getMessage();
        }
    }
    
    /**
     * Jackson转json，日期格式为YYYY-MM-DD HH:mm:ss
     * @param object object
     * @return json
     */
    public static String toJsonWithJacksonYMDHms(Object object) {
        try {
            return objectMapperYMDHms.writeValueAsString(object);
        } catch (Exception e) {
            return "Convert string error, message=" + e.getMessage();
        }
    }

    /**
     * Method to deserialize JSON content
     *
     * @param content JSON content to parse to build JSON tree
     *
     * @return 通常情况下返回值参照 {@link ObjectMapper#readTree(String)}
     *     只有当输入内容不合法的时候，返回 null
     */
    public static JsonNode readJsonNodeFromString(String content) {
        if (content == null) {
            return null;
        }

        try {
            return objectMapper.readTree(content);
        } catch (JsonParseException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
