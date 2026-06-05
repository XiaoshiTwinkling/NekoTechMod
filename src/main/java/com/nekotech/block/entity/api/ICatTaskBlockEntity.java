package com.nekotech.block.entity.api;

import com.nekotech.item.custom.NekoTag.NekoPlacedTag;
import com.nekotech.item.custom.NekoTag.NekoTask;
import net.minecraft.entity.passive.CatEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public interface ICatTaskBlockEntity {
    Map<ICatTaskBlockEntity, Map<NekoTask, NekoTaskHandler>> NEKO_TASK_HANDLER_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    default Map<NekoTask, NekoTaskHandler> getNekoTaskHandlers() {
        return NEKO_TASK_HANDLER_CACHE.computeIfAbsent(
                this,
                ignored -> createNekoTaskHandlers()
        );
    }

    Map<NekoTask, NekoTaskHandler> createNekoTaskHandlers();

    @FunctionalInterface
    interface NekoTaskHandler {
        void execute(
                CatEntity cat,
                NekoPlacedTag tag
        );
    }
}
