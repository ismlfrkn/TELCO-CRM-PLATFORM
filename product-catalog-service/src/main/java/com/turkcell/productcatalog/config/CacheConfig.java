package com.turkcell.productcatalog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Bölüm 8.2: "Read-heavy servis - Redis cache yoğun kullanılır". tariffs/addons cache'leri
 * JSON serileştirir (DTO'ları Serializable yapmaya gerek kalmadan) ve product-catalog.cache.ttl-seconds
 * ile yapılandırılan bir TTL taşır - gerçek gecersiz kilma (invalidation) ise servis katmanindaki
 * @CacheEvict'ler ile yazma anında yapılır, TTL sadece kaçırılan bir evict yolu için güvenlik agi.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConfigurationProperties(prefix = "product-catalog.cache")
    public CacheTtlProperties cacheTtlProperties() {
        return new CacheTtlProperties();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                           CacheTtlProperties cacheTtlProperties) {
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        // GenericJackson2JsonRedisSerializer'in NO-ARG constructor'i kendi ObjectMapper'inda
        // activateDefaultTyping cagirip her degere bir "@class" alani gomer, boylece deserialize
        // ederken hedef tipi (TariffResponse vb.) bilir. Burada oldugu gibi ObjectMapper'i disaridan
        // verince bu otomatik olmuyor - tip bilgisi olmadan Jackson her nesneyi duz LinkedHashMap'e
        // deserialize eder ve @Cacheable'li metotlarda ("(TariffResponse) cachedValue" gibi) cache
        // HIT aninda ClassCastException firlar (canli testte yakalandi, unit testler cache'i mock'ladigi
        // icin bunu hic tetiklemiyordu). activateDefaultTyping bunu tip bilgisini JSON'a gomerek cozer.
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheTtlProperties.getTtlSeconds()))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }

    public static class CacheTtlProperties {
        private long ttlSeconds = 600;

        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }
}
