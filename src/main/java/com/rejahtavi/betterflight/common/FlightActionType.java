package com.rejahtavi.betterflight.common;

// Simple enum used for CFlightUpdatePackets.
// Lets us send multiple messages with a single universal packet format.
public enum FlightActionType
{
    TAKEOFF,
    RECHARGE,
    FLAP,
    STOP,
    BOOST
}