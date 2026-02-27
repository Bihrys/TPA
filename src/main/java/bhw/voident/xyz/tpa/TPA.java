package bhw.voident.xyz.tpa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.text.Text;

import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.BlockPos;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TPA implements ModInitializer {

    private static final Map<UUID, UUID> requests = new HashMap<>();
    private static final Map<UUID, BlockPos> frozen = new HashMap<>();
    private static final Map<UUID, Integer> countdown = new HashMap<>();
    private static final Map<UUID, BlockPos> targetPos = new HashMap<>();
    private static final Set<UUID> toggleOff = new HashSet<>();

    // 家庭功能
    private static final Map<UUID, Map<String, BlockPos>> homes = new HashMap<>();
    // 死亡回点
    private static final Map<UUID, BlockPos> deathBack = new HashMap<>();

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ---------------------- TPA ----------------------
            dispatcher.register(literal("tpa").then(argument("player", EntityArgumentType.player()).executes(c -> {
                ServerPlayerEntity sender = c.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(c, "player");
                if (sender == null || target == null) return 1;

                if (sender.getUuid().equals(target.getUuid())) {
                    sender.sendMessage(Text.literal("你不能向自己传送"));
                    return 1;
                }

                if (toggleOff.contains(target.getUuid())) {
                    sender.sendMessage(Text.literal("对方关闭了 TPA"));
                    return 1;
                }

                requests.put(sender.getUuid(), target.getUuid());

                sender.sendMessage(Text.literal("已向 " + target.getName().getString() + " 发送请求"));

                MutableText accept = Text.literal("[ 接受 ]")
                        .formatted(Formatting.GREEN)
                        .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaaccept")));

                MutableText deny = Text.literal("[ 拒绝 ]")
                        .formatted(Formatting.RED)
                        .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpadeny")));

                target.sendMessage(Text.literal(sender.getName().getString() + " 想传送到你"));
                target.sendMessage(accept.append(Text.literal(" ")).append(deny));

                return 1;
            })));

            dispatcher.register(literal("tpaaccept").executes(c -> {
                ServerPlayerEntity b = c.getSource().getPlayer();
                if (b == null) return 1;

                UUID from = requests.entrySet().stream()
                        .filter(e -> e.getValue().equals(b.getUuid()))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);

                if (from == null) {
                    b.sendMessage(Text.literal("没有请求"));
                    return 1;
                }

                ServerPlayerEntity a = b.getServer().getPlayerManager().getPlayer(from);
                if (a == null) return 1;

                targetPos.put(from, b.getBlockPos()); // 锁定接受瞬间坐标

                a.sendMessage(Text.literal(b.getName().getString() + " 接受了你的请求"));
                b.sendMessage(Text.literal("已接受"));

                startTeleport(a);
                return 1;
            }));

            dispatcher.register(literal("tpadeny").executes(c -> {
                ServerPlayerEntity b = c.getSource().getPlayer();
                if (b == null) return 1;

                requests.entrySet().removeIf(e -> {
                    if (e.getValue().equals(b.getUuid())) {
                        ServerPlayerEntity a = b.getServer().getPlayerManager().getPlayer(e.getKey());
                        if (a != null) a.sendMessage(Text.literal("对方拒绝了你的请求"));
                        return true;
                    }
                    return false;
                });

                b.sendMessage(Text.literal("已拒绝"));
                return 1;
            }));

            // ---------------------- Home 功能 ----------------------
            dispatcher.register(literal("sethome").then(argument("name", StringArgumentType.word()).executes(c -> {
                ServerPlayerEntity player = c.getSource().getPlayer();
                if (player == null) return 1;

                String name = StringArgumentType.getString(c, "name");

                homes.putIfAbsent(player.getUuid(), new HashMap<>());
                Map<String, BlockPos> playerHomes = homes.get(player.getUuid());

                if (playerHomes.containsKey(name)) {
                    player.sendMessage(Text.literal("家已经存在: " + name).formatted(Formatting.RED));
                    return 1;
                }

                playerHomes.put(name, player.getBlockPos());
                player.sendMessage(Text.literal("已创建家: " + name).formatted(Formatting.GREEN));
                return 1;
            })));

            dispatcher.register(literal("delhome").then(argument("name", StringArgumentType.word()).executes(c -> {
                ServerPlayerEntity player = c.getSource().getPlayer();
                if (player == null) return 1;

                String name = StringArgumentType.getString(c, "name");
                Map<String, BlockPos> playerHomes = homes.get(player.getUuid());
                if (playerHomes == null || !playerHomes.containsKey(name)) {
                    player.sendMessage(Text.literal("没有找到家: " + name).formatted(Formatting.RED));
                    return 1;
                }

                playerHomes.remove(name);
                player.sendMessage(Text.literal("已删除家: " + name).formatted(Formatting.GREEN));
                return 1;
            })));

            dispatcher.register(literal("home").then(argument("name", StringArgumentType.word()).suggests((c, builder) -> {
                ServerPlayerEntity player = c.getSource().getPlayer();
                if (player == null) return builder.buildFuture();

                Map<String, BlockPos> playerHomes = homes.get(player.getUuid());
                if (playerHomes != null) {
                    for (String name : playerHomes.keySet()) builder.suggest(name);
                }
                return builder.buildFuture();
            }).executes(c -> {
                ServerPlayerEntity player = c.getSource().getPlayer();
                if (player == null) return 1;

                String name = StringArgumentType.getString(c, "name");
                Map<String, BlockPos> playerHomes = homes.get(player.getUuid());

                if (playerHomes == null || !playerHomes.containsKey(name)) {
                    player.sendMessage(Text.literal("没有找到家: " + name).formatted(Formatting.RED));
                    return 1;
                }

                targetPos.put(player.getUuid(), playerHomes.get(name));
                startTeleport(player);
                return 1;
            })));

            // ---------------------- Death / Back ----------------------
            // 玩家死亡事件监听，用于设置 deathBack
            // 这里示例用 Fabric 内置事件，需要你在外部注册 PlayerDeathCallback 之类
            // 假设已经监听了死亡，并调用 recordDeath(player)
            dispatcher.register(literal("back").executes(c -> {
                ServerPlayerEntity player = c.getSource().getPlayer();
                if (player == null) return 1;

                BlockPos pos = deathBack.get(player.getUuid());
                if (pos == null) {
                    player.sendMessage(Text.literal("没有死亡回点").formatted(Formatting.RED));
                    return 1;
                }

                targetPos.put(player.getUuid(), pos);
                startTeleport(player);
                return 1;
            }));

        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    // 玩家死亡时调用
    public static void recordDeath(ServerPlayerEntity player) {
        if (player == null) return;
        deathBack.put(player.getUuid(), player.getBlockPos());
        player.sendMessage(Text.literal("你死亡了，可使用 /back 回到死亡地点").formatted(Formatting.RED));
    }

    private void startTeleport(ServerPlayerEntity p) {
        frozen.put(p.getUuid(), p.getBlockPos());
        countdown.put(p.getUuid(), 100); // 5秒 = 100tick
        p.sendMessage(Text.literal("正在传送 5").formatted(Formatting.GREEN), true);
    }

    private void tick(MinecraftServer s) {
        Iterator<UUID> it = countdown.keySet().iterator();

        while (it.hasNext()) {
            UUID u = it.next();
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(u);
            if (p == null) {
                it.remove();
                frozen.remove(u);
                targetPos.remove(u);
                continue;
            }

            BlockPos f = frozen.get(u);
            if (!p.getBlockPos().equals(f)) {
                p.sendMessage(Text.literal("你移动了，传送已取消").formatted(Formatting.RED), true);
                p.playSound(SoundEvents.ENTITY_ITEM_BREAK, 2f, 1f);

                it.remove();
                frozen.remove(u);
                targetPos.remove(u);
                continue;
            }

            int t = countdown.get(u) - 1;
            countdown.put(u, t);

            if (t % 20 == 0) {
                p.sendMessage(Text.literal("正在传送 " + t / 20).formatted(Formatting.GREEN), true);
                p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1, 1);
            }

            if (t <= 0) {
                BlockPos tp = targetPos.get(u);
                if (tp != null) {
                    p.teleport(p.getServerWorld(), tp.getX(), tp.getY(), tp.getZ(), p.getYaw(), p.getPitch());
                }

                it.remove();
                frozen.remove(u);
                targetPos.remove(u);
            }
        }
    }
}
