package jsco.scv;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static jsco.scv.SlimeChunkViewer.client;

public class SCVRenderer
{
    public static final Set<ChunkPos> slimeChunks = Collections.synchronizedSet(new HashSet<>());
    public static final Set<ChunkPos> nonSlimeChunks = Collections.synchronizedSet(new HashSet<>());
    private static VertexBuffer solidBox;
    private static VertexBuffer outlinedBox;
    private final MatrixStack matrixStack;
    BlockPos camPos = getCameraBlockPos();
    float regionX;
    float regionZ;

    public SCVRenderer(MatrixStack matrixStack)
    {
        regionX = (camPos.getX() >> 9) * 512;
        regionZ = (camPos.getZ() >> 9) * 512;
        closeBuffers();
        this.matrixStack = matrixStack;
        solidBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        outlinedBox = new VertexBuffer(VertexBuffer.Usage.STATIC);

        Box box = new Box(BlockPos.ORIGIN);
        drawSolidBox(box, solidBox);
        drawOutlinedBox(box, outlinedBox);
    }
    public static void closeBuffers()
    {
        Stream.of(solidBox, outlinedBox).filter(Objects::nonNull).forEach(VertexBuffer::close);
    }

    private void setUp()
    {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        RenderSystem.setShader(GameRenderer::getPositionProgram);

        matrixStack.push();
    }
    private void finish()
    {
        matrixStack.pop();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public void renderBox(Box box, Color color)
    {
        renderBox(box,color,new Color(0,0,0,0));
    }

    public void renderBox(Box box, Color color, Color outlineColor)
    {
        setUp();

        SCVRenderer.applyRegionalRenderOffset(matrixStack);
        float[] colorF = color.getColorComponents(null);
        float[] colorF2 = outlineColor.getColorComponents(null);

        matrixStack.translate(box.minX - regionX, box.minY,
                box.minZ - regionZ);

        matrixStack.scale((float)(box.maxX - box.minX),
                (float)(box.maxY - box.minY), (float)(box.maxZ - box.minZ));

        Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
        Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
        ShaderProgram shader = RenderSystem.getShader();

        RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], (color.getAlpha())/255.0F);
        solidBox.bind();
        solidBox.draw(viewMatrix, projMatrix, shader);
        VertexBuffer.unbind();

        if(outlineColor.getAlpha() != 0) {
            RenderSystem.setShaderColor(colorF2[0], colorF2[1], colorF2[2], 255);
            outlinedBox.bind();
            outlinedBox.draw(viewMatrix, projMatrix, shader);
            VertexBuffer.unbind();
        }

        finish();
    }

    private void drawSolidBox(Box bb, VertexBuffer vertexBuffer)
    {
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION);
        drawSolidBox(bb, bufferBuilder);
        BufferBuilder.BuiltBuffer buffer = bufferBuilder.end();

        vertexBuffer.bind();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }
    private void drawSolidBox(Box bb, BufferBuilder bufferBuilder)
    {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
    }
    private void drawOutlinedBox(Box bb, VertexBuffer vertexBuffer)
    {
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION);
        drawOutlinedBox(bb, bufferBuilder);
        BufferBuilder.BuiltBuffer buffer = bufferBuilder.end();

        vertexBuffer.bind();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    private void drawOutlinedBox(Box bb, BufferBuilder bufferBuilder)
    {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
    }

    public static Vec3d getCameraPos()
    {
        Camera camera = client.getBlockEntityRenderDispatcher().camera;
        if(camera == null)
            return Vec3d.ZERO;
        return camera.getPos();
    }

    public static BlockPos getCameraBlockPos()
    {
        Camera camera = client.getBlockEntityRenderDispatcher().camera;
        if(camera == null)
            return BlockPos.ORIGIN;
        return camera.getBlockPos();
    }

    private static void applyRegionalRenderOffset(MatrixStack matrixStack)
    {

        Vec3d camPos = getCameraPos();
        BlockPos blockPos = getCameraBlockPos();

        int regionX = (blockPos.getX() >> 9) * 512;
        int regionZ = (blockPos.getZ() >> 9) * 512;

        matrixStack.translate(regionX - camPos.x, -camPos.y,
                regionZ - camPos.z);
    }

    public void renderSlimeChunks()
    {
        synchronized (SCVRenderer.slimeChunks)
        {
            for (ChunkPos c : SCVRenderer.slimeChunks)
            {
                Vec3d start = c.getStartPos().toCenterPos().add(0, (int) SlimeChunkViewer.getConfigProperty("renderHeight"),0);
                Vec3d end = start.add(16, 0, 16);
                this.renderBox(new Box(start, end), new Color(48,255,33,85+30));
            }
        }
    }

    public void renderNonSlimeChunks()
    {
        synchronized (SCVRenderer.nonSlimeChunks)
        {
            for (ChunkPos c : SCVRenderer.nonSlimeChunks)
            {
                Vec3d start = c.getStartPos().toCenterPos().add(0, (int) SlimeChunkViewer.getConfigProperty("renderHeight"), 0);
                Vec3d end = start.add(16, 0, 16);
                this.renderBox(new Box(start, end), new Color(255, 48, 33, 85+30));
            }
        }
    }
}
