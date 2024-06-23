package com.rejahtavi.betterflight.compat;

import com.illusivesoulworks.elytraslot.platform.ForgeElytraPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CuriosCompat
{
    private static final ForgeElytraPlatform elytraPlatform = new ForgeElytraPlatform();
    /**
     * Checks curios slots for elytra items
     *
     * @param player
     * @return ItemStack found; null if not found
     */
    public static ItemStack getCurioWings(@NotNull Player player)
    {
        return elytraPlatform.getEquipped(player);
    }
}
