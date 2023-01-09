package de.kalass.agime.trackactivity;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.android.common.simpleloader.ValueOrReference;

/**
* Created by klas on 22.01.14.
*/
class SplitResult {
    static Predicate<SplitResult> IS_INVERSE_CORRECTED = new Predicate<SplitResult>() {
        @Override
        public boolean apply(SplitResult input) {
            if (input == null) {
                return false;
            }
            return input.type != SplitType.NOTHING
                    && input.type != SplitType.FAILED_LIES_FULLY_WITHIN
                    && input.depth > 1;
        }
    };
    final SplitType type;
    final List<EntryToAdjust> entries;
    final int depth;

    private SplitResult(SplitType type, List<EntryToAdjust> entries, int depth) {
        this.type = type;
        this.entries = entries;
        this.depth = depth;
    }

    static SplitResult ofType(SplitType type, EntryToAdjust entry, int depth) {
        return new SplitResult(type, ImmutableList.of(entry), depth);
    }
    static SplitResult ofType(SplitType type, EntryToAdjust entry1, EntryToAdjust entry2, int depth) {
        return new SplitResult(type, ImmutableList.of(entry1, entry2), depth);
    }
}
