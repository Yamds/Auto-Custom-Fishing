package com.autofishing.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 钓鱼日志记录
 */
public class FishingLog {
    
    private static final int MAX_LOGS = 20;
    private static final List<LogEntry> logs = new ArrayList<>();
    
    /**
     * 日志条目
     */
    public static class LogEntry {
        public final int barNum;           // Bar编号 (1-9)
        public final double avgSpeed;      // 平均速度 (ms/section)
        public final int clickPosition;    // 点击位置
        public final long timestamp;       // 时间戳
        
        public LogEntry(int barNum, double avgSpeed, int clickPosition) {
            this.barNum = barNum;
            this.avgSpeed = avgSpeed;
            this.clickPosition = clickPosition;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 添加日志
     */
    public static synchronized void addLog(int barNum, double avgSpeed, int clickPosition) {
        logs.add(0, new LogEntry(barNum, avgSpeed, clickPosition));
        
        // 保持最多20条记录
        while (logs.size() > MAX_LOGS) {
            logs.remove(logs.size() - 1);
        }
    }
    
    /**
     * 获取所有日志
     */
    public static synchronized List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }
    
    /**
     * 清空日志
     */
    public static synchronized void clear() {
        logs.clear();
    }
    
    // 速度阈值计算（从TitleAnalyzer复制）
    private static double getEasyMinThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double frequency = minSpeed;
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static double getEasyMaxThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double maxSpeed = 150.0;
        double difficulty = 30.0;
        double frequency = minSpeed + (difficulty / 100.0) * (maxSpeed - minSpeed);
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static double getNormalMinThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double maxSpeed = 150.0;
        double difficulty = 30.0;
        double frequency = minSpeed + (difficulty / 100.0) * (maxSpeed - minSpeed);
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static double getNormalMaxThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double maxSpeed = 150.0;
        double difficulty = 60.0;
        double frequency = minSpeed + (difficulty / 100.0) * (maxSpeed - minSpeed);
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static double getHardMinThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double maxSpeed = 150.0;
        double difficulty = 60.0;
        double frequency = minSpeed + (difficulty / 100.0) * (maxSpeed - minSpeed);
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static double getHardMaxThreshold(int barNum) {
        int maxPosition = getMaxPosition(barNum);
        double minSpeed = 15.0;
        double maxSpeed = 150.0;
        double difficulty = 90.0;
        double frequency = minSpeed + (difficulty / 100.0) * (maxSpeed - minSpeed);
        double intervalMicros = 1_000_000.0 / frequency;
        return intervalMicros / 1000.0;
    }
    
    private static int getMaxPosition(int barNum) {
        if (barNum >= 1 && barNum <= 3) return 11;
        if (barNum >= 4 && barNum <= 6) return 22;
        if (barNum >= 7 && barNum <= 9) return 44;
        return 11;
    }
}
