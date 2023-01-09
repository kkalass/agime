package de.kalass.agime.overview.model;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Locale;

import de.kalass.agime.R;
import de.kalass.agime.model.ProjectModel;
import de.kalass.agime.provider.MCContract;
import de.kalass.android.common.util.DateUtil;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 14.01.14.
 */
class ByDayGroupHeader extends GroupHeader {
    static final int GROUP_TYPE = 5;
    private Context context;
    private DateTime _dateTime;

    public ByDayGroupHeader(Context context, DateTime dateTime) {
        super(GROUP_TYPE, dateTime == null ? DEFAULT_ITEM_ID : dateTime.withTimeAtStartOfDay().getMillis());
        this.context = context;

        _dateTime = dateTime;
    }

    @Override
    public String getLevel4Line() {
        // not shown in level 4
        return null;
    }

    public String getDisplayName(Resources resources) {
        return _dateTime == null ? resources.getString(R.string.activity_no_day_default) : TimeFormatUtil.formatTimeForHeader(context, _dateTime.toLocalDate()).toString();
    }

    public Integer getColorCode(Resources resources) {
        return null;
    }

    @Override
    public int compareTo(GroupHeader another) {
        if (another instanceof ByDayGroupHeader) {
            ByDayGroupHeader o = (ByDayGroupHeader)another;
            return _dateTime.compareTo(o._dateTime);
        }
        return super.compareTo(another);
    }
}
