package de.greensurvivors.eventhelper.messages;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.greensurvivors.eventhelper.EventHelper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MessageManager {
    private static final @NotNull Pattern DURATION_PATTERN = Pattern.compile("(-?[0-9]+)([tTsSmhHdDwWM])");
    private static final @NotNull String BUNDLE_FILE_PROTO_PATTERN = "(?:_.*)?.properties";
    private final @NotNull EventHelper plugin;
    private final @NotNull LoadingCache<@NotNull String, @NotNull ResourceBundle> resourceBundles = Caffeine.newBuilder().build(
        this::loadBundle); // todo check

    /// caches every component without placeholder for faster access in future and loads missing values automatically
    private final @NotNull LoadingCache<@NotNull LangPath, @NotNull Component> langCache = Caffeine.newBuilder().build(
        path -> MiniMessage.miniMessage().deserialize(
            getRawString(path)
        )
    );
    /// sometimes, like in case of strings, you need to compare two strings without caring for text-style
    private final @NotNull LoadingCache<@NotNull LangPath, @NotNull String> strippedLangCache = Caffeine.newBuilder().build(
        path -> MiniMessage.miniMessage().stripTags(
            getRawString(path)
        )
    );

    private @NotNull Locale locale = Locale.ENGLISH;

    public MessageManager(@NotNull EventHelper plugin) {
        this.plugin = plugin;
    }

    /**
     * @return the given duration formated as DDdHHhMMmSSsTTt
     * D = amount of days if any
     * H = amount of hours, if any
     * M = amount of minutes, if any
     * S = amount of seconds, if any
     * T = amount of ticks, if any
     */
    public static @NotNull Component formatTime(final @NotNull Duration duration) {
        StringBuilder timeStr = new StringBuilder();

        final long days = duration.toDaysPart();
        if (days != 0) {
            timeStr.append(days).append("d");
        }

        final int hours = duration.toHoursPart();
        if (hours != 0) {
            timeStr.append(hours).append("h");
        }

        final int minutes = duration.toMinutesPart();
        if (minutes != 0) {
            timeStr.append(minutes).append("m");
        }

        final int seconds = duration.toSecondsPart();
        if (seconds != 0) {
            timeStr.append(seconds).append("s");
        }

        // the tick rate isn't constant anymore, but can get adjusted ingame.
        // with a standard tick rate of 20 t/s / 1000 ms/s we would have duration.toMillisPart() / 50.
        final float ticksPerSecond = Bukkit.getServerTickManager().getTickRate();
        final int ticks = (int) (duration.toMillisPart() * ticksPerSecond / 1000);
        if (ticks != 0) {
            timeStr.append(ticks).append("t");
        }
        return Component.text(timeStr.toString());
    }

    /**
     * Try to get a time period of a string.
     * First try ISO-8601 duration, and afterward our own implementation
     * using the same time unit more than once is permitted.
     * Ticks are counted as standard 20 t/s, not doing anything special with the tick manager
     *
     * @return the duration, or null if not possible
     */
    public @Nullable Duration parseDuration(@NotNull String period) {
        try { //try Iso
            return Duration.parse(period);
        } catch (DateTimeParseException e) {
            plugin.getComponentLogger().warn("Couldn't get time period \"{}\" as duration. Trying to parse manual next.", period, e);
        }

        Matcher matcher = DURATION_PATTERN.matcher(period);
        Duration duration = Duration.ZERO;

        while (matcher.find()) {
            try {
                long num = Long.parseLong(matcher.group(1));
                String typ = matcher.group(2);
                duration = switch (typ) { // from periodPattern
                    case "t", "T" -> duration.plusMillis(50L * num); // ticks
                    case "s", "S" -> duration.plusSeconds(num);
                    case "m" -> duration.plusMinutes(num);
                    case "h", "H" -> duration.plusHours(num);
                    case "d", "D" -> duration.plusDays(num);
                    case "w", "W" -> duration.plusDays(Period.ofWeeks((int) num).getDays());
                    case "M" -> duration.plusDays(Period.ofMonths((int) num).getDays());
                    default -> duration;
                };
            } catch (NumberFormatException e) {
                plugin.getComponentLogger().warn("Couldn't get time period for {}", period, e);
            }
        }
        return duration == Duration.ZERO ? null : duration;
    }

    /**
     * Use with care, as this fetches raw aka yet unformatted strings, directly from the ressource bundle,
     * or the default of the LangPath
     */
    public @NotNull String getRawString(final @NotNull LangPath path) {
        try {
            return resourceBundles.get(path.getModulName()).getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            return path.getDefaultValue();
        }
    }

    /**
     * Sometimes it may be useful to have a set of alternative messages belonging to the very same LangPath.
     * Examples may include a random message or matching user input against a set of alternatives.
     * <p>
     * Use with care, as this fetches raw aka yet unformatted strings, directly from the ressource bundle,
     * or the default of the LangPath
     */
    private @NotNull Set<@NotNull String> getRawStringSet(final @NotNull String modulName,
                                                          final @NotNull LangPath path) {
        String value;
        try {
            value = resourceBundles.get(modulName).getString(path.getPath());
        } catch (MissingResourceException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING, "couldn't find path: \"" + path.getPath() + "\" in lang files using fallback.", e);
            value = path.getDefaultValue();
        }

        return Set.of(value.split("\\s?+,\\s?+"));
    }

    /// set the locale used by the base plugin and every module
    public void setLocale(final @NotNull Locale locale) {
        if (!this.locale.equals(locale)) {
            this.locale = locale;
            plugin.getLogger().info("Locale set to language: " + locale.toLanguageTag());

            // clear all cache
            langCache.invalidateAll();
            langCache.cleanUp();
            langCache.asMap().clear();
            strippedLangCache.invalidateAll();
            strippedLangCache.cleanUp();
            strippedLangCache.asMap().clear();
            resourceBundles.invalidateAll();
            resourceBundles.cleanUp();
            resourceBundles.asMap().clear();
        }
    }

    /// reload language file.
    public @Nullable ResourceBundle loadBundle(final @NotNull String modulName) {
        ResourceBundle resourceBundle = null; // reset last bundle

        // save all missing keys
        initLangFiles(modulName);

        final Path langDictionary = plugin.getDataFolder().toPath().resolve(modulName);
        final String bundleName = makeBundleName(modulName);

        try {
            URL[] urls = new URL[]{langDictionary.toUri().toURL()};
            resourceBundle = ResourceBundle.getBundle(bundleName, locale, new URLClassLoader(urls), UTF8ResourceBundleControl.get());

        } catch (SecurityException | MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, "Exception while reading lang bundle. Using internal", e);
        } catch (MissingResourceException ignored) { // how? missing write access?
            plugin.getLogger().log(Level.WARNING, "No translation file for " + UTF8ResourceBundleControl.get().toBundleName(bundleName, locale) + " found on disc. Using internal");
        }

        if (resourceBundle == null) { // fallback, since we are always trying to save defaults this never should happen
            try {
                resourceBundle = PropertyResourceBundle.getBundle(modulName + "." + bundleName, locale, plugin.getClass().getClassLoader(), new UTF8ResourceBundleControl());
            } catch (MissingResourceException e) {
                plugin.getLogger().log(Level.SEVERE, "Couldn't get Ressource bundle \"lang\" for locale \"" + locale.toLanguageTag() + "\". Messages WILL be broken!", e);
            }
        }

        // clear cache
        //resourceBundles.asMap().remove(modulName);
        // todo remove all langPath cache associated with this module
        //langCache.invalidateAll();
        //rawLangCache.invalidateAll();

        return resourceBundle;
    }

    /// the resource bundle should be named the modulName followed by "Lang"
    private static @NotNull String makeBundleName(final @NotNull String modulName) {
        return modulName + "Lang";
    }

    /// make sure reading and writing does happen as expected, by escaping special characters
    private @NotNull String saveConvert(final @NotNull String theString,
                                        final boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder convertedStrBuilder = new StringBuilder(bufLen);

        for (int i = 0; i < theString.length(); i++) {
            char aChar = theString.charAt(i);
            // Handle common case first
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    if (i + 1 < theString.length()) {
                        final char bChar = theString.charAt(i + 1);
                        if (bChar == ' ' || bChar == 't' || bChar == 'n' || bChar == 'r' ||
                            bChar == 'f' || bChar == '\\' || bChar == 'u' || bChar == '=' ||
                            bChar == ':' || bChar == '#' || bChar == '!') {
                            // don't double escape already escaped chars
                            convertedStrBuilder.append(aChar);
                            convertedStrBuilder.append(bChar);
                            i++;
                            continue;
                        } else {
                            // any other char following
                            convertedStrBuilder.append('\\');
                        }
                    } else {
                        // last char was a backslash. escape!
                        convertedStrBuilder.append('\\');
                    }
                }
                convertedStrBuilder.append(aChar);
                continue;
            }

            // escape non escaped chars that have to get escaped
            switch (aChar) {
                case ' ' -> {
                    if (escapeSpace) {
                        convertedStrBuilder.append('\\');
                    }
                    convertedStrBuilder.append(' ');
                }
                case '\t' -> convertedStrBuilder.append("\\t");
                case '\n' -> convertedStrBuilder.append("\\n");
                case '\r' -> convertedStrBuilder.append("\\r");
                case '\f' -> convertedStrBuilder.append("\\f");
                case '=', ':', '#', '!' -> {
                    convertedStrBuilder.append('\\');
                    convertedStrBuilder.append(aChar);
                }
                default -> convertedStrBuilder.append(aChar);
            }
        }

        return convertedStrBuilder.toString();
    }

    /**
     * saves all missing lang files from resources to the modules folder,
     * as well as appending the old ones with new key-value pairs
     */
    private void initLangFiles(final @NotNull String modulName) {
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jarUrl = src.getLocation();

            try (ZipInputStream zipStream = new ZipInputStream(jarUrl.openStream())) {
                final @NotNull String bundleName = makeBundleName(modulName);
                // don't worry about system specific path separators, zipEntry always uses "/"
                final @NotNull Pattern bundlePattern = Pattern.compile(modulName + "/" + bundleName + BUNDLE_FILE_PROTO_PATTERN);

                ZipEntry zipEntry;
                while ((zipEntry = zipStream.getNextEntry()) != null) {
                    // I don't know exactly why but ZipInputStream doesn't list all toplevel entries first anymore,
                    // So we have to iterate over the whole f*ing jar to find our lang files.
                    String entryName = zipEntry.getName();

                    if (bundlePattern.matcher(entryName).matches()) {
                        Path langFilePath = plugin.getDataFolder().toPath().resolve(entryName);

                        if (!Files.isRegularFile(langFilePath)) { // don't overwrite existing files
                            FileUtils.copyToFile(zipStream, langFilePath.toFile());
                        } else { // add defaults to file to expand in case there are key-value pairs missing
                            Properties defaults = new Properties();
                            // don't close reader, since we need the stream to be still open for the next entry!
                            defaults.load(new InputStreamReader(zipStream, StandardCharsets.UTF_8));

                            Properties current = new Properties();
                            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFilePath.toFile()), StandardCharsets.UTF_8)) {
                                current.load(reader);
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.WARNING, "couldn't get current properties file for " + entryName + "!", e);
                                continue;
                            }

                            try (FileWriter fw = new FileWriter(langFilePath.toFile(), StandardCharsets.UTF_8, true);
                                 // we are NOT using Properties#store since it gets rid of comments and doesn't guarantee ordering
                                 BufferedWriter bw = new BufferedWriter(fw)) {
                                boolean updated = false; // only write comment once
                                for (Map.Entry<Object, Object> translationPair : defaults.entrySet()) {
                                    if (current.get(translationPair.getKey()) == null) {
                                        if (!updated) {
                                            bw.newLine();
                                            bw.write("# New Values where added. Is everything else up to date? Time of update: " + new Date());
                                            bw.newLine();

                                            plugin.getLogger().fine("Updated langfile \"" + entryName + "\". Might want to check the new translation strings out!");

                                            updated = true;
                                        }

                                        String key = saveConvert((String) translationPair.getKey(), true);
                                        /* No need to escape embedded and trailing spaces for value, hence
                                         * pass false to flag.
                                         */
                                        String val = saveConvert((String) translationPair.getValue(), false);
                                        bw.write((key + "=" + val));
                                        bw.newLine();
                                    } // current already knows the key
                                } // end of for
                            } // end of try
                        } // end of else (file exists)
                    } // doesn't match
                } // end of elements
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't save lang files", e);
            }
        } else {
            plugin.getLogger().warning("Couldn't save lang files: no CodeSource!");
        }
    }

    /**
     * get a component from lang file and apply the given tag resolver.
     * Note: might be slightly slower than {@link #getLang(LangPath)} since this can not use cache.
     */
    public @NotNull Component getLang(final @NotNull LangPath path,
                                      final @NotNull TagResolver... resolver) {
        return MiniMessage.miniMessage().deserialize(getRawString(path), resolver);
    }

    ///get a component from lang file
    public @NotNull Component getLang(final @NotNull LangPath path) {
        return langCache.get(path);
    }

    /// send a component from the lang file to the audience, prefixed with this prefix.
    public void sendPrefixedLang(final @NotNull Audience audience,
                                 final @NotNull LangPath prefix,
                                 final @NotNull LangPath path) {

        audience.sendMessage(langCache.get(prefix).appendSpace().append(langCache.get(path)));
    }

    ///send a component from the lang file to the audience, prefixed with this prefix.
    public void sendLang(final @NotNull Audience audience,
                         final @NotNull LangPath path) {
        audience.sendMessage(langCache.get(path));
    }

    /// send a component to the audience, prefixed with this prefix.
    public void sendPrefixedLang(final @NotNull Audience audience,
                                 final @NotNull LangPath prefix,
                                 final @NotNull Component message) {
        audience.sendMessage(langCache.get(prefix).appendSpace().append(message));
    }

    /// send a component to the audience.
    public void sendLang(final @NotNull Audience audience,
                         final @NotNull Component message) {
        audience.sendMessage(message);
    }

    /**
     * broadcast a component from the lang file on the server, prefixed with this plugins prefix.
     */
    public void broadcastPrefixedLang(final @NotNull LangPath prefix,
                                      final @NotNull LangPath path) {
        Bukkit.broadcast(langCache.get(prefix).appendSpace().append(langCache.get(path)));
    }

    /// broadcast a component from the lang file on the server
    public void broadcastLang(final @NotNull LangPath path) {
        Bukkit.broadcast(langCache.get(path));
    }

    /**
     * send a component from the lang file to the audience, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendPrefixedLang(Audience, LangPath, LangPath)} since this can not use cache.
     */
    public void sendPrefixedLang(final @NotNull Audience audience,
                                 final @NotNull LangPath prefix,
                                 final @NotNull LangPath path,
                                 final @NotNull TagResolver... resolver) {
        audience.sendMessage(langCache.get(prefix).appendSpace().append(
            MiniMessage.miniMessage().deserialize(getRawString(path), resolver)));
    }

    /**
     * send a component from the lang file to the audience, applying the given tag resolver.
     * Note: might be slightly slower than {@link #sendLang(Audience, LangPath)} since this can not use cache.
     */
    public void sendLang(final @NotNull Audience audience,
                         final @NotNull LangPath path,
                         final @NotNull TagResolver... resolver) {
        audience.sendMessage(MiniMessage.miniMessage().deserialize(getRawString(path), resolver));
    }

    /**
     * broadcast a component from the lang file on the server, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #broadcastPrefixedLang(LangPath, LangPath)} since this can not use cache.
     */
    public void broadcastLang(final @NotNull LangPath prefix,
                              final @NotNull LangPath path,
                              final @NotNull TagResolver... resolver) {
        Bukkit.broadcast(langCache.get(prefix).appendSpace().append(
            MiniMessage.miniMessage().deserialize(getRawString(path), resolver)));
    }

    /**
     * broadcast a component from the lang file on the server, prefixed with this plugins prefix and applying the given tag resolver.
     * Note: might be slightly slower than {@link #broadcastLang(LangPath)} since this can not use cache.
     */
    public void broadcastLang(final @NotNull LangPath path,
                              final @NotNull TagResolver... resolver) {
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(getRawString(path), resolver));
    }

    ///Compares the plain text associated with the LangPath to the serialized component, ignoring casing
    public boolean isStrippedEqualsIgnoreCase(final @NotNull LangPath langPath, final @NotNull Component component) {
        String strToTest = PlainTextComponentSerializer.plainText().serialize(component).trim();

        return strippedLangCache.get(langPath).equalsIgnoreCase(strToTest);
    }
}
