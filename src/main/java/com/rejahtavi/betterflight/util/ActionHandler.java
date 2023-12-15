package com.rejahtavi.betterflight.util;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig;
import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.events.ClientEvents;
import com.rejahtavi.betterflight.client.Keybinding;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.network.CTSFlightActionPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.NoSuchElementException;

public class ActionHandler {
    private static int rechargeTickCounter = 0;
    private static int flareTickCounter = 0;
    public static int charge = BetterFlightCommonConfig.maxCharge;

      public static void tryTakeOff(LocalPlayer player) {
          if (ClientEvents.isElytraEquipped
                  && ClientEvents.offGroundTicks > BetterFlightCommonConfig.TAKE_OFF_JUMP_DELAY
                  && player.isSprinting()
                  && !player.isFallFlying()
                  && player.getDeltaMovement().length() > BetterFlightCommonConfig.TAKE_OFF_SPEED)
          {
              if (spendCharge(player, BetterFlightCommonConfig.takeOffCost)) {
                  CTSFlightActionPacket.send(FlightActionType.TAKEOFF);
                  FlightHandler.handleTakeoff(player);
                  // player.playSound(Sounds.SOUND_FLAP.get(), (float) ClientConfig.takeOffVolume,
                  // ClientConfig.FLAP_SOUND_PITCH);
                  player.playSound(Sounds.FLAP.get(), (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
                  //TODO playSounds only from servers perspective, instead of playing the sound twice, once at the client, and at the server?
              }
          }
      }

    public static void tryFlap(LocalPlayer player) {
          if (ClientEvents.isElytraEquipped
                  && ClientEvents.cooldown <= 0
                  && !player.isOnGround()
                  && player.isFallFlying())
          {
              if (spendCharge(player, BetterFlightCommonConfig.flapCost)) {
                  CTSFlightActionPacket.send(FlightActionType.FLAP);
                  FlightHandler.handleClassicFlap(player);
              }
          }
      }

    /**
     * Handles recharging flight stamina if player is touching the ground.
     * @param player
     */
    public static void handleRecharge(LocalPlayer player) {

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
    public static void tryFlare(LocalPlayer player) {
        if (ClientEvents.isElytraEquipped
                && Keybinding.flareKey.isDown()
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
    private static boolean spendCharge(LocalPlayer player, int points) {

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
    public static ItemStack findEquippedElytra(@NotNull LocalPlayer player) {

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
}
