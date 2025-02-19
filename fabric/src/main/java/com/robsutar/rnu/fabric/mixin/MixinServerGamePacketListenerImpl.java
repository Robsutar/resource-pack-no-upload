package com.robsutar.rnu.fabric.mixin;

import com.robsutar.rnu.InitializeHook;
import com.robsutar.rnu.ResourcePackNoUpload;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Objects;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleResourcePackResponse", at = @At("HEAD"))
    private void onHandleResourcePackResponse(ServerboundResourcePackPacket packet, CallbackInfo ci) {
        ResourcePackNoUpload serverMod = InitializeHook.instance().serverMod();
        if (serverMod == null) return;

        // Unfortunately, the ServerboundResourcePackPacket#action field is not publicly accessible, so here we use a
        // bit of reflection to expose this field.
        // Since ServerboundResourcePackPacket only has one field, this loop only runs once.

        for (Field field : packet.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                ServerboundResourcePackPacket.Action action = Objects.requireNonNull((ServerboundResourcePackPacket.Action) field.get(packet));

                serverMod.listener().onPlayerResourcePackStatus(player, action);
                return;
            } catch (IllegalAccessException ignored) {
            }
        }
        throw new IllegalArgumentException("Reflection could not be applied to check ServerboundResourcePackPacket#action");
    }
}
