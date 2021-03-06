package flavor.pie.consensus;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class Config {

    public static final TypeToken<Config> type = TypeToken.of(Config.class);

    @Setting public BanModule ban = new BanModule();
    @Setting public KickModule kick = new KickModule();
    @Setting public MuteModule mute = new MuteModule();
    @Setting public TimeModule time = new TimeModule();
    @Setting public CommandModule command = new CommandModule();
    @Setting public TriggerHolder triggers = new TriggerHolder();
    @Setting public WeatherModule weather = new WeatherModule();

    @ConfigSerializable
    public static class BanModule extends PollModule {
        @Setting("max-duration") public Duration maxDuration = Duration.of(1, ChronoUnit.DAYS);
    }

    @ConfigSerializable
    public static class KickModule extends PollModule { }

    @ConfigSerializable
    public static class MuteModule extends PollModule {
        @Setting("max-duration") public Duration maxDuration = Duration.of(1, ChronoUnit.HOURS);
    }

    @ConfigSerializable
    public static class TimeModule extends PollModule { }

    @ConfigSerializable
    public static class CommandModule extends PollModule {
        @Setting("allowed-commands") public List<String> allowedCommands = Collections.emptyList();
    }

    @ConfigSerializable
    public static class WeatherModule extends PollModule { }

    public static abstract class PollModule extends Module {
        @Setting public double majority = 0.5;
        @Setting("min-players") public int minPlayers = 10;
        @Setting public Duration duration = Duration.of(1, ChronoUnit.MINUTES);
    }

    public static abstract class Module {
        @Setting public boolean enabled = false;
    }

    @ConfigSerializable
    public static class TriggerHolder {
        @Setting public TimeTriggerModule time = new TimeTriggerModule();
        @Setting public WeatherTriggerModule weather = new WeatherTriggerModule();
    }

    @ConfigSerializable
    public static class TimeTriggerModule extends Module {
        @Setting public ListType type = ListType.BLACKLIST;
        @Setting public List<String> worlds = Collections.emptyList();
    }

    @ConfigSerializable
    public static class WeatherTriggerModule extends Module {
        @Setting public ListType type = ListType.BLACKLIST;
        @Setting public List<String> worlds = Collections.emptyList();
    }

    public enum ListType {
        @SuppressWarnings("unused") WHITELIST, BLACKLIST
    }

}
