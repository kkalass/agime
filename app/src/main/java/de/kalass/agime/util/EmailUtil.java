package de.kalass.agime.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import de.kalass.agime.Consts;
import de.kalass.agime.R;
import de.kalass.android.common.DialogUtils;

/**
 * Created by klas on 11.02.14.
 */
public class EmailUtil {

    private static final String LOG_TAG = "EmailUtil";

    public static void showEmailChooser(Context context, String emailAddress, String chooserText,
                                        String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);

        //intent.setType("message/rfc822");
        intent.setData(Uri.parse("mailto:" + emailAddress));
        //intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        Log.i(LOG_TAG, "Will query matching activities");

        final List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        Log.i(LOG_TAG, "result: " + resolveInfos);

        if (!hasActivities(resolveInfos)) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("message/rfc822");
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            final List<ResolveInfo> sendIntentResolve = context.getPackageManager().queryIntentActivities(sendIntent, 0);
            if (!hasActivities(sendIntentResolve)) {
               DialogUtils.showError(context, R.string.mail_chooser_no_mail_clients);
            } else {
                context.startActivity(Intent.createChooser(sendIntent, chooserText));
            }
        } else {
            context.startActivity(Intent.createChooser(intent, chooserText));
        }


    }

    private static boolean hasActivities(List<ResolveInfo> resolveInfos) {
        final int size = resolveInfos.size();
        if (size > 1) {
            return true;
        }
        if (size == 0) {
            return false;
        }
        final ResolveInfo resolveInfo = resolveInfos.get(0);
        if ("com.android.fallback.Fallback".equals(resolveInfo.activityInfo.name)) {
            // ignore special activity that is only present on emulator
            return false;
        }
        return true;
    }
}
