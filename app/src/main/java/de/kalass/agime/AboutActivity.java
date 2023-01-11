package de.kalass.agime;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;

import de.kalass.agime.analytics.AnalyticsActionToolBarActivity;
import de.kalass.agime.util.EmailUtil;
import de.kalass.android.common.DialogUtils;

import static de.kalass.android.common.DialogUtils.showSimpleHtmlDialog;
import static de.kalass.android.common.DialogUtils.showSimpleHtmlWebviewDialog;
import static de.kalass.android.common.DialogUtils.showSimpleHtmlWebviewDialogContent;

/**
 * Created by klas on 11.02.14.
 */
public class AboutActivity extends AnalyticsActionToolBarActivity {
    private static final String VERSION_UNAVAILABLE = "N/A";
    private static final String LOG_TAG = "AboutActivity";

    public AboutActivity() {
        super(R.layout.activity_about);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView top = (TextView)findViewById(R.id.about_top);
        TextView bottom = (TextView)findViewById(R.id.about_bottom);
        TextView credits = (TextView)findViewById(R.id.about_bottom_credits);
        TextView impressum = (TextView)findViewById(R.id.about_bottom_impressum);

        // Get app version
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        // Build the about body view and append the link to see OSS licenses
        SpannableStringBuilder aboutBodyTop = new SpannableStringBuilder();
        aboutBodyTop.append(Html.fromHtml(getString(R.string.about_body_top, versionName)));

        appendLink(aboutBodyTop, R.string.about_feedback, R.string.about_feedback_description, new ClickableSpan() {
            @Override
            public void onClick(View view) {
                sendFeedbackEmail();
            }
        });

        appendLink(aboutBodyTop, R.string.about_bugs, R.string.about_bugs_description, new ClickableSpan() {
            @Override
            public void onClick(View view) {
                sendBugReportEmail();
            }
        });

        SpannableStringBuilder aboutBodyBottom = new SpannableStringBuilder();
        aboutBodyBottom.append(getString(R.string.about_body_bottom));

        /*
         * Apparently, AGBs are not needed - and since I do not have my own AGBs, I will
         * use the default provided by german law.
         */
        /**
        appendLink(aboutBodyBottom, R.string.about_eula, new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showEula(AboutActivity.this);
            }
        });
*/
        appendLink(aboutBodyBottom, R.string.about_privacy, new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showPrivacyPolicy(AboutActivity.this);
            }
        });

        appendLink(aboutBodyBottom, R.string.about_licenses, new ClickableSpan() {
            @Override
            public void onClick(View view) {
                showOpenSourceLicenses(AboutActivity.this);
            }
        });

        top.setText(aboutBodyTop);
        top.setMovementMethod(LinkMovementMethod.getInstance());
        bottom.setText(aboutBodyBottom);
        bottom.setMovementMethod(LinkMovementMethod.getInstance());
        credits.setText(Html.fromHtml(getString(R.string.about_credits)));
        credits.setMovementMethod(LinkMovementMethod.getInstance());

        impressum.setText(Html.fromHtml(getString(R.string.about_impressum)));
        impressum.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String getSupportEmailBodyDetails() {
        final PackageInfo packageInfo = getPackageInfo();
        final String appVersion = packageInfo == null ? "?" : packageInfo.versionName;
        return getString(R.string.supportEmailBody,
                appVersion,
                Build.MODEL,
                Build.MANUFACTURER,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT
        );
    }

    private String getSupportEmailTitle(int resId) {
        final PackageInfo packageInfo = getPackageInfo();

        final String appVersion = packageInfo == null ? "?" : packageInfo.versionName;
        return getString(resId, appVersion);
    }

    private PackageInfo getPackageInfo() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOG_TAG, "Could not get package info about ourselves", e);
            return null;
        }
    }

    private void sendFeedbackEmail() {
        EmailUtil.showEmailChooser(this,
                Consts.SUPPORT_EMAIL_ADDRESS,
                getSupportEmailTitle(R.string.mail_chooser_feedback),
                getSupportEmailTitle(R.string.mail_subject_feedback),
                getSupportEmailBodyDetails());
    }

    private void sendBugReportEmail() {
        EmailUtil.showEmailChooser(this,
                Consts.SUPPORT_EMAIL_ADDRESS,
                getSupportEmailTitle(R.string.mail_chooser_bugreport),
                getSupportEmailTitle(R.string.mail_subject_bugreport),
                getSupportEmailBodyDetails());
    }

    private void appendLink(SpannableStringBuilder aboutBody, int linkTextResId, ClickableSpan callback) {
        SpannableString link = new SpannableString(getString(linkTextResId));
        link.setSpan(callback, 0, link.length(), 0);
        aboutBody.append("\n\n");
        aboutBody.append(link);
    }

    private void appendLink(SpannableStringBuilder aboutBody, int linkTextResId, int descrResId, ClickableSpan callback) {
        SpannableString link = new SpannableString(getString(linkTextResId));
        link.setSpan(callback, 0, link.length(), 0);
        aboutBody.append("\n\n");
        aboutBody.append(link);
        aboutBody.append("\n");
        aboutBody.append(getString(descrResId));
    }

    public static void showOpenSourceLicenses(FragmentActivity activity) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style> body { font-family: sans-serif; } pre { background-color: #eeeeee; padding: 1em; white-space: pre-wrap;word-wrap: break-word } </style></head><body>");
        String furtherLicenses = read(activity);
        sb.append(furtherLicenses);
        if (Consts.INCLUDE_GOOGLE_PLAY_SERVICES) {
            sb.append("<h3>Google Play Services</h3><pre>");
            sb.append(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity));
            sb.append("</pre>");
        }
        sb.append("</body></html>");

        showSimpleHtmlWebviewDialogContent(activity,
                "dialog_licenses",
                R.string.about_licenses,
                sb.toString());
    }

    private static String read(FragmentActivity activity) {
        String furtherLicenses = "";
        try {
            final InputStreamReader reader =  new InputStreamReader(activity.getAssets().open("licenses.html"), Charsets.UTF_8);
            try {

                furtherLicenses = CharStreams.toString(reader);
                Log.i(LOG_TAG, "has Linebreaks: " + furtherLicenses.contains("\n"));
                Log.i(LOG_TAG, "text: " + furtherLicenses);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to open license file", e);
            DialogUtils.showError(activity, R.string.about_licenses_failed);
        }
        return furtherLicenses;
    }


    public static void showPrivacyPolicy(FragmentActivity activity) {
        showSimpleHtmlWebviewDialog(activity,
                "dialog_privacy_policy",
                R.string.about_privacy,
                R.raw.privacy_policy
                );
    }

    public static void showEula(FragmentActivity activity) {
        showSimpleHtmlDialog(activity, "dialog_eula", R.string.about_eula, R.string.eula_text);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Do not call super: the superclass adds an entry for this about activity...

        // currently there is no menu
        return false;
    }
}
