package de.kalass.agime.backup;

import android.content.Context;
import android.graphics.Color;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.WireFormat;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import au.com.bytecode.opencsv.CSVWriter;
import de.kalass.agime.customfield.ActivityCustomFieldModel;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.customfield.CustomFieldValueModel;
import de.kalass.agime.model.ActivityTypeModel;
import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 26.11.13.
 */
public class CSVFileReaderWriter {

    //public static final String MIME_TYPE = "text/comma-separated-values";// text/plain
    public static final String MIME_TYPE = "text/csv";// text/plain
    private final Function<CSVFileFormatInput, SingleFileFormat> _fileFormatFactory;

    public interface SingleFileFormat {
        String[] getHeader();
        char getSeparator();
        char getQuoteCharacter();
        String getLineEnding();
        String[] write(TrackedActivityModel model);
    }

    public static class V1FileFormat implements SingleFileFormat {
        private static final String[] BASE_HEADER = new String[]{
                "Id",
                "start (ms)",
                "end (ms)",
                "created (ms)",
                "Typ-Id",
                "Typ-Name",
                "Details",
                "Kategorie-Id",
                "Kategorie-Name",
                "Kategorie-Farbe",
                "Projekt-Id",
                "Projekt-Name",
                "Projekt-Farbe",
        };
        public static final char SEPARATOR = ';';
        public static final char QUOTE = '"';
        public static final String LINE_ENDING = "\n";
        private final DateTimeFormatter _dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        private final String[] _header;

        private final Map<Long, Integer> _customFieldTypeIdToPosition;
        public V1FileFormat(List<CustomFieldTypeModel> customFields) {
            _customFieldTypeIdToPosition = buildPositionMap(customFields);
            _header = buildHeader(customFields, _customFieldTypeIdToPosition);
        }

        private Map<Long, Integer> buildPositionMap(List<CustomFieldTypeModel> customFields) {
            Map<Long, Integer> r = new HashMap<Long, Integer>(customFields.size());
            for (int i = 0; i < customFields.size(); i++) {
                final CustomFieldTypeModel typeModel = customFields.get(i);
                r.put(typeModel.getId(), 2*i + BASE_HEADER.length);
            }
            return r;
        }

        private String[] buildHeader(List<CustomFieldTypeModel> customFields, Map<Long, Integer> positionMap) {
            String[] header = new String[BASE_HEADER.length + 2*customFields.size()];
            for (int i = 0; i < BASE_HEADER.length; i++) {
                header[i] = BASE_HEADER[i];
            }
            for (int i = 0; i < customFields.size(); i++) {
                final CustomFieldTypeModel field = Preconditions.checkNotNull(customFields.get(i), "custom field must not be null");
                final Integer position = Preconditions.checkNotNull(positionMap.get(field.getId()), "no position found");
                header[position] = field.getName() + "-Id";
                header[position + 1] = field.getName();
            }
            return header;
        }


        @Override
        public String[] getHeader() {
            return _header;
        }

        @Override
        public char getSeparator() {
            return SEPARATOR;
        }

        @Override
        public char getQuoteCharacter() {
            return QUOTE;
        }

        @Override
        public String getLineEnding() {
            return LINE_ENDING;
        }

        @Override
        public String[] write(TrackedActivityModel item) {
            CategoryModel category = item.getCategory();
            ActivityTypeModel activityType = item.getActivityType();
            ProjectModel project = item.getProject();
            Integer projectColor = project == null ? null : project.getColorCode();

            String[] result = new String[_header.length];
            result[0] = formatId(item.getId());
            result[1] = formatDateTime(item.getStartTimeMillis());
            result[2] = formatDateTime(item.getEndTimeMillis());
            result[3] = formatDateTime(item.getCreatedAtMillis());
            result[4] = activityType == null ? "" : formatId(activityType.getId());
            result[5] = activityType == null ? "" : activityType.getName();
            result[6] = item.getDetails();
            result[7] = category == null ? "" : formatId(category.getId());
            result[8] = category == null ? "" : category.getName();
            result[9] = category == null ? "" : formatColor(category.getColour());
            result[10] = project == null ? "" : formatId(project.getId());
            result[11] = project == null ? "" : project.getName();
            result[12] = projectColor == null ? "" : formatColor(projectColor);
            // fill the rows with those custom fields that are known to us
            for (ActivityCustomFieldModel field : item.getCustomFieldData()) {
                final Integer position = _customFieldTypeIdToPosition.get(field.getTypeModel().getId());
                Preconditions.checkNotNull(position, "position not found");
                final CustomFieldValueModel valueModel = field.getValueModel();
                result[position] = valueModel == null ? null : Long.toString(valueModel.getId());
                result[position+1] = valueModel == null ? null : valueModel.getValue();
            }
            return result;
        }

        protected String formatColor(int color) {
            return String.format("#%06X", 0xFFFFFF & color);
        }

