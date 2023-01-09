package de.kalass.agime.overview;

import android.net.Uri;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckForNull;

import de.kalass.agime.backup.CSVFileReaderWriter;
import de.kalass.agime.customfield.CustomFieldTypeModel;
import de.kalass.agime.model.TrackedActivityModel;
import de.kalass.agime.overview.model.GroupHeaderType;
import de.kalass.agime.overview.model.GroupHeaderTypes;
import de.kalass.agime.overview.model.OverviewConfiguration;

/**
 * Created by klas on 14.07.15.
 */
public final class ExportCSVInput {
    final List<CustomFieldTypeModel> customFields;
    final List<CSVFileReaderWriter.FieldTypeModel> csvFields;
    final List<TrackedActivityModel> activities;
    @CheckForNull
    final Uri outputUri;


    ExportCSVInput(
            OverviewConfiguration configuration,
            List<TrackedActivityModel> activities
    ) {
        this(null, configuration, activities);
    }

    public ExportCSVInput(
            Uri outputUri,
            OverviewConfiguration configuration,
            List<TrackedActivityModel> activities
    ) {
        this.outputUri = outputUri;
        this.activities = activities;

        final ImmutableList.Builder<CustomFieldTypeModel> customFieldTypeModels = ImmutableList.builder();
        final ImmutableList.Builder<CSVFileReaderWriter.FieldTypeModel> fieldTypeModels = ImmutableList.builder();
        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.START_DAY);
        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.START);
        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.END);
        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.DURATION);
        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.DURATION_MINUTES);
        addFieldType(fieldTypeModels, customFieldTypeModels, configuration.getLevel1(), configuration.getLevel2(), configuration.getLevel3());

        fieldTypeModels.add(CSVFileReaderWriter.FieldTypeModel.DETAILS);

        addFieldType(fieldTypeModels, customFieldTypeModels, configuration.getLevel4());

        this.csvFields = fieldTypeModels.build();
        this.customFields = customFieldTypeModels.build();
    }



    private void addFieldType(ImmutableList.Builder<CSVFileReaderWriter.FieldTypeModel> fieldTypeModels,
                              ImmutableList.Builder<CustomFieldTypeModel> customFieldTypeModels,
                              Iterable<? extends GroupHeaderType> headerTypes
    ) {
        for (GroupHeaderType type : headerTypes) {
            CSVFileReaderWriter.FieldTypeModel typeModel = asFieldTypeModel(type);
            if (typeModel != null) {
                fieldTypeModels.add(typeModel);
            }
            CustomFieldTypeModel customField = asCustomField(type);
            if (customField != null) {
                customFieldTypeModels.add(customField);
            }
        }
    }

    private CSVFileReaderWriter.FieldTypeModel asFieldTypeModel(GroupHeaderType type) {
        if (type instanceof GroupHeaderTypes.CustomFieldType) {
            GroupHeaderTypes.CustomFieldType custom = (GroupHeaderTypes.CustomFieldType) type;
            return CSVFileReaderWriter.FieldTypeModel.custom(custom.getTypeModel());
        }
        if (type instanceof GroupHeaderTypes.Category) {
            return CSVFileReaderWriter.FieldTypeModel.CATEGORY;
        }
        if (type instanceof GroupHeaderTypes.Project) {
            return CSVFileReaderWriter.FieldTypeModel.PROJECT;
        }
        if (type instanceof GroupHeaderTypes.ActivityType) {
            return CSVFileReaderWriter.FieldTypeModel.ACTIVITY;
        }
        return null;
    }


    private CustomFieldTypeModel asCustomField(GroupHeaderType type) {
        if (type instanceof GroupHeaderTypes.CustomFieldType) {
            GroupHeaderTypes.CustomFieldType custom = (GroupHeaderTypes.CustomFieldType) type;
            return custom.getTypeModel();
        }
        return null;
    }

    private void addFieldType(ImmutableList.Builder<CSVFileReaderWriter.FieldTypeModel> fieldTypeModels,
                              ImmutableList.Builder<CustomFieldTypeModel> customFieldTypeModels,
                              GroupHeaderType... headerTypes
    ) {
        addFieldType(fieldTypeModels, customFieldTypeModels, Arrays.asList(headerTypes));
    }

}
