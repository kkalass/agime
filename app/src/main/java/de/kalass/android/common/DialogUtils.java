/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Adapted from https://iosched.googlecode.com/git/android/src/main/java/com/google/android/apps/iosched/util/HelpUtils.java
 */
package de.kalass.android.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import de.kalass.agime.R;

/**
 * This is a set of helper methods for showing simple dialogs.
 */
public final class DialogUtils {

    private static final String LOG_TAG = "DialogUtils";

    private DialogUtils() {

    }

    public static void showError(Context context, String errorMessage) {
        showError(context, context.getString(R.string.error_dialog_title), errorMessage);
    }

    public static void showError(Context context, int errorMessageResId) {
        showError(context, R.string.error_dialog_title, errorMessageResId);
    }

    public static void showError(Context context, String errorTitle, String errorMessage) {
        new AlertDialog.Builder(context)
                .setTitle(errorTitle)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // not much to do here
                    }
                })
                .show();
    }

    public static void showError(Context context, int errorTitleResId, int errorMessageResId) {
        showError(context, context.getString(errorTitleResId), context.getString(errorMessageResId));
    }

    public static void showSimpleHtmlDialog(FragmentActivity activity,
                                            String tagName,
                                            int titleResourceId, int textResourceId
    ) {
        FragmentTransaction ft = prepareShowDialog(activity, tagName);
        showDialog(new SimpleHTMLDialog(), SimpleHTMLDialog.args(titleResourceId, textResourceId), ft, tagName);
    }

    public static void showSimpleHtmlWebviewDialog(FragmentActivity activity,
                                            String tagName,
                                            int titleResourceId, int rawResId
    ) {
        FragmentTransaction ft = prepareShowDialog(activity, tagName);
        showDialog(new SimpleHTMLWebviewDialog(), SimpleHTMLWebviewDialog.args(titleResourceId, rawResId), ft, tagName);
    }
    public static void showSimpleHtmlWebviewDialogContent(FragmentActivity activity,
                                                   String tagName,
                                                   int titleResourceId, String contentString
    ) {
        FragmentTransaction ft = prepareShowDialog(activity, tagName);
        showDialog(new SimpleHTMLWebviewDialog(), SimpleHTMLWebviewDialog.argsContent(titleResourceId, contentString), ft, tagName);
    }

    public static void showSimpleInfoDialog(FragmentActivity activity,
                                            String tagName,
                                            int titleResourceId, int textResourceId
    ) {
        FragmentTransaction ft = prepareShowDialog(activity, tagName);
        showDialog(new SimpleInfoDialog(), SimpleInfoDialog.args(titleResourceId, textResourceId), ft, tagName);
    }

    public static FragmentTransaction prepareShowDialog(FragmentActivity activity, String tagName) {
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(tagName);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        return ft;
    }

    private static void showDialog(DialogFragment fragment, Bundle args, FragmentTransaction ft, String tagName) {
        fragment.setArguments(args);
        fragment.show(ft, tagName);
    }

    public static class SimpleHTMLWebviewDialog extends DialogFragment {

        public static Bundle args(int titleResourceId, int rawResId) {
            Bundle b = new Bundle();
            b.putInt("titleResourceId", titleResourceId);
            b.putInt("rawResId", rawResId);
            return b;
        }
        public static Bundle argsContent(int titleResourceId, String content) {
            Bundle b = new Bundle();
            b.putInt("titleResourceId", titleResourceId);
            b.putString("content", content);
            return b;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int titleResourceId = getArguments().getInt("titleResourceId");

            String content = getArguments().getString("content");
            WebView webView = new WebView(getActivity());

            if (getArguments().containsKey("rawResId")) {
                int rawResId = getArguments().getInt("rawResId");
                try {
                    String text = read(getActivity(), rawResId);
                    webView.loadDataWithBaseURL(null, text, "text/html", "utf-8", null);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

            } else {
                Preconditions.checkNotNull(content);
                try {
                    final String data = encodeForWebview(content);
                    //webView.loadData(data, "text/html", "utf-8");
                    webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
            return new AlertDialog.Builder(getActivity())
                    .setTitle(titleResourceId)
                    .setView(webView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }

        private String encodeForWebview(String content) throws UnsupportedEncodingException {
            return URLEncoder.encode(content, "utf-8").replaceAll("\\+", "%20");
        }
    }

    private static String read(Context context, int resId) throws IOException {
        final InputStreamReader reader =  new InputStreamReader(context.getResources().openRawResource(resId), Charsets.UTF_8);
        try {

            return CharStreams.toString(reader);
        } finally {
            reader.close();
        }
    }

    public static class SimpleInfoDialog extends DialogFragment {

        public static Bundle args(int titleResourceId, int textResourceId) {
            Bundle b = new Bundle();
            b.putInt("titleResourceId", titleResourceId);
            b.putInt("textResourceId", textResourceId);
            return b;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int titleResourceId = getArguments().getInt("titleResourceId");
            int textResourceId = getArguments().getInt("textResourceId");

            LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            final View view = layoutInflater.inflate(R.layout.simple_info_dialog, null);
            TextView heading = (TextView)view.findViewById(R.id.heading);
            TextView details = (TextView)view.findViewById(R.id.details);

            heading.setText(titleResourceId);
            details.setText(textResourceId);

            return new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

    public static class SimpleHTMLDialog extends DialogFragment {

        public static Bundle args(int titleResourceId, int textResourceId) {
            Bundle b = new Bundle();
            b.putInt("titleResourceId", titleResourceId);
            b.putInt("textResourceId", textResourceId);
            return b;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int titleResourceId = getArguments().getInt("titleResourceId");
            int textResourceId = getArguments().getInt("textResourceId");

            int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_normal);

            TextView bodyView = new TextView(getActivity());

            bodyView.setText(Html.fromHtml(getString(textResourceId)));
            bodyView.setMovementMethod(LinkMovementMethod.getInstance());
            bodyView.setPadding(padding, padding, padding, padding);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(titleResourceId)
                    .setView(bodyView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}