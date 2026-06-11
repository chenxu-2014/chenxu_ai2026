package org.example.redis.load;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类。
 * <p>
 * 主要职责：
 * 1. 创建并配置 RedisTemplate&lt;String, Object&gt; Bean
 * 2. 设置 Key / Value 的序列化方式，避免存入 Redis 后出现乱码
 * <p>
 * 序列化策略：
 * - Key（包括 Hash 的 field 名）→ StringRedisSerializer，在 Redis 中明文可读
 * - Value（包括 Hash 的 field 值）→ StringRedisSerializer，可读、可调试、可被其他语言客户端访问
 * <p>
 * 为什么不用默认的 JdkSerializationRedisSerializer：
 * JdkSerializationRedisSerializer 写入的是 Java 序列化字节流，不可读、
 * 不可跨语言、且占用空间更大。JSON 方式可读、可调试、可被其他语言客户端访问。
 */
@Configuration
public class RedisConfig {

    /**
     * 向 Spring 容器注册 RedisTemplate Bean。
     *
     * @param factory Redis 连接工厂，由 spring-boot-starter-data-redis 根据
     *                application.yml 中的 spring.data.redis 配置自动创建
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 1. 注入连接工厂（决定连哪台 Redis、端口、密码、超时等）
        template.setConnectionFactory(factory);

        // 2. 全部使用 String 序列化器（C_METER 各字段都是字符串）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 3. Key 序列化 —— 都用 String，保证 Redis 中 Key 可读（如 "meter:12345678"）
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 4. Value 序列化 —— C_METER 所有字段都是字符串，全用 String 序列化，读写一致且可读
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        // 5. 初始化模板（调用各序列化器的 afterPropertiesSet）
        template.afterPropertiesSet();
        return template;
    }
}
