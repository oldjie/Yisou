// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Exercises production storage using only this disposable fixture's private data. */
public final class LocalStoreProbeActivity extends Activity {
    private static final String OWNER = "org.nofeed.widgetfixture";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean passed = false;
        SharedPreferences fixturePrefs = null;
        try {
            // Java package access does not determine Android's storage owner.
            // Verify the fixture identity before touching its local preference file.
            check(OWNER.equals(getPackageName()));
            fixturePrefs = getSharedPreferences("local", MODE_PRIVATE);
            check(fixturePrefs.edit().clear().commit());
            verify(new LocalStore(this));
            passed = true;
        } catch (Exception | AssertionError failure) {
            // Do not log exception text: a JSON failure could contain a query.
        } finally {
            if (fixturePrefs != null) fixturePrefs.edit().clear().apply();
        }
        String status = passed ? "LOCAL_STORE_PASS" : "LOCAL_STORE_FAIL";
        TextView result = new TextView(this);
        result.setText(status); result.setContentDescription(status);
        result.setTextSize(20); result.setPadding(24, 48, 24, 24);
        setContentView(result);
        Log.i("YisouStorageProbe", status);
    }

    private static void verify(LocalStore store) throws Exception {
        SharedPreferences prefs = store.prefs;
        long before = System.currentTimeMillis();
        String firstQuery = "fixture-only-中文 &+%";
        store.record(SearchTarget.DOUBAN, firstQuery, false);
        JSONArray history = store.history();
        check(prefs.getLong("count", 0) == 1 && history.length() == 1);
        entry(history.getJSONObject(0), SearchTarget.DOUBAN, firstQuery, false);
        long time = history.getJSONObject(0).getLong("time");
        check(time >= before && time <= System.currentTimeMillis());

        store.record(SearchTarget.TAOBAO, "fixture-second", true);
        history = store.history();
        check(prefs.getLong("count", 0) == 2 && history.length() == 2);
        entry(history.getJSONObject(0), SearchTarget.TAOBAO, "fixture-second", true);
        entry(history.getJSONObject(1), SearchTarget.DOUBAN, firstQuery, false);

        // Seed the boundary once instead of recording hundreds of times on the UI thread.
        JSONArray seed = new JSONArray();
        for (int i = 0; i < 199; i++) seed.put(new JSONObject()
                .put("target", SearchTarget.XIANYU.name()).put("query", "fixture-seed-" + i)
                .put("time", i).put("web", false));
        check(prefs.edit().putString("history", seed.toString()).commit());
        store.record(SearchTarget.DOUBAN, "fixture-at-cap", false);
        history = store.history();
        check(prefs.getLong("count", 0) == 3 && history.length() == 200);
        entry(history.getJSONObject(199), SearchTarget.XIANYU, "fixture-seed-198", false);
        store.record(SearchTarget.CTRIP, "fixture-over-cap", true);
        history = store.history();
        check(prefs.getLong("count", 0) == 4 && history.length() == 200);
        entry(history.getJSONObject(0), SearchTarget.CTRIP, "fixture-over-cap", true);
        entry(history.getJSONObject(1), SearchTarget.DOUBAN, "fixture-at-cap", false);
        entry(history.getJSONObject(199), SearchTarget.XIANYU, "fixture-seed-197", false);

        String unchanged = prefs.getString("history", "");
        check(prefs.edit().putBoolean("save_history", false).commit());
        store.record(SearchTarget.WECHAT, "fixture-disabled", false);
        check(prefs.getLong("count", 0) == 5);
        check(unchanged.equals(prefs.getString("history", "")));

        check(prefs.edit().putBoolean("save_history", true).putString("history", "{invalid-json").commit());
        check(store.history().length() == 0);
        store.record(SearchTarget.XIANYU, "fixture-recovered", false);
        history = store.history();
        check(prefs.getLong("count", 0) == 6 && history.length() == 1);
        entry(history.getJSONObject(0), SearchTarget.XIANYU, "fixture-recovered", false);
    }

    private static void entry(JSONObject value, SearchTarget target, String query, boolean web) throws Exception {
        check(value.getString("target").equals(target.name()));
        check(value.getString("query").equals(query));
        check(value.getBoolean("web") == web);
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("LOCAL_STORE_FAIL");
    }
}
