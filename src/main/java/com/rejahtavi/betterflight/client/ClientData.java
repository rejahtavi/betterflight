package com.rejahtavi.betterflight.client;

public class ClientData
{
    private static boolean wingStatus = false;
    private static boolean isFlaring = false;
    private static int offGroundTicks = 0;
    private static boolean isFlightEnabled = true;
    private static int cooldown = 0;

    public static boolean isWearingFunctionalWings()
    {
        return wingStatus;
    }

    public static void setWingStatus(boolean wingStatus)
    {
        ClientData.wingStatus = wingStatus;
    }

    public static boolean isFlaring()
    {
        return isFlaring;
    }

    public static void setIsFlaring(boolean isFlaring)
    {
        ClientData.isFlaring = isFlaring;
    }

    public static int getOffGroundTicks()
    {
        return offGroundTicks;
    }

    public static void setOffGroundTicks(int offGroundTicks)
    {
        ClientData.offGroundTicks = offGroundTicks;
    }

    public static void tickOffGround()
    {
        ClientData.offGroundTicks++;
    }

    public static boolean isFlightEnabled()
    {
        return isFlightEnabled;
    }

    public static void setFlightEnabled(boolean isFlightEnabled)
    {
        ClientData.isFlightEnabled = isFlightEnabled;
    }

    public static int getCooldown()
    {
        return cooldown;
    }

    public static void setCooldown(int ticks)
    {
        ClientData.cooldown = ticks;
    }

    public static void subCooldown(int ticks)
    {
        ClientData.cooldown = Math.max(ClientData.cooldown - ticks, 0);
    }


}
