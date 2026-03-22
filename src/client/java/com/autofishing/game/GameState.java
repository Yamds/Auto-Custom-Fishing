package com.autofishing.game;

import com.autofishing.config.FishingLog;

public class GameState {
    
    public enum GameType {
        NONE,
        ACCURATE_CLICK,  // 河钓游戏
        TENSION,         // 海钓游戏
        HOLD,            // 按住游戏
        UNKNOWN
    }
    
    private GameType currentGame = GameType.NONE;
    private int pointerPosition = -1;
    private int lastPointerPosition = -1;
    private long lastUpdateTime = 0;
    private boolean movingRight = true;
    private double speed = 0;
    private int totalWidth = 0;
    private int targetStart = -1;
    private int targetEnd = -1;
    private boolean gameActive = false;
    private boolean shouldClick = false; // 标志：是否应该立即点击
    
    // 用于记录日志的数据
    private int currentBarType = -1;
    private double currentSpeed = 0.0;
    
    public void reset() {
        currentGame = GameType.NONE;
        pointerPosition = -1;
        lastPointerPosition = -1;
        lastUpdateTime = 0;
        movingRight = true;
        speed = 0;
        totalWidth = 0;
        targetStart = -1;
        targetEnd = -1;
        gameActive = false;
        shouldClick = false;
        currentBarType = -1;
        currentSpeed = 0.0;
    }
    
    public void updatePointerPosition(int position) {
        long currentTime = System.currentTimeMillis();
        
        if (lastPointerPosition != -1 && lastUpdateTime > 0) {
            int delta = position - lastPointerPosition;
            long timeDelta = currentTime - lastUpdateTime;
            
            if (delta != 0 && timeDelta > 0) {
                movingRight = delta > 0;
                speed = Math.abs(delta) / (double) timeDelta;
            }
        }
        
        lastPointerPosition = pointerPosition;
        pointerPosition = position;
        lastUpdateTime = currentTime;
    }
    
    // Getters and Setters
    public GameType getCurrentGame() { return currentGame; }
    public void setCurrentGame(GameType type) { this.currentGame = type; }
    
    public int getPointerPosition() { return pointerPosition; }
    
    public boolean isMovingRight() { return movingRight; }
    
    public double getSpeed() { return speed; }
    
    public int getTotalWidth() { return totalWidth; }
    public void setTotalWidth(int width) { this.totalWidth = width; }
    
    public int getTargetStart() { return targetStart; }
    public void setTargetStart(int start) { this.targetStart = start; }
    
    public int getTargetEnd() { return targetEnd; }
    public void setTargetEnd(int end) { this.targetEnd = end; }
    
    public boolean isGameActive() { return gameActive; }
    public void setGameActive(boolean active) { this.gameActive = active; }
    
    public boolean shouldClick() { return shouldClick; }
    public void setShouldClick(boolean should) { this.shouldClick = should; }
    
    public int getCurrentBarType() { return currentBarType; }
    public void setCurrentBarType(int barType) { this.currentBarType = barType; }
    
    public double getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(double speed) { this.currentSpeed = speed; }

}
