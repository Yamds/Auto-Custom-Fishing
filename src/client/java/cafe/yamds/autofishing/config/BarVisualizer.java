package cafe.yamds.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

/**
 * Bar可视化工具
 * 根据成功率生成彩色条形图
 */
public class BarVisualizer {
    
    // 成功率配置（从yml文件中提取）
    private static final double[][] BAR_SUCCESS_RATES = {
        // Bar 1 (11格)
        {0, 0, 0, 0.2, 0.6, 1, 0.6, 0.2, 0, 0, 0},
        // Bar 2 (11格)
        {0, 0.2, 0.6, 1, 0.6, 0.2, 0, 0, 0, 0, 0},
        // Bar 3 (11格)
        {0, 0, 0, 0, 0, 0.2, 0.6, 1, 0.6, 0.2, 0},
        // Bar 4 (22格)
        {0, 0, 0.1, 0.1, 0, 0, 0.2, 0.2, 0.6, 0.6, 1, 0.6, 0.2, 0, 0.2, 0.6, 1, 0.6, 0.2, 0.1, 0, 0},
        // Bar 5 (22格)
        {1, 0, 0.1, 0.2, 0.2, 0.6, 0.6, 1, 0.6, 0.2, 0, 0.2, 0.6, 1, 0.6, 0.2, 0, 1, 0.2, 0.1, 0, 0},
        // Bar 6 (22格)
        {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 1, 0, 1, 0, 1, 0, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
        // Bar 7 (44格)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.1, 0.1, 0, 0, 1, 0, 0, 0.3, 0.3, 0.3},
        // Bar 8 (44格)
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.2, 0.2, 0.2, 0.6, 1, 0.6, 0.2, 0.2, 0.2, 0.2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        // Bar 9 (44格)
        {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
    };
    
    /**
     * 生成Bar的可视化表示
     * @param barNum Bar编号 (1-9)
     * @return 彩色条形图Component
     */
    public static Component generateBarVisualization(int barNum) {
        return generateBarVisualization(barNum, -1);
    }
    
    /**
     * 生成Bar的可视化表示（带高亮位置）
     * @param barNum Bar编号 (1-9)
     * @param highlightPosition 高亮位置（1-based，-1表示不高亮）
     * @return 彩色条形图Component
     */
    public static Component generateBarVisualization(int barNum, int highlightPosition) {
        if (barNum < 1 || barNum > 9) {
            return Component.literal("Invalid bar number");
        }
        
        double[] rates = BAR_SUCCESS_RATES[barNum - 1];
        MutableComponent result = Component.literal("");
        
        // 所有bar都使用█符号
        String symbol = "█";
        
        for (int i = 0; i < rates.length; i++) {
            double rate = rates[i];
            // 如果是高亮位置，使用白色
            if (highlightPosition > 0 && i == highlightPosition - 1) {
                result.append(Component.literal(symbol).withStyle(ChatFormatting.WHITE));
            } else {
                ChatFormatting color = getColorForRate(rate);
                result.append(Component.literal(symbol).withStyle(color));
            }
        }
        
        return result;
    }
    
    /**
     * 根据成功率获取颜色
     * @param rate 成功率 (0.0-1.0)
     * @return 对应的颜色
     */
    private static ChatFormatting getColorForRate(double rate) {
        if (rate >= 1.0) {
            return ChatFormatting.GREEN;  // 绿色
        } else if (rate >= 0.6) {
            return ChatFormatting.YELLOW;  // 黄色
        } else if (rate >= 0.2) {
            return ChatFormatting.GOLD;  // 橙色
        } else {
            return ChatFormatting.RED;  // 红色
        }
    }
    
    /**
     * 获取Bar的长度
     * @param barNum Bar编号 (1-9)
     * @return Bar的格子数
     */
    public static int getBarLength(int barNum) {
        if (barNum < 1 || barNum > 9) {
            return 0;
        }
        return BAR_SUCCESS_RATES[barNum - 1].length;
    }
}
