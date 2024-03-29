package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.client.Keybinding;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientEvents {

    //INDEV
    public static Logger logger = LogManager.getLogger(BetterFlight.MODID);
    // Player state
    public static boolean isElytraEquipped = false;
    private static boolean wasFlapKeyDown = false;
    private static boolean wasToggleKeyDown = false;
    public static boolean isFlaring = false;
    public static int offGroundTicks = 0;
    public static boolean isFlightEnabled = true;

    // elytra damage
    public static double elytraDurability = 0.5D;
    public static int elytraDurabilityLeft = 1;

    // timers
    public static int cooldown = 0;
    private static boolean isDebugButtonDown = false;

    /**
     * default to full elytra meter on startup
     */
    public static void init() {
        InputHandler.charge = BetterFlightCommonConfig.maxCharge;
    }

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(Keybinding.toggleKey);
            event.register(Keybinding.flapKey);
            event.register(Keybinding.flareKey);
            event.register(Keybinding.widgetPosKey);
        }
    }

    // key event handling
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {

        Minecraft instance = Minecraft.getInstance();
        Player player = instance.player;
        if (player == null) return;

        if (event.getKey() == Keybinding.widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            HUDOverlay.cycleWidgetLocation();
        }

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {


        //Phase.START runs before vanilla handles client tick. Phase.END runs after vanilla
        if(event.phase == TickEvent.Phase.START) {

            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            //logger.info("Speed:" + player.getDeltaMovement().length());
            ItemStack elytraStack = InputHandler.findEquippedElytra(player);
            if(elytraStack != null)
            {
                isElytraEquipped = true;
                elytraDurabilityLeft = elytraStack.getMaxDamage() - elytraStack.getDamageValue();
                elytraDurability = (float) elytraStack.getDamageValue()/(float) elytraStack.getMaxDamage();
            }
            else { isElytraEquipped = false;}

            // track ground state for takeoff logic

            if (player.isOnGround()) {offGroundTicks = 0;}
            else {offGroundTicks++;}

            // decrement timers
            HUDOverlay.borderTick();
            if (cooldown > 0) cooldown--;

            InputHandler.handleRecharge(player);
            InputHandler.tryFlare(player);

//            while(Keybinding.flareKey.consumeClick())
//            {
//                if(!isDebugButtonDown)
//                {
//                    InputHandler.checkForAir(mc.level,player);
//                    isDebugButtonDown = true;
//                }
//            }



            if(Keybinding.flapKey.isDown() && wasFlapKeyDown == false) {
                if(cooldown <= 0 && isFlightEnabled){
                    if(BetterFlightCommonConfig.classicMode) {
                        InputHandler.classicFlight(player);
                    }
                    else InputHandler.modernFlight(player);
                }
                wasFlapKeyDown = true;
            }
            if(!Keybinding.flapKey.isDown() && wasFlapKeyDown)
                wasFlapKeyDown = false;

            if(Keybinding.toggleKey.isDown() && wasToggleKeyDown == false) {
                if(isFlightEnabled)
                {
                    isFlightEnabled = false;
                }
                else
                {
                    isFlightEnabled = true;
                }
                wasToggleKeyDown = true;
            }
            if (!Keybinding.toggleKey.isDown() && wasToggleKeyDown) {
                wasToggleKeyDown = false;
            }
//            if (!Keybinding.flareKey.isDown()) {
//                isDebugButtonDown = false;
//            }
        }
    }

}
