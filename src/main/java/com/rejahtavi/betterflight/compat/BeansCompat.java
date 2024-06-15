package com.rejahtavi.betterflight.compat;

import com.beansgalaxy.backpacks.data.BackData;
import com.beansgalaxy.backpacks.entity.Kind;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BeansCompat
{
    /**
     * Checks if player is wearing Wings in Bean backpack slot.
     *
     * @param player
     * @return ItemStack if found; null if no wings found
     */
    public static ItemStack getBeanWings(@NotNull Player player)
    {
        ItemStack elytraStack;
        elytraStack = BackData.get(player).getStack();
        if (Kind.isWings(elytraStack))
        {
            return elytraStack;
        }
        return null;
    }
}
