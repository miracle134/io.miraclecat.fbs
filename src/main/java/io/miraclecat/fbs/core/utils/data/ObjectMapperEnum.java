package io.miraclecat.fbs.core.utils.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * packageName    : io.miraclecat.fbs.core.utils
 * fileName       : ObjectMapperEnum
 * author         : MiracleCat
 * date           : 2024-10-08
 * description    : Jackson 3 (tools.jackson) 기반 ObjectMapper 모음
 *
 * Jackson 3 변경 사항:
 * - ObjectMapper 불변 → Builder로 생성
 * - JavaTimeModule 내장 (별도 등록 불필요)
 * - WRITE_DATES_AS_TIMESTAMPS → DateTimeFeature 로 이동
 * - ToXmlGenerator.Feature → XmlWriteFeature 로 이동
 * - JsonProcessingException(checked) → JacksonException(unchecked)
 */
@Slf4j
@Getter
public enum ObjectMapperEnum {
    /**
     * 기본 JSON ObjectMapper
     */
    JSON_DEFAULT {
        @Override
        protected ObjectMapper createMapper() {
            return jsonBuilder(false).build();
        }
    },

    /**
     * Pretty Print JSON ObjectMapper
     */
    JSON_PRETTY {
        @Override
        protected ObjectMapper createMapper() {
            return jsonBuilder(false)
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();
        }
    },

    /**
     * Null 값 제외 JSON ObjectMapper
     */
    JSON_NON_NULL {
        @Override
        protected ObjectMapper createMapper() {
            return jsonBuilder(false)
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                    .build();
        }
    },

    /**
     * 빈 값 제외 JSON ObjectMapper
     */
    JSON_NON_EMPTY {
        @Override
        protected ObjectMapper createMapper() {
            return jsonBuilder(false)
                    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                    .build();
        }
    },

    /**
     * 엄격한 JSON ObjectMapper
     */
    JSON_STRICT {
        @Override
        protected ObjectMapper createMapper() {
            return JsonMapper.builder()
                    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
        }
    },

    /**
     * 기본 XML Mapper
     */
    XML_DEFAULT {
        @Override
        protected ObjectMapper createMapper() {
            return xmlBuilder(false, true).build();
        }
    },

    /**
     * XML 선언 없는 XML Mapper
     */
    XML_NO_DECLARATION {
        @Override
        protected ObjectMapper createMapper() {
            return xmlBuilder(false, false).build();
        }
    },

    /**
     * 들여쓰기된 XML Mapper
     */
    XML_PRETTY {
        @Override
        protected ObjectMapper createMapper() {
            return xmlBuilder(false, true)
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();
        }
    },

    /**
     * 엄격한 XML Mapper
     */
    XML_STRICT {
        @Override
        protected ObjectMapper createMapper() {
            return xmlBuilder(true, true).build();
        }
    };

    private final ObjectMapper mapper;

    ObjectMapperEnum() {
        this.mapper = createMapper();
    }

    protected abstract ObjectMapper createMapper();

    // =============================================================================
    // Builder 공통 설정
    // =============================================================================

    /**
     * JSON 공통 Builder
     * @param failOnUnknown 알 수 없는 프로퍼티 실패 여부
     */
    private static JsonMapper.Builder jsonBuilder(boolean failOnUnknown) {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * XML 공통 Builder
     * @param failOnUnknown  알 수 없는 프로퍼티 실패 여부
     * @param xmlDeclaration XML 선언 출력 여부
     */
    private static XmlMapper.Builder xmlBuilder(boolean failOnUnknown, boolean xmlDeclaration) {
        return XmlMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown)
                .configure(XmlWriteFeature.WRITE_XML_DECLARATION, xmlDeclaration);
    }

    public XmlMapper getXmlMapper() {
        if (mapper instanceof XmlMapper xmlMapper) {
            return xmlMapper;
        }
        throw new IllegalStateException("이 ObjectMapperEnum은 XML 타입이 아닙니다: " + this.name());
    }

    // =============================================================================
    // JSON 직렬화 (객체 -> JSON 문자열)
    // =============================================================================

    /**
     * 객체를 JSON 문자열로 변환 (안전한 버전, 실패 시 빈 문자열 반환)
     */
    public String toJsonSafely(Object obj) {
        return toJsonSafely(obj, "");
    }

    /**
     * 객체를 JSON 문자열로 변환 (실패 시 기본값 반환)
     */
    public String toJsonSafely(Object obj, String defaultValue) {
        if (obj == null) {
            return defaultValue;
        }

        try {
            return mapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            log.warn("JSON 직렬화 실패: {}, 객체 타입: {}", e.getMessage(), obj.getClass().getSimpleName());
            return defaultValue;
        }
    }

    /**
     * 객체를 JSON 문자열로 변환 (예외 발생)
     */
    public String toJson(Object obj) throws JacksonException {
        if (obj == null) {
            return null;
        }
        return mapper.writeValueAsString(obj);
    }

