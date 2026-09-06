package com.modularmc.ten.api.client.gui;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record TextureRegion(Texture texture, int u, int v, int width, int height) {

    public static TextureRegion full(Texture texture) {
        return new TextureRegion(texture, 0, 0, texture.width(), texture.height());
    }

    public static @Nullable TextureRegion of(ResourceLocation rl) {
        Texture tex = Texture.of(rl);
        return tex != null ? full(tex) : null;
    }

    public static @Nullable TextureRegion of(ResourceLocation rl, int u, int v, int w, int h) {
        Texture tex = Texture.of(rl);
        return tex != null ? new TextureRegion(tex, u, v, w, h) : null;
    }

    public static @Nullable TextureRegion ofSprite(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return null;
        }
        Texture atlas = Texture.blockAtlas();
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        return new TextureRegion(atlas, (int) (u0 * atlas.width()), (int) (v0 * atlas.height()), (int) ((u1 - u0) * atlas.width()), (int) ((v1 - v0) * atlas.height()));
    }

    public ResourceLocation resource() {
        return texture.resource();
    }

    public TextureRegion sub(int offsetU, int offsetV, int subW, int subH) {
        return new TextureRegion(texture, u + offsetU, v + offsetV, subW, subH);
    }

    public TextureRegion row(int rows) {
        return new TextureRegion(texture, u, v + rows * height, width, height);
    }

    public TextureRegion line(int lines) {
        return new TextureRegion(texture, u + lines * width, v, width, height);
    }
}
