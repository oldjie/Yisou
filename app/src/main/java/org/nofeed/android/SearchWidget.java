// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

/** Personal widget: one search entry and five fixed, evenly sized icon buttons. */
public class SearchWidget extends AppWidgetProvider {
    private static Context appContext;
    private static BroadcastReceiver configurationObserver;
    private static final int[] ICONS = {R.id.widget_app_0, R.id.widget_app_1, R.id.widget_app_2,
            R.id.widget_app_3, R.id.widget_app_4};
    static void initialize(Context context) {
        appContext = context.getApplicationContext();
        if (configurationObserver != null) return;
        configurationObserver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) { refresh(); }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) appContext.registerReceiver(configurationObserver, filter, Context.RECEIVER_NOT_EXPORTED);
        else appContext.registerReceiver(configurationObserver, filter);
    }
    static void add(android.app.Activity activity) {
        AppWidgetManager manager = AppWidgetManager.getInstance(activity);
        ComponentName provider = new ComponentName(activity, SearchWidget.class);
        if (manager.isRequestPinAppWidgetSupported() && manager.requestPinAppWidget(provider, null, null)) return;
        new android.app.AlertDialog.Builder(activity).setTitle("从桌面添加")
                .setMessage("在桌面长按空白处或双指捏合，进入小组件列表，找到‘一搜 · 搜索（4×1）’。\n\n华为通常位于‘服务卡片’页面最底部的‘窗口小工具’。添加后点搜索框或五个应用图标即可搜索。")
                .setPositiveButton("知道了", null).show();
    }
    static void refresh() {
        if (appContext == null) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        new SearchWidget().onUpdate(appContext, manager, manager.getAppWidgetIds(new ComponentName(appContext, SearchWidget.class)));
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        initialize(context);
        java.util.List<SearchTarget> order = AppCatalog.ordered(context);
        for (int id : ids) {
            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.search_widget_compact);
            if ("system".equals(Appearance.mode(context))) {
                view.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background);
                view.setInt(R.id.widget_query, "setBackgroundResource", R.drawable.widget_search_field);
                if (Build.VERSION.SDK_INT >= 31) view.setColor(R.id.widget_query, "setTextColor", R.color.widget_muted);
                else view.setTextColor(R.id.widget_query, Appearance.palette(context).muted);
            } else {
                boolean dark = Appearance.isDark(context);
                view.setInt(R.id.widget_root, "setBackgroundResource", dark ? R.drawable.widget_background_dark : R.drawable.widget_background_light);
                view.setInt(R.id.widget_query, "setBackgroundResource", dark ? R.drawable.widget_search_field_dark : R.drawable.widget_search_field_light);
                view.setTextColor(R.id.widget_query, Appearance.palette(context).muted);
            }
            view.setOnClickPendingIntent(R.id.widget_query, input(context, id, null));
            int size = Math.min(160, Math.round(28 * context.getResources().getDisplayMetrics().density));
            for (int i = 0; i < ICONS.length; i++) {
                SearchTarget target = order.get(i);
                view.setImageViewBitmap(ICONS[i], AppIconView.render(context, AppCatalog.icon(context, target), size));
                view.setContentDescription(ICONS[i], target.label + "，输入并搜索");
                view.setOnClickPendingIntent(ICONS[i], input(context, id, target));
            }
            manager.updateAppWidget(id, view);
        }
    }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager manager, int id, Bundle options) {
        onUpdate(c, manager, new int[]{id});
    }
    private static PendingIntent input(Context context, int id, SearchTarget target) {
        Intent intent = new Intent(context, WidgetSearchActivity.class)
                .setAction(Intent.ACTION_MAIN).setPackage(context.getPackageName())
                .setData(Uri.parse("yisou://widget/" + id + "/" + (target == null ? "query" : target.name())))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (target != null) intent.putExtra("target", target.name());
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
