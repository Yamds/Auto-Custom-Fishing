package cafe.yamds.mixin;

import cafe.yamds.config.AutoFishConfig;
import cafe.yamds.game.TitleAnalyzer;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class TitlePacketMixin {
    
    /**
     * 拦截setTitle方法
     */
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Component component, CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled) {
            return;
        }
        
        try {
            if (component != null) {
                String content = component.getString();
                String fullContent = component.toString();
                
                TitleAnalyzer.analyzeTitle(content);
                if (!fullContent.equals(content)) {
                    TitleAnalyzer.analyzeTitle(fullContent);
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 拦截setSubtitle方法
     */
    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Component component, CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled) {
            return;
        }
        
        try {
            if (component != null) {
                String content = component.getString();
                String fullContent = component.toString();
                
                TitleAnalyzer.analyzeSubtitle(content);
                if (!fullContent.equals(content)) {
                    TitleAnalyzer.analyzeSubtitle(fullContent);
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 拦截setOverlayMessage方法（ActionBar）
     */
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void onSetOverlayMessage(Component component, boolean bl, CallbackInfo ci) {
        if (!AutoFishConfig.get().modEnabled) {
            return;
        }
        
        try {
            if (component != null) {
                String content = component.getString();
                String fullContent = component.toString();
                
                // ActionBar也可能包含游戏UI
                TitleAnalyzer.analyzeSubtitle(content);
                if (!fullContent.equals(content)) {
                    TitleAnalyzer.analyzeSubtitle(fullContent);
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }
}
