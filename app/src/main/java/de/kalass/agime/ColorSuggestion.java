package de.kalass.agime;

import android.content.res.Resources;
import android.graphics.Color;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.kalass.agime.model.CategoryModel;
import de.kalass.agime.model.ProjectModel;

/**
 * Created by klas on 14.11.13.
 */
public class ColorSuggestion {
    public static int suggest(
            int[] colors, int id
    ) {
        int idx = id % colors.length;
        return colors[idx];
    }

    public static int getCategoryColor(Resources resources, CategoryModel category) {
        if (category == null) {
            return resources.getIntArray(R.array.category_colors)[0];
        }
        return getCategoryColor(resources, (int)category.getId(), category.getColour());
    }

    public static int getCategoryColor(Resources resources, int id, int categoryColor) {
        //return suggestCategoryColor(resources, id);
        return categoryColor;
    }

    public static int suggestCategoryColor(Resources resources, int id) {
        return suggest(resources.getIntArray(R.array.category_colors), id + 1 /*skipping category 1 default, because that is the fallback*/);
    }

    public static int getProjectColor(Resources resources, ProjectModel projectModel) {
        if (projectModel == null) {
            return resources.getColor(R.color.project_background_default);
        }
        return suggestProjectColor(resources, (int) projectModel.getId(), projectModel.getColorCode());
    }

    public static int suggestProjectColor(Resources resources, int id, Integer colorCode) {
        if (colorCode != null) {
            return colorCode.intValue();
        }

        return suggest(resources.getIntArray(R.array.project_colors), id);
    }

    public static List<Integer> getAll(Resources resources) {
        LinkedHashSet<Integer> result = new LinkedHashSet<Integer>(100);

        int[] colors = resources.getIntArray(R.array.material_all_colors);

        for (int ci : colors) {
            result.add(ci);
        }

        return ImmutableList.copyOf(result);
    }
}
