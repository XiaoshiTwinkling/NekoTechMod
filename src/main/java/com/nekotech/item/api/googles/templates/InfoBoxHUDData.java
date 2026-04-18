package com.nekotech.item.api.googles.templates;

import com.nekotech.item.api.googles.GoogleAbstractHUD;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 信息框HUD数据模板
 * 用于显示标题和内容文本，宽度固定，高度自适应
 */
public class InfoBoxHUDData extends GoogleAbstractHUD {
    private final Text title;
    private final Text content;
    private final int maxWidth = 200; // 固定宽度
    private int calculatedHeight = 100; // 默认高度

    public InfoBoxHUDData(BlockPos pos, Text title, Text content) {
        super(pos);
        this.title = title;
        this.content = content;
        this.width = maxWidth;
        this.height = calculatedHeight;
    }

    public InfoBoxHUDData(BlockPos pos,Text title, Text content, int customWidth) {
        super(pos);
        this.title = title;
        this.content = content;
        this.width = customWidth;
        this.height = calculatedHeight;
    }

    @Override
    public String getType() {
        return "info_box";
    }

    @Override
    public @Nullable Text getTitle() {
        return title;
    }

    public Text getContent() {
        return content;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    @Override
    public Map<String, Object> getRenderData() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", this.title);
        data.put("content", this.content);
        data.put("maxWidth", this.maxWidth);
        return data;
    }
}
