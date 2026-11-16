package com.farmlink.api.router.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RouterRedisConfiguration {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisScript<List> initSessionScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/init_session.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<List> acquireLockScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/acquire_lock.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<List> heartbeatLockScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/heartbeat_lock.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<List> completeTurnScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/complete_turn.lua"));
        script.setResultType(List.class);
        return script;
    }
}

