package de.kalass.agime.trackactivity;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;

/**
* Created by klas on 21.01.14.
*/
public class InsertOrUpdateInput {
    final Long entityId;

    final String details;
    final long startTimeMillis;
    final long endTimeMillis;

    final Long activityTypeId;
    final String activityTypeName;

    final Long categoryTypeId;
    final String categoryName;
    final Integer categoryColor;

    final Long projectId;
    final String projectName;
    final Integer projectColor;
    final List<InsertOrUpdateOperationFactory> additional;

    final Long originalInsertDurationMillis;
    final Long originalUpdateDurationMillis;
    final Long originalUpdateCount;

    final List<RecurringDAO.Data> acquisitionTimeCandidates;

    public InsertOrUpdateInput(Long entityId,
                               String details, long startTimeMillis,
                               long endTimeMillis, Long activityTypeId, String activityTypeName,
                               Long categoryTypeId, String categoryName, Integer categoryColor,
                               Long projectId, String projectName, Integer projectColor,
                               List<InsertOrUpdateOperationFactory> additional,
                               List<RecurringDAO.Data> acquisitionTimeCandidates,
                               Long originalInsertDurationMillis, Long originalUpdateDurationMillis, Long originalUpdateCount) {
        this.entityId = entityId;
        this.details = details;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.activityTypeId = activityTypeId;
        this.activityTypeName = activityTypeName;
        this.categoryTypeId = categoryTypeId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectColor = projectColor;
        this.additional = additional;
        this.acquisitionTimeCandidates = acquisitionTimeCandidates;
        this.originalInsertDurationMillis = originalInsertDurationMillis;
        this.originalUpdateDurationMillis = originalUpdateDurationMillis;
        this.originalUpdateCount = originalUpdateCount;
    }
}
