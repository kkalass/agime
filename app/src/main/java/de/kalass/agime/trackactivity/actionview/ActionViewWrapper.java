package de.kalass.agime.trackactivity.actionview;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.kalass.agime.R;
import de.kalass.android.common.activity.BaseViewWrapper;

/**
* Created by klas on 31.01.14.
*/
public final class ActionViewWrapper extends BaseViewWrapper {

    //public static final int LAYOUT = R.layout.tracked_activities_action_header;
    public static final int ID_ROOT = R.id.actionview;
    public static final int ID_HEADING = R.id.heading;
    public static final int ID_HEADING_TEXT = R.id.heading_text;
    public static final int ID_HEADING_TEXT_ICON = R.id.heading_text_icon;
    public static final int ID_DETAILS  = R.id.details;
    public static final int ID_BUTTONS = R.id.buttons;
    public static final int ID_BUTTON1 = R.id.button;
    public static final int ID_BUTTON2 = R.id.button2;

    public final View root;
    public final View heading;
    public final TextView headingText;
    public final View headingTextIcon;
    public final TextView details;
    public final LinearLayout buttons;
    public final Button button1;
    public final TextView button2;

    private int lastButtonsVisibility;

    public ActionViewWrapper(View view) {
        super(view);
        root = getView(ID_ROOT);
        heading = getView(ID_HEADING);
        headingText = getTextView(ID_HEADING_TEXT);
        headingTextIcon = getView(ID_HEADING_TEXT_ICON);
        details = getTextView(ID_DETAILS);
        buttons = getLinearLayout(ID_BUTTONS);
        button1 = getButton(ID_BUTTON1);
        button2 = getTextView(ID_BUTTON2);
    }

    public void setButtonsVisibility(int visibility) {
        buttons.setVisibility(visibility);
        lastButtonsVisibility = visibility;
    }
    public void restoreButtonsVisibility() {
        buttons.setVisibility(lastButtonsVisibility);
    }

}
