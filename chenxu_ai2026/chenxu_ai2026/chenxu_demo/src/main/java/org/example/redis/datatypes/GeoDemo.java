package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.data.geo.GeoResult;

/**
 * Redis Geo（地理位置）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * Geo 底层使用 ZSet（有序集合），score 采用 Geohash 编码（52 位整数）。
 * Geohash 是一种空间填充曲线（Z-order curve），将经纬度编码为一维整数，
 * 保留空间邻近性：两点地理距离越近，Geohash 前缀越相似。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 附近的人 —— GEOADD + GEORADIUS 查找指定半径内的用户
 *   ├─────── 附近门店/POI —— 外卖/打车场景中查找最近的商家或司机
 *   ├─────── 骑手/司机路径追踪 —— GEODIST 计算实际行驶距离
 *   ├─────── 位置签到 —— GEOADD 记录用户签到位置
 *   └─────── LBS 推荐 —— 结合 ZSet 做距离排序推荐
 * </pre>
 *
 * 常用命令：
 *   GEOADD key lng lat member  → 添加位置
 *   GEOPOS key member          → 获取位置经纬度
 *   GEODIST key m1 m2 [unit]   → 计算两个位置距离
 *   GEORADIUS key lng lat r m  → 查找半径内的位置
 *   GEORADIUSBYMEMBER key m r  → 以某成员为中心查找附近位置
 */
@Component
public class GeoDemo {

    private static final Logger log = LoggerFactory.getLogger(GeoDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:geo:";

    /**
     * 附近门店查询。
     * 场景：外卖 App 中查找用户附近 3 公里内的营业门店。
     * GEOADD 添加门店位置，GEORADIUS 以用户位置为中心搜索。
     */
    public String nearbyShops() {
        String key = KEY_PREFIX + "shops";

        // GEOADD: 批量添加门店位置（经度, 纬度, 名称）
        redisTemplate.opsForGeo().add(key, new Point(121.4737, 31.2304), "肯德基(南京东路店)");
        redisTemplate.opsForGeo().add(key, new Point(121.4789, 31.2350), "星巴克(外滩店)");
        redisTemplate.opsForGeo().add(key, new Point(121.4680, 31.2256), "海底捞(人民广场店)");
        redisTemplate.opsForGeo().add(key, new Point(121.4850, 31.2420), "麦当劳(外白渡桥店)");
        redisTemplate.opsForGeo().add(key, new Point(121.4900, 31.2280), "喜茶(陆家嘴店)");

        // 以南京东路地铁站为中心（121.4715, 31.2337），搜索半径 1000 米内的门店
        Point center = new Point(121.4715, 31.2337);
        Distance radius = new Distance(1, Metrics.KILOMETERS);

        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo().radius(key,
                        new Circle(center, radius),
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance()
                                .includeCoordinates()
                                .sortAscending()
                                .limit(10));

        StringBuilder sb = new StringBuilder("【附近门店查询（南京东路地铁站周边 1 公里）】\n");
        if (results != null && results.getContent() != null) {
            List<GeoResult<RedisGeoCommands.GeoLocation<Object>>> content = results.getContent();
            if (content.isEmpty()) {
                sb.append("  附近 1 公里内没有找到门店\n");
            } else {
                for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : content) {
                    RedisGeoCommands.GeoLocation<Object> location = result.getContent();
                    Point loc = location.getPoint();
                    sb.append("  ").append(location.getName())
                            .append(" — 距离: ").append(String.format("%.0f", result.getDistance().getValue() * 1000))
                            .append(" 米")
                            .append(" (坐标: ")
                            .append(String.format("%.4f, %.4f", loc.getX(), loc.getY()))
                            .append(")\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 计算两个位置之间的距离（GEODIST）。
     * 场景：预估配送距离/时间、行程计费。
     */
    public String geoDistance() {
        String key = KEY_PREFIX + "shops";

        Distance distance = redisTemplate.opsForGeo().distance(
                key, "肯德基(南京东路店)", "星巴克(外滩店)", Metrics.KILOMETERS);

        // GEOPOS: 获取某个成员的经纬度
        List<Point> positions = redisTemplate.opsForGeo().position(key, "肯德基(南京东路店)", "海底捞(人民广场店)");

        StringBuilder sb = new StringBuilder("【位置距离计算】\n");
        if (distance != null) {
            sb.append("  肯德基(南京东路店) → 星巴克(外滩店) 距离: ")
                    .append(String.format("%.0f", distance.getValue() * 1000)).append(" 米\n");
        }
        if (positions != null && positions.size() == 2) {
            sb.append("  肯德基坐标: ").append(positions.get(0)).append("\n");
            sb.append("  海底捞坐标: ").append(positions.get(1));
        }
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Geo 地理位置数据类型 Demo ==========\n\n");
        sb.append("【底层结构】基于 ZSet，score 采用 52 位 Geohash 编码\n");
        sb.append("Z-order 曲线保留空间邻近性。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 附近的人/门店 —— GEORADIUS 查找半径内位置\n");
        sb.append("  2. 骑行/配送距离 —— GEODIST 精确计算距离\n");
        sb.append("  3. LBS 推荐 —— 结合 ZSet 做距离排序\n");
        sb.append("  4. 位置签到 —— GEOADD 记录用户位置\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(nearbyShops()).append("\n\n");
        sb.append("  ").append(geoDistance()).append("\n");
        return sb.toString();
    }
}
