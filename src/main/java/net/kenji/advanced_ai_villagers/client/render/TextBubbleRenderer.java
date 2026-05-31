package net.kenji.advanced_ai_villagers.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kenji.advanced_ai_villagers.AdvancedAiVillagers;
import net.kenji.advanced_ai_villagers.api.SpeechManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;


@Mod.EventBusSubscriber(modid = AdvancedAiVillagers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TextBubbleRenderer {


    private static final double BUBBLE_RENDER_RANGE = 12;
    private static final ResourceLocation SPEECH_BUBBLE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AdvancedAiVillagers.MODID, "textures/gui/speech_bubble.png");

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Entity cameraEntity = event.getCamera().getEntity();
        Level level = cameraEntity.level();
        AABB boundingBox = cameraEntity.getBoundingBox().inflate(BUBBLE_RENDER_RANGE);
        List<Entity> entities = level.getEntities(cameraEntity, boundingBox);
        for (Entity entity : entities) {
            if (!(entity instanceof Villager villager)) continue;  // ✅ skip non-villagers
            String tagText = villager.getPersistentData().getString(SpeechManager.SPEECH_BUBBLE_TAG);
            if (tagText.isEmpty()) continue;                       // ✅ skip untagged villagers

            renderSpeechBubble(event.getPoseStack(), event.getCamera(), villager, tagText);
        }
    }

    private static void renderSpeechBubble(PoseStack poseStack, Camera camera, Entity entity, String text) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        Vec3 camPos = camera.getPosition();

        float partialTick = Minecraft.getInstance().getPartialTick();
        double horizontalOffset = -0.675; // positive = right from villager's perspective (world X)
        double verticalOffset   = -0.75; // negative = lower

        double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - camPos.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - camPos.y + entity.getBbHeight() + 0.5;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - camPos.z;;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera.rotation());

        poseStack.translate(horizontalOffset, verticalOffset, 0f);
        float scale = 0.0145f;
        poseStack.scale(-scale, -scale, scale);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // --- Image dimensions (your PNG is 700x560 roughly, define logical size) ---
        float bubbleW = 200f;
        float bubbleH = 100f; // will grow with text (see below)

        // --- Wrap text and calculate required height ---
        int maxLineWidth = (int)(bubbleW * 0.75f); // inner text area ~75% of bubble width
        List<FormattedCharSequence> lines = font.split(Component.literal(text), maxLineWidth);

        int lineHeight = font.lineHeight + 1;
        float textBlockHeight = lines.size() * lineHeight;

        // Grow bubble height to fit text, with a minimum
        float innerPadTop = bubbleH * 0.15f;   // ~15% from top (avoid tail area)
        float innerPadBottom = bubbleH * 0.30f; // ~30% from bottom (the tail takes space)
        float minInnerH = bubbleH - innerPadTop - innerPadBottom;
        if (textBlockHeight > minInnerH) {
            bubbleH += (textBlockHeight - minInnerH);
        }

        float bubbleX = -bubbleW / 2f;
        float bubbleY = -bubbleH;  // render upward from anchor point

        // --- Draw speech bubble texture ---
        drawTexturedQuad(poseStack, bufferSource, bubbleX, bubbleY, bubbleW, bubbleH,1);


        // --- Draw each line of text centered inside the bubble ---
        // Text sits in the upper portion; tail is at the bottom
        float textAreaTop = bubbleY + innerPadTop;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float lineX = -font.width(line) / 2f;
            float lineY = textAreaTop + i * lineHeight;
            font.drawInBatch(
                    line,
                    lineX, lineY,
                    0x222222,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    LightTexture.FULL_BRIGHT
            );
        }
        drawTexturedQuad(poseStack, bufferSource, bubbleX, bubbleY, bubbleW, bubbleH, 60); // ~25% alpha
        poseStack.translate(0, 0, -0.01f); // small forward offset in local space.

        drawTextLines(poseStack, bufferSource, font, lines, lineHeight, textAreaTop, true, 80);

        bufferSource.endBatch();
        // --- Draw VISIBLE pass on top (in front of walls, full opacity) ---
        drawTexturedQuad(poseStack, bufferSource, bubbleX, bubbleY, bubbleW, bubbleH, 255);
        poseStack.translate(0, 0, -0.01f); // small forward offset in local space.

        drawTextLines(poseStack, bufferSource, font, lines, lineHeight, textAreaTop, false, 255);

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private static void drawTextLines(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                      Font font, List<FormattedCharSequence> lines,
                                      int lineHeight, float textAreaTop,
                                      boolean seeThrough, int alpha) {
        // Pack alpha into the color int
        int color = (alpha << 24) | 0x222222;

        Font.DisplayMode mode = seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float lineX = -font.width(line) / 2f;
            float lineY = textAreaTop + i * lineHeight;
            font.drawInBatch(
                    line,
                    lineX, lineY,
                    color,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    mode,
                    0,
                    LightTexture.FULL_BRIGHT
            );
        }
    }

    private static void drawTexturedQuad(PoseStack poseStack, MultiBufferSource bufferSource,
                                         float x, float y, float w, float h, int alpha) {
        Matrix4f matrix = poseStack.last().pose();

        // Use textSeeThrough for the occluded pass, text for the visible pass
        RenderType type = alpha < 255
                ? RenderType.textSeeThrough(SPEECH_BUBBLE_TEXTURE)
                : RenderType.text(SPEECH_BUBBLE_TEXTURE);

        VertexConsumer buf = bufferSource.getBuffer(type);

        buf.vertex(matrix, x,     y + h, 0).color(255, 255, 255, alpha).uv(0,1).uv2(LightTexture.FULL_BRIGHT).endVertex();
        buf.vertex(matrix, x + w, y + h, 0).color(255, 255, 255, alpha).uv(1,1).uv2(LightTexture.FULL_BRIGHT).endVertex();
        buf.vertex(matrix, x + w, y,     0).color(255, 255, 255, alpha).uv(1,0).uv2(LightTexture.FULL_BRIGHT).endVertex();
        buf.vertex(matrix, x,     y,     0).color(255, 255, 255, alpha).uv(0,0).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }
}