        protected int parseColor(String color) {
            Preconditions.checkArgument(color.startsWith("#"));
            return Color.parseColor(color);
        }

        protected Integer parseColorNullable(String color) {
            return Strings.isNullOrEmpty(color) ? null : parseColor(color);
        }

        protected String formatId(long id) {
            return Long.toString(id);
        }

        protected long parseId(String id) {
            return Long.parseLong(id, 10);
        }

        protected Long parseIdNullable(String id) {
            return Strings.isNullOrEmpty(id) ? null : parseId(id);
        }

        protected String formatDateTime(long millis) {
            return _dateTimeFormatter.print(millis);
        }

        protected long parseDateTime(String timeString) {
            return _dateTimeFormatter.parseDateTime(timeString).getMillis();
        }
    }

    public static abstract class FieldTypeModel {
        public static final FieldTypeModel START = new SimpleFieldTypeModel("Start");
        public static final FieldTypeModel START_DAY =  new SimpleFieldTypeModel("Tag");
        public static final FieldTypeModel END_DAY =  new SimpleFieldTypeModel("Tag");
        public static final FieldTypeModel END = new SimpleFieldTypeModel("End");
        public static final FieldTypeModel DURATION = new SimpleFieldTypeModel("Dauer");
        public static final FieldTypeModel DURATION_MINUTES = new SimpleFieldTypeModel("Dauer (Minuten)");
        public static final FieldTypeModel ACTIVITY = new SimpleFieldTypeModel("Aktivität");
        public static final FieldTypeModel CATEGORY = new SimpleFieldTypeModel("Kategorie");
        public static final FieldTypeModel PROJECT = new SimpleFieldTypeModel("Projekt");
        public static final FieldTypeModel DETAILS = new SimpleFieldTypeModel("Details");

        private static final class SimpleFieldTypeModel extends FieldTypeModel {
            private final String _lbl;

            private SimpleFieldTypeModel(String lbl) {
                _lbl = Preconditions.checkNotNull(lbl);
            }

            public String getLabel() {
                return _lbl;
            }

            @Override
            public int hashCode() {
                return _lbl.hashCode();
            }

            @Override
            public boolean equals(Object o) {
                if (o instanceof SimpleFieldTypeModel) {
                    SimpleFieldTypeModel m = (SimpleFieldTypeModel)o;
                    return _lbl.equals(m._lbl);
                }
                return false;
            }
        }

        private static final class CustomFieldType extends FieldTypeModel {
            private final String _lbl;
            private final CustomFieldTypeModel _model;

            private CustomFieldType(CustomFieldTypeModel model) {
                _lbl = Preconditions.checkNotNull(model.getName());
                _model = model;
            }

            public String getLabel() {
                return _lbl;
            }

            @Override
            public int hashCode() {
                return (int)_model.getId();
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (o instanceof CustomFieldType) {
                    CustomFieldType cf = (CustomFieldType)o;
                    return cf._model.getId() == this._model.getId();
                }
                return false;
            }
        }

        public static FieldTypeModel custom(CustomFieldTypeModel model) {
            return new CustomFieldType(model);
        }

        public abstract String getLabel();

        @Override
        public abstract boolean equals(Object o);

        @Override
        public abstract int hashCode();
    }

    public static class V2FileFormat implements SingleFileFormat {
        public static final String MIME_TYPE = CSVFileReaderWriter.MIME_TYPE;
        private final DateTimeFormatter _timeFormatter = DateTimeFormat.forPattern("HH:mm");
        private final DateTimeFormatter _dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

        private final Context _context;
        private final String[] _header;
        private final Map<FieldTypeModel, Integer> _fieldPositions;

        public V2FileFormat(Context context, List<FieldTypeModel> customFields) {
            _context = context;
            _header = buildHeader(customFields);
            _fieldPositions = createFieldPositions(customFields);
        }

        private Map<FieldTypeModel, Integer> createFieldPositions(List<FieldTypeModel> fields) {
            ImmutableMap.Builder<FieldTypeModel, Integer> m = ImmutableMap.builder();
            for (int i = 0; i < fields.size(); i++) {
                m.put(fields.get(i), i);
            }
            return m.build();
        }

        private String[] buildHeader(List<FieldTypeModel> fields) {
            String[] header = new String[fields.size()];

            for (int i = 0; i < header.length; i++) {
                FieldTypeModel field = fields.get(i);
                header[i] = field.getLabel();
            }
            return header;
        }

        protected String getCustomFieldName(CustomFieldTypeModel field) {
            return field.getName();
        }


        @Override
        public String[] getHeader() {
            return _header;
        }

        @Override
        public char getSeparator() {
            return ',';
        }

        @Override
        public char getQuoteCharacter() {
            return '"';
        }

        @Override
        public String getLineEnding() {
            return "\r\n";
        }

