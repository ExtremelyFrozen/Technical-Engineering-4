package com.modularmc.ten.api.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ClientUtil {

    private static final LoadingCache<ResourceLocation, Optional<Dimension>> TEXTURE_SIZE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(128).build(new CacheLoader<>() {

        @Override
        public Optional<Dimension> load(ResourceLocation rl) {
            return readPngSize(rl);
        }
    });

    private ClientUtil() {}

    private static Optional<Dimension> readPngSize(ResourceLocation rl) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) {
                return Optional.empty();
            }
            try (InputStream in = res.get().open(); NativeImage image = NativeImage.read(in)) {
                return Optional.of(new Dimension(image.getWidth(), image.getHeight()));
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static int @Nullable [] getTextureSize(ResourceLocation rl) {
        Optional<Dimension> dim = TEXTURE_SIZE_CACHE.getUnchecked(rl);
        return dim.map(Dimension::toIntArray).orElse(null);
    }

    public static TextureAtlasSprite getBlockSprite(ResourceLocation rl) {
        return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(rl);
    }

    public static @Nullable TextureRegion getFaceSprite(BlockState state, Direction face) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource rand = RandomSource.create();
        List<BakedQuad> quads = model.getQuads(state, face, rand, ModelData.EMPTY, null);
        if (!quads.isEmpty()) {
            return TextureRegion.ofSprite(quads.getFirst().getSprite());
        }
        quads = model.getQuads(state, null, rand, ModelData.EMPTY, null);
        if (!quads.isEmpty()) {
            return TextureRegion.ofSprite(quads.getFirst().getSprite());
        }
        return TextureRegion.ofSprite(model.getParticleIcon(ModelData.EMPTY));
    }

    private record Dimension(int width, int height) {

        public int[] toIntArray() {
            return new int[] { width, height };
        }
    }
}
