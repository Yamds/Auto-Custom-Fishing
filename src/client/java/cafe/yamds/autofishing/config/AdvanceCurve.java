package cafe.yamds.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 提前量曲线
 * 根据速度（ms/section）计算提前量
 */
public class AdvanceCurve {
    
    private List<CurvePoint> points = new ArrayList<>();
    
    public AdvanceCurve() {
        // 默认曲线：3个点
        points.add(new CurvePoint(0.0, 1.0));    // 最快速度：提前1格
        points.add(new CurvePoint(15.0, 0.5));   // 中等速度：提前0.5格
        points.add(new CurvePoint(35.0, 0.0));   // 最慢速度：不提前
        Collections.sort(points);
    }
    
    /**
     * 添加控制点
     */
    public void addPoint(double speedMs, double advanceAmount) {
        // 限制范围
        speedMs = Math.max(0.0, Math.min(35.0, speedMs));
        advanceAmount = Math.max(-5.0, Math.min(5.0, advanceAmount));
        
        points.add(new CurvePoint(speedMs, advanceAmount));
        Collections.sort(points);
    }
    
    /**
     * 移除控制点
     */
    public void removePoint(CurvePoint point) {
        points.remove(point);
        // 至少保留2个点
        if (points.size() < 2) {
            points.clear();
            points.add(new CurvePoint(0.0, 1.0));
            points.add(new CurvePoint(35.0, 0.0));
        }
    }
    
    /**
     * 更新控制点位置
     */
    public void updatePoint(CurvePoint point, double newSpeedMs, double newAdvanceAmount) {
        // 限制范围
        newSpeedMs = Math.max(0.0, Math.min(35.0, newSpeedMs));
        newAdvanceAmount = Math.max(-5.0, Math.min(5.0, newAdvanceAmount));
        
        point.speedMs = newSpeedMs;
        point.advanceAmount = newAdvanceAmount;
        Collections.sort(points);
    }
    
    /**
     * 获取所有控制点
     */
    public List<CurvePoint> getPoints() {
        return new ArrayList<>(points);
    }
    
    /**
     * 设置所有控制点
     */
    public void setPoints(List<CurvePoint> newPoints) {
        this.points = new ArrayList<>(newPoints);
        Collections.sort(this.points);
        
        // 至少保留2个点
        if (this.points.size() < 2) {
            this.points.clear();
            this.points.add(new CurvePoint(0.0, 1.0));
            this.points.add(new CurvePoint(35.0, 0.0));
        }
    }
    
    /**
     * 根据速度计算提前量（线性插值）
     */
    public double getAdvanceAmount(double speedMs) {
        if (points.isEmpty()) {
            return 0.0;
        }
        
        if (points.size() == 1) {
            return points.get(0).advanceAmount;
        }
        
        // 如果速度小于最小点，使用最小点的值
        if (speedMs <= points.get(0).speedMs) {
            return points.get(0).advanceAmount;
        }
        
        // 如果速度大于最大点，使用最大点的值
        if (speedMs >= points.get(points.size() - 1).speedMs) {
            return points.get(points.size() - 1).advanceAmount;
        }
        
        // 线性插值
        for (int i = 0; i < points.size() - 1; i++) {
            CurvePoint p1 = points.get(i);
            CurvePoint p2 = points.get(i + 1);
            
            if (speedMs >= p1.speedMs && speedMs <= p2.speedMs) {
                // 线性插值公式
                double t = (speedMs - p1.speedMs) / (p2.speedMs - p1.speedMs);
                return p1.advanceAmount + t * (p2.advanceAmount - p1.advanceAmount);
            }
        }
        
        return 0.0;
    }
    
    /**
     * 查找距离指定位置最近的控制点
     */
    public CurvePoint findNearestPoint(double speedMs, double advanceAmount, double threshold) {
        CurvePoint nearest = null;
        double minDistance = threshold;
        
        for (CurvePoint point : points) {
            double distance = Math.sqrt(
                Math.pow(point.speedMs - speedMs, 2) + 
                Math.pow(point.advanceAmount - advanceAmount, 2)
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = point;
            }
        }
        
        return nearest;
    }
}
