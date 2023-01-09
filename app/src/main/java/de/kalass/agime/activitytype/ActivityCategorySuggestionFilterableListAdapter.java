package de.kalass.agime.activitytype;

import android.content.Context;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.List;

import de.kalass.agime.ColorSuggestion;
import de.kalass.agime.R;
import de.kalass.agime.loader.ActivityCategorySuggestionSyncLoader;
import de.kalass.agime.model.ActivityCategorySuggestionModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.adapter.AbstractViewModelListAdapter;

/**
* Created by klas on 06.11.13.
*/
class ActivityCategorySuggestionFilterableListAdapter
    extends AbstractViewModelListAdapter<ActivityCategorySuggestionModel>
    implements Filterable
{

    private final Context context;

    public ActivityCategorySuggestionFilterableListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        this.context = context;
    }

    @Override
    protected View fillView(View view, ActivityCategorySuggestionModel model, int position) {
        TextView titleView = (TextView)view.findViewById(android.R.id.text1);

        view.setBackgroundColor(ColorSuggestion.getCategoryColor(context.getResources(), model.getCategory()));
        titleView.setTextColor(context.getResources().getColor(R.color.category_text));
        titleView.setText(model.getName());
        return view;
    }


    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ActivityCategorySuggestionSyncLoader loader = new ActivityCategorySuggestionSyncLoader(context);
                try {
                    List<ActivityCategorySuggestionModel> models = loader.loadByName(constraint);
                    FilterResults results = new FilterResults();
                    results.count = models.size();
                    results.values = models;
                    return results;
                } finally {
                   loader.close();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                ActivityCategorySuggestionModel result = (ActivityCategorySuggestionModel)resultValue;
                return result.getName();
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                setItems((List<ActivityCategorySuggestionModel>)results.values);
            }
        };
    }
}
