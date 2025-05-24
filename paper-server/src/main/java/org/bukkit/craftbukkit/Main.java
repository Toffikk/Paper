package org.bukkit.craftbukkit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.util.PathConverter;
import io.papermc.paper.ServerBuildInfo;
import com.destroystokyo.paper.PaperVersionFetcher;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.SharedConstants;

public class Main {
    public static final java.time.Instant BOOT_TIME = java.time.Instant.now(); // Paper - track initial start time
    public static boolean useJline = true;
    public static boolean useConsole = true;

    // Paper start - Reset loggers after shutdown
    static {
        System.setProperty("java.util.logging.manager", "io.papermc.paper.log.CustomLogManager");
    }
    // Paper end - Reset loggers after shutdown

    public static void main(String[] args) {
        // Todo: Installation script
        if (System.getProperty("jdk.nio.maxCachedBufferSize") == null) System.setProperty("jdk.nio.maxCachedBufferSize", "262144"); // Paper - cap per-thread NIO cache size; https://www.evanjones.ca/java-bytebuffer-leak.html
        OptionParser parser = new OptionParser() {
            {
                this.acceptsAll(Main.asList("?", "help"), "Show the help");

                this.acceptsAll(Main.asList("c", "config"), "Properties file to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("server.properties"))
                        .describedAs("Properties file");

                this.acceptsAll(Main.asList("P", "plugins"), "Plugin directory to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("plugins"))
                        .describedAs("Plugin directory");

                this.acceptsAll(Main.asList("h", "host", "server-ip"), "Host to listen on")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("Hostname or IP");

                this.acceptsAll(Main.asList("W", "world-dir", "universe", "world-container"), "World container")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("."))
                        .describedAs("Directory containing worlds");

                this.acceptsAll(Main.asList("w", "world", "level-name"), "World name")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("World name");

                this.acceptsAll(Main.asList("p", "port", "server-port"), "Port to listen on")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Port");

                this.accepts("serverId", "Server ID")
                        .withRequiredArg();

                this.accepts("jfrProfile", "Enable JFR profiling");

                this.accepts("pidFile", "pid File")
                        .withRequiredArg()
                        .withValuesConvertedBy(new PathConverter());

                this.acceptsAll(Main.asList("o", "online-mode"), "Whether to use online authentication")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .describedAs("Authentication");

                this.acceptsAll(Main.asList("s", "size", "max-players"), "Maximum amount of players")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Server size");

                this.acceptsAll(Main.asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg()
                        .ofType(SimpleDateFormat.class)
                        .describedAs("Log date format");

                this.acceptsAll(Main.asList("log-pattern"), "Specfies the log filename pattern")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("server.log")
                        .describedAs("Log filename");

                this.acceptsAll(Main.asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(0)
                        .describedAs("Max log size");

                this.acceptsAll(Main.asList("log-count"), "Specified how many log files to cycle through")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(1)
                        .describedAs("Log count");

                this.acceptsAll(Main.asList("log-append"), "Whether to append to the log file")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .defaultsTo(true)
                        .describedAs("Log append");

                this.acceptsAll(Main.asList("log-strip-color"), "Strips color codes from log file");

                this.acceptsAll(Main.asList("b", "bukkit-settings"), "File for bukkit settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("bukkit.yml"))
                        .describedAs("Yml file");

                this.acceptsAll(Main.asList("C", "commands-settings"), "File for command settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("commands.yml"))
                        .describedAs("Yml file");

                this.acceptsAll(Main.asList("forceUpgrade"), "Whether to force a world upgrade");
                this.acceptsAll(Main.asList("eraseCache"), "Whether to force cache erase during world upgrade");
                this.acceptsAll(Main.asList("recreateRegionFiles"), "Whether to recreate region files during world upgrade");
                this.accepts("safeMode", "Loads level with vanilla datapack only"); // Paper
                this.acceptsAll(Main.asList("nogui"), "Disables the graphical console");

                this.acceptsAll(Main.asList("nojline"), "Disables jline and emulates the vanilla console");

                this.acceptsAll(Main.asList("noconsole"), "Disables the console");

                this.acceptsAll(Main.asList("v", "version"), "Show the CraftBukkit Version");

                this.acceptsAll(Main.asList("demo"), "Demo mode");

                this.acceptsAll(Main.asList("initSettings"), "Only create configuration files and then exit"); // SPIGOT-5761: Add initSettings option

                this.acceptsAll(Main.asList("S", "spigot-settings"), "File for spigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("spigot.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("paper-dir", "paper-settings-directory"), "Directory for Paper settings")
                    .withRequiredArg()
                    .ofType(File.class)
                    .defaultsTo(new File(io.papermc.paper.configuration.PaperConfigurations.CONFIG_DIR))
                    .describedAs("Config directory");
                acceptsAll(asList("paper", "paper-settings"), "File for Paper settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("paper.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("add-plugin", "add-extra-plugin-jar"), "Specify paths to extra plugin jars to be loaded in addition to those in the plugins folder. This argument can be specified multiple times, once for each extra plugin jar path.")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File[] {})
                        .describedAs("Jar file");

                acceptsAll(asList("server-name"), "Name of the server")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("Unknown Server")
                        .describedAs("Name");
            }
        };

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (joptsimple.OptionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (options.has("v")) {
            System.out.println(CraftServer.class.getPackage().getImplementationVersion());
        } else {
            // Do you love Java using + and ! as string based identifiers? I sure do!
            String path = new File(".").getAbsolutePath();
            if (path.contains("!") || path.contains("+")) {
                System.err.println("Cannot run server in a directory with ! or + in the pathname. Please rename the affected folders and try again.");
                return;
            }

            // Paper start - Improve java version check
            boolean skip = Boolean.getBoolean("Paper.IgnoreJavaVersion");
            String javaVersionName = System.getProperty("java.version");
            // J2SE SDK/JRE Version String Naming Convention
            boolean isPreRelease = javaVersionName.contains("-");
            if (isPreRelease) {
                if (!skip) {
                    System.err.println("Unsupported Java detected (" + javaVersionName + "). You are running an unsupported, non official, version. Only general availability versions of Java are supported. Please update your Java version. See https://docs.papermc.io/paper/faq#unsupported-java-detected-what-do-i-do for more information.");
                    return;
                }

                System.err.println("Unsupported Java detected ("+ javaVersionName + "), but the check was skipped. Proceed with caution! ");
            }
            // Paper end - Improve java version check

            try {
                if (options.has("nojline")) {
                    System.setProperty(net.minecrell.terminalconsole.TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false");
                    useJline = false;
                }

                if (options.has("noconsole")) {
                    Main.useConsole = false;
                    useJline = false; // Paper
                    System.setProperty(net.minecrell.terminalconsole.TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false"); // Paper
                }


                System.setProperty("library.jansi.version", "Paper"); // Paper - set meaningless jansi version to prevent git builds from crashing on Windows
                System.setProperty("jdk.console", "java.base"); // Paper - revert default console provider back to java.base so we can have our own jline

                SharedConstants.tryDetectVersion();
                new io.papermc.paper.ServerBuildInfoImpl();
                if (System.getProperty("IReallyKnowWhatIAmDoingISwear") == null) {
                    final ServerBuildInfo build = ServerBuildInfo.buildInfo();
                    final OptionalInt buildNumber = build.buildNumber();
                    final String repo = "PaperMC/Paper";
                    int distance = PaperVersionFetcher.DISTANCE_ERROR;

                    if (build.buildNumber().isEmpty() && build.gitCommit().isEmpty()) {
                        System.out.println("*** You are running a development version without access to version information ***");
                    } else {
                        if (buildNumber.isPresent()) {
                            distance = PaperVersionFetcher.fetchDistanceFromSiteApi(build, buildNumber.getAsInt());
                        } else {
                            final Optional<String> gitBranch = build.gitBranch();
                            final Optional<String> gitCommit = build.gitCommit();
                            if (gitBranch.isPresent() && gitCommit.isPresent()) {
                                distance = PaperVersionFetcher.fetchDistanceFromGitHub(repo, gitBranch.get(), gitCommit.get());
                                }
                            }

                        switch (distance) {
                            case PaperVersionFetcher.DISTANCE_ERROR -> System.err.println("*** Error obtaining version information! Can't fetch version info ***");
                            case 0 -> {}
                            case PaperVersionFetcher.DISTANCE_UNKNOWN -> System.out.println("*** You are running an unknown version! Can't fetch version info ***");
                            default -> {
                                if (distance > 5) {
                                    System.err.println("*** Warning, you've not updated in a while! ***");
                                    System.err.println("*** You are " + distance + " builds behind!");
                                    System.err.println("*** Please download a new build from https://papermc.io/downloads/paper ***");
                                } else {
                                    System.out.println("*** There's a new build available to download ***");
                                    System.out.println("*** Currently you are " + distance + " builds behind");
                                    System.out.println("*** You can download a new build from https://papermc.io/downloads/paper ***");
                                }
                        }
                    };
                }
            }
                io.papermc.paper.PaperBootstrap.boot(options);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            
        }
    }

    private static List<String> asList(String... params) {
        return Arrays.asList(params);
    }
}