        private void set(String[] result, FieldTypeModel field, Object value) {
            set(result, field, value == null? "" : value.toString());
        }

        private void set(String[] result, ActivityCustomFieldModel field) {
            CustomFieldValueModel valueModel = field.getValueModel();
            set(result, FieldTypeModel.custom(field.getTypeModel()), valueModel == null ? null : valueModel.getValue());
        }

        private void set(String[] result, FieldTypeModel field, String value) {
            if (_fieldPositions.containsKey(field)) {
                int pos = _fieldPositions.get(field);
                result[pos] = value;
            }
        }

        @Override
        public String[] write(TrackedActivityModel item) {
            CategoryModel category = item.getCategory();
            ActivityTypeModel activityType = item.getActivityType();
            ProjectModel project = item.getProject();

            long durationMillis = item.getEndTimeMillis() - item.getStartTimeMillis();
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
            String durationString = TimeFormatUtil.formatDuration(_context, durationMillis).toString();
            String[] result = new String[_header.length];
            set(result, FieldTypeModel.START, formatTime(item.getStartTimeMillis()));
            set(result, FieldTypeModel.START_DAY, formatDate(item.getStartTimeMillis()));
            set(result, FieldTypeModel.END_DAY, formatDate(item.getStartTimeMillis()));
            set(result, FieldTypeModel.END, formatTime(item.getEndTimeMillis()));
            set(result, FieldTypeModel.DURATION, durationString);
            set(result, FieldTypeModel.DURATION_MINUTES, Long.toString(durationMinutes));
            set(result, FieldTypeModel.ACTIVITY, activityType == null ? "" : activityType.getName());
            set(result, FieldTypeModel.CATEGORY, category == null ? "" : category.getName());
            set(result, FieldTypeModel.PROJECT, project == null ? "" : project.getName());
            set(result, FieldTypeModel.DETAILS, item.getDetails());

            // fill the rows with those custom fields that are known to us
            for (ActivityCustomFieldModel field : item.getCustomFieldData()) {
                set(result, field);
            }
            return result;
        }


        protected String formatTime(long millis) {
            return _timeFormatter.print(millis);
        }

        protected String formatDate(long millis) {
            return _dateFormatter.print(millis);
        }
    }


    public static final class CSVFileFormatInput {
        private final List<CustomFieldTypeModel> _models;

        public CSVFileFormatInput(List<CustomFieldTypeModel> models) {
            _models = models;
        }

        public List<CustomFieldTypeModel> getModels() {
            return _models;
        }
    }

    public static final class V1FileFormatFactory implements Function<CSVFileFormatInput, SingleFileFormat> {

        @Nullable
        @Override
        public SingleFileFormat apply(CSVFileFormatInput input) {
            return new V1FileFormat(input.getModels());
        }
    }

    /**
     * Writes CSV files with the specified type in the specified order, appending additional custom fields.
     */
    public static final class V2FileFormatFactory implements Function<CSVFileFormatInput, SingleFileFormat> {

        private final Context _context;
        private final ImmutableList<FieldTypeModel> types;

        public V2FileFormatFactory(Context context, FieldTypeModel... typeModels) {
            this(context, Arrays.asList(typeModels));
        }

        public V2FileFormatFactory(Context context, Iterable<FieldTypeModel> typeModels) {
            this.types = ImmutableList.copyOf(typeModels);
            _context = context;
        }

        @Nullable
        @Override
        public SingleFileFormat apply(CSVFileFormatInput input) {
            ImmutableList.Builder<WireFormat.FieldType> b = ImmutableList.builder();
            //for (Field)
            return new V2FileFormat(_context, types);
        }
    }

    public CSVFileReaderWriter(
            Function<CSVFileFormatInput, SingleFileFormat> fileFormatFactory
    ) {
        _fileFormatFactory = fileFormatFactory;
    }

    public void writeAll(File targetFile,
                         List<CustomFieldTypeModel> customFieldTypeModels,
                         List<TrackedActivityModel> data) throws IOException {
        writeAll(new FileOutputStream(targetFile), customFieldTypeModels, data);
    }

    public void writeAll(OutputStream stream,
                         List<CustomFieldTypeModel> customFieldTypeModels,
                         List<TrackedActivityModel> data) throws IOException {
        writeAll(_fileFormatFactory.apply(new CSVFileFormatInput(customFieldTypeModels)), stream, data);
    }

    private void writeAll(SingleFileFormat format,
                          OutputStream stream,
                          List<TrackedActivityModel> data) throws IOException {
        CSVWriter writer = null;

        try {
            writer = new CSVWriter(new OutputStreamWriter(stream), format.getSeparator(), format.getQuoteCharacter(), format.getLineEnding());

            writer.writeNext(format.getHeader());

            for (TrackedActivityModel item : data) {
                if (!item.isFakeEntry()) {
                    writer.writeNext(format.write(item));
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }


}
