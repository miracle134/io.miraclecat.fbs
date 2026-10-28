package io.miraclecat.fbs.core.utils.data;

import io.miraclecat.fbs.core.response.HttpsStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.miraclecat.fbs.core.exception.GlobalException.business;

/**
 * packageName    : io.miraclecat.fbs.core.utils.redis
 * fileName       : RedisUtil
 * author         : MiracleCat
 * date           : 25. 10. 21.
 * description    : Redis 작업을 위한 유틸리티 클래스
 *
 * 주요 기능:
 * - Connection Pool 관리
 * - String, Hash, List, Set, Sorted Set 작업
 * - TTL 관리 및 캐시 패턴
 * - 트랜잭션 및 파이프라인 지원
 * - JSON 직렬화/역직렬화
 * - Builder 패턴을 통한 유연한 설정
 */
@Slf4j
@Component
public class RedisUtil implements ApplicationContextAware {

	private static ApplicationContext applicationContext;
	private static StringRedisTemplate stringRedisTemplate;
	private static RedisTemplate<String, Object> redisTemplate;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		applicationContext = context;
		stringRedisTemplate = context.getBean(StringRedisTemplate.class);
		redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
		log.info("RedisUtil 초기화 완료 - Lettuce 기반");
	}

	// =============================================================================
	// String 작업
	// =============================================================================

	/**
	 * String 저장
	 */
	public static void set(String key, String value) {
		try {
			stringRedisTemplate.opsForValue().set(key, value);
		} catch (Exception e) {
			log.error("Redis SET 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * String 저장 (TTL 포함)
	 */
	public static void set(String key, String value, long seconds) {
		try {
			stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
		} catch (Exception e) {
			log.error("Redis SET with TTL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * String 조회
	 */
	public static String get(String key) {
		try {
			return stringRedisTemplate.opsForValue().get(key);
		} catch (Exception e) {
			log.error("Redis GET 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 존재하지 않을 때만 저장
	 */
	public static boolean setIfAbsent(String key, String value) {
		try {
			Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
			return result != null && result;
		} catch (Exception e) {
			log.error("Redis SETNX 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 존재하지 않을 때만 저장 (TTL 포함)
	 */
	public static boolean setIfAbsent(String key, String value, long seconds) {
		try {
			Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(seconds));
			return result != null && result;
		} catch (Exception e) {
			log.error("Redis SETNX with TTL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 여러 키 조회
	 */
	public static List<String> multiGet(Collection<String> keys) {
		try {
			return stringRedisTemplate.opsForValue().multiGet(keys);
		} catch (Exception e) {
			log.error("Redis MGET 실패: {} {}", e.getMessage(), keys, e);
			throw business("Redis 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 증가
	 */
	public static long increment(String key) {
		try {
			Long result = stringRedisTemplate.opsForValue().increment(key);
			return result != null ? result : 0L;
		} catch (Exception e) {
			log.error("Redis INCR 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 증가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 지정 값만큼 증가
	 */
	public static long incrementBy(String key, long delta) {
		try {
			Long result = stringRedisTemplate.opsForValue().increment(key, delta);
			return result != null ? result : 0L;
		} catch (Exception e) {
			log.error("Redis INCRBY 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 증가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 감소
	 */
	public static long decrement(String key) {
		try {
			Long result = stringRedisTemplate.opsForValue().decrement(key);
			return result != null ? result : 0L;
		} catch (Exception e) {
			log.error("Redis DECR 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 감소 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// Hash 작업
	// =============================================================================

	/**
	 * Hash 필드 저장
	 */
	public static void hset(String key, String field, String value) {
		try {
			stringRedisTemplate.opsForHash().put(key, field, value);
		} catch (Exception e) {
			log.error("Redis HSET 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash 여러 필드 저장
	 */
	public static void hmset(String key, Map<String, String> hash) {
		try {
			stringRedisTemplate.opsForHash().putAll(key, hash);
		} catch (Exception e) {
			log.error("Redis HMSET 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash 필드 조회
	 */
	public static String hget(String key, String field) {
		try {
			Object value = stringRedisTemplate.opsForHash().get(key, field);
			return value != null ? value.toString() : null;
		} catch (Exception e) {
			log.error("Redis HGET 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash 전체 조회
	 */
	public static Map<Object, Object> hgetAll(String key) {
		try {
			return stringRedisTemplate.opsForHash().entries(key);
		} catch (Exception e) {
			log.error("Redis HGETALL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash 필드 삭제
	 */
	public static void hdel(String key, Object... fields) {
		try {
			stringRedisTemplate.opsForHash().delete(key, fields);
		} catch (Exception e) {
			log.error("Redis HDEL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 삭제 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash 필드 존재 확인
	 */
	public static boolean hexists(String key, String field) {
		try {
			return stringRedisTemplate.opsForHash().hasKey(key, field);
		} catch (Exception e) {
			log.error("Redis HEXISTS 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Hash 확인 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// List 작업
	// =============================================================================

	/**
	 * List 왼쪽에 추가
	 */
	public static void lpush(String key, String... values) {
		try {
			stringRedisTemplate.opsForList().leftPushAll(key, values);
		} catch (Exception e) {
			log.error("Redis LPUSH 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 추가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List 오른쪽에 추가
	 */
	public static void rpush(String key, String... values) {
		try {
			stringRedisTemplate.opsForList().rightPushAll(key, values);
		} catch (Exception e) {
			log.error("Redis RPUSH 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 추가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List 왼쪽에서 팝
	 */
	public static String lpop(String key) {
		try {
			return stringRedisTemplate.opsForList().leftPop(key);
		} catch (Exception e) {
			log.error("Redis LPOP 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 팝 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List 오른쪽에서 팝
	 */
	public static String rpop(String key) {
		try {
			return stringRedisTemplate.opsForList().rightPop(key);
		} catch (Exception e) {
			log.error("Redis RPOP 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 팝 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List 범위 조회
	 */
	public static List<String> lrange(String key, long start, long end) {
		try {
			return stringRedisTemplate.opsForList().range(key, start, end);
		} catch (Exception e) {
			log.error("Redis LRANGE 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List 길이
	 */
	public static long llen(String key) {
		try {
			Long size = stringRedisTemplate.opsForList().size(key);
			return size != null ? size : 0L;
		} catch (Exception e) {
			log.error("Redis LLEN 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis List 길이 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// Set 작업
	// =============================================================================

	/**
	 * Set 추가
	 */
	public static void sadd(String key, String... members) {
		try {
			stringRedisTemplate.opsForSet().add(key, members);
		} catch (Exception e) {
			log.error("Redis SADD 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Set 추가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Set 제거
	 */
	public static void srem(String key, Object... members) {
		try {
			stringRedisTemplate.opsForSet().remove(key, members);
		} catch (Exception e) {
			log.error("Redis SREM 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Set 제거 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Set 멤버 확인
	 */
	public static boolean sismember(String key, String member) {
		try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, member));
		} catch (Exception e) {
			log.error("Redis SISMEMBER 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Set 확인 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Set 전체 조회
	 */
	public static Set<String> smembers(String key) {
		try {
			return stringRedisTemplate.opsForSet().members(key);
		} catch (Exception e) {
			log.error("Redis SMEMBERS 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Set 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Set 크기
	 */
	public static long scard(String key) {
		try {
			Long size = stringRedisTemplate.opsForSet().size(key);
			return size != null ? size : 0L;
		} catch (Exception e) {
			log.error("Redis SCARD 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Set 크기 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// Sorted Set 작업
	// =============================================================================

	/**
	 * Sorted Set 추가
	 */
	public static void zadd(String key, double score, String member) {
		try {
			stringRedisTemplate.opsForZSet().add(key, member, score);
		} catch (Exception e) {
			log.error("Redis ZADD 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 추가 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 범위 조회 (점수 낮은순)
	 */
	public static Set<String> zrange(String key, long start, long end) {
		try {
			return stringRedisTemplate.opsForZSet().range(key, start, end);
		} catch (Exception e) {
			log.error("Redis ZRANGE 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 범위 조회 (점수 높은순)
	 */
	public static Set<String> zrevrange(String key, long start, long end) {
		try {
			return stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
		} catch (Exception e) {
			log.error("Redis ZREVRANGE 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 점수와 함께 조회
	 */
	public static Set<TypedTuple<String>> zrangeWithScores(String key, long start, long end) {
		try {
			return stringRedisTemplate.opsForZSet().rangeWithScores(key, start, end);
		} catch (Exception e) {
			log.error("Redis ZRANGE with scores 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 순위 조회 (0부터 시작)
	 */
	public static Long zrank(String key, String member) {
		try {
			return stringRedisTemplate.opsForZSet().rank(key, member);
		} catch (Exception e) {
			log.error("Redis ZRANK 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 순위 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 역순 순위 조회
	 */
	public static Long zrevrank(String key, String member) {
		try {
			return stringRedisTemplate.opsForZSet().reverseRank(key, member);
		} catch (Exception e) {
			log.error("Redis ZREVRANK 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 순위 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Sorted Set 점수 조회
	 */
	public static Double zscore(String key, String member) {
		try {
			return stringRedisTemplate.opsForZSet().score(key, member);
		} catch (Exception e) {
			log.error("Redis ZSCORE 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis Sorted Set 점수 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// 공통 작업
	// =============================================================================

	/**
	 * 키 삭제
	 */
	public static boolean delete(String key) {
		try {
			Boolean result = stringRedisTemplate.delete(key);
			return result != null && result;
		} catch (Exception e) {
			log.error("Redis DEL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 삭제 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 여러 키 삭제
	 */
	public static long delete(Collection<String> keys) {
		try {
			Long result = stringRedisTemplate.delete(keys);
			return result != null ? result : 0L;
		} catch (Exception e) {
			log.error("Redis DEL multiple 실패: {} {}", e.getMessage(), keys, e);
			throw business("Redis 삭제 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키 존재 확인
	 */
	public static boolean exists(String key) {
		try {
			Boolean result = stringRedisTemplate.hasKey(key);
			return result != null && result;
		} catch (Exception e) {
			log.error("Redis EXISTS 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 확인 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * TTL 설정
	 */
	public static boolean expire(String key, long seconds) {
		try {
			Boolean result = stringRedisTemplate.expire(key, Duration.ofSeconds(seconds));
			return result != null && result;
		} catch (Exception e) {
			log.error("Redis EXPIRE 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis TTL 설정 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * TTL 조회 (초)
	 */
	public static long ttl(String key) {
		try {
			Long result = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
			return result != null ? result : -2L;
		} catch (Exception e) {
			log.error("Redis TTL 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis TTL 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 패턴 매칭 키 검색 (운영환경 주의!)
	 */
	public static Set<String> keys(String pattern) {
		try {
			return stringRedisTemplate.keys(pattern);
		} catch (Exception e) {
			log.error("Redis KEYS 실패: {} {}", e.getMessage(), pattern, e);
			throw business("Redis 키 검색 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	// =============================================================================
	// 객체 직렬화 작업
	// =============================================================================

	/**
	 * 객체 저장 (JSON)
	 */
	public static <T> void setObject(String key, T object) {
		setObject(key, object, ObjectMapperEnum.JSON_DEFAULT);
	}

	/**
	 * 객체 저장 (JSON, TTL 포함)
	 */
	public static <T> void setObject(String key, T object, long seconds) {
		setObject(key, object, seconds, ObjectMapperEnum.JSON_DEFAULT);
	}

	/**
	 * 객체 저장 (커스텀 Mapper)
	 */
	public static <T> void setObject(String key, T object, ObjectMapperEnum mapperType) {
		try {
			String json = mapperType.toJsonSafely(object);
			if (json == null || json.isEmpty()) {
				throw business("객체 직렬화 실패", HttpsStatusCode.INTERNAL_SERVER_ERROR);
			}
			set(key, json);
		} catch (Exception e) {
			log.error("Redis 객체 저장 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 객체 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 객체 저장 (커스텀 Mapper, TTL 포함)
	 */
	public static <T> void setObject(String key, T object, long seconds, ObjectMapperEnum mapperType) {
		try {
			String json = mapperType.toJsonSafely(object);
			if (json == null || json.isEmpty()) {
				throw business("객체 직렬화 실패", HttpsStatusCode.INTERNAL_SERVER_ERROR);
			}
			set(key, json, seconds);
		} catch (Exception e) {
			log.error("Redis 객체 저장 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 객체 저장 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 객체 조회 (JSON)
	 */
	public static <T> T getObject(String key, Class<T> clazz) {
		return getObject(key, clazz, ObjectMapperEnum.JSON_DEFAULT);
	}

	/**
	 * 객체 조회 (커스텀 Mapper)
	 */
	public static <T> T getObject(String key, Class<T> clazz, ObjectMapperEnum mapperType) {
		try {
			String json = get(key);
			if (json == null || json.isEmpty()) {
				return null;
			}
			return mapperType.fromJsonSafely(json, clazz);
		} catch (Exception e) {
			log.error("Redis 객체 조회 실패: {} {}", e.getMessage(), key, e);
			throw business("Redis 객체 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

    public static <T> T getObject(String key, TypeReference<T> typeRef) {
        return getObject(key, typeRef, ObjectMapperEnum.JSON_DEFAULT);
    }

    public static <T> T getObject(String key, TypeReference<T> typeRef, ObjectMapperEnum mapperType) {
        try {
            String json = get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return mapperType.fromJsonSafely(json, typeRef);
        } catch (Exception e) {
            log.error("Redis 객체 조회 실패: {} {}", e.getMessage(), key, e);
            throw business("Redis 객체 조회 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
        }
    }


	// =============================================================================
	// 캐시 패턴
	// =============================================================================

	/**
	 * 캐시 조회 또는 로드
	 */
	public static <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader, long ttlSeconds) {
		T cached = getObject(key, clazz);
		if (cached != null) {
			return cached;
		}

		T loaded = loader.get();
		if (loaded != null) {
			setObject(key, loaded, ttlSeconds);
		}
		return loaded;
	}

	/**
	 * 캐시 무효화 (패턴)
	 */
	public static long invalidatePattern(String pattern) {
		Set<String> keysToDelete = keys(pattern);
		if (keysToDelete != null && !keysToDelete.isEmpty()) {
			return delete(keysToDelete);
		}
		return 0L;
	}

	// =============================================================================
	// 트랜잭션
	// =============================================================================

	/**
	 * 트랜잭션 실행
	 */
	public static List<Object> executeTransaction(TransactionCallback callback) {
		try {
			return stringRedisTemplate.execute((RedisCallback<List<Object>>) connection -> {
				connection.multi();
				callback.execute(connection);
				return connection.exec();
			});
		} catch (Exception e) {
			log.error("Redis 트랜잭션 실패: {}", e.getMessage(), e);
			throw business("Redis 트랜잭션 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

	@FunctionalInterface
	public interface TransactionCallback {
		void execute(org.springframework.data.redis.connection.RedisConnection connection);
	}

	// =============================================================================
	// 파이프라인
	// =============================================================================

	/**
	 * 파이프라인 실행
	 */
	public static List<Object> executePipeline(PipelineCallback callback) {
		try {
			return stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
				callback.execute(connection);
				return null;
			});
		} catch (Exception e) {
			log.error("Redis 파이프라인 실패: {}", e.getMessage(), e);
			throw business("Redis 파이프라인 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * 파이프라인 실행 (High-level API)
     */
    public static void executePipelinedOperations(PipelineOperationsCallback callback) {
        try {
            stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) {
                    callback.execute(operations);
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Redis 파이프라인 실패: {}", e.getMessage(), e);
            throw business("Redis 파이프라인 실패: " + e.getMessage(), HttpsStatusCode.INTERNAL_SERVER_ERROR);
        }
    }

	@FunctionalInterface
	public interface PipelineCallback {
		void execute(org.springframework.data.redis.connection.RedisConnection connection);
	}

    @FunctionalInterface
    public interface PipelineOperationsCallback {
        void execute(RedisOperations<String, String> operations);
    }

}