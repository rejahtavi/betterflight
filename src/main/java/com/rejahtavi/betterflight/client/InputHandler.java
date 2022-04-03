package com.rejahtavi.betterflight.client;

import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.rejahtavi.betterflight.BetterFlight;
import com.rejahtavi.betterflight.common.FlightActionType;
import com.rejahtavi.betterflight.common.FlightActions;
import com.rejahtavi.betterflight.config.Config;
import com.rejahtavi.betterflight.config.Config.WidgetLocation;
import com.rejahtavi.betterflight.network.CFlightActionPacket;

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
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, value = Dist.CLIENT)
public class InputHandler {

    public static KeyBinding takeOffKey;
    public static KeyBinding flapKey;
    public static KeyBinding flareKey;
    public static KeyBinding widgetPosKey;

    public static Random random = new Random(System.currentTimeMillis());

    // elytra status timers, counters, & other behavior
    public static int elytraTicksFlared = 0;
    public static int elytraCooldown = 0;
    public static int elytraRecharge = 0;
    public static int elytraCharge = Config.ELYTRA_MAX_CHARGE;
    public static boolean isElytraEquipped;
    public static boolean isFlaring;
    public static double elytraDurability = 1.0D;
    public static int elytraDamageValue = 1;

    // meter border color timers
    public static int showRechargeTicks = 0;
    public static int showDepletionTicks = 0;