    // =============================================================================
    // JSON 역직렬화 (JSON 문자열 -> 객체)
    // =============================================================================

    /**
     * JSON 문자열을 객체로 변환 (안전한 버전)
     */
    public <T> T fromJsonSafely(String json, Class<T> clazz) {
        return fromJsonSafely(json, clazz, null);
    }

    /**
     * JSON 문자열을 객체로 변환 (실패 시 기본값 반환)
     */
    public <T> T fromJsonSafely(String json, Class<T> clazz, T defaultValue) {
        if (json == null || json.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return mapper.readValue(json, clazz);
        } catch (JacksonException e) {
            log.warn("JSON 역직렬화 실패: {}, 대상 클래스: {}", e.getMessage(), clazz.getSimpleName());
            return defaultValue;
        }
    }

    /**
     * JSON 문자열을 객체로 변환 (예외 발생)
     */
    public <T> T fromJson(String json, Class<T> clazz) throws JacksonException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return mapper.readValue(json, clazz);
    }

    /**
     * JSON 문자열을 TypeReference로 변환 (제네릭 지원)
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) throws JacksonException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return mapper.readValue(json, typeReference);
    }

    /**
     * JSON 문자열을 TypeReference로 변환 (안전한 버전)
     */
    public <T> T fromJsonSafely(String json, TypeReference<T> typeReference) {
        return fromJsonSafely(json, typeReference, null);
    }

    /**
     * JSON 문자열을 TypeReference로 변환 (안전한 버전)
     */
    public <T> T fromJsonSafely(String json, TypeReference<T> typeReference, T defaultValue) {
        if (json == null || json.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return mapper.readValue(json, typeReference);
        } catch (JacksonException e) {
            log.warn("JSON 역직렬화 실패 (TypeReference): {}", e.getMessage());
            return defaultValue;
        }
    }

    // =============================================================================
    // Map과 JSON 간 변환
    // =============================================================================

    /**
     * Map을 JSON 문자열로 변환
     */
    public String mapToJson(Map<String, Object> map) {
        return toJsonSafely(map, "{}");
    }

    /**
     * JSON 문자열을 Map으로 변환
     */
    public Map<String, Object> jsonToMap(String json) {
        return fromJsonSafely(json, new TypeReference<Map<String, Object>>() {}, new HashMap<>());
    }

    /**
     * JSON 문자열을 Map으로 변환 (유효하지 않으면 null 반환)
     */
    public Map<String, Object> jsonToMapOrNull(String json) {
        if (!isValidJson(json)) {
            return null;
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            log.warn("JSON을 Map으로 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 객체를 Map으로 변환
     */
    public Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        try {
            String json = mapper.writeValueAsString(obj);
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            log.warn("객체를 Map으로 변환 실패: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Map을 객체로 변환
     */
    public <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        try {
            String json = mapper.writeValueAsString(map);
            return mapper.readValue(json, clazz);
        } catch (JacksonException e) {
            log.warn("Map을 객체로 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    // =============================================================================
    // 리스트 처리
    // =============================================================================

    /**
     * JSON 배열 문자열을 List로 변환
     */
    public <T> List<T> jsonArrayToList(String jsonArray, Class<T> elementClass) {
        if (!isJsonArray(jsonArray)) {
            return new ArrayList<>();
        }

        try {
            TypeReference<List<T>> typeRef = new TypeReference<List<T>>() {};
            List<T> list = mapper.readValue(jsonArray, typeRef);

            List<T> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    T convertedItem = mapper.convertValue(item, elementClass);
                    result.add(convertedItem);
                }
            }
            return result;
        } catch (JacksonException e) {
            log.warn("JSON 배열을 List로 변환 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * List를 JSON 배열 문자열로 변환
     */
    public String listToJsonArray(List<?> list) {
        return toJsonSafely(list, "[]");
    }

    // =============================================================================
    // JSON 검증
    // =============================================================================

    /**
     * 문자열이 유효한 JSON인지 검증
     */
    public boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            mapper.readTree(json);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * JSON 문자열이 객체({})인지 확인
     */
    public boolean isJsonObject(String json) {
        if (!isValidJson(json)) {
            return false;
        }

        try {
            JsonNode node = mapper.readTree(json);
            return node.isObject();
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * JSON 문자열이 배열([])인지 확인
     */
    public boolean isJsonArray(String json) {
        if (!isValidJson(json)) {
            return false;
        }

        try {
            JsonNode node = mapper.readTree(json);
            return node.isArray();
        } catch (JacksonException e) {
            return false;
        }
    }

    // =============================================================================
    // JSON Path 처리
    // =============================================================================

    /**
     * JSON에서 지정된 경로의 값을 문자열로 추출
     */
    public String extractValueAsString(String json, String path) {
        JsonNode node = extractNode(json, path);
        return node != null ? node.asString() : null;
    }

    /**
     * JSON에서 지정된 경로의 값을 정수로 추출
     */
    public Integer extractValueAsInt(String json, String path) {
        JsonNode node = extractNode(json, path);
        return node != null && node.isNumber() ? node.asInt() : null;
    }

    /**
     * JSON에서 지정된 경로의 값을 불린으로 추출
     */
    public Boolean extractValueAsBoolean(String json, String path) {
        JsonNode node = extractNode(json, path);
        return node != null && node.isBoolean() ? node.asBoolean() : null;
    }

    /**
     * JSON에서 지정된 경로의 값을 지정된 타입으로 추출
     */
    public <T> T extractValue(String json, String path, Class<T> clazz) {
        JsonNode node = extractNode(json, path);
        if (node == null) {
            return null;
        }

        try {
            return mapper.treeToValue(node, clazz);
        } catch (JacksonException e) {
            log.warn("JSON Path 값 추출 실패: {}, 경로: {}", e.getMessage(), path);
            return null;
        }
    }

    /**
     * JSON에서 지정된 경로의 노드 추출 (내부 메서드)
     */
    private JsonNode extractNode(String json, String path) {
        if (!isValidJson(json) || path == null || path.trim().isEmpty()) {
            return null;
        }

        try {
            JsonNode rootNode = mapper.readTree(json);
            String[] pathParts = path.split("\\.");

            JsonNode currentNode = rootNode;
            for (String part : pathParts) {
                if (currentNode == null) {
                    return null;
                }

                // 배열 인덱스 처리 (예: items[0])
                if (part.contains("[") && part.contains("]")) {
                    String fieldName = part.substring(0, part.indexOf("["));
                    String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));

                    try {
                        int index = Integer.parseInt(indexStr);
                        currentNode = currentNode.get(fieldName);
                        if (currentNode != null && currentNode.isArray()) {
                            currentNode = currentNode.get(index);
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                } else {
                    currentNode = currentNode.get(part);
                }
            }

            return currentNode;
        } catch (JacksonException e) {
            log.warn("JSON 노드 추출 실패: {}, 경로: {}", e.getMessage(), path);
            return null;
        }
    }

    // =============================================================================
    // JSON 포맷팅
    // =============================================================================

    /**
     * JSON 문자열 압축 (공백 제거)
     */
    public String compactJson(String json) {
        if (!isValidJson(json)) {
            return json;
        }

        try {
            JsonNode node = mapper.readTree(json);
            return JSON_DEFAULT.getMapper().writeValueAsString(node);
        } catch (JacksonException e) {
            log.warn("JSON 압축 실패: {}", e.getMessage());
            return json;
        }
    }

    /**
     * JSON 문자열 포맷팅 (Pretty Print)
     */
    public String formatJson(String json) {
        if (!isValidJson(json)) {
            return json;
        }

        try {
            JsonNode node = mapper.readTree(json);
            return JSON_PRETTY.getMapper().writeValueAsString(node);
        } catch (JacksonException e) {
            log.warn("JSON 포맷팅 실패: {}", e.getMessage());
            return json;
        }
    }

    // =============================================================================
    // 병합 및 필터링
    // =============================================================================

    /**
     * 두 JSON 객체를 병합 (두 번째 객체가 우선)
     */
    public String mergeJsonObjects(String json1, String json2) {
        if (!isJsonObject(json1) || !isJsonObject(json2)) {
            return json2 != null ? json2 : json1;
        }

        try {
            Map<String, Object> merged = jsonToMap(json1);
            Map<String, Object> second = jsonToMap(json2);
            merged.putAll(second);

            return mapToJson(merged);
        } catch (Exception e) {
            log.warn("JSON 객체 병합 실패: {}", e.getMessage());
            return json2 != null ? json2 : json1;
        }
    }

    /**
     * JSON 객체에서 지정된 필드만 추출
     */
    public String filterJsonFields(String json, String... fieldNames) {
        if (!isJsonObject(json) || fieldNames == null || fieldNames.length == 0) {
            return json;
        }

        try {
            JsonNode node = mapper.readTree(json);
            Map<String, Object> filtered = new HashMap<>();

            for (String fieldName : fieldNames) {
                JsonNode fieldNode = node.get(fieldName);
                if (fieldNode != null) {
                    Object value = mapper.treeToValue(fieldNode, Object.class);
                    filtered.put(fieldName, value);
                }
            }

            return mapToJson(filtered);
        } catch (JacksonException e) {
            log.warn("JSON 필드 필터링 실패: {}", e.getMessage());
            return json;
        }
    }

    // =============================================================================
    // 타입 정보
    // =============================================================================

    public boolean isJsonType() {
        return this.name().startsWith("JSON_");
    }

    public boolean isXmlType() {
        return this.name().startsWith("XML_");
    }

    public String getContentType() {
        if (isJsonType()) {
            return "application/json";
        } else if (isXmlType()) {
            return "application/xml";
        }
        return "application/json";
    }
}