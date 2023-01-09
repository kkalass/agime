package de.kalass.agime.activitytype;

public class InsertOrUpdateInputBuilder {
    private String activityTypeName;
    private Long categoryTypeId;
    private String categoryName;
    private Integer categoryColor;
    private Long entityId;

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

    public InsertOrUpdateInput createInsertOrUpdateInput() {
        return new InsertOrUpdateInput(entityId, activityTypeName, categoryTypeId, categoryName, categoryColor);
    }

    public InsertOrUpdateInputBuilder setEntityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }
}