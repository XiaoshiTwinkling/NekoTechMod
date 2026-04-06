package com.nekotech.item.api.googles;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 可以被google看到的HUD的基类
 * 在./template软件包中有HUD的模板可供使用
 */
public abstract class GoogleAbstractHUD {
    // 常量定义
    public static final int DEFAULT_WIDTH = 176;
    public static final int DEFAULT_HEIGHT = 166;
    public static final int ITEM_SLOT_SIZE = 18;

    // 位置和尺寸
    protected int x = 0;
    protected int y = 0;
    protected int width = DEFAULT_WIDTH;
    protected int height = DEFAULT_HEIGHT;

    /**
     * 获取HUD类型标识符
     */
    public abstract String getType();

    /**
     * 获取HUD的标题
     */
    public abstract @Nullable Text getTitle();

    /**
     * 获取HUD的所有渲染数据
     */
    public abstract Map<String, Object> getRenderData();

    // Getter/Setter
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
