package com.autofishing.config;

import com.autofishing.AutoFishingMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动钓鱼配置
 * 使用Cloth Config提供UI界面
 */
public class AutoFishConfig {
    
    // ========== 主设置 ==========
    public boolean modEnabled = true;  // Mod总开关
    public double globalAdvance = 0.0;  // 全局提前量（-2.0 到 2.0）
    public double autoRecastMinutes = 2.0;  // 抛竿后自动收杆（分钟），默认值来自游戏中调校的最佳延迟（autofishing.json）
    
    // ========== 曲线设置 ==========
    // 每个bar的提前量曲线
    public Map<Integer, List<CurvePoint>> barCurves = new HashMap<>();
    
    // ========== 单例和持久化 ==========
    private static AutoFishConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/autofishing.json");
    
    public AutoFishConfig() {
        // 初始化所有bar的默认曲线
        // 默认值 = 用户在游戏中调校的最佳延迟（对应 versions/26.2-Fabric 0.19.3/config/autofishing.json）
        // Bar 1-3: 11格bar，简单配置
        for (int i = 1; i <= 3; i++) {
            List<CurvePoint> curve = new ArrayList<>();
            curve.add(new CurvePoint(0.0, 0.2));
            curve.add(new CurvePoint(35.0, -0.2));
            barCurves.put(i, curve);
        }
        
        // Bar 4-6: 22格bar，中等配置
        for (int i = 4; i <= 6; i++) {
            List<CurvePoint> curve = new ArrayList<>();
            curve.add(new CurvePoint(0.0, 1.5));
            curve.add(new CurvePoint(16.8, 1.2));
            curve.add(new CurvePoint(20.0, 0.9));
            curve.add(new CurvePoint(35.0, 0.7));
            barCurves.put(i, curve);
        }
        
        // Bar 7-9: 44格bar，高难度配置
        for (int i = 7; i <= 9; i++) {
            List<CurvePoint> curve = new ArrayList<>();
            curve.add(new CurvePoint(0.0, 2.4));
            curve.add(new CurvePoint(31.8, 1.7));
            curve.add(new CurvePoint(35.0, 1.5));
            barCurves.put(i, curve);
        }
    }
    
    public static AutoFishConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }
    
    public static void load() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    instance = GSON.fromJson(reader, AutoFishConfig.class);
                    // 确保所有bar曲线都存在
                    // Bar 1-3
                    for (int i = 1; i <= 3; i++) {
                        if (!instance.barCurves.containsKey(i) || instance.barCurves.get(i).isEmpty()) {
                            List<CurvePoint> curve = new ArrayList<>();
                            curve.add(new CurvePoint(0.0, 0.2));
                            curve.add(new CurvePoint(35.0, -0.2));
                            instance.barCurves.put(i, curve);
                        }
                    }
                    // Bar 4-6
                    for (int i = 4; i <= 6; i++) {
                        if (!instance.barCurves.containsKey(i) || instance.barCurves.get(i).isEmpty()) {
                            List<CurvePoint> curve = new ArrayList<>();
                            curve.add(new CurvePoint(0.0, 1.5));
                            curve.add(new CurvePoint(16.8, 1.2));
                            curve.add(new CurvePoint(20.0, 0.9));
                            curve.add(new CurvePoint(35.0, 0.7));
                            instance.barCurves.put(i, curve);
                        }
                    }
                    // Bar 7-9
                    for (int i = 7; i <= 9; i++) {
                        if (!instance.barCurves.containsKey(i) || instance.barCurves.get(i).isEmpty()) {
                            List<CurvePoint> curve = new ArrayList<>();
                            curve.add(new CurvePoint(0.0, 2.4));
                            curve.add(new CurvePoint(31.8, 1.7));
                            curve.add(new CurvePoint(35.0, 1.5));
                            instance.barCurves.put(i, curve);
                        }
                    }
                    AutoFishingMod.LOGGER.info("Config loaded from {}", CONFIG_FILE.getPath());
                }
            } else {
                instance = new AutoFishConfig();
                save();
                AutoFishingMod.LOGGER.info("Created default config at {}", CONFIG_FILE.getPath());
            }
        } catch (Exception e) {
            AutoFishingMod.LOGGER.error("Failed to load config", e);
            instance = new AutoFishConfig();
        }

        // 确保新增配置有默认值（负数视为异常，重置为调校后的默认延迟）
        if (instance.autoRecastMinutes < 0) {
            instance.autoRecastMinutes = 2.0;
        }
    }
    
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
            AutoFishingMod.LOGGER.info("Config saved to {}", CONFIG_FILE.getPath());
        } catch (IOException e) {
            AutoFishingMod.LOGGER.error("Failed to save config", e);
        }
    }
    
    /**
     * 根据速度从曲线获取提前量
     */
    public double getAdvanceAmountFromCurve(int barType, double speedMs) {
        List<CurvePoint> points = barCurves.get(barType);
        if (points == null || points.isEmpty()) {
            return 0.0;
        }
        
        AdvanceCurve curve = new AdvanceCurve();
        curve.setPoints(points);
        return curve.getAdvanceAmount(speedMs);
    }
}
