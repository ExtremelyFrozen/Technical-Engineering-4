package com.modularmc.ten.data;

import com.modularmc.ten.common.data.Mat;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 生成 {@code c:mats/<材料>} 聚合标签（锭或粉皆可入），供感应炉那类「材料族」输入使用。
 * <p>
 * 自动化靠两点，不需要运行期写标签：成员一律 {@code required:false}，材料缺席时条目自动为空、
 * 引用它的配方因输入标签为空而永不匹配；成员表由 {@link Mat} 的注册形态推导，
 * 加材料不必再手抄标签文件。NeoForge 1.21.1 没有受支持的加载期标签写入点
 * （{@code TagsUpdatedEvent} 在冻结之后），所以这条链只能在 DataGen 侧完成。
 */
public class TENMaterialTagGen implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final PackOutput output;

    public TENMaterialTagGen(PackOutput output) {
        this.output = output;
    }

    /**
     * 有粉形态的材料才生成聚合标签。锭侧不作判据：铁/铜/金的锭用原版物品，
     * {@code isRegistered("ingot")} 为 false，但 {@code c:ingots/X} 标签照样存在；
     * 真缺席时 optional 成员自动为空，不影响标签。
     */
    public static boolean hasMatTag(Mat mat) {
        return mat.isRegistered("dust");
    }

    /** 按材料 id 查（兼容表里只有字符串，拿不到 {@link Mat} 枚举）。 */
    public static boolean hasMatTag(String id) {
        for (var mat : Mat.values())
            if (mat.id.equals(id)) return hasMatTag(mat);
        return false;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dir = output.getOutputFolder().resolve("data/c/tags/item/mats");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var mat : Mat.values()) {
            if (hasMatTag(mat))
                futures.add(save(cache, dir.resolve(mat.id + ".json"), build(mat)));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private JsonObject build(Mat mat) {
        var j = new JsonObject();
        j.addProperty("replace", false);
        var values = new JsonArray();
        values.add(optionalTagRef("c:ingots/" + mat.id));
        values.add(optionalTagRef("c:dusts/" + mat.id));
        j.add("values", values);
        return j;
    }

    private static JsonObject optionalTagRef(String tag) {
        var o = new JsonObject();
        o.addProperty("id", "#" + tag);
        o.addProperty("required", false);
        return o;
    }

    private CompletableFuture<?> save(CachedOutput cache, Path path, JsonObject json) {
        path.getParent().toFile().mkdirs();
        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() {
        return "Material Aggregate Tags";
    }
}
