package de.greensurvivors.eventhelper;

import io.papermc.paper.math.Position;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Utils {
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

    @SuppressWarnings("UnstableApiUsage") // Position
    public static @NotNull Map<String, Double> serializePosition(final @NotNull Position position) {
        return Map.of("x", position.x(), "y", position.y(), "z", position.z());
    }

    @SuppressWarnings("UnstableApiUsage") // Position
    public static @NotNull Position deserializePosition(final @NotNull Map<String, ?> map) throws NoSuchElementException {
        if (map.get("x") instanceof Number x) {
            if (map.get("y") instanceof Number y) {
                if (map.get("z") instanceof Number z) {
                    return Position.fine(x.doubleValue(), y.doubleValue(), z.doubleValue());
                } else {
                    throw new NoSuchElementException("Serialized Position " + map + " does not contain an valid z value.");
                }
            } else {
                throw new NoSuchElementException("Serialized Position " + map + " does not contain an valid y value.");
            }
        } else {
            throw new NoSuchElementException("Serialized Position " + map + " does not contain an valid x value.");
        }
    }

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
