package de.kalass.agime.overview.model;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by klas on 28.11.13.
 */
public final class OverviewUtil {

    private OverviewUtil() {
    }

    public static final <K, V> Map<K, Collection<V>> group(
            Iterable<V> iterable, Function<V, K> keyFkt
    ) {
        return Multimaps.<K, V>index(iterable, keyFkt).asMap();
    }

    public static final <T, V, G> List<T> convertAndSort(
            T[] resultContainer,
            Map<G, Collection<V>> entries,
            Function<Map.Entry<G, Collection<V>>, T> converter,
            Ordering<T> ordering
    ) {
        final int size = entries.size();
        Preconditions.checkArgument(resultContainer.length == size, "ResultContainer and map must be of equal size");
        int i = 0;
        Iterator<Map.Entry<G, Collection<V>>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<G, Collection<V>> e = it.next();
            T projectOverview = converter.apply(e);
            resultContainer[i] = Preconditions.checkNotNull(projectOverview);
            i++;
        }
        Preconditions.checkState(i == resultContainer.length);
        Arrays.sort(resultContainer, ordering);
        return ImmutableList.copyOf(resultContainer);
    }

}
