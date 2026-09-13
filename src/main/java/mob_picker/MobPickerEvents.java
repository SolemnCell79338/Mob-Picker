package mob_picker.event;

import mob_picker.item.MobPickerItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobPickerEvents {
    private static final Map<UUID, GameType> PREVIOUS_GAMEMODES = new HashMap<>();
    private static final Map<UUID, FrozenData> FROZEN_PLAYERS = new HashMap<>();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        if (stack.getItem() instanceof MobPickerItem && event.getHand() == InteractionHand.MAIN_HAND) {
            CompoundTag tag = stack.getOrCreateTag();
            if (tag.contains("CapturedMobID")) {
                return;
            }
            if (target instanceof EnderDragon) {
                return;
            }
            if (target instanceof LivingEntity livingTarget) {
                tag.putString("CapturedMobName", livingTarget.getName().getString());
                tag.putDouble("CapturedMobHealth", (double) livingTarget.getHealth());
                tag.putDouble("CapturedMobMaxHealth", (double) livingTarget.getMaxHealth());
                
                if (livingTarget instanceof ServerPlayer targetPlayer) {
                    UUID playerUUID = targetPlayer.getUUID();
                    tag.putUUID("CapturedMobID", playerUUID);
                    tag.putBoolean("IsPlayer", true);
                    
                    String gmName = targetPlayer.gameMode.getGameModeForPlayer().getName();
                    String formattedGm = gmName.substring(0, 1).toUpperCase() + gmName.substring(1);
                    tag.putString("CapturedPlayerGamemode", formattedGm);
                    
                    PREVIOUS_GAMEMODES.put(playerUUID, targetPlayer.gameMode.getGameModeForPlayer());
                    double freezeY = targetPlayer.getY() + 200.0;
                    FROZEN_PLAYERS.put(playerUUID, new FrozenData(freezeY, targetPlayer.level().dimension()));
                    
                    targetPlayer.teleportTo(targetPlayer.getX(), freezeY, targetPlayer.getZ());
                    targetPlayer.setGameMode(GameType.SPECTATOR);
                    targetPlayer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 99999, 0, false, false));
                } else {
                    UUID mobUUID = UUID.randomUUID();
                    tag.putUUID("CapturedMobID", mobUUID);
                    tag.putBoolean("IsPlayer", false);
                    CompoundTag entityData = new CompoundTag();
                    livingTarget.save(entityData);
                    tag.put("CapturedMobData", entityData);
                    tag.putString("CapturedMobType", EntityType.getKey(livingTarget.getType()).toString());
                    livingTarget.discard();
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof MobPickerItem && event.getHand() == InteractionHand.MAIN_HAND) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("CapturedMobID")) {
                return;
            }
            UUID mobUUID = tag.getUUID("CapturedMobID");
            boolean isPlayer = tag.getBoolean("IsPlayer");
            BlockPos spawnPos = event.getPos().relative(event.getFace());
            
            if (isPlayer) {
                ServerPlayer targetPlayer = ((ServerLevel) level).getServer().getPlayerList().getPlayer(mobUUID);
                if (targetPlayer != null) {
                    targetPlayer.teleportTo((double) spawnPos.getX() + 0.5, (double) spawnPos.getY(), (double) spawnPos.getZ() + 0.5);
                    GameType oldMode = PREVIOUS_GAMEMODES.getOrDefault(mobUUID, GameType.SURVIVAL);
                    targetPlayer.setGameMode(oldMode);
                    targetPlayer.removeEffect(MobEffects.INVISIBILITY);
                    PREVIOUS_GAMEMODES.remove(mobUUID);
                    FROZEN_PLAYERS.remove(mobUUID);
                }
            } else {
                String typeStr = tag.getString("CapturedMobType");
                CompoundTag entityData = tag.getCompound("CapturedMobData");
                EntityType.byString(typeStr).ifPresent(type -> {
                    Entity entity = type.create(level);
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.load(entityData);
                        livingEntity.setUUID(UUID.randomUUID());
                        livingEntity.moveTo((double) spawnPos.getX() + 0.5, (double) spawnPos.getY(), (double) spawnPos.getZ() + 0.5, 0.0f, 0.0f);
                        level.addFreshEntity(livingEntity);
                    }
                });
            }
            stack.setTag(null);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            FrozenData data = FROZEN_PLAYERS.get(playerUUID);
            if (data != null && player.level().dimension().equals(data.dimension())) {
                if (Math.abs(player.getY() - data.y()) > 0.1) {
                    player.teleportTo(player.getX(), data.y(), player.getZ());
                }
                if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    player.setGameMode(GameType.SPECTATOR);
                }
            }
        }
    }

    private record FrozenData(double y, ResourceKey<Level> dimension) {
    }
}