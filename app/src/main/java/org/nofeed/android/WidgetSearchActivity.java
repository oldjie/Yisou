// SPDX-License-Identifier: AGPL-3.0-only
package org.nofeed.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** User-triggered, private input window for launcher widgets. */
public class WidgetSearchActivity extends Activity {
    private SearchTarget selected;
    private EditText query;
    private TextView platform;
    private LocalStore store;
    private Appearance.Palette colors;
    private boolean dark;
    private String appearanceMode;
    private boolean submitting, leaving;
    private final Runnable keyboardRequest = () -> {
        if (query != null && !submitting && !leaving && !isFinishing() && !isDestroyed() && hasWindowFocus())
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(query, InputMethodManager.SHOW_IMPLICIT);
    };

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(Appearance.wrap(base));
    }

    @Override public void onCreate(Bundle state) {
        Appearance.apply(this, true);
        super.onCreate(state); setFinishOnTouchOutside(true);
        colors = Appearance.palette(this); dark = Appearance.isDark(this); appearanceMode = Appearance.mode(this);
        Appearance.bars(this, colors.surface);
        store = new LocalStore(this); SearchWidget.initialize(this);
        selected = parse(state != null ? state.getString("target") : getIntent().getStringExtra("target"));
        if (selected == null) selected = parse(store.prefs.getString("default_target", ""));
        getWindow().setGravity(Gravity.BOTTOM);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(12));
        GradientDrawable paper = new GradientDrawable(); paper.setColor(colors.surface); paper.setCornerRadius(dp(22)); root.setBackground(paper);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        platform = label("", 16); platform.setGravity(Gravity.CENTER_VERTICAL); platform.setContentDescription("切换搜索应用"); platform.setOnClickListener(v -> choose(false));
        header.addView(platform, new LinearLayout.LayoutParams(0, dp(44), 1)); updatePlatform();
        TextView close = label("×", 22); close.setGravity(Gravity.CENTER); close.setContentDescription("关闭快捷搜索"); close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44))); root.addView(header);
        LinearLayout field = new LinearLayout(this); field.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable fill = new GradientDrawable(); fill.setColor(colors.field); fill.setCornerRadius(dp(12)); field.setBackground(fill);
        query = new EditText(this); query.setSingleLine(true); query.setTextSize(17); query.setTextColor(colors.ink);
        query.setHintTextColor(colors.hint); query.setHint("搜索…"); query.setPadding(dp(14), 0, dp(4), 0);
        query.setGravity(Gravity.CENTER_VERTICAL); query.setIncludeFontPadding(false); query.setBackgroundColor(Color.TRANSPARENT);
        query.setInputType(android.text.InputType.TYPE_CLASS_TEXT); query.setTypeface(getResources().getFont(R.font.noto_serif_sc_regular));
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH); query.setContentDescription("快捷搜索关键词");
        query.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        if (state != null) query.setText(state.getString("draft", ""));
        query.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_UP)) { submit(); return true; }
            return false;
        });
        field.addView(query, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView go = label("→", 25); go.setGravity(Gravity.CENTER); go.setContentDescription("提交快捷搜索"); go.setOnClickListener(v -> submit());
        field.addView(go, new LinearLayout.LayoutParams(dp(52), dp(56))); root.addView(field);
        setContentView(root); getWindow().setLayout(-1, -2);
        query.requestFocus(); query.postDelayed(keyboardRequest, 160);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView label(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(colors.ink); v.setTypeface(Typeface.DEFAULT); v.setIncludeFontPadding(false); return v;
    }
    @Override protected void onResume() {
        super.onResume();
        if (query != null && (dark != Appearance.isDark(this) || !appearanceMode.equals(Appearance.mode(this)))) recreate();
    }
    private SearchTarget parse(String name) { try { return SearchTarget.valueOf(name); } catch (Exception ignored) { return null; } }
    private void updatePlatform() { platform.setText(selected == null ? "选择搜索应用 ▾" : selected.label + " ▾"); }
    private void choose(boolean submitAfterSelection) {
        java.util.List<SearchTarget> order = AppCatalog.ordered(this);
        String[] names = new String[order.size()];
        for (int i=0; i<order.size(); i++) names[i] = order.get(i).label;
        new AlertDialog.Builder(this).setTitle("选择应用").setItems(names, (d, which) -> {
            selected = order.get(which); updatePlatform(); if (submitAfterSelection) submit();
        }).show();
    }
    private void submit() {
        if (submitting || leaving || isFinishing()) return;
        String value = query.getText().toString().trim();
        if (value.isEmpty()) { query.setError("先写下想搜索的内容"); return; }
        if (selected == null) { choose(true); return; }
        // Keep the editor in place while resolving the destination. Hiding the
        // IME first would resize this bottom window before a slow app appears.
        submitting = true;
        try { SearchLauncher.launch(this, store, selected, value, this::completeHandoff); }
        finally { if (!leaving) submitting = false; }
    }
    private void completeHandoff() {
        if (leaving || isFinishing()) return;
        leaving = true;
        query.removeCallbacks(keyboardRequest);
        // A successful start does not mean the destination has drawn yet.
        // Hide our own surface now instead of retaining a floating editor
        // during the destination's cold start. Failed starts never get here.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().getDecorView().setVisibility(View.INVISIBLE);
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(), 0);
        finish(); overridePendingTransition(0, 0);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putString("draft", query.getText().toString());
        state.putString("target", selected == null ? "" : selected.name());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); selected = parse(intent.getStringExtra("target"));
        if (selected == null) selected = parse(store.prefs.getString("default_target", ""));
        updatePlatform(); query.requestFocus();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused && query != null && !submitting && !leaving) query.post(keyboardRequest);
    }
    @Override protected void onDestroy() {
        if (query != null) query.removeCallbacks(keyboardRequest);
        super.onDestroy();
    }
    @Override public void startActivity(Intent intent) {
        String owner = intent.getPackage();
        if (owner == null && intent.getComponent() != null) owner = intent.getComponent().getPackageName();
        boolean target = false;
        for (SearchTarget t : SearchTarget.values()) if (t.packageName.equals(owner)) target = true;
        boolean web = Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && ("https".equals(intent.getData().getScheme()) || "http".equals(intent.getData().getScheme()));
        // Start the destination in its own task before closing this small input
        // window. Permission settings keep this window/draft for the return.
        if (target || web) { super.startActivity(new Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); completeHandoff(); }
        else super.startActivity(intent);
    }
}
