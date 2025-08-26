package org.minefortress.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ArchersCommand extends MineFortressCommand {

    /*
    // fortress archers spawn <num> <blockPos>
     */
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("fortress")
                .then(literal("archers")
                    .then(literal("spawn")
                        .then(argument("num", IntegerArgumentType.integer())
                            .executes(context -> {
                                final var num = IntegerArgumentType.getInteger(context, "num");
                                final var fightManager = getServerManagersProvider(context).getFightManager();
                                fightManager.spawnDebugArchers(num, context.getSource().getPlayer());

                                return 1;
                            })
                        )
                    )
                )
        );
    }
}
