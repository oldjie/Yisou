package org.nofeed.android;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
public final class TargetOrderingTest {
    private static void check(boolean condition, String why) { if (!condition) throw new AssertionError(why); }
    public static void main(String[] args) {
        EnumSet<SearchTarget> installed = EnumSet.of(SearchTarget.TAOBAO, SearchTarget.PINDUODUO);
        List<SearchTarget> defaults = TargetOrdering.resolve("", installed);
        check(defaults.equals(Arrays.asList(SearchTarget.TAOBAO, SearchTarget.PINDUODUO,
                SearchTarget.DOUBAN, SearchTarget.XIANYU, SearchTarget.WECHAT)), "Installed-first default retains all five apps");
        String legacy = "WECHAT,MEITUAN,DOUBAN,TAOBAO,XIANYU,BILIBILI,PINDUODUO,XIAOHONGSHU,CTRIP,AMAP,TAOBAO_SHANGOU";
        String expected = "WECHAT,DOUBAN,TAOBAO,XIANYU,PINDUODUO";
        List<SearchTarget> migrated = TargetOrdering.resolve(legacy, installed);
        check(TargetOrdering.encode(migrated).equals(expected), "Full-edition order preserves surviving apps and skips removed IDs");
        check(TargetOrdering.resolve(expected, EnumSet.noneOf(SearchTarget.class)).equals(migrated), "Manual order overrides installation changes");
        check(TargetOrdering.resolve("WECHAT,UNKNOWN,WECHAT", installed).size()==5, "Duplicate and invalid IDs cannot hide apps");
        check(TargetOrdering.resolve("MEITUAN,UNKNOWN", installed).equals(defaults), "Removed-only order falls back safely");
        check(TargetOrdering.resolve(null, EnumSet.noneOf(SearchTarget.class)).equals(Arrays.asList(SearchTarget.values())), "All missing apps retain icons");
        System.out.println("Target ordering tests passed (five-app migration, installed priority, manual override, malformed state).");
    }
}
