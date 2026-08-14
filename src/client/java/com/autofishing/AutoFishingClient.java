package com.autofishing;

import com.autofishing.config.AutoFishConfig;
import com.autofishing.config.AutoFishConfigScreen;
import com.autofishing.config.KeyBindings;
import com.autofishing.fishing.FishingController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;

public class AutoFishingClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        AutoFishingMod.LOGGER.info("Auto Fishing Client initialized!");
        
        // 加载配置
        AutoFishConfig.load();
        
        // 注册快捷键
        KeyBindings.register();
        
        // 注册tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查快捷键 - 切换mod开关
            while (KeyBindings.toggleAutoFishing.consumeClick()) {
                AutoFishConfig config = AutoFishConfig.get();
                config.modEnabled = !config.modEnabled;
                AutoFishConfig.save();
                
                if (client.player != null) {
                    client.player.sendSystemMessage(
                        Component.literal(
                            config.modEnabled ? "§a[Auto Fishing] Enabled" : "§c[Auto Fishing] Disabled"
                        )
                    );
                }
                AutoFishingMod.LOGGER.info("Auto Fishing: {}", config.modEnabled ? "ON" : "OFF");
            }
            
            // 检查快捷键 - 打开配置界面
            while (KeyBindings.openConfig.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(AutoFishConfigScreen.createConfigScreen(client.gui.screen()));
                }
            }
            
            // 执行自动钓鱼逻辑
            if (AutoFishConfig.get().modEnabled && client.player != null) {
                FishingController.tick(client);
            }
        });
    }
}
