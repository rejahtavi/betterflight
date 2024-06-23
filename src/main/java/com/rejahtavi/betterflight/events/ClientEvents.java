package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientData;
import com.rejahtavi.betterflight.client.Keybinding;
import com.rejahtavi.betterflight.client.gui.ClassicHudOverlay;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.util.ElytraData;
import com.rejahtavi.betterflight.util.InputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientEvents
{

    //INDEV
    public static Logger logger = LogManager.getLogger(BetterFlight.MODID);
    // Player state
    private static boolean wasFlapKeyDown = false;
    private static boolean wasToggleKeyDown = false;

    // elytra damage
    public static double elytraDurability = 0.5D;

    // timers
    private static final boolean isDebugButtonDown = false;

    /**
     * default to full elytra meter on startup
     */
    public static void init()
    {
        InputHandler.charge = BetterFlightCommonConfig.maxCharge;
    }

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents
    {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event)
        {
            event.register(Keybinding.toggleKey);
            event.register(Keybinding.flapKey);
            event.register(Keybinding.flareKey);
            event.register(Keybinding.widgetPosKey);
        }
    }

    // key event handling
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {

        Minecraft instance = Minecraft.getInstance();
        Player player = instance.player;
        if (player == null) return;

        if (event.getKey() == Keybinding.widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS)
        {
            ClassicHudOverlay.cycleWidgetLocation();
        }

    }

    //ticks when world is running
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && event.side == LogicalSide.CLIENT)
        {
            if (event.player == null) return;
            InputHandler.handleRecharge(event);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event)
    {


        //Phase.START runs before vanilla handles client tick. Phase.END runs after vanilla
        if (event.phase == TickEvent.Phase.START)
        {

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            //logger.info("Speed:" + player.getDeltaMovement().length());
            updateWingStatus(player);

            // track ground state for takeoff logic

            if (player.onGround())
            {
                ClientData.setOffGroundTicks(0);
            } else
            {
                ClientData.tickOffGround();
            }

            // decrement timers
            ClassicHudOverlay.borderTick();
            if (ClientData.getCooldown() > 0) ClientData.subCooldown(1);


            InputHandler.tryFlare(player);

//            while(Keybinding.flareKey.consumeClick())
//            {
//                if(!isDebugButtonDown)
//                {
//                    InputHandler.checkForAir(mc.level,player);
//                    isDebugButtonDown = true;
//                }
//            }


            if (Keybinding.flapKey.isDown() && !wasFlapKeyDown)
            {
                if (ClientData.getCooldown() <= 0 && ClientData.isFlightEnabled())
                {
                    if (BetterFlightCommonConfig.classicMode)
                    {
                        InputHandler.classicFlight(player);
                    } else InputHandler.modernFlight(player);
                }
                wasFlapKeyDown = true;
            }
            if (!Keybinding.flapKey.isDown() && wasFlapKeyDown)
                wasFlapKeyDown = false;

            if (Keybinding.toggleKey.isDown() && !wasToggleKeyDown)
            {
                ClientData.setFlightEnabled(!ClientData.isFlightEnabled());
                wasToggleKeyDown = true;
            }
            if (!Keybinding.toggleKey.isDown() && wasToggleKeyDown)
            {
                wasToggleKeyDown = false;
            }
//            if (!Keybinding.flareKey.isDown()) {
//                isDebugButtonDown = false;
//            }
        }
    }

    /**
     * Checks if player is wearing functional wings and updates status
     *
     * @param player to check and update
     */
    private static void updateWingStatus(LocalPlayer player)
    {
        ElytraData elytraStack = InputHandler.findWings(player);
        if (elytraStack != null && elytraStack.durabilityRemaining() > 1)
        {
            ClientData.setWingStatus(true);
            elytraDurability = elytraStack.durabilityPercent();
        } else
        {
            ClientData.setWingStatus(false);
        }
    }

}
