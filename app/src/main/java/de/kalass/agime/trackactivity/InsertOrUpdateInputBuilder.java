package de.kalass.agime.trackactivity;

import java.util.List;

import de.kalass.agime.acquisitiontime.RecurringDAO;

public class InsertOrUpdateInputBuilder {
    private Long entityId;
    private String details;
    private long startTimeMillis;
    private long endTimeMillis;
    private Long activityTypeId;
    private String activityTypeName;
    private Long categoryTypeId;
    private String categoryName;
    private Integer categoryColor;
    private Long projectId;
    private String projectName;
    private Integer projectColor;
    private List<InsertOrUpdateOperationFactory> additional;
    private Long originalInsertDurationMillis;
    private Long originalUpdateDurationMillis;
    private Long originalUpdateCount;
    private List<RecurringDAO.Data> acquisitionTimeCandidates;

    public InsertOrUpdateInputBuilder setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }

    public InsertOrUpdateInputBuilder setDetails(String details) {
        this.details = details;
        return this;
    }

    public InsertOrUpdateInputBuilder setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
        return this;
    }

    public InsertOrUpdateInputBuilder setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
        return this;
    }

    public InsertOrUpdateInputBuilder setActivityTypeId(Long activityTypeId) {
        this.activityTypeId = activityTypeId;
        return this;
    }

    public InsertOrUpdateInputBuilder setActivityTypeName(String activityTypeName) {
        this.activityTypeName = activityTypeName;
        return this;
    }

    public InsertOrUpdateInputBuilder setCategoryTypeId(Long categoryTypeId) {
        this.categoryTypeId = categoryTypeId;
        return this;
    }

    public InsertOrUpdateInputBuilder setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public InsertOrUpdateInputBuilder setCategoryColor(Integer categoryColor) {
        this.categoryColor = categoryColor;
        return this;
    }

    public InsertOrUpdateInputBuilder setProjectId(Long projectId) {
        this.projectId = projectId;
        return this;
    }

    public InsertOrUpdateInputBuilder setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public InsertOrUpdateInputBuilder setProjectColor(Integer projectColor) {
        this.projectColor = projectColor;
        return this;
    }

    public InsertOrUpdateInputBuilder setAdditional(List<InsertOrUpdateOperationFactory> additional) {
        this.additional = additional;
        return this;
    }

    public InsertOrUpdateInputBuilder setOriginalInsertDurationMillis(Long originalInsertDurationMillis) {
        this.originalInsertDurationMillis = originalInsertDurationMillis;
        return this;
    }

    public InsertOrUpdateInputBuilder setOriginalUpdateDurationMillis(Long originalUpdateDurationMillis) {
        this.originalUpdateDurationMillis = originalUpdateDurationMillis;
        return this;
    }

    public InsertOrUpdateInputBuilder setOriginalUpdateCount(Long originalUpdateCount) {
        this.originalUpdateCount = originalUpdateCount;
        return this;
    }

    public InsertOrUpdateInput createInsertOrUpdateInput() {
        return new InsertOrUpdateInput(
                entityId,
                details, startTimeMillis, endTimeMillis, activityTypeId, activityTypeName,
                categoryTypeId, categoryName, categoryColor, projectId, projectName,
                projectColor, additional, acquisitionTimeCandidates,
                originalInsertDurationMillis, originalUpdateDurationMillis, originalUpdateCount);
    }

    public InsertOrUpdateInputBuilder setAcquisitionTimeCandidates(List<RecurringDAO.Data> acquisitionTimeCandidates) {
        this.acquisitionTimeCandidates = acquisitionTimeCandidates;
        return this;
    }
}