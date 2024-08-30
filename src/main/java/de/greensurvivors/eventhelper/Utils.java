package de.greensurvivors.eventhelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.stream.Collectors;

public class Utils {

    /**
     * Creates an unmodifiable sequenced set out of the given elements
     */
    @SafeVarargs
    public static <T> SequencedSet<T> unmodifiableSequencedSetOf(T... elements) {
        return Collections.unmodifiableSequencedSet(
            (SequencedSet<T>) Arrays.stream(elements)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
