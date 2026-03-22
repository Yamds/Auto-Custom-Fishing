package com.autofishing.config;

/**
 * 曲线上的控制点
 */
public class CurvePoint implements Comparable<CurvePoint> {
    public double speedMs;      // 速度（毫秒/section）
    public double advanceAmount; // 提前量（格数）
    
    public CurvePoint(double speedMs, double advanceAmount) {
        this.speedMs = speedMs;
        this.advanceAmount = advanceAmount;
    }
    
    @Override
    public int compareTo(CurvePoint other) {
        return Double.compare(this.speedMs, other.speedMs);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CurvePoint)) return false;
        CurvePoint other = (CurvePoint) obj;
        return Double.compare(speedMs, other.speedMs) == 0 
            && Double.compare(advanceAmount, other.advanceAmount) == 0;
    }
    
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(speedMs);
        bits ^= Double.doubleToLongBits(advanceAmount) * 31;
        return (int)(bits ^ (bits >>> 32));
    }
}
