package flavor.pie.consensus;

import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.ban.Ban;
import org.spongepowered.api.util.ban.BanTypes;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public class Commands {

    @Inject
    private Consensus plugin;
    @Inject
    private Game game;

    void registerCommands() {
        game.getCommandManager().getOwnedBy(this).forEach(game.getCommandManager()::removeMapping);
        CommandSpec.Builder poll = CommandSpec.builder();

        if (plugin.config.enabledModes.contains(Config.Mode.BAN)) {
            CommandSpec ban = CommandSpec.builder()
                    .executor(this::ban)
                    .arguments(
                            GenericArguments.player(Text.of("player")),
                            GenericArguments.optionalWeak(GenericArguments.duration(Text.of("duration"))),
                            GenericArguments.text(Text.of("reason"), TextSerializers.FORMATTING_CODE, true)
                    )
                    .build();
            poll.child(ban, "ban");
        }

        if (plugin.config.enabledModes.contains(Config.Mode.MUTE)) {
            CommandSpec mute = CommandSpec.builder()
                    .executor(this::mute)
                    .arguments(
                            GenericArguments.player(Text.of("player")),
                            GenericArguments.optionalWeak(GenericArguments.duration(Text.of("duration"))),
                            GenericArguments.text(Text.of("reason"), TextSerializers.FORMATTING_CODE, true)
                    )
                    .build();
            poll.child(mute, "mute");
        }

        if (plugin.config.enabledModes.contains(Config.Mode.KICK)) {
            CommandSpec kick = CommandSpec.builder()
                    .executor(this::kick)
                    .arguments(
                            GenericArguments.player(Text.of("player")),
                            GenericArguments.text(Text.of("reason"), TextSerializers.FORMATTING_CODE, true)
                    )
                    .build();
            poll.child(kick, "kick");
        }

        if (plugin.config.enabledModes.contains(Config.Mode.TIME)) {
            CommandSpec time = CommandSpec.builder()
                    .executor(this::time)
                    .arguments(
                            GenericArguments.integer(Text.of("time")),
                            GenericArguments.optional(GenericArguments.world(Text.of("world")))
                    )
                    .build();
            poll.child(time, "time");
        }

        if (plugin.config.enabledModes.contains(Config.Mode.COMMAND)) {
            CommandSpec command = CommandSpec.builder()
                    .executor(this::command)
                    .arguments(
                            GenericArguments.remainingJoinedStrings(Text.of("command"))
                    )
                    .build();
            poll.child(command, "command");
        }

        CommandSpec dummy = CommandSpec.builder()
                .executor(this::dummy)
                .arguments(
                        GenericArguments.text(Text.of("text"), TextSerializers.FORMATTING_CODE, true),
                        GenericArguments.optionalWeak(GenericArguments.doubleNum(Text.of("majority")), 0.5),
                        GenericArguments.optional(GenericArguments.duration(Text.of("duration")))
                ).build();

        poll.child(dummy, "dummy");
        game.getCommandManager().register(this, poll.build(), "poll");
    }

    public CommandResult ban(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        Duration duration = args.<Duration>getOne("duration").orElse(plugin.config.ban.maxDuration);
        if (duration.compareTo(plugin.config.ban.maxDuration) > 0 && (plugin.config.ban.override == null || !src.hasPermission(plugin.config.ban.override))) {
            throw new CommandException(Text.of("The maximum duration that players can be votebanned for is ", plugin.durationToString(plugin.config.ban.maxDuration)));
        }
        if (plugin.config.ban.exempt != null && p.hasPermission(plugin.config.ban.exempt) && (plugin.config.ban.override == null || !src.hasPermission(plugin.config.ban.override))) {
            throw new CommandException(Text.of("This person cannot be banned!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.ban.minPlayers != 0 && size < plugin.config.ban.minPlayers) {
            throw new CommandException(Text.of("Cannot voteban; not enough players online (required: ", plugin.config.ban.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.RED, "ban ", p.getName(), " for ", reason, " for ", plugin.durationToString(duration)), i -> {
            if (plugin.config.ban.majority * (double) size <= i) {
                game.getServiceManager().provideUnchecked(BanService.class).addBan(Ban.builder()
                        .type(BanTypes.PROFILE)
                        .profile(p.getProfile())
                        .expirationDate(Instant.now().plus(duration))
                        .reason(reason)
                        .source(Text.of("Majority vote"))
                        .startDate(Instant.now())
                        .build());
                p.kick(Text.of("Banned by majority vote for ", reason, " for ", plugin.durationToString(duration)));
                return true;
            } else {
                return false;
            }
        }, plugin.config.time.duration);
        return CommandResult.success();
    }

    public CommandResult mute(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        Duration duration = args.<Duration>getOne("duration").orElse(plugin.config.mute.maxDuration);
        if (duration.compareTo(plugin.config.mute.maxDuration) > 0 && (plugin.config.mute.override == null || !src.hasPermission(plugin.config.mute.override))) {
            throw new CommandException(Text.of("The maximum duration that players can be votemuted for is ", plugin.durationToString(plugin.config.mute.maxDuration)));
        }
        if (plugin.config.mute.exempt != null && p.hasPermission(plugin.config.mute.exempt) && (plugin.config.mute.override == null || !src.hasPermission(plugin.config.mute.override))) {
            throw new CommandException(Text.of("This person cannot be banned!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.mute.minPlayers != 0 && size < plugin.config.mute.minPlayers) {
            throw new CommandException(Text.of("Cannot votemute; not enough players online (required: ", plugin.config.mute.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.YELLOW, "mute ", p.getName(), " for ", reason, " for ", plugin.durationToString(duration)), i -> {
            if (plugin.config.mute.majority * (double) size <= i) {
                plugin.mutes.put(p.getUniqueId(), Instant.now().plus(duration));
                return true;
            } else {
                return false;
            }
        }, plugin.config.time.duration);
        return CommandResult.success();
    }

    public CommandResult kick(CommandSource src, CommandContext args) throws CommandException {
        Player p = args.<Player>getOne("player").get();
        Text reason = args.<Text>getOne("reason").get();
        if (plugin.config.kick.exempt != null && p.hasPermission(plugin.config.kick.exempt) && (plugin.config.kick.override == null || !src.hasPermission(plugin.config.kick.override))) {
            throw new CommandException(Text.of("This person cannot be kicked!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.kick.minPlayers != 0 && size < plugin.config.kick.minPlayers) {
            throw new CommandException(Text.of("Cannot votekick; not enough players online (required: ", plugin.config.kick.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.GOLD, "kick ", p.getName(), " for ", reason), i -> {
            if (plugin.config.mute.majority * (double) size <= i) {
                p.kick(Text.of("Kicked by majority vote for ", reason));
                return true;
            } else {
                return false;
            }
        }, plugin.config.time.duration);
        return CommandResult.success();
    }

    public CommandResult time(CommandSource src, CommandContext args) throws CommandException {
        int time = args.<Integer>getOne("time").get();
        if (time < 0 || time > 24_000) {
            throw new CommandException(Text.of(time, " is not a valid time of day!"));
        }
        Optional<WorldProperties> world_ =  args.getOne("world");
        WorldProperties world;
        if (world_.isPresent()) {
            world = world_.get();
        } else {
            if (src instanceof Player) {
                world = ((Player) src).getWorld().getProperties();
            } else {
                throw new CommandException(Text.of("You must specify a world!"));
            }
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (plugin.config.time.minPlayers != 0 && size < plugin.config.time.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to change the time; not enough players online (required: ", plugin.config.time.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.AQUA, "change the time to ", time, " in world ", world.getWorldName()), i -> {
            if (plugin.config.time.majority * (double) size <= i) {
                long worldTime = world.getWorldTime();
                int currentTime = (int) (worldTime % 24_000);
                int timeToAdd;
                if (currentTime < time) {
                    timeToAdd = time - currentTime;
                } else {
                    timeToAdd = 24_000 - (currentTime - time);
                }
                world.setWorldTime(worldTime + timeToAdd);
                return true;
            } else {
                return false;
            }
        }, plugin.config.time.duration);
        return CommandResult.success();
    }

    public CommandResult command(CommandSource src, CommandContext args) throws CommandException {
        String command = args.<String>getOne("command").get();
        if ((plugin.config.command.override == null || !src.hasPermission(plugin.config.command.override))
                && plugin.config.command.allowedCommands.stream().noneMatch(command::startsWith)) {
            throw new CommandException(Text.of("This command cannot be used!"));
        }
        int size = game.getServer().getOnlinePlayers().size();
        if (size < plugin.config.command.minPlayers) {
            throw new CommandException(Text.of("Cannot vote to run a command; not enough players online (required: ", plugin.config.command.minPlayers, ")!"));
        }
        plugin.startBooleanVote(src, Text.of(TextColors.WHITE, "run the command ", command), i -> {
            if (plugin.config.command.majority * (double) size <= i) {
                game.getCommandManager().process(game.getServer().getConsole(), command);
                return true;
            } else {
                return false;
            }
        }, plugin.config.command.duration);
        return CommandResult.success();
    }

    public CommandResult dummy(CommandSource src, CommandContext args) throws CommandException {
        Text text = args.<Text>getOne("text").get();
        Duration duration = args.<Duration>getOne("duration").orElse(Duration.of(1, ChronoUnit.MINUTES));
        double majority = args.<Double>getOne("majority").get();
        int size = game.getServer().getOnlinePlayers().size();
        plugin.startBooleanVote(src, text, i -> majority * (double) size <= i, duration);
        return CommandResult.success();
    }

}
