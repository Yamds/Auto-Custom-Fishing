package cafe.yamds.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * 自动钓鱼配置界面
 * 使用Cloth Config API构建
 */
public class AutoFishConfigScreen {
    
    public static Screen createConfigScreen(Screen parent) {
        AutoFishConfig config = AutoFishConfig.get();
        
        // 创建配置构建器
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.autofishing.title"))
            .setSavingRunnable(AutoFishConfig::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // ========== 主设置分类 ==========
        ConfigCategory mainCategory = builder.getOrCreateCategory(
            Component.translatable("config.autofishing.category.main")
        );
        
        // Mod总开关
        mainCategory.addEntry(entryBuilder
            .startBooleanToggle(
                Component.translatable("config.autofishing.mod_enabled"),
                config.modEnabled
            )
            .setDefaultValue(true)
            .setTooltip(Component.translatable("config.autofishing.mod_enabled.tooltip"))
            .setSaveConsumer(value -> config.modEnabled = value)
            .build()
        );
        
        // 全局提前量
        mainCategory.addEntry(entryBuilder
            .startDoubleField(
                Component.translatable("config.autofishing.global_advance"),
                config.globalAdvance
            )
            .setDefaultValue(0.0)
            .setMin(-1.0)
            .setMax(3.0)
            .setTooltip(Component.translatable("config.autofishing.global_advance.tooltip"))
            .setSaveConsumer(value -> config.globalAdvance = value)
            .build()
        );

        // 自动收杆时间（分钟）
        mainCategory.addEntry(entryBuilder
            .startDoubleField(
                Component.translatable("config.autofishing.auto_recast_minutes"),
                config.autoRecastMinutes
            )
            .setDefaultValue(2.0)
            .setMin(0.0)
            .setTooltip(Component.translatable("config.autofishing.auto_recast_minutes.tooltip"))
            .setSaveConsumer(value -> config.autoRecastMinutes = value)
            .build()
        );

        // 抛竿间隔（秒）
        mainCategory.addEntry(entryBuilder
            .startDoubleField(
                Component.translatable("config.autofishing.cast_interval"),
                config.castIntervalSeconds
            )
            .setDefaultValue(1.0)
            .setMin(0.1)
            .setMax(10.0)
            .setTooltip(Component.translatable("config.autofishing.cast_interval.tooltip"))
            .setSaveConsumer(value -> config.castIntervalSeconds = value)
            .build()
        );

        // 抛竿间隔随机浮动（秒）
        mainCategory.addEntry(entryBuilder
            .startDoubleField(
                Component.translatable("config.autofishing.cast_interval_random"),
                config.castIntervalRandomSeconds
            )
            .setDefaultValue(0.5)
            .setMin(0.0)
            .setMax(5.0)
            .setTooltip(Component.translatable("config.autofishing.cast_interval_random.tooltip"))
            .setSaveConsumer(value -> config.castIntervalRandomSeconds = value)
            .build()
        );
        
        // TODO: 快捷键设置需要使用Fabric Key Binding API
        mainCategory.addEntry(entryBuilder
            .startTextDescription(
                Component.translatable("config.autofishing.keybind.info")
            )
            .build()
        );
        
        // ========== 钓鱼日志 ==========
        ConfigCategory logCategory = builder.getOrCreateCategory(
            Component.translatable("config.autofishing.category.logs")
        );
        
        // 日志标题
        logCategory.addEntry(entryBuilder
            .startTextDescription(
                Component.translatable("config.autofishing.logs.title")
            )
            .build()
        );
        
        // 显示最近20条日志
        List<FishingLog.LogEntry> logs = FishingLog.getLogs();
        if (logs.isEmpty()) {
            logCategory.addEntry(entryBuilder
                .startTextDescription(
                    Component.translatable("config.autofishing.logs.empty")
                )
                .build()
            );
        } else {
            for (FishingLog.LogEntry log : logs) {
                // 日志行：Bar编号 + 速度档位
                logCategory.addEntry(entryBuilder
                    .startTextDescription(
                        Component.literal(String.format("Bar %d | %.1fms",
                            log.barNum, log.avgSpeed))
                    )
                    .build()
                );
                
                // 彩色条形图（高亮点击位置）
                logCategory.addEntry(entryBuilder
                    .startTextDescription(
                        BarVisualizer.generateBarVisualization(log.barNum, log.clickPosition)
                    )
                    .build()
                );
            }
        }
        
        // ========== Bar1-9 预判设置（展开式） ==========
        ConfigCategory predictionCategory = builder.getOrCreateCategory(
            Component.translatable("config.autofishing.category.prediction")
        );
        
        for (int barNum = 1; barNum <= 9; barNum++) {
            createBarCategoryInline(predictionCategory, entryBuilder, barNum);
        }
        
        return builder.build();
    }
    
    /**
     * 在同一分类中创建Bar配置（展开式）
     */
    private static void createBarCategoryInline(ConfigCategory category, ConfigEntryBuilder entryBuilder, 
                                               int barNum) {
        // 创建final变量供匿名内部类使用
        final int finalBarNum = barNum;
        
        // Bar标题
        category.addEntry(entryBuilder
            .startTextDescription(
                Component.literal("§l§n" + String.format("Bar %d", barNum))
            )
            .build()
        );

        // Bar可视化 - 显示彩色条形图
        category.addEntry(entryBuilder
                .startTextDescription(
                        BarVisualizer.generateBarVisualization(barNum)
                )
                .build()
        );
        
        // 创建一个按钮来打开曲线编辑器
        // 使用Cloth Config的自定义widget功能
        category.addEntry(new me.shedaniel.clothconfig2.api.AbstractConfigListEntry<Void>(
            Component.literal("§6§l打开 Bar " + barNum + " 曲线编辑器"),
            false
        ) {
            private net.minecraft.client.gui.components.Button button;
            
            @Override
            public Void getValue() {
                return null;
            }
            
            @Override
            public java.util.Optional<Void> getDefaultValue() {
                return java.util.Optional.empty();
            }
            
            @Override
            public void save() {
                // 不需要保存
            }
            
            @Override
            public int getItemHeight() {
                return 20;
            }
            
            @Override
            public java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return button == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(button);
            }
            
            @Override
            public java.util.List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
                return button == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(button);
            }
            
            @Override
            public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int index, int y, int x, 
                             int entryWidth, int entryHeight, int mouseX, int mouseY, 
                             boolean isHovered, float delta) {
                if (button == null) {
                    button = net.minecraft.client.gui.components.Button.builder(
                        Component.literal("打开曲线编辑器"),
                        btn -> {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                            net.minecraft.client.gui.screens.Screen currentScreen = mc.gui.screen();
                            mc.gui.setScreen(new CurveEditorScreen(currentScreen, finalBarNum));
                        }
                    )
                    .bounds(x + entryWidth / 2 - 75, y, 150, 20)
                    .build();
                } else {
                    button.setX(x + entryWidth / 2 - 75);
                    button.setY(y);
                }
                button.extractRenderState(graphics, mouseX, mouseY, delta);
            }
        });

        // 分隔线
        category.addEntry(entryBuilder
            .startTextDescription(
                Component.literal("§7§m" + "─".repeat(50))
            )
            .build()
        );
    }
}
