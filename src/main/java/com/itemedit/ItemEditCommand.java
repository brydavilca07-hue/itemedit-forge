package com.itemedit;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * /itemedit: edita el objeto que tienes en la mano.
 *
 *   /itemedit nombre <texto>                    (usa & para colores)
 *   /itemedit lore agregar <texto>
 *   /itemedit lore borrar
 *   /itemedit enchanted <encantamiento> <nivel>  (nivel 0 = quitarlo, maximo 32767)
 *   /itemedit irrompible                        (activa/desactiva)
 *   /itemedit reparar
 *   /itemedit atributo <danio|armadura|velocidad|vida> <valor>
 *   /itemedit atributo borrar
 */
@Mod.EventBusSubscriber(modid = ItemEditMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ItemEditCommand {

    private static final SimpleCommandExceptionType NO_ITEM =
            new SimpleCommandExceptionType(Component.literal("Debes tener un objeto en la mano."));

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("itemedit")
                .requires(src -> src.hasPermission(2)); // operadores / trucos activados

        // nombre
        root.then(Commands.literal("nombre")
                .then(Commands.argument("texto", StringArgumentType.greedyString())
                        .executes(ItemEditCommand::setName)));

        // lore
        root.then(Commands.literal("lore")
                .then(Commands.literal("agregar")
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(ItemEditCommand::addLore)))
                .then(Commands.literal("borrar").executes(ItemEditCommand::clearLore)));

        // encantar con nivel libre
        root.then(Commands.literal("enchanted")
                .then(Commands.argument("encantamiento",
                                ResourceArgument.resource(event.getBuildContext(), Registries.ENCHANTMENT))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(0, 32767))
                                .executes(ItemEditCommand::enchant))));

        // irrompible y reparar
        root.then(Commands.literal("irrompible").executes(ItemEditCommand::toggleUnbreakable));
        root.then(Commands.literal("reparar").executes(ItemEditCommand::repair));

        // atributos
        LiteralArgumentBuilder<CommandSourceStack> attr = Commands.literal("atributo");
        attr.then(attributeNode("danio", Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION));
        attr.then(attributeNode("armadura", Attributes.ARMOR, AttributeModifier.Operation.ADDITION));
        attr.then(attributeNode("vida", Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION));
        attr.then(attributeNode("velocidad", Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_BASE));
        attr.then(Commands.literal("borrar").executes(ItemEditCommand::clearAttributes));
        root.then(attr);

        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attributeNode(
            String name, Attribute attribute, AttributeModifier.Operation op) {
        return Commands.literal(name)
                .then(Commands.argument("valor", DoubleArgumentType.doubleArg(-100000.0, 100000.0))
                        .executes(ctx -> setAttribute(ctx, name, attribute, op)));
    }

    // ------------------------------------------------------------------

    private static ItemStack held(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            throw NO_ITEM.create();
        }
        return stack;
    }

    private static void ok(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> message, false);
    }

    private static int setName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        String text = StringArgumentType.getString(ctx, "texto").replace('&', '§');
        stack.setHoverName(Component.literal(text));
        ok(ctx, Component.literal("Nombre cambiado a: " + text));
        return 1;
    }

    private static int addLore(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        String text = StringArgumentType.getString(ctx, "texto").replace('&', '§');
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = display.getList("Lore", Tag.TAG_STRING);
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(text))));
        display.put("Lore", lore);
        ok(ctx, Component.literal("Linea agregada: " + text));
        return 1;
    }

    private static int clearLore(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        CompoundTag display = stack.getTagElement("display");
        if (display != null) {
            display.remove("Lore");
        }
        ok(ctx, Component.literal("Descripcion borrada."));
        return 1;
    }

    private static int enchant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        Holder.Reference<Enchantment> holder = ResourceArgument.getEnchantment(ctx, "encantamiento");
        Enchantment enchantment = holder.value();
        int level = IntegerArgumentType.getInteger(ctx, "nivel");

        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
        if (level <= 0) {
            map.remove(enchantment);
        } else {
            map.put(enchantment, level);
        }
        EnchantmentHelper.setEnchantments(map, stack);

        if (level <= 0) {
            ok(ctx, Component.literal("Encantamiento quitado."));
        } else {
            ok(ctx, Component.literal("Encantado: ").append(enchantment.getFullname(level)));
        }
        return level;
    }

    private static int toggleUnbreakable(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        CompoundTag tag = stack.getOrCreateTag();
        boolean now = !tag.getBoolean("Unbreakable");
        tag.putBoolean("Unbreakable", now);
        ok(ctx, Component.literal(now ? "El objeto ahora es irrompible." : "El objeto ya no es irrompible."));
        return 1;
    }

    private static int repair(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        stack.setDamageValue(0);
        ok(ctx, Component.literal("Objeto reparado."));
        return 1;
    }

    private static int setAttribute(CommandContext<CommandSourceStack> ctx, String name,
                                    Attribute attribute, AttributeModifier.Operation op)
            throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        double value = DoubleArgumentType.getDouble(ctx, "valor");
        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "itemedit_" + name, value, op);
        stack.addAttributeModifier(attribute, modifier, Mob.getEquipmentSlotForItem(stack));
        ok(ctx, Component.literal("Atributo " + name + ": " + value + " agregado."));
        return 1;
    }

    private static int clearAttributes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = held(ctx);
        stack.getOrCreateTag().remove("AttributeModifiers");
        ok(ctx, Component.literal("Atributos personalizados borrados."));
        return 1;
    }
}
