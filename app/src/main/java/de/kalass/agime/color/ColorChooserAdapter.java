package de.kalass.agime.color;

import android.content.Context;
import android.view.View;

import com.google.common.collect.ImmutableList;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.android.common.adapter.AbstractListAdapter;

/**
* Created by klas on 22.11.13.
*/
public class ColorChooserAdapter extends AbstractListAdapter<Integer> {

    public ColorChooserAdapter(Context context) {
        super(context, R.layout.color_chooser, R.layout.color_chooser);
    }

    private static List<Integer> mix(Integer colorCode, List<Integer> all) {
        if (colorCode == null || all.contains(colorCode)) {
            return all;
        }
        ImmutableList.Builder<Integer> colors = ImmutableList.builder();
        colors.add(colorCode);
        colors.addAll(all);
        return colors.build();
    }

    public void setColor(Integer colorCode) {
        final List<Integer> colorCodes = mix(colorCode, ColorSuggestion.getAll(getContext().getResources()));
        setItems(colorCodes);
    }

    @Override
    protected View fillView(View view, Integer model, int position) {
        view.findViewById(R.id.color).setBackgroundColor(model.intValue());
        return view;
    }

    public int getColorPosition(Integer colorCode) {
        if (colorCode == null) {
            return -1;
        }
        for (int i = 0; i < getCount(); i++) {
            Integer color = (Integer)getItem(i);
            if (color != null && color.intValue() == colorCode) {
                return i;
            }
        }
        return -1;
    }

}
