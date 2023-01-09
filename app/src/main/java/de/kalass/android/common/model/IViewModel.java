package de.kalass.android.common.model;

import com.google.common.base.Function;

/**
 * Created by klas on 01.11.13.
 */
public interface IViewModel {
    static final Function<IViewModel, Long> GET_ID = new Function<IViewModel, Long>() {
        @Override
        public Long apply(IViewModel item) {
            return item == null ? null : item.getId();
        }
    };
    long getId();
}
