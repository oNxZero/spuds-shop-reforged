package net.lucab.shops.screen;

public enum ToggleButtonID {
    CreativeToggle("Crea"),
    ShopStyleToggle("Styl"),
    IgnoreNBTToggle("iNBT"),
    EffectsToggle("Efex");

    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, ToggleButtonID> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.INT.map(ToggleButtonID::fromNetworkId, ToggleButtonID::ordinal);

    private static ToggleButtonID fromNetworkId(int id) {
        ToggleButtonID[] buttons = values();
        if (id < 0 || id >= buttons.length) {
            throw new io.netty.handler.codec.DecoderException("Invalid shop toggle ID: " + id);
        }
        return buttons[id];
    }

    private final String serialised;

    ToggleButtonID(String s) {
        this.serialised = s;
    }

    public String getSerialised(){
        return serialised;
    }

    public static ToggleButtonID fromString(String s) {
        for (ToggleButtonID id : ToggleButtonID.values()) {
            if (id.getSerialised().equalsIgnoreCase(s)) {
                return id;
            }
        }
        throw new IllegalArgumentException("Unknown ToggleButtonID: " + s);
    }
}
