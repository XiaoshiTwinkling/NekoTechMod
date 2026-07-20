package com.nekotech.catcamera;

import org.jetbrains.annotations.Nullable;

public interface CatCameraChannelAccess {
    @Nullable CatCameraChannelData neko_technology$getCatCameraChannel();
    void neko_technology$setCatCameraChannel(@Nullable CatCameraChannelData data);
    boolean neko_technology$isCatCameraChannelActive();
    boolean neko_technology$isCatCameraChannelReconciled();
    void neko_technology$setCatCameraChannelReconciled(boolean reconciled);
}
