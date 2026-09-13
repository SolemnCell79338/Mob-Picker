package mob_picker.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class MobPickerItem extends Item {
    public MobPickerItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level world = player.level();
        if (!world.isClientSide()) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("CapturedMobName", target.getName().getString());
            tag.putDouble("CapturedMobHealth", (double) target.getHealth());
            tag.putDouble("CapturedMobMaxHealth", (double) target.getMaxHealth());
            
            if (target instanceof Player p) {
                String mode = p.getAbilities().instabuild ? "Creative" : (p.isSpectator() ? "Spectator" : (p.isCreative() ? "Creative" : "Survival"));
                tag.putString("CapturedGameMode", mode);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CapturedMobName")) {
            return Component.literal("Mob Picker ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("(" + tag.getString("CapturedMobName") + ")").withStyle(ChatFormatting.GRAY));
        }
        return Component.literal("Mob Picker").withStyle(ChatFormatting.WHITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CapturedMobName")) {
            list.add(Component.literal("Health: ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(String.format("%.1f / %.1f", tag.getDouble("CapturedMobHealth"), tag.getDouble("CapturedMobMaxHealth"))).withStyle(ChatFormatting.GRAY)));
            if (tag.contains("CapturedGameMode")) {
                list.add(Component.literal("Gamemode: ").withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(tag.getString("CapturedGameMode")).withStyle(ChatFormatting.GRAY)));
            }
        }
        super.appendHoverText(stack, world, list, flag);
    }
}