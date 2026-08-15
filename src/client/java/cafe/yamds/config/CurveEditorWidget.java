package cafe.yamds.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 曲线编辑器 Widget
 * 允许用户通过拖拽控制点来编辑提前量曲线
 */
public class CurveEditorWidget extends AbstractWidget {
    
    private static final int PADDING = 10;
    private static final int POINT_RADIUS = 4;
    private static final int GRID_COLOR = 0xFF404040;
    private static final int AXIS_COLOR = 0xFF808080;
    private static final int CURVE_COLOR = 0xFF00FF00;
    private static final int POINT_COLOR = 0xFFFFFFFF;
    private static final int POINT_HOVER_COLOR = 0xFFFFFF00;
    
    private final AdvanceCurve curve;
    private CurvePoint draggedPoint = null;
    private CurvePoint hoveredPoint = null;
    private boolean isDragging = false;
    
    // 坐标范围
    private static final double MIN_SPEED = 0.0;
    private static final double MAX_SPEED = 35.0;
    private static final double MIN_ADVANCE = -1.0;
    private static final double MAX_ADVANCE = 3.0;
    private static final double ADVANCE_SNAP = 0.1; // 提前量吸附单位
    private static final double SPEED_SNAP = 0.1; // 速度吸附单位
    
    public CurveEditorWidget(int x, int y, int width, int height, AdvanceCurve curve) {
        super(x, y, width, height, Component.literal("Curve Editor"));
        this.curve = curve;
    }
    
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 绘制背景
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
        
        // 计算绘图区域
        int plotX = getX() + PADDING;
        int plotY = getY() + PADDING;
        int plotWidth = width - 2 * PADDING;
        int plotHeight = height - 2 * PADDING;
        
        // 绘制网格
        drawGrid(graphics, plotX, plotY, plotWidth, plotHeight);
        
        // 绘制坐标轴
        drawAxes(graphics, plotX, plotY, plotWidth, plotHeight);
        
        // 绘制曲线
        drawCurve(graphics, plotX, plotY, plotWidth, plotHeight);
        
        // 绘制控制点
        drawPoints(graphics, plotX, plotY, plotWidth, plotHeight, mouseX, mouseY);
        
