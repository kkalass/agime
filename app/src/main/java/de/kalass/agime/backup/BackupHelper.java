package de.kalass.agime.backup;

import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
* Created by klas on 14.02.14.
*/
public interface BackupHelper {

    public static abstract class BackupFileFilter<T> implements Predicate<T> {
        static final Pattern FILENAME_PATTERN = Pattern.compile(Pattern.quote(BackupService.AGIME_BACKUP_PREFIX) + ".*" + Pattern.quote(BackupService.AGIME_BACKUP_SUFFIX));

        @Override
        public boolean apply(@Nullable T t) {
            return t != null && FILENAME_PATTERN.matcher(getName(t)).matches();
        }

        public abstract String getName(@Nonnull T t);
    }

    public static abstract class BackupFileOrdering<T> extends Ordering<T> {

        @Override
        public int compare(@Nullable T t, @Nullable T t2) {
            String lhs = t == null ? null : getName(t);
            String rhs = t == null ? null : getName(t2);
            if (lhs == null && rhs == null) {
                return 0;
            }
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }

            return -1 * lhs.compareToIgnoreCase(rhs);
        }

        public abstract String getName(@Nonnull T t);
    }

    public boolean prepare();
    public void start(BackupData.PersonalBackup data);
    public void finish();
}
