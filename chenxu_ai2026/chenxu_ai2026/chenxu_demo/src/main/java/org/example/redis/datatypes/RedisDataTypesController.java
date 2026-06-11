package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Redis 各数据类型场景统一演示 Controller。
 *
 * 提供 REST 接口触发所有数据结构的场景 Demo，方便快速体验和验证。
 *
 * 接口说明：
 * <pre>
 *   GET /redis/types          → 查看所有数据类型列表
 *   GET /redis/types/string   → String 类型 Demo
 *   GET /redis/types/list     → List 类型 Demo
 *   GET /redis/types/set      → Set 类型 Demo
 *   GET /redis/types/zset     → Sorted Set 类型 Demo
 *   GET /redis/types/hash     → Hash 类型 Demo
 *   GET /redis/types/bitmap   → Bitmap 类型 Demo
 *   GET /redis/types/hll      → HyperLogLog 类型 Demo
 *   GET /redis/types/geo      → Geo 类型 Demo
 *   GET /redis/types/stream   → Stream 类型 Demo
 *   GET /redis/types/clean    → 清理所有 demo key（按 "demo:*" 前缀）
 * </pre>
 */
@RestController
@RequestMapping("/redis/types")
public class RedisDataTypesController {

    private static final Logger log = LoggerFactory.getLogger(RedisDataTypesController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringDemo stringDemo;

    @Autowired
    private ListDemo listDemo;

    @Autowired
    private SetDemo setDemo;

    @Autowired
    private SortedSetDemo sortedSetDemo;

    @Autowired
    private HashDemo hashDemo;

    @Autowired
    private BitmapDemo bitmapDemo;

    @Autowired
    private HyperLogLogDemo hyperLogLogDemo;

    @Autowired
    private GeoDemo geoDemo;

    @Autowired
    private StreamDemo streamDemo;

    /**
     * 查看所有支持的数据类型和场景概览。
     */
    @GetMapping
    public String index() {
        return """
                Redis 数据类型场景 Demo

                可用接口:
                  GET /redis/types/string   -> String 类型
                  GET /redis/types/list     -> List 类型
                  GET /redis/types/set      -> Set 类型
                  GET /redis/types/zset     -> Sorted Set (ZSet) 类型
                  GET /redis/types/hash     -> Hash 类型
                  GET /redis/types/bitmap   -> Bitmap 类型
                  GET /redis/types/hll      -> HyperLogLog 类型
                  GET /redis/types/geo      -> Geo 类型
                  GET /redis/types/stream   -> Stream 类型
                  GET /redis/types/clean    -> 清理所有 demo 数据
                """;
    }

    @GetMapping("/string")
    public String stringDemo() {
        return stringDemo.demoAll();
    }

    @GetMapping("/list")
    public String listDemo() {
        return listDemo.demoAll();
    }

    @GetMapping("/set")
    public String setDemo() {
        return setDemo.demoAll();
    }

    @GetMapping("/zset")
    public String sortedSetDemo() {
        return sortedSetDemo.demoAll();
    }

    @GetMapping("/hash")
    public String hashDemo() {
        return hashDemo.demoAll();
    }

    @GetMapping("/bitmap")
    public String bitmapDemo() {
        return bitmapDemo.demoAll();
    }

    @GetMapping("/hll")
    public String hyperLogLogDemo() {
        return hyperLogLogDemo.demoAll();
    }

    @GetMapping("/geo")
    public String geoDemo() {
        return geoDemo.demoAll();
    }

    @GetMapping("/stream")
    public String streamDemo() {
        return streamDemo.demoAll();
    }

    /**
     * 清理所有 demo 数据（按前缀 "demo:*" 匹配删除）。
     * 注意：KEYS 命令在生产环境慎用，此处仅用于本演示。
     */
    @GetMapping("/clean")
    public String clean() {
        Set<String> keys = redisTemplate.keys("demo:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            return "已清理 " + keys.size() + " 个 demo key";
        }
        return "没有需要清理的 demo 数据";
    }
}
