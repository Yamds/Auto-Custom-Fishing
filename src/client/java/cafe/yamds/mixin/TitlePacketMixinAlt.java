package cafe.yamds.mixin;

import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.game.TitleAnalyzer;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 备用方案：拦截Hud的setTitle和setSubtitle方法
 * 这个方法更稳定，不依赖于网络数据包的具体实现
 */
@Mixin(Hud.class)
public abstract class TitlePacketMixinAlt {
    
    @Shadow
    private Component title;
    
    @Shadow
    private Component subtitle;
    
    /**
     * 拦截setTitle方法
     */
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Component title, CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled || title == null) {
            return;
        }
        
        try {
            String content = title.getString();
            if (content != null && !content.isEmpty()) {
                // 简单的字符串分析
                TitleAnalyzer.analyzeTitle(content);
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 拦截setSubtitle方法
     */
    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Component subtitle, CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled || subtitle == null) {
            return;
        }
        
        try {
            String content = subtitle.getString();
            if (content != null && !content.isEmpty()) {
                // 简单的字符串分析
                TitleAnalyzer.analyzeSubtitle(content);
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
}