        // 绘制标签
        drawLabels(graphics, plotX, plotY, plotWidth, plotHeight);
    }
    
    private void drawGrid(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        // 垂直网格线（每5ms一条）
        for (int i = 0; i <= 7; i++) {
            int gridX = x + width - (int)(i * width / 7.0); // 从右到左
            graphics.fill(gridX, y, gridX + 1, y + height, GRID_COLOR);
        }
        
        // 水平网格线（每0.5格一条，共8条，范围-1到3）
        for (int i = 0; i <= 8; i++) {
            int gridY = y + height - (int)(i * height / 8.0);
            graphics.fill(x, gridY, x + width, gridY + 1, GRID_COLOR);
        }
    }
    
    private void drawAxes(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        // X轴（底部）
        graphics.fill(x, y + height, x + width, y + height + 1, AXIS_COLOR);
        
        // Y轴（左侧）
        graphics.fill(x, y, x + 1, y + height, AXIS_COLOR);
    }
    
    private void drawCurve(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        // 绘制曲线（采样100个点）
        int prevX = -1, prevY = -1;
        
        for (int i = 0; i <= 100; i++) {
            double speed = MAX_SPEED - (MAX_SPEED - MIN_SPEED) * i / 100.0; // 从35到0
            double advance = curve.getAdvanceAmount(speed);
            
            int curveX = x + width - (int)((speed - MIN_SPEED) / (MAX_SPEED - MIN_SPEED) * width); // 从右到左
            int curveY = y + height - (int)((advance - MIN_ADVANCE) / (MAX_ADVANCE - MIN_ADVANCE) * height);
            
            if (prevX >= 0) {
                drawLine(graphics, prevX, prevY, curveX, curveY, CURVE_COLOR);
            }
            
            prevX = curveX;
            prevY = curveY;
        }
    }
    
    private void drawPoints(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<CurvePoint> points = curve.getPoints();
        hoveredPoint = null;
        
        for (CurvePoint point : points) {
            int pointX = x + width - (int)((point.speedMs - MIN_SPEED) / (MAX_SPEED - MIN_SPEED) * width); // 从右到左
            int pointY = y + height - (int)((point.advanceAmount - MIN_ADVANCE) / (MAX_ADVANCE - MIN_ADVANCE) * height);
            
            // 检查鼠标悬停
            boolean isHovered = Math.sqrt(Math.pow(mouseX - pointX, 2) + Math.pow(mouseY - pointY, 2)) < POINT_RADIUS * 2;
            if (isHovered) {
                hoveredPoint = point;
            }
            
            int color = isHovered ? POINT_HOVER_COLOR : POINT_COLOR;
            
            // 绘制控制点
            graphics.fill(pointX - POINT_RADIUS, pointY - POINT_RADIUS, 
                         pointX + POINT_RADIUS, pointY + POINT_RADIUS, color);
            
            // 绘制点的坐标
            if (isHovered) {
                String label = String.format("%.1fms, %.1f", point.speedMs, point.advanceAmount);
                graphics.text(net.minecraft.client.Minecraft.getInstance().font, label, pointX + 8, pointY - 4, 0xFFFFFFFF);
            }
        }
    }
    
    private void drawLabels(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        
        // X轴标签（从右到左：35ms到0ms）
        graphics.text(font, "35ms", x - 15, y + height + 5, 0xFFFFFFFF);
        graphics.text(font, "0ms", x + width - 15, y + height + 5, 0xFFFFFFFF);
        graphics.text(font, "速度 (ms/section)", x + width / 2 - 40, y + height + 15, 0xFFFFFFFF);
        
        // Y轴标签
        graphics.text(font, "3", x - 15, y - 5, 0xFFFFFFFF);
        graphics.text(font, "1", x - 15, y + height / 2 - 5, 0xFFFFFFFF);
        graphics.text(font, "-1", x - 20, y + height - 5, 0xFFFFFFFF);
    }
    
    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        // 简单的线段绘制（Bresenham算法）
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            
            if (x1 == x2 && y1 == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, boolean bl) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.buttonInfo().button();
        
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        int plotX = getX() + PADDING;
        int plotY = getY() + PADDING;
        int plotWidth = width - 2 * PADDING;
        int plotHeight = height - 2 * PADDING;
        
        // 转换鼠标坐标到曲线坐标（横坐标从右到左）
        double speed = MAX_SPEED - (mouseX - plotX) / (double)plotWidth * (MAX_SPEED - MIN_SPEED);
        double advance = MAX_ADVANCE - (mouseY - plotY) / (double)plotHeight * (MAX_ADVANCE - MIN_ADVANCE);
        
        // 吸附到0.1的倍数
        speed = Math.round(speed / SPEED_SNAP) * SPEED_SNAP;
        speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
        
        advance = Math.round(advance / ADVANCE_SNAP) * ADVANCE_SNAP;
        advance = Math.max(MIN_ADVANCE, Math.min(MAX_ADVANCE, advance));
        
        if (button == 0) { // 左键
            // 检查是否点击了已有的点
            CurvePoint nearest = curve.findNearestPoint(speed, advance, 2.0);
            
            if (nearest != null) {
                // 开始拖拽
                draggedPoint = nearest;
                isDragging = true;
            } else {
                // 添加新点
                curve.addPoint(speed, advance);
            }
            return true;
        } else if (button == 1) { // 右键
            // 删除最近的点
            CurvePoint nearest = curve.findNearestPoint(speed, advance, 2.0);
            if (nearest != null) {
                curve.removePoint(nearest);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (isDragging && draggedPoint != null) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            
            int plotX = getX() + PADDING;
            int plotY = getY() + PADDING;
            int plotWidth = width - 2 * PADDING;
            int plotHeight = height - 2 * PADDING;
            
            // 转换鼠标坐标到曲线坐标（横坐标从右到左）
            double speed = MAX_SPEED - (mouseX - plotX) / (double)plotWidth * (MAX_SPEED - MIN_SPEED);
            double advance = MAX_ADVANCE - (mouseY - plotY) / (double)plotHeight * (MAX_ADVANCE - MIN_ADVANCE);
            
            // 吸附到0.1的倍数
            speed = Math.round(speed / SPEED_SNAP) * SPEED_SNAP;
            speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
            
            advance = Math.round(advance / ADVANCE_SNAP) * ADVANCE_SNAP;
            advance = Math.max(MIN_ADVANCE, Math.min(MAX_ADVANCE, advance));
            
            curve.updatePoint(draggedPoint, speed, advance);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent) {
        int button = mouseButtonEvent.buttonInfo().button();
        if (button == 0 && isDragging) {
            isDragging = false;
            draggedPoint = null;
            return true;
        }
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, 
                   Component.literal("Advance Curve Editor"));
    }
}
