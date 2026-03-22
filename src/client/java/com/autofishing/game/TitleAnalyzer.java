package com.autofishing.game;

import com.autofishing.config.AutoFishConfig;
import com.autofishing.config.FishingLog;

public class TitleAnalyzer {
    
    private static final GameState gameState = new GameState();
    
    // 速度检测相关
    private static final int MAX_POSITION_HISTORY = 50;
    private static long[] positionTimestamps = new long[MAX_POSITION_HISTORY];
    private static int[] positionValues = new int[MAX_POSITION_HISTORY];
    private static int historyIndex = 0;
    private static int historyCount = 0;
    private static double averageSpeed = 0.0; // 毫秒/section
    
    public static GameState getGameState() {
        return gameState;
    }
    
    /**
     * 分析Title文本，判断游戏类型
     */
    public static void analyzeTitle(String jsonText) {
        try {
            if (jsonText == null || jsonText.isEmpty()) {
                return;
            }
            
            // 检测是否包含游戏相关的关键字
            if (jsonText.contains("progress") || jsonText.contains("Progress")) {
                gameState.setGameActive(true);
            }
            
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 分析Subtitle文本，提取指针位置
     * 
     * Subtitle结构（4部分）：
     * 1. barImage (default font) - 进度条图形
     * 2. offset_chars - 第一组偏移 = pointerOffset + progress = -183 + progress
     * 3. pointerImage (default font) - 指针图形
     * 4. offset_chars - 第二组偏移 = totalWidth - progress - pointerWidth = 175 - progress - 5
     */
    public static void analyzeSubtitle(String jsonText) {
        try {
            if (jsonText == null || jsonText.isEmpty()) {
                return;
            }
            
            // 只处理 Component.toString() 的输出（包含 font= 信息）
            if (!jsonText.contains("font=") || !jsonText.contains("customfishing")) {
                return;
            }
            
            // 从 Component.toString() 输出中提取信息
            int barType = -1;
            int firstOffset = 0;  // 第一组 offset_chars
            int secondOffset = 0; // 第二组 offset_chars
            int offsetGroupCount = 0;
            
            // 查找所有 literal{...}[style={font=...}] 的部分
            int pos = 0;
            while ((pos = jsonText.indexOf("literal{", pos)) != -1) {
                int endBrace = jsonText.indexOf("}", pos);
                if (endBrace == -1) break;
                
                // 提取字符
                String chars = jsonText.substring(pos + 8, endBrace);
                
                // 检查样式
                int styleStart = jsonText.indexOf("[style=", endBrace);
                if (styleStart != -1 && styleStart - endBrace < 5) {
                    int styleEnd = jsonText.indexOf("]", styleStart);
                    if (styleEnd != -1) {
                        String style = jsonText.substring(styleStart, styleEnd);
                        
                        // 检查是否是 offset_chars 字体
                        if (style.contains("customfishing:offset_chars")) {
                            int groupOffset = 0;
                            for (char c : chars.toCharArray()) {
                                int offset = getOffsetForChar(c);
                                if (offset != 0) {
                                    groupOffset += offset;
                                }
                            }
                            
                            // 分别记录第一组和第二组偏移
                            if (offsetGroupCount == 0) {
                                firstOffset = groupOffset;
                                offsetGroupCount++;
                            } else if (offsetGroupCount == 1) {
                                secondOffset = groupOffset;
                                offsetGroupCount++;
                            }
                        }
                        // 检查是否是 default 字体（包含进度条）
                        else if (style.contains("customfishing:default")) {
                            for (char c : chars.toCharArray()) {
                                int bar = getBarType(c);
                                if (bar > 0) {
                                    barType = bar;
                                }
                            }
                        }
                    }
                }
                
                pos = endBrace + 1;
            }
            
            if (offsetGroupCount == 2 && barType > 0) {
                // 如果是游戏刚开始，标记游戏激活
                if (!gameState.isGameActive()) {
                    gameState.setGameActive(true);
                    gameState.setCurrentGame(GameState.GameType.ACCURATE_CLICK);
                }
                
                // 根据 bar 类型获取参数
                int maxPosition = getMaxPositionForBar(barType);
                int widthPerSection = getWidthPerSection(barType);
                int totalWidth = maxPosition * widthPerSection - 1;
                int pointerOffset = -183;
                int pointerWidth = 5;
                
                // 从第一组偏移计算 progress
                // firstOffset = pointerOffset + progress = -183 + progress
                // progress = firstOffset - pointerOffset = firstOffset + 183
                int progress = firstOffset - pointerOffset;
                
                // 验证：第二组偏移应该等于 totalWidth - progress - pointerWidth
                int expectedSecondOffset = totalWidth - progress - pointerWidth;
                
                // 计算位置 (1-based)
                // 使用四舍五入到section中心，减少边界波动
                // 例如：widthPerSection=8时，progress=12 → (12+4)/8 = 2 → position=2
                int position = (progress + widthPerSection / 2) / widthPerSection + 1;
                position = Math.max(1, Math.min(maxPosition, position));
                
                // 记录position和时间戳，用于速度计算
                long currentTime = System.nanoTime();
                recordPositionTimestamp(position, currentTime);
                
                // 计算平均速度（毫秒/section）
                calculateAverageSpeed();
                
                gameState.updatePointerPosition(position);
                
                // 判断是否应该点击（包含时间预判）
                boolean shouldClickNow = false;
                int targetPosition = position;
                
                // 从曲线获取提前量（新系统）
                double configAdvance = AutoFishConfig.get().getAdvanceAmountFromCurve(barType, averageSpeed);
                
                // 加上全局提前量
                double totalAdvance = configAdvance + AutoFishConfig.get().globalAdvance;
                
                // 时间预判：只对bar22和bar44，且position>=3，且速度达到hard下限
                if ((maxPosition == 22 || maxPosition == 44) 
                        && position >= 3 
                        && historyCount >= 3
                        && averageSpeed > 0) {
                    // 查找下一个成功位置
                    int nextSuccessPos = findNextSuccessPosition(barType, position, maxPosition);
                    
                    if (nextSuccessPos > 0) {
                        // 计算到达成功位置需要的时间（毫秒）
                        int positionsToGo = nextSuccessPos - position;
                        double timeToSuccess = positionsToGo * averageSpeed; // 毫秒
                        
                        // 根据配置的提前量计算提前时间
                        double advanceTime = totalAdvance * averageSpeed; // 提前量(格) × 速度(ms/格) = 提前时间(ms)
                        
                        // 判断是否应该点击：当前时间 + 提前时间 >= 到达成功位置的时间
                        if (timeToSuccess <= advanceTime) {
                            shouldClickNow = true;
                            targetPosition = nextSuccessPos;
                        } else if (positionsToGo == 0) {
                            // 到达成功位置但不需要提前
                            shouldClickNow = true;
                            targetPosition = nextSuccessPos;
                        }
                    }
                }
                else if (getSuccessRate(barType, position) >= 1.0) {
                    shouldClickNow = true;
                }
                
                // 判断是否在成功位置
                boolean isSuccessPosition = getSuccessRate(barType, position) >= 1.0;
                
                // 在ActionBar显示位置、速度（中文）
                double successRate = getSuccessRate(barType, position);
                String colorCode;
                String colorName;
                if (successRate >= 1.0) {
                    colorCode = "§a"; // 绿色
                    colorName = "绿色";
                } else if (successRate >= 0.6) {
                    colorCode = "§e"; // 黄色
                    colorName = "黄色";
                } else if (successRate >= 0.2) {
                    colorCode = "§6"; // 橙色（金色）
                    colorName = "橙色";
                } else {
                    colorCode = "§c"; // 红色
                    colorName = "红色";
                }
                
                sendActionBarMessage(String.format("%sBar %d §7| §f位置: %d/%d §7| %s%s §7| §f速度: %.1fms §7",
                    colorCode, barType, position, maxPosition, colorCode, colorName, averageSpeed));
                
                // 如果应该点击，触发点击（不在这里记录日志）
                if (shouldClickNow) {
                    gameState.setTargetStart(targetPosition);
                    gameState.setTargetEnd(targetPosition);
                    gameState.setShouldClick(true); // 设置标志，让FishingController执行点击
                    
                    // 保存当前游戏数据，供FishingController记录日志使用
                    gameState.setCurrentBarType(barType);
                    gameState.setCurrentSpeed(averageSpeed);
                } else {
                    gameState.setTargetStart(-1);
                    gameState.setTargetEnd(-1);
                    gameState.setShouldClick(false);
                }
            }
            
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 获取进度条类型
     */
    private static int getBarType(char c) {
        switch (c) {
            case '\ub001': return 0; // pointer
            case '\ub002': return 1; // bar1 (11 sections)
            case '\ub003': return 2; // bar2 (11 sections)
            case '\ub004': return 3; // bar3 (11 sections)
            case '\ub005': return 4; // bar4 (22 sections)
            case '\ub006': return 5; // bar5 (22 sections)
            case '\ub007': return 6; // bar6 (22 sections)
            case '\ub008': return 7; // bar7 (44 sections)
            case '\ub009': return 8; // bar8 (44 sections)
            case '\ub00a': return 9; // bar9 (44 sections)
            case '\ub00b': return 10; // bar_rainbow
            case '\ub00c': return 11; // bar10
            default: return -1;
        }
    }
    
    /**
     * 获取每个bar的最大位置数
     */
    private static int getMaxPositionForBar(int barType) {
        switch (barType) {
            case 1:
            case 2:
            case 3:
                return 11; // bar1-3 有11个section
            case 4:
            case 5:
            case 6:
                return 22; // bar4-6 有22个section
            case 7:
            case 8:
            case 9:
                return 44; // bar7-9 有44个section
            default:
                return 11;
        }
    }
    
    /**
     * 获取每个section的宽度（像素）
     */
    private static int getWidthPerSection(int barType) {
        switch (barType) {
            case 1:
            case 2:
            case 3:
                return 16; // bar1-3 每个section 16像素
            case 4:
            case 5:
            case 6:
                return 8; // bar4-6 每个section 8像素
            case 7:
            case 8:
            case 9:
                return 4; // bar7-9 每个section 4像素
            default:
                return 16;
        }
    }
    
    /**
     * 记录position和时间戳
     */
    private static void recordPositionTimestamp(int position, long timestamp) {
        positionTimestamps[historyIndex] = timestamp;
        positionValues[historyIndex] = position;
        historyIndex = (historyIndex + 1) % MAX_POSITION_HISTORY;
        if (historyCount < MAX_POSITION_HISTORY) {
            historyCount++;
        }
    }
    
    /**
     * 计算平均速度（毫秒/section）
     * 使用最近的position变化来计算
     */
    private static void calculateAverageSpeed() {
        if (historyCount < 2) {
            averageSpeed = 0.0;
            return;
        }
        
        // 计算所有相邻position之间的时间差
        double totalTime = 0.0;
        int totalPositionChange = 0;
        int validSamples = 0;
        
        for (int i = 1; i < historyCount; i++) {
            int prevIdx = (historyIndex - historyCount + i - 1 + MAX_POSITION_HISTORY) % MAX_POSITION_HISTORY;
            int currIdx = (historyIndex - historyCount + i + MAX_POSITION_HISTORY) % MAX_POSITION_HISTORY;
            
            int posChange = Math.abs(positionValues[currIdx] - positionValues[prevIdx]);
            long timeDiff = positionTimestamps[currIdx] - positionTimestamps[prevIdx];
            
            // 只统计相邻position（变化1-3个section，过滤掉反转等异常情况）
            if (posChange > 0 && posChange <= 3 && timeDiff > 0) {
                totalTime += timeDiff / 1_000_000.0; // 纳秒转毫秒
                totalPositionChange += posChange;
                validSamples++;
            }
        }
        
        if (validSamples > 0 && totalPositionChange > 0) {
            averageSpeed = totalTime / totalPositionChange; // 毫秒/section
        } else {
            averageSpeed = 0.0;
        }
    }
    
    /**
     * 查找下一个成功位置（从当前位置向前查找）
     * @return 下一个成功位置，如果没有则返回-1
     */
    private static int findNextSuccessPosition(int barType, int currentPosition, int maxPosition) {
        // 向前查找最多5个位置
        for (int ahead = 0; ahead <= 5; ahead++) {
            int checkPos = currentPosition + ahead;
            if (checkPos > maxPosition) {
                break;
            }
            if (getSuccessRate(barType, checkPos) >= 1.0) {
                return checkPos;
            }
        }
        return -1;
    }
    
    /**
     * 发送 ActionBar 消息
     */
    private static void sendActionBarMessage(String message) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
    
    /**
     * 将字符串转换为码点表示（用于显示Unicode私用区字符）
     */
    private static String toCodePoints(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        str.codePoints().forEach(cp -> {
            // 如果是私用区字符（U+E000-U+F8FF, U+F0000-U+FFFFD, U+100000-U+10FFFD）
            // 或者是我们关心的特殊字符，显示为码点
            if ((cp >= 0xE000 && cp <= 0xF8FF) || 
                (cp >= 0xF0000 && cp <= 0xFFFFD) || 
                (cp >= 0x100000 && cp <= 0x10FFFD) ||
                (cp >= 0xB000 && cp <= 0xB0FF) ||  // customfishing bar/pointer
                (cp >= 0xF800 && cp <= 0xF8FF)) {  // customfishing offset_chars
                result.append("U+").append(String.format("%04X", cp));
            } else {
                result.appendCodePoint(cp);
            }
        });
        
        return result.toString();
    }
    
    /**
     * 判断当前位置是否是成功位置（success-rate = 1.0）
     * 根据 default.yml 中的 success-rate-sections 配置
     */
    private static boolean isSuccessPosition(int barType, int position) {
        return getSuccessRate(barType, position) >= 1.0;
    }
    
    /**
     * 获取当前位置的成功率
     * 根据 default.yml 中的 success-rate-sections 配置
     */
    private static double getSuccessRate(int barType, int position) {
        switch (barType) {
            case 1: // bar1
                if (position == 6) return 1.0;
                if (position == 5 || position == 7) return 0.6;
                if (position == 4 || position == 8) return 0.2;
                return 0.0;
            case 2: // bar2
                if (position == 4) return 1.0;
                if (position == 3 || position == 5) return 0.6;
                if (position == 2 || position == 6) return 0.2;
                return 0.0;
            case 3: // bar3
                if (position == 8) return 1.0;
                if (position == 7 || position == 9) return 0.6;
                if (position == 6 || position == 10) return 0.2;
                return 0.0;
            case 4: // bar4
                if (position == 11 || position == 17) return 1.0;
                if (position == 9 || position == 10 || position == 12 || position == 16 || position == 18) return 0.6;
                if (position == 7 || position == 8 || position == 13 || position == 15 || position == 19) return 0.2;
                if (position == 3 || position == 4 || position == 20) return 0.1;
                return 0.0;
            case 5: // bar5
                if (position == 8 || position == 14 || position == 18) return 1.0;
                if (position == 1 || position == 6 || position == 7 || position == 9 || position == 13 || position == 15) return 0.6;
                if (position == 4 || position == 5 || position == 10 || position == 12 || position == 16 || position == 19) return 0.2;
                if (position == 3 || position == 20) return 0.1;
                return 0.0;
            case 6: // bar6
                if (position == 9 || position == 11 || position == 13) return 1.0;
                if (position >= 1 && position <= 8) return 0.1;
                if (position >= 15 && position <= 22) return 0.1;
                return 0.0;
            case 7: // bar7
                if (position == 39) return 1.0;
                if (position == 42 || position == 43 || position == 44) return 0.3;
                if (position == 35 || position == 36) return 0.1;
                return 0.0;
            case 8: // bar8
                if (position == 21) return 1.0;
                if (position == 20 || position == 22) return 0.6;
                if (position >= 17 && position <= 19 || position >= 23 && position <= 26) return 0.2;
                return 0.0;
            case 9: // bar9
                if (position == 44) return 1.0;
                return 0.0;
            default:
                return 0.0;
        }
    }
    
    /**
     * 根据 Unicode 字符获取偏移值（基于 offset_chars.json）
     */
    private static int getOffsetForChar(char c) {
        switch (c) {
            case '\uf801': return -3;
            case '\uf802': return -4;
            case '\uf803': return -6;
            case '\uf804': return -10;
            case '\uf805': return -18;
            case '\uf806': return -34;
            case '\uf807': return -66;
            case '\uf808': return -130;
            case '\uf811': return -1;
            case '\uf812': return 1;
            case '\uf813': return 3;
            case '\uf814': return 7;
            case '\uf815': return 15;
            case '\uf816': return 31;
            case '\uf817': return 63;
            case '\uf818': return 127;
            default: return 0;
        }
    }
    
    /**
     * 重置游戏状态
     */
    public static void reset() {
        gameState.reset();
        
        // 重置速度检测数据
        historyIndex = 0;
        historyCount = 0;
        averageSpeed = 0.0;
        positionTimestamps = new long[MAX_POSITION_HISTORY];
        positionValues = new int[MAX_POSITION_HISTORY];
        
    }
}
