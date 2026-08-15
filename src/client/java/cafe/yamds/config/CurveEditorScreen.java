package cafe.yamds.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 曲线编辑器界面
 */
public class CurveEditorScreen extends Screen {
    
    private final Screen parent;
    private final int barNum;
    private final AdvanceCurve curve;
    private CurveEditorWidget curveWidget;
    
    public CurveEditorScreen(Screen parent, int barNum) {
        super(Component.literal("Bar " + barNum + " 提前量曲线编辑器"));
        this.parent = parent;
        this.barNum = barNum;
        
        // 从配置加载曲线
        List<CurvePoint> points = AutoFishConfig.get().barCurves.get(barNum);
        this.curve = new AdvanceCurve();
        if (points != null && !points.isEmpty()) {
            this.curve.setPoints(points);
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 创建曲线编辑器 Widget
        int editorWidth = Math.min(600, width - 40);
        int editorHeight = Math.min(400, height - 100);
        int editorX = (width - editorWidth) / 2;
        int editorY = 40;
        
        curveWidget = new CurveEditorWidget(editorX, editorY, editorWidth, editorHeight, curve);
        addRenderableWidget(curveWidget);
        
        // 添加按钮
        int buttonY = editorY + editorHeight + 10;
        
        // 保存按钮
        addRenderableWidget(Button.builder(
            Component.literal("保存"),
            button -> {
                // 保存曲线到配置
                AutoFishConfig.get().barCurves.put(barNum, curve.getPoints());
                AutoFishConfig.save();
                minecraft.gui.setScreen(parent);
            }
        ).bounds(width / 2 - 105, buttonY, 100, 20).build());
        
        // 取消按钮
        addRenderableWidget(Button.builder(
            Component.literal("取消"),
            button -> minecraft.gui.setScreen(parent)
        ).bounds(width / 2 + 5, buttonY, 100, 20).build());
        
        // 重置按钮
        addRenderableWidget(Button.builder(
            Component.literal("重置为默认"),
            button -> {
                // 根据不同的bar设置不同的默认曲线
                List<CurvePoint> defaultCurve = new ArrayList<>();
                if (barNum >= 1 && barNum <= 3) {
                    // Bar 1-3
                    defaultCurve.add(new CurvePoint(0.0, 0.2));
                    defaultCurve.add(new CurvePoint(35.0, -0.2));
                } else if (barNum >= 4 && barNum <= 6) {
                    // Bar 4-6
                    defaultCurve.add(new CurvePoint(0.0, 1.5));
                    defaultCurve.add(new CurvePoint(16.8, 1.2));
                    defaultCurve.add(new CurvePoint(20.0, 0.9));
                    defaultCurve.add(new CurvePoint(35.0, 0.7));
                } else if (barNum >= 7 && barNum <= 9) {
                    // Bar 7-9
                    defaultCurve.add(new CurvePoint(0.0, 2.4));
                    defaultCurve.add(new CurvePoint(31.8, 1.7));
                    defaultCurve.add(new CurvePoint(35.0, 1.5));
                }
                curve.setPoints(defaultCurve);
            }
        ).bounds(width / 2 - 210, buttonY, 100, 20).build());
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染所有组件（包括背景）
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        
        // 绘制标题
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        
        // 绘制说明
        String help = "左键：添加/拖动点 | 右键：删除点 | 横轴：速度(ms) | 纵轴：提前量(格)";
        graphics.centeredText(font, help, width / 2, height - 30, 0xFFAAAAAA);
    }
    
    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
