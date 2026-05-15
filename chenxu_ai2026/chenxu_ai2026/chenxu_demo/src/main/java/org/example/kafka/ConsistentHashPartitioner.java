package org.example.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.utils.Utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性Hash分区器
 * 相同key的消息始终路由到同一个分区，保证消息消费顺序
 */
public class ConsistentHashPartitioner implements Partitioner {

    private static final int VIRTUAL_NODE_COUNT = 100;
    private final SortedMap<Long, Integer> ring = new TreeMap<>();

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        int numPartitions = cluster.partitionCountForTopic(topic);
        if (keyBytes == null) {
            return Utils.toPositive(Utils.murmur2(
                    String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8))) % numPartitions;
        }
        buildRing(numPartitions);
        long hash = Utils.toPositive(Utils.murmur2(keyBytes));
        SortedMap<Long, Integer> tailMap = ring.tailMap(hash);
        long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);
    }

    private void buildRing(int numPartitions) {
        if (!ring.isEmpty() && ring.size() == numPartitions * VIRTUAL_NODE_COUNT) {
            return;
        }
        ring.clear();
        for (int partition = 0; partition < numPartitions; partition++) {
            for (int i = 0; i < VIRTUAL_NODE_COUNT; i++) {
                byte[] bytes = ("partition-" + partition + "-vn-" + i).getBytes(StandardCharsets.UTF_8);
                long hash = Utils.toPositive(Utils.murmur2(bytes));
                ring.put(hash, partition);
            }
        }
    }

    @Override
    public void close() {
        ring.clear();
    }
}