    public static void init() {
        // register keybinds
        InputHandler.takeOffKey = new KeyBinding(BetterFlight.MODID + ".takeoff", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(InputHandler.takeOffKey);

        InputHandler.flapKey = new KeyBinding(BetterFlight.MODID + ".flap", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(InputHandler.flapKey);

        InputHandler.flareKey = new KeyBinding(BetterFlight.MODID + ".flare", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(InputHandler.flareKey);

        InputHandler.widgetPosKey = new KeyBinding(BetterFlight.MODID + ".widget", KeyConflictContext.IN_GAME,
                KeyModifier.NONE, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10), BetterFlight.MODID);
        ClientRegistry.registerKeyBinding(InputHandler.widgetPosKey);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        PlayerEntity player = mc.player;
        if (player == null) return;

        // check that we have an elytra with enough durability for it to be usable
        updateElytraStatus();

        // tick the elytra cooldown
        if (elytraCooldown > 0) elytraCooldown--;

        // tick the timers that flash the border white (recharge) or red (depletion)
        if (showRechargeTicks > 0) showRechargeTicks--;
        if (showDepletionTicks > 0) showDepletionTicks--;

        // do we still need to wait for the recharge timer?
        if (elytraRecharge > 0) {
            // yes. tick it.
            elytraRecharge--;
            if (player.isOnGround()) {
                // and if we're on the ground, tick it 4 more times
                // this gives the 5x speed up in recharge rate while standing on solid ground
                elytraRecharge -= 4;
                // ensure we don't underflow
                if (elytraRecharge < 0) elytraRecharge = 0;
            }
        }
        else {
            // no. this tick we can refill a point, if needed
            if (elytraCharge < Config.ELYTRA_MAX_CHARGE) {
                // if possible, add a point to the meter.
                elytraCharge++;
                // reset the recharge timer
                elytraRecharge = Config.elytraRechargeTicks;
                // set the timer that flashes the border white for the recharge event
                showRechargeTicks = Config.BORDER_FLASH_TICKS;
            }
        }

        // flare handling (applied every tick, not per key press)
        // when the key is held, check whether we can flare this tick or not.
        if (flareKey.isDown()
                && isElytraEquipped
                && elytraCooldown == 0
                && elytraCharge > 0
                && !player.isOnGround()
                && player.isFallFlying()) {

            // all good, tell the server we are flaring this tick.
            BetterFlight.NETWORK.sendToServer(new CFlightActionPacket(FlightActionType.FLARE));

            // apply the flare impulse locally (tiny compared to takeoff and flapping!)
            FlightActions.applyFlareImpulse(player);

            // count this tick towards number of ticks flared so far
            elytraTicksFlared++;

            // remember that flaring occured this tick (for HUD overlay)
            isFlaring = true;

            // check if we have flared long enough to spend a point from the meter
            if (elytraTicksFlared >= Config.elytraFlareTicksPerChargePoint) {
                // yes, reset the counter, spend the point, and flash the border red
                elytraTicksFlared = 0;
                elytraCharge--;
                showDepletionTicks = Config.BORDER_FLASH_TICKS;
            }
        }
        else {
            // we are not flaring this tick.
            // decay away the counter, so that the player isn't unexpectedly hit with
            // an unusually fast cost next time.
            if (elytraTicksFlared > 0) {
                elytraTicksFlared--;
            }
            // remember that flaring did not occur this tick (for HUD overlay)
            isFlaring = false;
        }
    }

    // update button state immediately upon an input event
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        PlayerEntity player = mc.player;
        if (player == null) return;
        
        // take off key was pressed -- check if take off is possible
        if (event.getKey() == takeOffKey.getKey().getValue()) {
            if ((event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT)
                    && elytraCooldown == 0
                    && elytraCharge > 0
                    && player.isOnGround()
                    && player.isSprinting()
                    && !player.isFallFlying()
                    && player.getDeltaMovement().length() > 0.2500D) {

                // all good, tell the server we are taking off
                BetterFlight.NETWORK.sendToServer(new CFlightActionPacket(FlightActionType.TAKEOFF));

                // perform the take off locally
                FlightActions.applyTakeOffImpulse(player);

                // reset the recharge timer
                elytraRecharge = Config.elytraRechargeTicks;

                // spend a point of charge on the meter
                elytraCharge--;

                // set the timer that flashes the border red for the lost point of charge
                showDepletionTicks = Config.BORDER_FLASH_TICKS;

                // play the flap sound locally for takeoff
                // the above packet will also cause this to be played for everyone nearby
                player.playSound(BetterFlight.FLAP_SOUND, (float) Config.TAKE_OFF_VOLUME, 2.0f);
            }
        }

        // flap key was pressed -- validate whether conditions are met to allow flapping
        // wings
        if (event.getKey() == flapKey.getKey().getValue()) {
            if (event.getAction() == GLFW.GLFW_PRESS
                    && elytraCooldown == 0
                    && elytraCharge > 0
                    && !player.isOnGround()
                    && player.isFallFlying()
                    && elytraCooldown == 0) {

                // all good, tell the server we are flapping the elytra
                BetterFlight.NETWORK.sendToServer(new CFlightActionPacket(FlightActionType.FLAP));

                // perform the flap locally
                FlightActions.applyFlapImpulse(player);

                // reset the flap cooldown and meter recharge timers
                elytraCooldown = Config.elytraCooldownTicks;
                elytraRecharge = Config.elytraRechargeTicks;

                // spend a point of charge on the meter
                elytraCharge--;

                // set the timer that flashes the border red for the lost point of charge
                showDepletionTicks = Config.BORDER_FLASH_TICKS;

                // play the flap sound locally
                // the above packet will also cause this to be played for everyone nearby
                player.playSound(BetterFlight.FLAP_SOUND, (float) Config.FLAP_VOLUME, 2.0f);
            }
        }

        // cycle the location of the elytra meter between the available options
        if (event.getKey() == widgetPosKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {

            switch (Config.widgetLocation) {
                case BAR_CENTER:
                    Config.widgetLocation = WidgetLocation.BAR_LEFT;
                    break;
                case BAR_LEFT:
                    Config.widgetLocation = WidgetLocation.BAR_RIGHT;
                    break;
                case BAR_RIGHT:
                    Config.widgetLocation = WidgetLocation.CURSOR_BELOW;
                    break;
                case CURSOR_BELOW:
                    Config.widgetLocation = WidgetLocation.CURSOR_LEFT;
                    break;
                case CURSOR_LEFT:
                    Config.widgetLocation = WidgetLocation.CURSOR_RIGHT;
                    break;
                case CURSOR_RIGHT:
                    Config.widgetLocation = WidgetLocation.CURSOR_ABOVE;
                    break;
                case CURSOR_ABOVE:
                    Config.widgetLocation = WidgetLocation.BAR_CENTER;
                    break;
                default:
                    Config.widgetLocation = WidgetLocation.BAR_CENTER;
                    break;

            }

            // immediately store change to config file so we remember on next startup
            Config.COMMON.widgetLocation.set(Config.widgetLocation);
            Config.COMMON.widgetLocation.save();
        }

    }

    @SuppressWarnings("deprecation")
    private static void updateElytraStatus() {

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        PlayerEntity player = mc.player;
        if (player == null) return;
        
        // assume no elytra, then search for one
        isElytraEquipped = false;
        elytraDurability = 0.0f;
        elytraDamageValue = 1;

        // check the player's chest slot for elytra
        ItemStack elytraStack = player.getItemBySlot(EquipmentSlotType.CHEST).getStack();
        if (Config.elytraItems.contains(elytraStack.getItem())) {
            // elytra is present in the chest slot
            isElytraEquipped = true;
        }

        // if dependencies are present, check the curios slots as well
        if (BetterFlight.isCuriousElytraLoaded) {
            for (Item elytraItem : Config.elytraItems) {
                if (CuriosApi.getCuriosHelper().findEquippedCurio(elytraItem, player).isPresent()) {
                    // elytra is present in a curio slot
                    isElytraEquipped = true;
                    elytraStack = CuriosApi.getCuriosHelper()
                            .findEquippedCurio(elytraItem, player)
                            .get().getRight().getStack();
                }
            }
        }

        // even if we found an elytra, we have to account for durability.
        // if the durability is too low, we can't use it.
        if (isElytraEquipped) {
            // fully spent elytras have a damage value of 1
            elytraDamageValue = elytraStack.getDamageValue();
            if (elytraDamageValue > 1) {
                // this elytra is still usable. get a friendlier variant of the damage value for
                // display.
                elytraDurability = elytraStack.getItem().getDurabilityForDisplay(elytraStack);
            }
            else {
                // this elytra has broken and can not be used after all
                isElytraEquipped = false;
            }
        }
    }
}
