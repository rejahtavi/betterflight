package com.rejahtavi.betterflight.client;

import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.client.ClientConfig.HudLocation;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.ServerConfig;
import com.rejahtavi.betterflight.common.ServerLogic;
import com.rejahtavi.betterflight.common.Sounds;
import com.rejahtavi.betterflight.network.CFlightActionPacket;
import com.rejahtavi.betterflight.network.SElytraChargePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class ClientLogic {

    // keybinds
    public static KeyBinding takeOffKey;
    public static KeyBinding flapKey;
    public static KeyBinding flareKey;
    public static KeyBinding widgetPosKey;

    // state
    public static boolean isElytraEquipped = false;
    public static boolean isFlaring = false;
    public static int charge = ServerConfig.maxCharge;

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
        ClientLogic.takeOffKey = new KeyBinding(BetterFlight.MODID + ".keys.takeoff", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(ClientLogic.takeOffKey);

        ClientLogic.flapKey = new KeyBinding(BetterFlight.MODID + ".keys.flap", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(ClientLogic.flapKey);

        ClientLogic.flareKey = new KeyBinding(BetterFlight.MODID + ".keys.flare", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(ClientLogic.flareKey);

        ClientLogic.widgetPosKey = new KeyBinding(BetterFlight.MODID + ".keys.widget", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(ClientLogic.widgetPosKey);

        // default to full elytra meter on startup
        charge = ServerConfig.maxCharge;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        PlayerEntity player = mc.player;
        if (player == null) return;

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
        if (mc == null) return;
        PlayerEntity player = mc.player;
        if (player == null) return;

        // track ground state for takeoff logic
        if (player.isOnGround()) {
            offGroundTickCounter = 0;
        } else {
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

    private static void tryTakeOff(PlayerEntity player) {
        if (isElytraEquipped
                && offGroundTickCounter > ServerConfig.TAKE_OFF_JUMP_DELAY
                && player.isSprinting()
                && !player.isFallFlying()
                && player.getDeltaMovement().length() > ServerConfig.TAKE_OFF_SPEED) {

            if (spendCharge(player, ServerConfig.takeOffCost)) {
                CFlightActionPacket.send(FlightActionType.TAKEOFF);
                ServerLogic.applyTakeOffImpulse(player);
                player.playSound(Sounds.FLAP, (float) ClientConfig.takeOffVolume, ClientConfig.FLAP_SOUND_PITCH);
            }
        }
    }

    private static void tryFlap(PlayerEntity player) {
        if (isElytraEquipped
                && cooldownTimer <= 0
                && !player.isOnGround()
                && player.isFallFlying()) {

            if (spendCharge(player, ServerConfig.flapCost)) {
                CFlightActionPacket.send(FlightActionType.FLAP);
                ServerLogic.applyFlapImpulse(player);
                player.playSound(Sounds.FLAP, (float) ClientConfig.flapVolume, ClientConfig.FLAP_SOUND_PITCH);
            }
        }
    }

    private static void handleRecharge(PlayerEntity player) {

        if (player.isCreative()) {
            charge = ServerConfig.maxCharge;
            return;
        }

        int threshold = player.isOnGround() ? ServerConfig.rechargeTicksOnGround : ServerConfig.rechargeTicksInAir;

        if (rechargeTickCounter < threshold) {
            rechargeTickCounter++;
        }

        if (!isFlaring && rechargeTickCounter >= threshold && charge < ServerConfig.maxCharge) {

            if (player.getFoodData().getFoodLevel() > ServerConfig.minFood) {
                charge++;
                rechargeTickCounter = 0;
                rechargeBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
                CFlightActionPacket.send(FlightActionType.RECHARGE);
                player.causeFoodExhaustion((float) ServerConfig.exhaustionPerChargePoint);
            }
        }
    }

    private static void handleFlare(PlayerEntity player) {
        if (flareKey.isDown()
                && isElytraEquipped
                && (player.isCreative() || charge > 0)
                && !player.isOnGround()
                && player.isFallFlying()) {

            CFlightActionPacket.send(FlightActionType.FLARE);
            ServerLogic.applyFlareImpulse(player);

            flareTickCounter++;
            isFlaring = true;

            if (flareTickCounter >= ServerConfig.flareTicksPerChargePoint) {
                spendCharge(player, 1);
                flareTickCounter = 0;
            }
        } else {
            if (flareTickCounter > 0) {
                flareTickCounter--;
            }
            isFlaring = false;
        }
    }

    private static boolean spendCharge(PlayerEntity player, int points) {

        if (player.isCreative()) return true;

        if (charge >= points) {
            charge -= points;
            rechargeTickCounter = 0;
            cooldownTimer = ServerConfig.cooldownTicks;
            depletionBorderTimer = ClientConfig.BORDER_FLASH_TICKS;
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static void updateElytraStatus(PlayerEntity player) {

        // assume no elytra, then search for one
        isElytraEquipped = false;
        elytraDurability = 0.0f;
        elytraDurabilityLeft = 0;

        // check the player's chest slot for elytra
        ItemStack elytraStack = player.getItemBySlot(EquipmentSlotType.CHEST).getStack();
        if (ServerConfig.elytraItems.contains(elytraStack.getItem())) {
            // elytra is present in the chest slot
            isElytraEquipped = true;
        }

        // if dependencies are present, check the curios slots as well
        if (BetterFlight.isCuriousElytraLoaded) {
            for (Item elytraItem : ServerConfig.elytraItems) {
                if (CuriosApi.getCuriosHelper().findEquippedCurio(elytraItem, player).isPresent()) {
                    isElytraEquipped = true;
                    elytraStack = CuriosApi.getCuriosHelper()
                            .findEquippedCurio(elytraItem, player)
                            .get().getRight().getStack();
                }
            }
        }

        // even if we found an elytra, we can't use it if durability is too low
        if (isElytraEquipped) {

            elytraDurabilityLeft = elytraStack.getMaxDamage() - elytraStack.getDamageValue();

            if (elytraDurabilityLeft > 1) {
                elytraDurability = elytraStack.getItem().getDurabilityForDisplay(elytraStack);
            } else {
                // this elytra has broken
                isElytraEquipped = false;
            }
        }
    }

    private static void cycleWidgetLocation() {

        switch (ClientConfig.hudLocation) {
            case BAR_CENTER:
                ClientConfig.hudLocation = HudLocation.BAR_LEFT;
                break;
            case BAR_LEFT:
                ClientConfig.hudLocation = HudLocation.BAR_RIGHT;
                break;
            case BAR_RIGHT:
                ClientConfig.hudLocation = HudLocation.CURSOR_ABOVE;
                break;
            case CURSOR_ABOVE:
                ClientConfig.hudLocation = HudLocation.CURSOR_RIGHT;
                break;
            case CURSOR_RIGHT:
                ClientConfig.hudLocation = HudLocation.CURSOR_BELOW;
                break;
            case CURSOR_BELOW:
                ClientConfig.hudLocation = HudLocation.CURSOR_LEFT;
                break;
            case CURSOR_LEFT:
                ClientConfig.hudLocation = HudLocation.BAR_CENTER;
                break;
        }

        ClientConfig.CLIENT.hudLocation.set(ClientConfig.hudLocation);
        ClientConfig.CLIENT.hudLocation.save();
    }

    public static void handleSElytraChargePacket(SElytraChargePacket message, Supplier<Context> context) {
        charge = message.getCharge();
    }
}
