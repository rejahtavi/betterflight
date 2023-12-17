package com.rejahtavi.betterflight.events;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.HUDOverlay;
import com.rejahtavi.betterflight.client.Keybinding;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.util.ActionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientEvents {

    //INDEV
    static Logger logger = LogManager.getLogger(BetterFlight.MODID);
    private static boolean devMode = true;

    // Player state
    public static boolean isElytraEquipped = false;
    private static boolean isKeyDown = false;
    public static boolean isFlaring = false;
    public static int offGroundTicks = 0;

    // elytra damage
    public static double elytraDurability = 0.5D;
    public static int elytraDurabilityLeft = 1;

    // timers
    public static int cooldown = 0;
    private static boolean boosted = false;

    /**
     * default to full elytra meter on startup
     */
    public static void init() {
        ActionHandler.charge = BetterFlightCommonConfig.maxCharge;
    }

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            //event.register(Keybinding.takeOffKey);
            event.register(Keybinding.flapKey);
            event.register(Keybinding.flareKey);
            event.register(Keybinding.widgetPosKey);
        }
    }

    // key event handling
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {

        Minecraft instance = Minecraft.getInstance();
        LocalPlayer player = instance.player;
        if (player == null) return;

        if (event.getKey() == Keybinding.widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            HUDOverlay.cycleWidgetLocation();
        }

        //INDEV remove this later. Just trying to check scanner
        //if (Keybinding.flapKey.isDown() && !boosted && player.isFallFlying()) {
        //TODO This is really messy logic. Please don't use this to determine flap vs boost
        // Don't forget take off and flapping should be a double jump, not from standing.
        //TODO standardize the names between the normal flap and the stronger one
/*        if (Keybinding.flapKey.isDown() && !boosted) {
            if(checkForAir(instance.level,player)&&!player.isOnGround())
                FlightHandler.handleModernFlap(player);
            else if(player.isOnGround()&&player.isSprinting() || player.isFallFlying() && !checkForAir(instance.level,player))
                FlightHandler.handleModernTakeoff(player);
            else FlightHandler.handleModernFlap(player);
            boosted = true;
        }

        if (Keybinding.flareKey.consumeClick())
        {
            logger.info("X: "+ player.getXRot());
            logger.info("Y: "+ player.getYRot());
        }*/

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {


        //Phase.START runs before vanilla handles client tick. Phase.END runs after vanilla
        if(event.phase == TickEvent.Phase.END) {

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            if (devMode) {
                logger.info("Speed:" + player.getDeltaMovement().length());
            }
            ItemStack elytraStack = ActionHandler.findEquippedElytra(player);
            if(elytraStack != null)
            {
                isElytraEquipped = true;
                elytraDurabilityLeft = elytraStack.getMaxDamage() - elytraStack.getDamageValue();
                elytraDurability = (float) elytraStack.getDamageValue()/(float) elytraStack.getMaxDamage();
            }
            else { isElytraEquipped = false;}

            // track ground state for takeoff logic
            if (player.isOnGround()) {
                offGroundTicks = 0;
            }
            else {
                offGroundTicks++;
            }

            // decrement timers
            HUDOverlay.borderTick();
            if (cooldown > 0) cooldown--;

            ActionHandler.handleRecharge(player);
            ActionHandler.tryFlare(player);

            while(Keybinding.flapKey.consumeClick()) {
                if(cooldown <= 0 && !isKeyDown){
                    if(BetterFlightCommonConfig.classicMode) {
                        ActionHandler.classicFlight(player);
                    }
                    else ActionHandler.modernFlight(player);
                }
                isKeyDown = true;
            }
            if (!Keybinding.flapKey.isDown()) {
                isKeyDown = false;}
        }
    }

    //region INDEV experimental blocks scanner

    //TODO Scan area around player for air
    //Referencing https://github.com/VentureCraftMods/MC-Gliders/blob/2a2df716fd47f312e0b1c0b593cb43437019f53e/common/src/main/java/net/venturecraft/gliders/util/GliderUtil.java#L183
    public static boolean checkForAir(Level world, LivingEntity player) {
        AABB boundingBox = player.getBoundingBox().move(0, -1.5, 0);
        // contract(2,5,2)
        // tp dev 432 75 -412
        // 430 74 -414
        // 432 71 -412
        //
        //contract(0,2,0) captures block at players feet and the block below.
        List<BlockState> blocks = world.getBlockStatesIfLoaded(boundingBox).toList();
        for(BlockState n : blocks)
            logger.debug(n);
        //Block.isShapeFullBlock();
        //TODO Exclude non-solid, non-cube blocks in the filter, like minecraft:grass and minecraft:torch
        Stream<BlockState> filteredBlocks = blocks.stream().filter(blockState -> !blockState.isAir());
        if (filteredBlocks.toList().isEmpty()) {
            return true;
        }
        return false;
    }

}
