package com.rejahtavi.betterflight.util;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.network.CTSFlightActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputHandler {
    private static int rechargeTickCounter = 0;
    private static int flareTickCounter = 0;
    public static int charge = BetterFlightCommonConfig.maxCharge;

    public static boolean classicFlight(Player player) {
        if (canTakeOff(player))
            return classicTakeOff(player);
        else if (canFlap(player))
            return classicFlap(player);
        return false;
    }

    private static boolean canFlap(Player player) {
        return ClientEvents.isElytraEquipped && !player.isOnGround() && player.isFallFlying();
    }

    private static boolean canTakeOff(Player player) {
        return ClientEvents.isElytraEquipped
                && ClientEvents.offGroundTicks > BetterFlightCommonConfig.TAKE_OFF_JUMP_DELAY
                && player.isSprinting()
                && !player.isFallFlying()
                && player.getDeltaMovement().length() > BetterFlightCommonConfig.TAKE_OFF_SPEED;
    }

    public static boolean modernFlight(Player player) {

        return false;
    }


    /**
     * Coordinates client-server classic take off action if player has enough charge
     * @param player
     * @return true if action was successful
     */
    public static boolean classicTakeOff(Player player) {
          if (spendCharge(player, BetterFlightCommonConfig.takeOffCost)) {
              CTSFlightActionPacket.send(FlightActionType.TAKEOFF);
              FlightHandler.handleClassicTakeoff(player);
              return true;
          }
        return false;
    }

    /**
     * Coordinates client-server classic flap action if player has enough charge
     * @param player
     * @return true if action was successful
     */
    public static boolean classicFlap(Player player) {
          if (spendCharge(player, BetterFlightCommonConfig.flapCost)) {
              CTSFlightActionPacket.send(FlightActionType.FLAP);
              FlightHandler.handleClassicFlap(player);
              return true;
          }
        return false;
    }

    /**
     * Handles recharging flight stamina if player is touching the ground and not flaring. Triggers per tick.
     * @param player
     */
    public static void handleRecharge(Player player) {

          if (player.isCreative()) {
              charge = BetterFlightCommonConfig.maxCharge;
              return;
          }

          int chargeThreshold = player.isOnGround() ? BetterFlightCommonConfig.rechargeTicksOnGround : BetterFlightCommonConfig.rechargeTicksInAir;

          if (rechargeTickCounter < chargeThreshold) {
              rechargeTickCounter++;
          }

          if (!ClientEvents.isFlaring && rechargeTickCounter >= chargeThreshold && charge < BetterFlightCommonConfig.maxCharge) {

              if (player.getFoodData().getFoodLevel() > BetterFlightCommonConfig.minFood) {
                  charge++;
                  rechargeTickCounter = 0;
                  HUDOverlay.setRechargeBorderTimer(ClientConfig.BORDER_FLASH_TICKS);
                  CTSFlightActionPacket.send(FlightActionType.RECHARGE);
                  player.getFoodData().addExhaustion((float) BetterFlightCommonConfig.exhaustionPerChargePoint);
              }
          }
      }

    //MAYBE rework flare or introduce a new method to "glide"? Like being able to hold one's position while in the air like a bird.
    public static void tryFlare(Player player) {
        if (ClientEvents.isElytraEquipped
                && (player.isCreative() || charge > 0)
                && !player.isOnGround()
                && player.isFallFlying()) {

            CTSFlightActionPacket.send(FlightActionType.FLARE);
            FlightHandler.handleFlare(player);

            flareTickCounter++;
            ClientEvents.isFlaring = true;

            if (flareTickCounter >= BetterFlightCommonConfig.flareTicksPerChargePoint) {
                spendCharge(player, 1);
                flareTickCounter = 0;
            }
        }
        else {
            if (flareTickCounter > 0) {
                flareTickCounter--;
            }
            ClientEvents.isFlaring = false;
        }
    }

    /**
     * Spends flight stamina if possible.
     * @param player target player
     * @param points how much stamina to spend
     * @return true if creative mode or action was successful
     */
    private static boolean spendCharge(Player player, int points) {

        if (player.isCreative()) return true;

        if (charge >= points) {
            charge -= points;
            rechargeTickCounter = 0;
            ClientEvents.cooldown = BetterFlightCommonConfig.cooldownTicks;
            HUDOverlay.setDepletionBorderTimer(ClientConfig.BORDER_FLASH_TICKS);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Looks for an equipped elytra on the target player
     * @param player
     * @return itemstack an elytra was found; null if not found
     */
    public static ItemStack findEquippedElytra(@NotNull Player player) {

        // check the player's chest slot for elytra
        ItemStack elytraStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (BetterFlightCommonConfig.elytraItems.contains(elytraStack.getItem())) {
            isWorkingElytra(elytraStack);
            return isWorkingElytra(elytraStack) ? elytraStack : null;
        }

        // if dependencies are present, check the curios slots as well
        if (BetterFlight.isCuriousElytraLoaded) {
            for (Item elytraItem : BetterFlightCommonConfig.elytraItems) {
                try {
                    elytraStack = CuriosApi.getCuriosHelper().findFirstCurio(player, elytraItem)
                            .orElseThrow()
                            .stack();
                    return isWorkingElytra(elytraStack) ? elytraStack : null;
                }
                catch(NoSuchElementException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Check if ItemStack is a usable elytra with durability left
     * @param elytraStack ItemStack to check
     * @return true if elytra is functional
     */
    private static boolean isWorkingElytra(ItemStack elytraStack) {
        return elytraStack.getMaxDamage() - elytraStack.getDamageValue() > 1;
    }

    //TODO Scan area around player for air
    //Referencing https://github.com/VentureCraftMods/MC-Gliders/blob/2a2df716fd47f312e0b1c0b593cb43437019f53e/common/src/main/java/net/venturecraft/gliders/util/GliderUtil.java#L183
    public static boolean checkForAir(Level world, LivingEntity player) {
        AABB boundingBox = player.getBoundingBox().move(0, -1.5, 0);
//        AABB boundingBox = new AABB(player.blockPosition());
        // contract(2,5,2)
        // tp dev 432 75 -412
        // 430 74 -414
        // 432 71 -412
        //
        //contract(0,2,0) captures block at players feet and the block below.

        Stream<BlockPos> blocks = getBlockPosIfLoaded(world,boundingBox);
        //TODO Exclude non-solid, non-cube blocks in the filter, like minecraft:grass and minecraft:torch
        Stream<BlockPos> filteredBlocks = blocks.filter(
                pos -> {
                    return world.getBlockState(pos).isCollisionShapeFullBlock(world,pos);
                });
        if (filteredBlocks.toList().isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Returns stream of BlockPos in given AABB, if the chunks are already loaded.
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
