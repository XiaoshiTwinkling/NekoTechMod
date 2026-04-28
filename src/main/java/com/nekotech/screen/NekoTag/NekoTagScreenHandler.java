package com.nekotech.screen.NekoTag;

import com.nekotech.item.ModItems;
import com.nekotech.screen.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public class NekoTagScreenHandler extends ScreenHandler {

    private final Hand hand;

    public NekoTagScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, Hand.MAIN_HAND);
    }

    public NekoTagScreenHandler(int syncId, PlayerInventory inventory, Hand hand) {
        super(ModScreenHandlers.NEKO_TAG, syncId);
        this.hand = hand;

        addPlayerInventory(inventory, 35, 140);
        addPlayerHotbar(inventory, 35, 198);
    }

    public Hand getHand() {
        return hand;
    }

    private void addPlayerInventory(PlayerInventory inventory, int startX, int startY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        startX + column * 18,
                        startY + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(PlayerInventory inventory, int startX, int startY) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    inventory,
                    column,
                    startX + column * 18,
                    startY
            ));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        ItemStack stack = player.getStackInHand(hand);
        return stack.isOf(ModItems.neko_tag);
    }
}
