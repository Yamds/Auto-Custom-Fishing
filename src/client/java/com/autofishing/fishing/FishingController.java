package com.autofishing.fishing;

import com.autofishing.AutoFishingMod;
import com.autofishing.config.AutoFishConfig;
import com.autofishing.config.FishingLog;
import com.autofishing.game.GameState;
import com.autofishing.game.TitleAnalyzer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;

import java.util.Random;

public class FishingController {
    
    private static final Random RANDOM = new Random();
    private static long lastActionTime = 0;
    private static FishingState state = FishingState.IDLE;
    private static long reeledTime = 0; // 收杆时间
    private static boolean waitingForGame = false; // 是否在等待游戏开始
    private static long castStartTime = 0; // 投竿开始时间（用于超时重抛）
    
    private enum FishingState {
        IDLE,           // 空闲
        CASTING,        // 投竿中
        WAITING_BITE,   // 等待咬钩
        FISH_BITE,      // 鱼咬钩了，准备收杆
        PLAYING_GAME,   // 游戏中
        REELING         // 收竿中
    }
    
    /**
     * 鱼咬钩回调（从 FishingHookMixin 调用）
     */
    public static void onFishBite() {
        if (state == FishingState.WAITING_BITE) {
            state = FishingState.FISH_BITE;
            sendActionBarMessage("§e⚡上钩!");
        }
    }
    
    /**
     * 发送 ActionBar 消息
     */
    private static void sendActionBarMessage(String message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(message),
                    true  // true = ActionBar
                );
            }
        } catch (Exception e) {
            // 忽略
        }
    }
    
    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        
        LocalPlayer player = client.player;
        
        // 检查是否持有鱼竿
        if (!isHoldingFishingRod(player)) {
            if (state != FishingState.IDLE) {
                reset();
            }
            return;
        }
        
        // 获取游戏状态
        GameState gameState = TitleAnalyzer.getGameState();
        long currentTime = System.currentTimeMillis();

        // 超时未收杆：强制收杆并重新抛竿
        if ((state == FishingState.CASTING || state == FishingState.WAITING_BITE)
            && shouldForceRecast(currentTime)) {
            sendActionBarMessage("§7超时未收杆，重新抛竿...");
            performClick(client);
            waitingForGame = false;
            scheduleRecast();
            reset();
            return;
        }
        
        // 状态机逻辑
        switch (state) {
            case IDLE:
                // 如果没有鱼钩实体，尝试投竿
                if (player.fishing == null) {
                    castRod(client);
                    state = FishingState.CASTING;
                }
                break;
                
            case CASTING:
                // 等待鱼钩实体生成
                if (player.fishing != null) {
                    state = FishingState.WAITING_BITE;
                }
                break;
                
            case WAITING_BITE:
                // 检查是否开始游戏（已经收杆后）
                if (gameState.isGameActive()) {
                    state = FishingState.PLAYING_GAME;
                    waitingForGame = false;
                }
                // 如果在等待游戏开始，检查超时
                else if (waitingForGame && System.currentTimeMillis() - reeledTime > 500) {
                    // 0.5秒内没有检测到游戏，说明钓到了普通物品
                    sendActionBarMessage("§7钓到普通物品，重新抛竿...");
                    waitingForGame = false;
                    scheduleRecast();
                    reset();
                }
                // 检查鱼钩是否消失
                else if (player.fishing == null) {
                    if (waitingForGame) {
                        // 鱼钩消失但没有游戏，说明钓到了普通物品
                        sendActionBarMessage("§7钓到普通物品，重新抛竿...");
                        waitingForGame = false;
                        scheduleRecast();
                    }
                    reset();
                }
                break;
                
            case FISH_BITE:
                // 鱼咬钩了，立即收杆触发小游戏
                performClick(client);
                reeledTime = System.currentTimeMillis();
                waitingForGame = true;
                castStartTime = 0;
                state = FishingState.WAITING_BITE; // 收杆后等待游戏开始
                break;
                
            case PLAYING_GAME:
                // 游戏进行中 - 检测shouldClick标志并立即收杆
                if (gameState.isGameActive()) {
                    // 检查是否应该点击（由TitleAnalyzer设置）
                    if (gameState.shouldClick()) {
                        // 立即收杆
                        AutoFishingMod.LOGGER.info("§a✓ SUCCESS POSITION! Clicking at position {}", gameState.getPointerPosition());
                        performClick(client);
                        
                        // 记录到钓鱼日志（只在收杆时记录）
                        if (gameState.getCurrentBarType() > 0) {
                            FishingLog.addLog(
                                gameState.getCurrentBarType(),
                                gameState.getCurrentSpeed(),
                                gameState.getPointerPosition()
                            );
                        }
                        
                        gameState.setShouldClick(false); // 重置标志
                        state = FishingState.REELING;
                    }
                } else if (player.fishing == null) {
                    // 游戏结束
                    AutoFishingMod.LOGGER.info("=== GAME END ===");
                    scheduleRecast();
                    reset();
                }
                break;
                
            case REELING:
                // 等待收竿完成
                if (player.fishing == null) {
                    scheduleRecast();
                    reset();
                }
                break;
        }
    }
    
    /**
     * 执行点击（收竿）
     */
    private static void performClick(Minecraft client) {
        if (client.gameMode != null && client.player != null) {
            client.gameMode.useItem(
                client.player,
                InteractionHand.MAIN_HAND
            );
            lastActionTime = System.currentTimeMillis();
            sendActionBarMessage("§d⚡上钩!");
        }
    }
    
    /**
     * 投竿
     */
    private static void castRod(Minecraft client) {
        long currentTime = System.currentTimeMillis();
        
        // 防止操作过快
        if (currentTime - lastActionTime < 500) {
            return;
        }
        
        if (client.gameMode != null && client.player != null) {
            client.gameMode.useItem(
                client.player,
                InteractionHand.MAIN_HAND
            );
            lastActionTime = currentTime;
            castStartTime = currentTime;
            sendActionBarMessage("§b🎣等待咬钩...");
        }
    }
    
    /**
     * 安排重新投竿（间隔可配置，带随机浮动）
     */
    private static void scheduleRecast() {
        // 基础间隔 ± 随机浮动（秒），模拟真人操作节奏
        AutoFishConfig cfg = AutoFishConfig.get();
        double base = Math.max(0.1, cfg.castIntervalSeconds);
        double range = Math.max(0.0, cfg.castIntervalRandomSeconds);
        double delayMs = (base - range + RANDOM.nextDouble() * range * 2.0) * 1000.0;
        if (delayMs < 200) {
            delayMs = 200; // 下限保护，避免操作过快
        }
        final long sleepMs = (long) delayMs;
        new Thread(() -> {
            try {
                Thread.sleep(sleepMs);
                state = FishingState.IDLE;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 检查是否持有鱼竿
     */
    private static boolean isHoldingFishingRod(LocalPlayer player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        return mainHand.getItem() == Items.FISHING_ROD || offHand.getItem() == Items.FISHING_ROD;
    }
    
    /**
     * 重置状态
     */
    private static void reset() {
        state = FishingState.IDLE;
        waitingForGame = false;
        reeledTime = 0;
        castStartTime = 0;
        TitleAnalyzer.reset();
    }

    private static boolean shouldForceRecast(long currentTime) {
        double minutes = AutoFishConfig.get().autoRecastMinutes;
        if (minutes <= 0 || castStartTime <= 0) {
            return false;
        }
        long timeoutMs = (long) (minutes * 60_000L);
        return currentTime - castStartTime >= timeoutMs;
    }
}
