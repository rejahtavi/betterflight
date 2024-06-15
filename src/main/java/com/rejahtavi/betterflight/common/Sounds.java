package com.rejahtavi.betterflight.common;

import com.rejahtavi.betterflight.BetterFlight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Sounds
{

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BetterFlight.MODID);

    public static final RegistryObject<SoundEvent> FLAP = createEvent("betterflight.flap");
    public static final RegistryObject<SoundEvent> BOOST = createEvent("betterflight.boost");

    private static RegistryObject<SoundEvent> createEvent(String sound)
    {
        return SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(BetterFlight.MODID, sound)));
    }

}
