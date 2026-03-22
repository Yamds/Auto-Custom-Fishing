package com.autofishing.mixin;

import com.autofishing.AutoFishingMod;
import com.autofishing.config.AutoFishConfig;
import com.autofishing.fishing.FishingController;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public class FishingHookMixin {
    
    @Shadow
    private boolean biting;
    
    private boolean lastBitingState = false;
    
    /**
     * 监听 FishingHook 的 tick 方法
     * 当 biting 从 false 变为 true 时表示鱼上钩了
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled) {
            return;
        }

        try {
            // 检查这个鱼钩是否属于当前玩家
            FishingHook hook = (FishingHook)(Object)this;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            
            // 只处理当前玩家的鱼钩
            if (mc.player == null || hook.getPlayerOwner() != mc.player) {
                return;
            }
            
            // 检测 biting 状态变化（从 false 到 true）
            if (biting && !lastBitingState) {
                FishingController.onFishBite();
                AutoFishingMod.LOGGER.info("[FishingHook] Fish is biting!");
            }
            lastBitingState = biting;
        } catch (Exception e) {
            AutoFishingMod.LOGGER.error("[FishingHook] Error in tick", e);
        }
    }
}
