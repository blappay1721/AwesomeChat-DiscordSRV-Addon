package dev.adf.awesomechatdiscord;

import dev.adf.awesomechatdiscord.vendor.objectholders.ICPlaceholder;
import dev.adf.awesomechatdiscord.vendor.utils.CustomStringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Self-check for the one piece of logic this fork authors rather than inherits:
 * turning AwesomeChat's trigger config into the renderer's regexes.
 * <p>
 * It runs the patterns through the real {@link ICPlaceholder#colorCodeIgnoredPattern},
 * because that rewrite is what a pattern actually faces at runtime -- an earlier
 * version of this check tested the raw pattern only, passed, and shipped a regex
 * that {@code colorCodeIgnoredPattern} turned into literal text matching nothing.
 * <p>
 * Run it with {@code check-triggers.sh}, which puts the compiled classes on the
 * classpath. Must stay a mirror of {@code AwesomeChatBridge.triggerPattern}.
 */
public class TriggerPatternCheck {

    static Pattern triggerPattern(String prefix, String suffix, List<String> keywords) {
        String alternation = keywords.stream()
                .sorted((a, b) -> b.length() - a.length())
                .map(CustomStringUtils::escapeMetaCharacters)
                .collect(Collectors.joining("|"));
        String regex = suffix.isEmpty()
                ? "(?i)" + CustomStringUtils.escapeMetaCharacters(prefix) + "(?:" + alternation + ")(?!\\w)"
                : "(?i)" + CustomStringUtils.escapeMetaCharacters(prefix) + "(?:" + alternation + ")"
                        + CustomStringUtils.escapeMetaCharacters(suffix);
        return Pattern.compile(regex);
    }

    /** What the renderer actually matches against, colour-code rewrite included. */
    static Pattern live(String prefix, String suffix, List<String> keywords) {
        return ICPlaceholder.colorCodeIgnoredPattern(triggerPattern(prefix, suffix, keywords));
    }

    static boolean hits(Pattern p, String s) {
        return p.matcher(s).find();
    }

    public static void main(String[] args) {
        List<String> item = Arrays.asList("item", "hand", "this");
        List<String> inv = Arrays.asList("inventory", "inv");
        List<String> ender = Arrays.asList("enderchest", "echest", "ec");

        Pattern pItem = live("[", "]", item);
        Pattern pInv = live("[", "]", inv);
        Pattern pEnder = live("[", "]", ender);

        // The regression that shipped: a quoted pattern survives this far but matches
        // nothing once colorCodeIgnoredPattern has rewritten it.
        assert !pItem.pattern().contains("\\Q") : "pattern must not use \\Q..\\E: " + pItem.pattern();

        // Each trigger matches its own group.
        assert hits(pItem, "look at [item] here") : "plain [item] must match: " + pItem.pattern();
        assert hits(pItem, "[hand]");
        assert hits(pItem, "[THIS]") : "triggers are case-insensitive";
        assert hits(pInv, "check [inv]");
        assert hits(pInv, "check [inventory]");
        assert hits(pEnder, "[ec] [echest] [enderchest]");

        // And not another group's.
        assert !hits(pItem, "[inv]");
        assert !hits(pInv, "[item]");
        assert !hits(pEnder, "[inventory]") : "[inventory] must not match as [ec]";
        assert !hits(pInv, "[enderchest]");

        // The whole point of the rewrite: colour codes between characters are ignored.
        assert hits(pItem, "§a[§bitem§c]") : "colour codes around the trigger";
        assert hits(pItem, "[i§ftem]") : "colour code inside the keyword";

        // Longest-first: [inventory] is one match, not [inv] plus "entory]".
        Matcher m = pInv.matcher("[inventory]");
        assert m.find() && m.group().equals("[inventory]") : "expected whole [inventory]";

        // A custom suffix is honoured.
        Pattern custom = live("<", ">", item);
        assert hits(custom, "<item>");
        assert !hits(custom, "[item]");

        // An empty suffix needs the word-boundary guard, or [items] would match.
        Pattern noSuffix = live("[", "", item);
        assert hits(noSuffix, "[item");
        assert !hits(noSuffix, "[items") : "empty suffix must not match a longer word";

        // Regex metacharacters in the prefix stay literal.
        Pattern meta = live("(", ")", item);
        assert hits(meta, "(item)");
        assert !hits(meta, "item");

        System.out.println("raw   : " + triggerPattern("[", "]", item).pattern());
        System.out.println("live  : " + pItem.pattern());
        System.out.println("TriggerPatternCheck: all assertions passed");
    }
}
