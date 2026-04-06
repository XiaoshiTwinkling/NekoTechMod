package com.nekotech.client.hud;

import com.nekotech.client.hud.templates.ContainerItemsHUDClient;
import com.nekotech.item.api.googles.GoogleAbstractHUD;
import com.nekotech.item.api.googles.templates.ContainerHUDData;
import org.jetbrains.annotations.Nullable;

/**
 * HUD工厂，将主模块的HUD数据转换为客户端的渲染器喵~
 */
public class HudFactory {
    @Nullable
    public static GoogleAbstractHUDClient createHUD(@Nullable GoogleAbstractHUD hubData) {
        if (hubData == null) return null;

        String hudType = hubData.getType();

        switch (hudType) {
            case "container":
                if (hubData instanceof ContainerHUDData containerData) {
                    return new ContainerItemsHUDClient(containerData);
                }
                break;
            // 可以添加更多HUD类型
        }

        return null;
    }
}
