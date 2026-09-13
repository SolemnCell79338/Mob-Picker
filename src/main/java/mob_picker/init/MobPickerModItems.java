
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package mob_picker.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.Item;

import mob_picker.item.MobPickerItem;

import mob_picker.MobPickerMod;

public class MobPickerModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, MobPickerMod.MODID);
	public static final RegistryObject<Item> MOB_PICKER = REGISTRY.register("mob_picker", () -> new MobPickerItem());
}
