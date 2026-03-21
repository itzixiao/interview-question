package cn.itzixiao.interview.cache.memcached.service;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Memcached 服务封装类
 * 
 * <p>提供常用的缓存操作方法，包括：</p>
 * <ul>
 *   <li>基本的 CRUD 操作（set/get/add/replace/delete）</li>
 *   <li>原子操作（自增、自减）</li>
 *   <li>CAS（Compare And Set）操作</li>
 *   <li>批量操作（multiGet）</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "memcached.enabled", havingValue = "true")
public class MemcachedService {

    private final MemcachedClient memcachedClient;
    
    private static final int DEFAULT_EXPIRE = 3600;

    public MemcachedService(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    public boolean set(String key, Object value, int expire) {
        try {
            return memcachedClient.set(key, expire, value);
        } catch (TimeoutException e) {
            log.error("Memcached set timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached set interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            log.error("Memcached set error, key: {}", key, e);
            return false;
        }
    }

    public boolean set(String key, Object value) {
        return set(key, value, DEFAULT_EXPIRE);
    }

    public boolean add(String key, Object value, int expire) {
        try {
            return memcachedClient.add(key, expire, value);
        } catch (TimeoutException e) {
            log.error("Memcached add timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached add interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            log.error("Memcached add error, key: {}", key, e);
            return false;
        }
    }

    public boolean replace(String key, Object value, int expire) {
        try {
            return memcachedClient.replace(key, expire, value);
        } catch (TimeoutException e) {
            log.error("Memcached replace timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached replace interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            log.error("Memcached replace error, key: {}", key, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) memcachedClient.get(key);
        } catch (TimeoutException e) {
            log.error("Memcached get timeout, key: {}", key, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached get interrupted, key: {}", key, e);
            return null;
        } catch (MemcachedException e) {
            log.error("Memcached get error, key: {}", key, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return memcachedClient.get(keys);
        } catch (TimeoutException e) {
            log.error("Memcached multiGet timeout, keys: {}", keys, e);
            return Collections.emptyMap();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached multiGet interrupted, keys: {}", keys, e);
            return Collections.emptyMap();
        } catch (MemcachedException e) {
            log.error("Memcached multiGet error, keys: {}", keys, e);
            return Collections.emptyMap();
        }
    }

    public boolean delete(String key) {
        try {
            return memcachedClient.delete(key);
        } catch (TimeoutException e) {
            log.error("Memcached delete timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached delete interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            log.error("Memcached delete error, key: {}", key, e);
            return false;
        }
    }

    public long incr(String key, long delta) {
        return incr(key, delta, delta, DEFAULT_EXPIRE);
    }

    public long incr(String key, long delta, long initValue, int expire) {
        try {
            return memcachedClient.incr(key, delta, initValue, expire);
        } catch (TimeoutException e) {
            log.error("Memcached incr timeout, key: {}", key, e);
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached incr interrupted, key: {}", key, e);
            return -1;
        } catch (MemcachedException e) {
            log.error("Memcached incr error, key: {}", key, e);
            return -1;
        }
    }

    public long decr(String key, long delta) {
        try {
            return memcachedClient.decr(key, delta);
        } catch (TimeoutException e) {
            log.error("Memcached decr timeout, key: {}", key, e);
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached decr interrupted, key: {}", key, e);
            return -1;
        } catch (MemcachedException e) {
            log.error("Memcached decr error, key: {}", key, e);
            return -1;
        }
    }

    public boolean cas(String key, Object value, long cas, int expire) {
        try {
            return memcachedClient.cas(key, expire, value, cas);
        } catch (TimeoutException e) {
            log.error("Memcached cas timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached cas interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            log.error("Memcached cas error, key: {}", key, e);
            return false;
        }
    }

    public Map<String, String> getStats() {
        try {
            Map<java.net.InetSocketAddress, Map<String, String>> stats = memcachedClient.getStats();
            if (stats != null && !stats.isEmpty()) {
                return stats.values().iterator().next();
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Memcached getStats error", e);
            return Collections.emptyMap();
        }
    }

    /**
     * CAS 值封装类
     */
    public static class MemcachedCASValue<T> {
        private final long cas;
        private final T value;
        
        public MemcachedCASValue(long cas, T value) {
            this.cas = cas;
            this.value = value;
        }
        
        public long getCas() {
            return cas;
        }
        
        public T getValue() {
            return value;
        }
    }
}
