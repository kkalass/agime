package de.kalass.agime.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

import de.kalass.agime.Consts;
import de.kalass.agime.R;
import de.kalass.android.common.DialogUtils;

/**
 * Created by klas on 11.02.14.
 */
public class EmailUtil {


    public static void showEmailChooser(Context context, String emailAddress, String chooserText,
                                        String subject, String text) {
        if (!showIntentChooser(context,createSendIntent(emailAddress, subject, text), chooserText)){
            if (!showIntentChooser(context,createSendToIntent(emailAddress, subject, text), chooserText)) {
                DialogUtils.showError(context, R.string.mail_chooser_no_mail_clients);
            }
        }
    }

    private static boolean showIntentChooser(Context context, Intent sendIntent, String chooserText) {
        final List<ResolveInfo> sendIntentResolve = context.getPackageManager().queryIntentActivities(sendIntent, 0);
        if (hasActivities(sendIntentResolve)) {
            context.startActivity(Intent.createChooser(sendIntent, chooserText));
            return true;
        }
        return false;
    }

    @NonNull
    private static Intent createSendToIntent(String emailAddress, String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + emailAddress +"?subject=" + Uri.encode(subject)+"&body="+Uri.encode(text)));
        return intent;
    }

    @NonNull
    private static Intent createSendIntent(String emailAddress, String subject, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        return intent;
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
