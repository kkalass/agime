package de.kalass.agime.activitytype;

/**
* Created by klas on 21.01.14.
*/
public class InsertOrUpdateInput {
    final Long entityId;

    final String activityTypeName;

    final Long categoryTypeId;
    final String categoryName;
    final Integer categoryColor;


    public InsertOrUpdateInput(Long entityId, String activityTypeName,
                               Long categoryTypeId, String categoryName, Integer categoryColor
    ) {
        this.entityId = entityId;
        this.activityTypeName = activityTypeName;
        this.categoryTypeId = categoryTypeId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
    }
}
