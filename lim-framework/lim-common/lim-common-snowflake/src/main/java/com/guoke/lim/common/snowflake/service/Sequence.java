package com.guoke.lim.common.snowflake.service;

/**
 * 分布式高并发趋势递增ID
 *
 * @author luGuo
 * @date 2022/3/21 15:54
 */
public class Sequence {

    /**
     * 时间原点，一般取最近系统时间，指定时间后不可更改
     */
    private static final long ORIGIN_TIME = 1647850236045L;

    /**
     * 雪花ID 分配
     * 总长度 64位
     * 1bit符号   默认为0一般不做修改
     * 41bit时间  相对originTime的增量，（1L<<41）/(1000L*3600*24*365)=69年
     * 10bit机器位 容器中机器唯一id,（一般dataCenterId + workId）可根据情况适当缩减此机器位数
     * 12bit序列号 同一毫秒同一机器可生成2^12=4096，可适当扩展
     */
    private static final int SEQUENCE_BITS = 12;
    private static final int WORK_ID_BITS = 5;
    private static final int DATA_CENTER_ID_BITS = 5;

    /**
     * 左移位数
     */
    private static final int WORK_ID_LEFT_SHIFT = SEQUENCE_BITS;
    private static final int DATA_CENTER_ID_LEFT_SHIFT = SEQUENCE_BITS + WORK_ID_BITS;
    private static final int DATA_TIME_LEFT_SHIFT = SEQUENCE_BITS + WORK_ID_BITS + DATA_CENTER_ID_BITS;
    public static final int CLOCK_OFFSET = 5;

    /**
     * 掩码
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 默认机器ID
     */
    private static final long DATA_CENTER_ID_MASK = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long WORK_ID_MASK = ~(-1L << WORK_ID_BITS);

    /**
     * 默认机器ID
     */
    private final long dataCenterId;
    private final long workId;

    /**
     * 序列号计数器
     */
    private volatile long sequence = 1L;
    /**
     * 上次生成id 的时间，这里默认指定为 时间原点
     */
    private volatile long lastDateTime = ORIGIN_TIME;

    /**
     * 获取序列号
     *
     * @return 雪花ID
     */
    public synchronized long getSnowId() {
        long timestamp = getTime();

        //时钟回拨、闰秒等情况
        if (timestamp < lastDateTime) {
            long offset = lastDateTime - timestamp;
            if (offset < CLOCK_OFFSET) {
                try {
                    wait(offset << 1);
                    timestamp = getTime();
                    if (timestamp < lastDateTime) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }

        if (timestamp == lastDateTime) {
            //同一时钟， 序列自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                //当前毫秒已达最大,获取下一毫秒
                timestamp = getNextTime();
                lastDateTime = timestamp;
            }
        } else {
            //新的时钟，从头指定序列
            sequence = 1L;
            lastDateTime = timestamp;
        }

        return (timestamp - ORIGIN_TIME) << DATA_TIME_LEFT_SHIFT
                | dataCenterId << DATA_CENTER_ID_LEFT_SHIFT
                | workId << WORK_ID_LEFT_SHIFT
                | sequence;
    }

    /**
     * 获取下一时钟
     *
     * @return 下一时钟
     */
    public long getNextTime() {
        long currentTime = getTime();
        if (currentTime <= lastDateTime) {
            return getNextTime();
        }
        return currentTime;
    }

    /**
     * 获取当前系统时钟
     *
     * @return 当前时钟时间戳
     */
    public long getTime() {
        return System.currentTimeMillis();
    }


    public Sequence() {
        dataCenterId = getDataCenterId();
        workId = getWorkId();
    }

    public Sequence(long dataCenterId, long workId) {
        //
        this.dataCenterId = dataCenterId;
        this.workId = workId;
    }

    public long getDataCenterId() {
        //TODO
        return 0L;
    }

    public long getWorkId() {
        //TODO
        return 0L;
    }

}
