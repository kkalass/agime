package de.kalass.android.common.simpleloader;

import android.text.Selection;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Created by klas on 18.02.14.
 */
public class DBSelectionUtil {

    public static final SelectionArgRenderer BASE_RENDERER = new BaseSelectionArgRenderer();

    public interface  Builder {
        Builder in(String columnName, Iterable<?> values);
        Builder eq(String columnName, Object value);
        Builder neq(String columnName, Object value);
        Builder like(String columnName, Object value);
        Builder likeSerialized(String columnName, String value);
        Builder match(String columnName, Object value);
        Builder gt(String columnName, Object value);
        Builder gte(String columnName, Object value);
        Builder lt(String columnName, Object value);
        Builder lte(String columnName, Object value);
        Builder and();
        Builder or();
        Builder push();
        Builder pull();
        String buildSelection();
        String[] buildArgs();

        Builder blockBegin();
        Builder blockEnd();


    }

    public static class BuilderImpl implements  Builder {
        private final StringBuilder sb = new StringBuilder();
        private String[] args;
        private List<String> argsList;
        private SelectionArgRenderer _renderer;
        private int whereCount = 0;

        BuilderImpl(SelectionArgRenderer renderer) {
            _renderer = renderer;
        }

        public BuilderImpl(BuilderImpl other) {
            sb.append(other.sb.toString());
            if (other.args == null) {
                args = null;
            } else {
                args = Arrays.asList(other.args).toArray(new String[other.args.length]);
            }
            _renderer = other._renderer;
            whereCount = other.whereCount;
            if (other.argsList == null) {
                argsList = null;
            } else {
                argsList = new ArrayList<>(other.argsList.size());
                argsList.addAll(other.argsList);
            }
        }

        public Builder in(String columnName, Iterable<?> id) {
            whereCount++;
            appendIn(_renderer, sb, columnName, id);
            return this;
        }

        private StringBuilder appendIn(SelectionArgRenderer renderer, StringBuilder sb, String columnName, Iterable<?> id) {
            if (Iterables.isEmpty(id)) {
                // whatever the column value is, it simply cannot be part of the empty list - use and expression
                // that will always evaluate to FALSE
                sb.append(" _ID IS NULL ");
                return sb;
            }

            sb.append(columnName).append(" in (");
            Iterator<?> it = id.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                sb.append("?");
                appendArg(renderer.toSelectionArg(columnName, o));
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb;
        }

        private void appendArg(String value) {
            if (args == null && argsList == null) {
                args = new String[] {value};
                argsList = null;
            } else if (args != null && argsList == null) {
                argsList = new ArrayList<String>();
                for (String a: args) {
                    argsList.add(a);
                }
                argsList.add(value);
                args = null;
            } else {
                argsList.add(value);
            }
        }

        public Builder op(String columnName, String op, Object value) {
            whereCount++;
            sb.append(columnName).append(op).append(" ? ");
            appendArg(value.toString());
            return this;
        }

        public Builder gt(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            sb.append(" > ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return this;
        }

        public Builder gte(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            sb.append(" >= ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return this;
        }
        public Builder lt(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            sb.append(" < ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return this;
        }

        public Builder lte(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            sb.append(" <= ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return this;
        }

        public Builder eq(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            if (value == null) {
                sb.append(" IS NULL ");
            } else {
                sb.append(" = ? ");
                appendArg(_renderer.toSelectionArg(columnName, value));
            }
            return this;
        }

        @Override
        public Builder match(String columnName, Object value) {
            whereCount++;
            sb.append(columnName).append(" MATCH ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return null;
        }

        @Override
        public Builder like(String columnName, Object value) {
            whereCount++;
            sb.append(columnName).append(" LIKE ? ");
            appendArg(_renderer.toSelectionArg(columnName, value));
            return null;
        }

        @Override
        public Builder likeSerialized(String columnName, String value) {
            whereCount++;
            sb.append(columnName).append(" LIKE ? ");
            appendArg(value);
            return null;
        }

        public Builder neq(String columnName, Object value) {
            whereCount++;
            sb.append(columnName);
            if (value == null) {
                sb.append(" IS NOT NULL ");
            } else {
                sb.append(" != ? ");
                appendArg(_renderer.toSelectionArg(columnName, value));
            }
            return this;
        }

        @Override
        public Builder and() {
            if (whereCount > 0) {
                sb.append(" AND ");
            }
            return this;
        }
        @Override
        public Builder push() {
            sb.append(" ( ");
            return this;
        }
        @Override
        public Builder pull() {
            sb.append(" ) ");
            return this;
        }
        @Override
        public Builder or() {

            sb.append(" OR ");

            return this;
        }

        @Override
        public Builder blockBegin() {
            sb.append(" ( ");
            return this;
        }

        @Override
        public Builder blockEnd() {
            sb.append(" ) ");
            return this;
        }

        public String buildSelection() {
            return sb.toString();
        }

        public String[] buildArgs() {
            return argsList == null ? args : argsList.toArray(new String[argsList.size()]);
        }
    }

    public interface SelectionArgRenderer {
        /**
         * Needed for "where in" queries: convert to a string that can *safely* be used
         * in the "selection". Note that not all java types can be safely converted and
         * will throw an IllegalArgumentException
         * @throws  java.lang.IllegalArgumentException if the argument is for example a string and cannot be safely used in a selection
         */
        @Nonnull
        String toUnsafeSelectionPart(@Nonnull String param, @Nonnull Object o) throws IllegalArgumentException;

        /**
         * Converts  the instance to a string for selection argument. Selection arguments
         * are safe against SQL injection.
         *
         */
        @Nonnull
        String toSelectionArg(@Nonnull String param, @Nonnull Object o);
    }

    public static class BaseSelectionArgRenderer implements SelectionArgRenderer {
        @Nonnull
        @Override
        public String toSelectionArg(@Nonnull final String param, @Nonnull final Object o) {
            return o.toString();
        }

        @Nonnull
        @Override
        public String toUnsafeSelectionPart(@Nonnull final String param, @Nonnull final Object o) throws IllegalArgumentException {
            Preconditions.checkArgument(o instanceof Number);
            return Number.class.cast(o).toString();
        }
    }

    public static Builder builder(SelectionArgRenderer renderer) {
        return new BuilderImpl(renderer);
    }

    public static Builder builder() {
        return new BuilderImpl(BASE_RENDERER);
    }



    private static StringBuilder unsafeAppendIn(SelectionArgRenderer renderer, StringBuilder sb, String columnName, Iterable<?> id) {
        sb.append(columnName).append(" in (");
        Iterator<?> it = id.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            // The renderer has to make sure it is safe...
            sb.append(renderer.toUnsafeSelectionPart(columnName, o));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb;
    }

}
