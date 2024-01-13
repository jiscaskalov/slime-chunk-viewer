package jsco.scv.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import jsco.scv.SCVConfig;
import jsco.scv.SCVRenderer;
import jsco.scv.SlimeChunkViewer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

import static jsco.scv.SlimeChunkViewer.client;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Shadow public abstract void render(float tickDelta, long startTime, boolean tick);

    @Shadow public abstract void renderWorld(float tickDelta, long limitTime, MatrixStack matrices);

    @Inject(
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0),
            method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V")
    private void onRenderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        client.getProfiler().push("slime-chunk-viewer" + "_render");

        SCVRenderer renderer = new SCVRenderer(matrices);
        if ((boolean) SlimeChunkViewer.getConfigProperty("renderValid")) renderer.renderSlimeChunks();
        if ((boolean) SlimeChunkViewer.getConfigProperty("renderInvalid")) renderer.renderNonSlimeChunks();

        RenderSystem.applyModelViewMatrix();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
}

@Mixin(ClientConnection.class)
abstract class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void handlePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof ChunkDataS2CPacket chunk) {
            int xPosition = chunk.getChunkX();
            int zPosition = chunk.getChunkZ();
            long seed = (long) SlimeChunkViewer.getConfigProperty("seed");

            if (seed == 0) return;

            Random rnd = new Random(seed + (int) (xPosition * xPosition * 0x4c1906) + (int) (xPosition * 0x5ac0db) + (int) (zPosition * zPosition) * 0x4307a7L + (int) (zPosition * 0x5f24f) ^ 0x3ad8025fL);

            boolean isSlimeChunk = (rnd.nextInt(10) == 0);
            if (isSlimeChunk) SCVRenderer.slimeChunks.add(new ChunkPos(xPosition, zPosition));
            else SCVRenderer.nonSlimeChunks.add(new ChunkPos(xPosition, zPosition));
        }
    }
}

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
        if ((long) SlimeChunkViewer.getConfigProperty("seed") == 0) client.player.sendMessage(Text.of(Formatting.YELLOW + "[SCV] Set a seed in the config menu or by using /setseed!"));
    }
}
