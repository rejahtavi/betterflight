package com.rejahtavi.betterflight.util;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.ClientData;
import com.rejahtavi.betterflight.client.Keybinding;
import com.rejahtavi.betterflight.client.gui.ClassicHudOverlay;
import com.rejahtavi.betterflight.client.gui.StaminaHudOverlay;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.compat.BeansCompat;
import com.rejahtavi.betterflight.compat.CuriosCompat;
import com.rejahtavi.betterflight.network.FlightMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

import java.util.stream.Stream;

public class InputHandler
{
    private static int rechargeTickCounter = 0;
    private static int flareTickCounter = 0;
    public static int charge = BetterFlightCommonConfig.maxCharge;

    public static boolean classicFlight(Player player)
    {
        if (canTakeOff(player))
            return classicTakeOff(player);
        else if (canFlap(player))
            return classicFlap(player);
        return false;
    }

    public static boolean modernFlight(Player player)
    {
        if (canFlap(player))
        {
            if (spendCharge(player, BetterFlightCommonConfig.flapCost))
            {
                if (!checkForAir(player.level(), player))
                {
                    FlightHandler.handleModernBoost(player);
                } else
                {
                    FlightHandler.handleModernFlap(player);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean canFlap(Player player)
    {
        return ClientData.isWearingFunctionalWings() && !player.onGround() && player.isFallFlying();
    }

    private static boolean canTakeOff(Player player)
    {
        return ClientData.isWearingFunctionalWings()
                && ClientData.getOffGroundTicks() > BetterFlightCommonConfig.TAKE_OFF_JUMP_DELAY
                && player.isSprinting()
                && !player.isFallFlying()
                && player.getDeltaMovement().length() > BetterFlightCommonConfig.TAKE_OFF_SPEED;
    }


    /**
     * Coordinates client-server classic take off action if player has enough charge
     *
     * @param player
     * @return true if action was successful
     */
    public static boolean classicTakeOff(Player player)
    {
        if (spendCharge(player, BetterFlightCommonConfig.takeOffCost))
        {
            FlightHandler.handleClassicTakeoff(player);
            return true;
        }
        return false;
    }

    /**
     * Coordinates client-server classic flap action if player has enough charge
     *
     * @param player
     * @return true if action was successful
     */
    public static boolean classicFlap(Player player)
    {
        if (spendCharge(player, BetterFlightCommonConfig.flapCost))
        {
            FlightHandler.handleClassicFlap(player);
            return true;
        }
        return false;
    }

    /**
     * Handles recharging flight stamina if player is touching the ground and not flaring. Triggers per tick.
     *
     * @param event
     * @side both
     */
    //TODO Neoforge 1.21 break this up to client and server logical
    public static void handleRecharge(PlayerTickEvent event)
    {
        Player player = event.player;
        if (player.isCreative())
        {
            charge = BetterFlightCommonConfig.maxCharge;
            return;
        }

        int chargeThreshold = player.onGround() ? BetterFlightCommonConfig.rechargeTicksOnGround : BetterFlightCommonConfig.rechargeTicksInAir;

        if (rechargeTickCounter < chargeThreshold)
        {
            rechargeTickCounter++;
        }

        if (!ClientData.isFlaring() && rechargeTickCounter >= chargeThreshold && charge < BetterFlightCommonConfig.maxCharge)
        {

            if (player.getFoodData().getFoodLevel() > BetterFlightCommonConfig.minFood)
            {
                charge++;
                rechargeTickCounter = 0;
                ClassicHudOverlay.setRechargeBorderTimer(ClientConfig.BORDER_FLASH_TICKS);
                StaminaHudOverlay.startRegenAnimation();
                FlightMessages.sendToServer(FlightActionType.RECHARGE);
            }
        }
    }

    //MAYBE rework flare or introduce a new method to "glide"? Like being able to hold one's position while in the air like a bird.
    public static void tryFlare(Player player)
    {
        if (ClientData.isWearingFunctionalWings()
                && ClientData.isFlightEnabled()
                && Keybinding.flareKey.isDown()
                && ((player.isCreative() || charge > 0) || player.isInWater() || player.isInLava())
                && !player.onGround()
                && player.isFallFlying())
        {

            if (player.isInWater() || player.isInLava())
            {
                FlightHandler.handleFlightStop();
                return;
            }

            FlightHandler.handleFlare(player);

            flareTickCounter++;
            ClientData.setIsFlaring(true);

            if (flareTickCounter >= BetterFlightCommonConfig.flareTicksPerChargePoint)
            {
                spendCharge(player, 1);
                flareTickCounter = 0;
            }
        } else
        {
            if (flareTickCounter > 0)
            {
                flareTickCounter--;
            }
            ClientData.setIsFlaring(false);
        }
    }

    /**
     * Spends flight stamina if possible.
     *
     * @param player target player
     * @param points how much stamina to spend
     * @return true if creative mode or action was successful
     */
    private static boolean spendCharge(Player player, int points)
    {
        if (player.isCreative()) return true;

        if (charge >= points)
        {
            charge = Math.max(0,charge-points);
            rechargeTickCounter = 0;
            ClientData.setCooldown(BetterFlightCommonConfig.cooldownTicks);
            ClassicHudOverlay.setDepletionBorderTimer(ClientConfig.BORDER_FLASH_TICKS);
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * Find equipped and working elytra items from the player
     *
     * @param player to check
     * @return ItemStack wings are found; null if wings not found or broken
     */
    public static ElytraData findWings(Player player)
    {
        ItemStack itemStack = findWingsItemStack(player);
        if (itemStack == null)
            return null;
        int durabilityRemaining = itemStack.getMaxDamage() - itemStack.getDamageValue();
        float durabilityPercent = (float) itemStack.getDamageValue() / (float) itemStack.getMaxDamage();

        return new ElytraData(itemStack, durabilityRemaining, durabilityPercent);
    }

    /**
     * Returns ItemStack of player equipped wings
     *
     * @param player
     * @return ItemStack of equipped wings; null if not found
     */
    private static ItemStack findWingsItemStack(Player player)
    {
        // check the player's chest slot for elytra
        ItemStack elytraStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (elytraStack.canElytraFly(player))
        {
            return elytraStack;
        }
        if (BetterFlight.isCuriousElytraLoaded)
        {
            elytraStack = CuriosCompat.getCurioWings(player);
            if (elytraStack != ItemStack.EMPTY) return elytraStack;

        }
        if (BetterFlight.isBeanBackpackLoaded)
        {
            elytraStack = BeansCompat.getBeanWings(player);
            if (elytraStack != ItemStack.EMPTY) return elytraStack;
        }
        return null;
    }

    public static boolean checkForAir(Level world, LivingEntity player)
    {
        AABB boundingBox = player.getBoundingBox()
                .setMaxY(player.getBoundingBox().minY + 3.5)
                .inflate(1D, 0D, 1D)
                .move(0, -1.5D, 0);
        Stream<BlockPos> blocks = getBlockPosIfLoaded(world, boundingBox);
        Stream<BlockPos> filteredBlocks = blocks.filter(
                pos ->
                {
                    BlockState block = world.getBlockState(pos);
                    return block.isCollisionShapeFullBlock(world, pos) || block.liquid(); //checks if block is solid or fluid
                });
        return filteredBlocks.toList().isEmpty();
    }

    /**
     * Returns stream of BlockPos in given AABB, if the chunks are already loaded.
     *
     * @param world
     * @param boundingBox
     * @return Stream of BlockPos found
     */
    private static Stream<BlockPos> getBlockPosIfLoaded(Level world, AABB boundingBox)
    {
        int i = Mth.floor(boundingBox.minX);
        int j = Mth.floor(boundingBox.maxX);
        int k = Mth.floor(boundingBox.minY);
        int l = Mth.floor(boundingBox.maxY);
        int i1 = Mth.floor(boundingBox.minZ);
        int j1 = Mth.floor(boundingBox.maxZ);
        return world.hasChunksAt(i, k, i1, j, l, j1) ? BlockPos.betweenClosedStream(boundingBox) : Stream.empty();
    }
}
