package com.rejahtavi.betterflight.compat;

import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.NoSuchElementException;

public class CuriosCompat
{
    /**
     * Checks curios slots for elytra items
     * @param player
     * @return ItemStack found; null if not found
     */
    public static ItemStack getCurioWings(@NotNull Player player)
    {
        ItemStack elytraStack = null;
        for (Item elytraItem : BetterFlightCommonConfig.elytraItems) {
            try {
                elytraStack = CuriosApi.getCuriosHelper().findFirstCurio(player, elytraItem)
                        .orElseThrow()
                        .stack();
            }
            catch(NoSuchElementException ignored) {
            }
        }
        return elytraStack;
    }
}
