package com.rejahtavi.betterflight.common;

import java.util.HashSet;
import java.util.Set;

import com.rejahtavi.betterflight.BetterFlight;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = BetterFlight.MODID, bus = Bus.MOD)
public class Sounds {
    
    static Set<SoundEvent> registeredEvents = new HashSet<>();
    public static SoundEvent FLAP = registerSound("flap");

    private static SoundEvent registerSound(String name) {
        ResourceLocation resourceLocation = new ResourceLocation(BetterFlight.MODID, name);
        SoundEvent soundEvent = new SoundEvent(resourceLocation);
        registeredEvents.add(soundEvent.setRegistryName(resourceLocation));
        return soundEvent;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent soundEvent : registeredEvents)
            event.getRegistry().register(soundEvent);
    }
}
