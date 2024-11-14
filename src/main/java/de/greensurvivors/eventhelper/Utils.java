package de.greensurvivors.eventhelper;

import io.papermc.paper.math.Position;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Utils {
    /// config key for {@link #positionVersion}
    private final static @NotNull String VERSION_PATH = "version";
    /// the version this config is saved under. Will be important, when the position data evolves, and we need a datafixerupper
    private final static @NotNull ComparableVersion positionVersion = new ComparableVersion("1.0.0");

    final static String Digits = "(\\p{Digit}+)";
    final static String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final static String Exp = "[eE][+-]?" + Digits;
    final static String fpRegex =
        ("[\\x00-\\x20]*" + // Optional leading "whitespace"
            "[+-]?(" +         // Optional sign character
            //"NaN|" +           // "NaN" string
            //"Infinity|" +      // "Infinity" string

            // A decimal floating-point string representing a finite positive
            // number without a leading sign has at most five basic pieces:
            // Digits . Digits ExponentPart FloatTypeSuffix
            //
            // Since this method allows integer-only strings as input
            // in addition to strings of floating-point literals, the
            // two sub-patterns below are simplifications of the grammar
            // productions from the Java Language Specification, 2nd
            // edition, section 3.10.2.

            // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
            "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

            // . Digits ExponentPart_opt FloatTypeSuffix_opt
            "(\\.(" + Digits + ")(" + Exp + ")?)|" +

            // Hexadecimal strings
            "((" +
            // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
            "(0[xX]" + HexDigits + "(\\.)?)|" +

            // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
            "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

            ")[pP][+-]?" + Digits + "))" +
            "[fFdD]?))" +
            "[\\x00-\\x20]*");// Optional trailing "whitespace"
    private static final Pattern FLOAT_PATTERN = Pattern.compile(fpRegex);

    /**
     * Test if a String can safely convert into a double
     *
     * @param toTest String input
     */
    private static boolean isDouble(String toTest) {
        if (toTest == null) {
            return false;
        }

        if (toTest.isEmpty()) { //empty
            return false;
        }

        return FLOAT_PATTERN.matcher(toTest).find();
    }

    /**
     * Since papers fine position does not have any serialization of its own, we have to implement it ourselves
     */
    @SuppressWarnings("UnstableApiUsage") // Position
    public static @NotNull Map<String, Object> serializePosition(final @NotNull Position position) {
        return Map.of(
            VERSION_PATH, positionVersion.toString(),
            "type", position.isBlock() ? "block" : position.isFine() ? "fine" : "unknown",
            "x", position.x(),
            "y", position.y(),
            "z", position.z()
        );
    }

    /**
     * Since papers position does not have any deserialization of its own, we have to implement it ourselves
     *
     * @throws NoSuchElementException if x, y or z isn't mapped to a number
     */
    @SuppressWarnings("UnstableApiUsage") // Position
    public static @NotNull Position deserializePosition(Map<String, ?> serialized) throws IllegalArgumentException {
        final @NotNull EventHelper plugin = EventHelper.getPlugin();

        if (serialized.get(VERSION_PATH) instanceof String versionStr) {
            final ComparableVersion lastVersion = new ComparableVersion(versionStr);

            if (positionVersion.compareTo(lastVersion) < 0) {
                plugin.getComponentLogger().warn("Serialized Position {} was saved in a newer data version ({}), " +
                        "expected: {}. Trying to load anyway but this most definitely will be broken!",
                    serialized, lastVersion, positionVersion);
            }
        } else {
            plugin.getComponentLogger().warn("Serialized Position {} was saved without a valid data version, " +
                    "expected: {}. Trying to load anyway but this might be broken!",
                serialized, positionVersion);
        }

        if (serialized.get("x") instanceof Number x) {
            if (serialized.get("y") instanceof Number y) {
                if (serialized.get("z") instanceof Number z) {
                    if (serialized.get("type") instanceof String type) {
                        if (type.equalsIgnoreCase("block")) {
                            return Position.block(x.intValue(), y.intValue(), z.intValue());
                        } else if (type.equalsIgnoreCase("fine")) {
                            return Position.fine(x.doubleValue(), y.doubleValue(), z.doubleValue());
                        } else {
                            plugin.getComponentLogger().warn("Serialized Position {} was saved without a valid type ({}), " +
                                    "expected: (fine, block). Trying to load anyway as fine.",
                                serialized, type);

                            //throw new IllegalArgumentException("Argument " + type + " is not a position type.");
                            return Position.fine(x.doubleValue(), y.doubleValue(), z.doubleValue());
                        }
                    } else {
                        plugin.getComponentLogger().warn("Serialized Position {} was saved without a valid type. Trying to load anyway as fine.", serialized);

                        //throw new IllegalArgumentException("Argument " + serialized.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY) + " is not a position type.");

                        return Position.fine(x.doubleValue(), y.doubleValue(), z.doubleValue());
                    }
                } else {
                    throw new NoSuchElementException("Serialized Position " + serialized + " does not contain an valid z value.");
                }
            } else {
                throw new NoSuchElementException("Serialized Position " + serialized + " does not contain an valid y value.");
            }
        } else {
            throw new NoSuchElementException("Serialized Position " + serialized + " does not contain an valid x value.");
        }
    }

    /**
     * This function checks all the keys of a map and calls the consumer, if one of them isn't a String
     */
    public static @NotNull Map<@NotNull String, ?> checkSerialzedMap(final @NotNull Map<?, ?> map, Consumer<Map.Entry<?, ?>> unexpectedTypeConsumer) {
        final @NotNull Map<String, Object> result = new LinkedHashMap<>(map.size());

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            } else {
                unexpectedTypeConsumer.accept(entry);
            }
        }

        return result;
    }
}
