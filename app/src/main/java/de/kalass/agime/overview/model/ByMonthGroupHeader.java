package de.kalass.agime.overview.model;

import android.content.Context;
import android.content.res.Resources;

import org.joda.time.DateTime;

import de.kalass.agime.R;
import de.kalass.android.common.util.TimeFormatUtil;

/**
 * Created by klas on 14.01.14.
 */
class ByMonthGroupHeader extends GroupHeader {
    static final int GROUP_TYPE = 7;
    private Context context;
    private DateTime _dateTime;

    public ByMonthGroupHeader(Context context, DateTime dateTime) {
        super(GROUP_TYPE, dateTime == null ? DEFAULT_ITEM_ID : (dateTime.getYear() << 8) + dateTime.getMonthOfYear());
        this.context = context;

        _dateTime = dateTime;
    }

    @Override
    public String getLevel4Line() {
        // not shown in level 4
        return null;
    }

    public String getDisplayName(Resources resources) {
        return _dateTime == null ? resources.getString(R.string.activity_no_day_default) : TimeFormatUtil.formatMonthForHeader(context, _dateTime.toLocalDate()).toString();
    }

    public Integer getColorCode(Resources resources) {
        return null;
    }

    @Override
    public int compareTo(GroupHeader another) {
        if (another instanceof ByMonthGroupHeader) {
            ByMonthGroupHeader o = (ByMonthGroupHeader)another;
            return _dateTime.compareTo(o._dateTime);
        }
        return super.compareTo(another);
    }
}
