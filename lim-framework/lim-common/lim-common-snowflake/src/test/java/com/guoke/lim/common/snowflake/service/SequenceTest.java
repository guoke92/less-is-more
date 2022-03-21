package com.guoke.lim.common.snowflake.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author luGuo
 * @date 2022/3/21 23:29
 */
public class SequenceTest {

    public static void main(String[] args) {
        Sequence sequence = new Sequence();

        Set<Long> idSet = new CopyOnWriteArraySet<>();

        List<Integer> collect = Stream.iterate(1, x -> ++x).limit(100000).collect(Collectors.toList());

        //并发获取雪花id
        collect.stream().parallel().forEach(item -> {
            long snowId = sequence.getSnowId();
            idSet.add(snowId);
            System.out.print("snowId = " + snowId);
            System.out.println("========idSet = " + idSet.size());
        });

        System.out.println("collect = " + collect.size());
        System.out.println("idSet = " + idSet.size());

    }
}
