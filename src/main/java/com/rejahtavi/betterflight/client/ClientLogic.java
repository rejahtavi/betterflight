package com.rejahtavi.betterflight.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig.HudLocation;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.CommonEvents;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.network.CFlightActionPacket;
import com.rejahtavi.betterflight.network.SElytraChargePacket;
import com.rejahtavi.betterflight.util.FlightHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientLogic {

    // key mappings
    public static final KeyMapping takeOffKey = new KeyMapping(BetterFlight.MODID + ".keys.takeoff",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, BetterFlight.MODID);
    public static final KeyMapping flapKey = new KeyMapping(BetterFlight.MODID + ".keys.flap",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, BetterFlight.MODID);
    public static final KeyMapping flareKey = new KeyMapping(BetterFlight.MODID + ".keys.flare",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, BetterFlight.MODID);
    public static final KeyMapping widgetPosKey = new KeyMapping(BetterFlight.MODID + ".keys.widget",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F10, BetterFlight.MODID);

    // state
    public static boolean isElytraEquipped = false;
    public static boolean isFlaring = false;
    public static int charge = CommonEvents.maxCharge;

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
        charge = CommonEvents.maxCharge;
    }

    // key mapping registration
    @Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(takeOffKey);
            event.register(flapKey);
            event.register(flareKey);
            event.register(widgetPosKey);
        }
    }

    // key event handling
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // don't react to key presses if a screen or chat is open
        if (mc.screen != null) return;

        if (event.getKey() == takeOffKey.getKey().getValue()
                && (event.getAction() == GLFW.GLFW_PRESS)) {
            // && (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT)) {
            tryTakeOff(player);
        }

        if (event.getKey() == flapKey.getKey().getValue()
                && event.getAction() == GLFW.GLFW_PRESS) {
            tryFlap(player);
        }

        if (event.getKey() == widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            cycleWidgetLocation();
        }
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

        updateElytraStatus(player);
        handleRecharge(player);
        handleFlare(player);
    }

    private static void tryTakeOff(LocalPlayer player) {
        if (isElytraEquipped
                && offGroundTickCounter > CommonEvents.TAKE_OFF_JUMP_DELAY
                && player.isSprinting()
                && !player.isFallFlying()
                && player.getDeltaMovement().length() > CommonEvents.TAKE_OFF_SPEED) {

            if (spendCharge(player, CommonEvents.takeOffCost)) {
                CFlightActionPacket.send(FlightActionType.TAKEOFF);
                FlightHandler.handleTakeoff(player);
                // player.playSound(Sounds.SOUND_FLAP.get(), (float) ClientConfig.takeOffVolume,
                // ClientConfig.FLAP_SOUND_PITCH);
                player.playSound(Sounds.FLAP.get(), (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
            }
        }
    }

    private static void tryFlap(LocalPlayer player) {
        if (isElytraEquipped
                && cooldownTimer <= 0
                && !player.isOnGround()
                && player.isFallFlying()) {

            if (spendCharge(player, CommonEvents.flapCost)) {
                CFlightActionPacket.send(FlightActionType.FLAP);
                FlightHandler.handleFlap(player);
                player.playSound(Sounds.FLAP.get(), (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
            }
        }
    }

    private static void handleRecharge(LocalPlayer player) {

        if (player.isCreative()) {
            charge = CommonEvents.maxCharge;
            return;
        }

        int threshold = player.isOnGround() ? CommonEvents.rechargeTicksOnGround : CommonEvents.rechargeTicksInAir;

        if (rechargeTickCounter < threshold) {
            rechargeTickCounter++;
        }

        if (!isFlaring && rechargeTickCounter >= threshold && charge < CommonEvents.maxCharge) {

            if (player.getFoodData().getFoodLevel() > CommonEvents.minFood) {
                charge++;
                rechargeTickCounter = 0;
                rechargeBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
                CFlightActionPacket.send(FlightActionType.RECHARGE);
                player.causeFoodExhaustion((float) CommonEvents.exhaustionPerChargePoint);
            }
        }
    }

    private static void handleFlare(LocalPlayer player) {
        if (isElytraEquipped
                && flareKey.isDown()
                && (player.isCreative() || charge > 0)
                && !player.isOnGround()
                && player.isFallFlying()) {

            CFlightActionPacket.send(FlightActionType.FLARE);
            FlightHandler.handleFlare(player);

            flareTickCounter++;
            isFlaring = true;

            if (flareTickCounter >= CommonEvents.flareTicksPerChargePoint) {
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
            cooldownTimer = CommonEvents.cooldownTicks;
            depletionBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
            return true;
        }
        else {
            return false;
        }
    }

    private static void updateElytraStatus(LocalPlayer player) {

        // assume no elytra, then search for one
        isElytraEquipped = false;
        elytraDurability = 0.0f;
        elytraDurabilityLeft = 0;

        // check the player's chest slot for elytra
        ItemStack elytraStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (CommonEvents.elytraItems.contains(elytraStack.getItem())) {
            // elytra is present in the chest slot
            isElytraEquipped = true;
        }

        // if dependencies are present, check the curios slots as well
        if (BetterFlight.isCuriousElytraLoaded) {
            for (Item elytraItem : CommonEvents.elytraItems) {
                if (CuriosApi.getCuriosHelper().findEquippedCurio(elytraItem, player).isPresent()) {
                    isElytraEquipped = true;
                    elytraStack = CuriosApi.getCuriosHelper()
                            .findEquippedCurio(elytraItem, player)
                            .get().getRight();
                }
            }
        }

        // even if we found an elytra, we can't use it if durability is too low
        if (isElytraEquipped) {

            elytraDurabilityLeft = elytraStack.getMaxDamage() - elytraStack.getDamageValue();

            if (elytraDurabilityLeft > 1) {
                elytraDurability = (float) elytraStack.getItem().getDamage(elytraStack)
                        / (float) elytraStack.getMaxDamage();
            }
            else {
                // this elytra has broken
                isElytraEquipped = false;
            }
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

    public static void handleSElytraChargePacket(SElytraChargePacket message) {
        charge = message.getCharge();
    }
}
