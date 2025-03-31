package com.lowdragmc.lowdraglib.utils;

import com.google.common.base.Charsets;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.util.Optional;

/**
 * @author KilaBash
 * @date 2022/05/13
 * @implNote CustomResourcePack
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomResourcePack extends PathPackResources {
    private final PackType type;
    private final String namespace;

    public CustomResourcePack(File location, String namespace, PackType type) {
        super(new PackLocationInfo(namespace, Component.literal(namespace), PackSource.DEFAULT, Optional.empty()), location.toPath());
        new File(location, namespace).mkdirs();
        this.namespace = namespace;
        this.type = type;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pathSegments) {
        String fileName = String.join("/", pathSegments);
        if ("pack.mcmeta".equals(fileName)) {
            String description = "Generated resources for " + namespace;
            String fallback = "Mod resources.";
            String pack = String.format("{\"pack\":{\"pack_format\":" + SharedConstants.getCurrentVersion().getPackVersion(type) + ",\"description\":{\"translate\":\"%s\",\"fallback\":\"%s.\"}}}", description, fallback);
            return () -> IOUtils.toInputStream(pack, Charsets.UTF_8);
        }
        return super.getRootResource(pathSegments);
    }

}
