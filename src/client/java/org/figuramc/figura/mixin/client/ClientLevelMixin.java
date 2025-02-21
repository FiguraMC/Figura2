package org.figuramc.figura.mixin.client;

import com.google.gson.JsonObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.avatars.AvatarTemplate;
import org.figuramc.figura.data.AvatarMaterials;
import org.figuramc.figura.data.NewAvatarImporter;
import org.figuramc.figura.directory.FiguraDir;
import org.figuramc.figura.manage.AvatarManager;
import org.figuramc.figura.manage.CemManager;
import org.figuramc.figura.util.ClientUtils;
import org.figuramc.figura.util.exception.ExceptionUtils;
import org.figuramc.figura.vanillamodel.EntityExporter;
import org.figuramc.figura.vanillamodel.ModelPartAlias;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;

@SuppressWarnings("rawtypes")
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void resetLoaded(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, Holder holder, int i, int j, LevelRenderer levelRenderer, boolean bl, long l, int k, CallbackInfo ci) {
        FiguraModClient.LOADED_TEST_AVATAR = false;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void testing(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        // TESTING CODE, AVATAR LOADING
        if (!FiguraModClient.LOADED_TEST_AVATAR) {

            AvatarManager.ENTITY_AVATARS.clear();
            CemManager.clear();

            AvatarManager.ENTITY_AVATARS.load(ClientUtils.getLocalUUID(), CompletableFuture.supplyAsync(ExceptionUtils.wrapChecked(() -> {
                Path avatarPath = FiguraDir.AVATARS.get().resolve("test_avatar");
                AvatarMaterials materials = NewAvatarImporter.importPath(avatarPath);
                return AvatarTemplate.LOCAL_PLAYER_AVATAR.construct(ClientUtils.getLocalUUID(), materials);
            }, CompletionException::new)));

            FiguraModClient.LOADED_TEST_AVATAR = true;

            // Also export some stuff for testing
            try {
                Path exportsFolder = FiguraDir.EXPORTS.get();
                JsonObject playerExport = EntityExporter.exportPlayer(PlayerSkin.Model.SLIM, new EntityExporter.ExporterOptions(ModelPartAlias.Group.ELYTRA, ModelPartAlias.Group.STUCK_ARROW));
                Files.writeString(exportsFolder.resolve("slim_player.figmodel"), playerExport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                JsonObject foxExport = EntityExporter.exportEntity(EntityType.FOX, new EntityExporter.ExporterOptions());
                Files.writeString(exportsFolder.resolve("fox.figmodel"), foxExport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                JsonObject endCrystalExport = EntityExporter.exportEntity(EntityType.END_CRYSTAL, EntityExporter.LITERALLY_ALL_PARTS);
                Files.writeString(exportsFolder.resolve("end_crystal.figmodel"), endCrystalExport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                JsonObject enderDragonExport = EntityExporter.exportEntity(EntityType.ENDER_DRAGON, EntityExporter.LITERALLY_ALL_PARTS);
                Files.writeString(exportsFolder.resolve("ender_dragon.figmodel"), enderDragonExport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                JsonObject arrowExport = EntityExporter.exportEntity(EntityType.ARROW, EntityExporter.LITERALLY_ALL_PARTS);
                Files.writeString(exportsFolder.resolve("arrow.figmodel"), arrowExport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                FiguraMod.LOGGER.error("Failed to store file", e);
            }

        }
    }

}
