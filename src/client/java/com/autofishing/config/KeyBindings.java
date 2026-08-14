package com.autofishing.config;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定
 */
public class KeyBindings {
    
    // 创建自定义按键分类
    public static final KeyMapping.Category AUTO_FISHING_CATEGORY = 
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autofishing", "autofishing"));
    
    public static KeyMapping toggleAutoFishing;
    public static KeyMapping openConfig;
    
    public static void register() {
        toggleAutoFishing = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.autofishing.toggle",
            GLFW.GLFW_KEY_R,  // 默认按键：R
            AUTO_FISHING_CATEGORY  // 使用自定义分类
        ));
        
        openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.autofishing.config",
            GLFW.GLFW_KEY_O,  // 默认按键：O
            AUTO_FISHING_CATEGORY  // 使用自定义分类
        ));
    }
}
