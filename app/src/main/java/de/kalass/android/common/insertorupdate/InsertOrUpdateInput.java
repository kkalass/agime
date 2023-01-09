package de.kalass.android.common.insertorupdate;

/**
* Created by klas on 21.01.14.
*/
public class InsertOrUpdateInput {
    final Long activityTypeId;
    final String activityTypeName;

    final Long categoryTypeId;
    final String categoryName;
    final Integer categoryColor;


    public InsertOrUpdateInput(Long activityTypeId, String activityTypeName,
                               Long categoryTypeId, String categoryName, Integer categoryColor
    ) {
        this.activityTypeId = activityTypeId;
        this.activityTypeName = activityTypeName;
        this.categoryTypeId = categoryTypeId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
    }
}
