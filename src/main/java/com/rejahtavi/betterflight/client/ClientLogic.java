package com.rejahtavi.betterflight.client;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig.HudLocation;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.BetterFlightCommonConfig;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.network.CTSFlightActionPacket;
import com.rejahtavi.betterflight.network.STCElytraChargePacket;
import com.rejahtavi.betterflight.util.FlightHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientLogic {

    static Logger logger = LogManager.getLogger(BetterFlight.MODID);

    // state
    public static boolean isElytraEquipped = false;
    public static boolean hasFlapped = false;
    public static boolean isFlaring = false;
    public static int charge = BetterFlightCommonConfig.maxCharge;

    // elytra damage
    public static double elytraDurability = 0.5D;
    public static int elytraDurabilityLeft = 1;

    // counters
    public static int rechargeTickCounter = 0;
    public static int flareTickCounter = 0;
    public static int offGroundTickCounter = 0;

    // timers
    public static int cooldownTimer = 0;
    public static int rechargeBorderTimer = 0;
    public static int depletionBorderTimer = 0;

    public static void init() {
        // default to full elytra meter on startup
        charge = BetterFlightCommonConfig.maxCharge;
    }

    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(Keybinding.takeOffKey);
            event.register(Keybinding.flapKey);
            event.register(Keybinding.flareKey);
            event.register(Keybinding.widgetPosKey);
        }
    }

    // key event handling
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {

        Minecraft instance = Minecraft.getInstance();
        if (instance.player == null) return;

        if (Keybinding.takeOffKey.isDown() && !instance.player.isFallFlying()) {
            tryTakeOff(instance.player);
            //hasFlapped = true;
        }
        if (Keybinding.flapKey.isDown() && instance.player.isFallFlying() && !hasFlapped) {
            tryFlap(instance.player);
            hasFlapped = true;
        }

        if (event.getKey() == Keybinding.widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            cycleWidgetLocation();
        }

        //INDEV remove this later. Just trying to check scanner
//        if (Keybinding.flareKey.isDown()) {
//            logger.info("isAir: " + checkAir(instance.player.blockPosition(),instance.player.level,instance.player));
//        }

    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // track ground state for takeoff logic
        if (player.isOnGround()) {
            offGroundTickCounter = 0;
        }
        else {
            offGroundTickCounter++;
        }

        // decrement timers
        if (depletionBorderTimer > 0) depletionBorderTimer--;
        if (rechargeBorderTimer > 0) rechargeBorderTimer--;
        if (cooldownTimer > 0) cooldownTimer--;

        ItemStack elytraStack = findEquippedElytra(player);
        if(elytraStack != null)
        {
            isElytraEquipped = true;
            elytraDurabilityLeft = elytraStack.getMaxDamage() - elytraStack.getDamageValue();
            elytraDurability = (float) elytraStack.getDamageValue()/(float) elytraStack.getMaxDamage();
        }
        else { isElytraEquipped = false;}
        handleRecharge(player);
        handleFlare(player);

        if (!Keybinding.flapKey.isDown() || !Keybinding.takeOffKey.isDown() && hasFlapped) {
            hasFlapped = false;}
    }

    //region INDEV experimental blocks scanner
    private static boolean isAir (LivingEntity livingEntity) {
        if (!livingEntity.isOnGround()
                //&& livingEntity.level.getBlockState(livingEntity.blockPosition().below(2)).isAir()
                && livingEntity.level.getBlockState(livingEntity.blockPosition().below()).isAir())
        {
            return true;
        }
        else {return false;}
    }

    private static boolean hasAirSpace (LivingEntity livingEntity) {
        Iterator<BlockPos> iterator = BlockPos.withinManhattanStream(livingEntity.blockPosition(), 2, 3, 2).iterator(); iterator.hasNext();
        BlockPos pos = iterator.next();
        BlockState blockState = livingEntity.level.getBlockState(pos);
        if (blockState.isAir()) {

        }
        return false;
    }

    //TODO Scan area around player for air
    //Referencing https://github.com/VentureCraftMods/MC-Gliders/blob/2a2df716fd47f312e0b1c0b593cb43437019f53e/common/src/main/java/net/venturecraft/gliders/util/GliderUtil.java#L183
    public static boolean checkAir(BlockPos playerPosition, Level world, LivingEntity player) {
        AABB boundingBox = player.getBoundingBox().contract(2, 5, 2);
        //FIXME Scanning box is not centered on the players feet. It starts at it. Example data below
        // contract(2,5,2)
        // tp dev 432 75 -412
        // 430 74 -414
        // 432 71 -412
        List<BlockState> blocks = world.getBlockStatesIfLoaded(boundingBox).toList();
        for(BlockState n : blocks)
            logger.debug(n);
        //TODO Exclude non-solid, non-cube blocks in the filter, like minecraft:grass and minecraft:torch
        Stream<BlockState> filteredBlocks = blocks.stream().filter(blockState -> !blockState.isAir());
        if (filteredBlocks.toList().size() == 0) {
            //player.setDeltaMovement(0, 0.5, 0);
            return true;
        }
        return false;
    }
  //endregion
    //TODO move logic for determining if player can takeoff/fly to event listener onKeyPress
    private static void tryTakeOff(LocalPlayer player) {
        if (isElytraEquipped
                && offGroundTickCounter > BetterFlightCommonConfig.TAKE_OFF_JUMP_DELAY
                && player.isSprinting()
                && !player.isFallFlying()
                && player.getDeltaMovement().length() > BetterFlightCommonConfig.TAKE_OFF_SPEED) {

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

    private static void tryFlap(LocalPlayer player) {
        if (isElytraEquipped
                && cooldownTimer <= 0
                && !player.isOnGround()
                && player.isFallFlying()) {

            if (spendCharge(player, BetterFlightCommonConfig.flapCost)) {
                CTSFlightActionPacket.send(FlightActionType.FLAP);
                FlightHandler.handleFlap(player);
                player.playSound(Sounds.FLAP.get(), (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
            }
        }
    }

    private static void handleRecharge(LocalPlayer player) {

        if (player.isCreative()) {
            charge = BetterFlightCommonConfig.maxCharge;
            return;
        }

        int threshold = player.isOnGround() ? BetterFlightCommonConfig.rechargeTicksOnGround : BetterFlightCommonConfig.rechargeTicksInAir;

        if (rechargeTickCounter < threshold) {
            rechargeTickCounter++;
        }

        if (!isFlaring && rechargeTickCounter >= threshold && charge < BetterFlightCommonConfig.maxCharge) {

            if (player.getFoodData().getFoodLevel() > BetterFlightCommonConfig.minFood) {
                charge++;
                rechargeTickCounter = 0;
                rechargeBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
                CTSFlightActionPacket.send(FlightActionType.RECHARGE);
                player.causeFoodExhaustion((float) BetterFlightCommonConfig.exhaustionPerChargePoint);
            }
        }
    }

    //MAYBE rework flare or introduce a new method to "glide"? Like being able to hold one's position while in the air like a bird.
    private static void handleFlare(LocalPlayer player) {
        if (isElytraEquipped
                && Keybinding.flareKey.isDown()
                && (player.isCreative() || charge > 0)
                && !player.isOnGround()
                && player.isFallFlying()) {

            CTSFlightActionPacket.send(FlightActionType.FLARE);
            FlightHandler.handleFlare(player);

            flareTickCounter++;
            isFlaring = true;

            if (flareTickCounter >= BetterFlightCommonConfig.flareTicksPerChargePoint) {
                spendCharge(player, 1);
                flareTickCounter = 0;
            }
        }
        else {
            if (flareTickCounter > 0) {
                flareTickCounter--;
            }
            isFlaring = false;
        }
    }

    /**
     * Determines if Flight stamina should be spent
     * @param player target player
     * @param points how much stamina to spend
     * @return true if creative mode or player is able to use stamina
     */
    private static boolean spendCharge(LocalPlayer player, int points) {

        if (player.isCreative()) return true;

        if (charge >= points) {
            charge -= points;
            rechargeTickCounter = 0;
            cooldownTimer = BetterFlightCommonConfig.cooldownTicks;
            depletionBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
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
    private static ItemStack findEquippedElytra(@NotNull LocalPlayer player) {

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
                catch(NoSuchElementException e) {
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
        // even if we found an elytra, we can't use it if durability is too low
        if (elytraStack.getMaxDamage() - elytraStack.getDamageValue() > 1) {
            return true;
        }
        else {
            // this elytra has broken
            return false;
        }
    }

    private static void cycleWidgetLocation() {

        switch (ClientConfig.hudLocation) {
            case BAR_CENTER -> ClientConfig.hudLocation = HudLocation.BAR_LEFT;
            case BAR_LEFT -> ClientConfig.hudLocation = HudLocation.BAR_RIGHT;
            case BAR_RIGHT -> ClientConfig.hudLocation = HudLocation.CURSOR_ABOVE;
            case CURSOR_ABOVE -> ClientConfig.hudLocation = HudLocation.CURSOR_RIGHT;
            case CURSOR_RIGHT -> ClientConfig.hudLocation = HudLocation.CURSOR_BELOW;
            case CURSOR_BELOW -> ClientConfig.hudLocation = HudLocation.CURSOR_LEFT;
            case CURSOR_LEFT -> ClientConfig.hudLocation = HudLocation.BAR_CENTER;
        }

        ClientConfig.CLIENT.hudLocation.set(ClientConfig.hudLocation);
        ClientConfig.CLIENT.hudLocation.save();
    }

    public static void handleSElytraChargePacket(STCElytraChargePacket message) {
        charge = message.getCharge();
    }
}
