package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Bukkit- und BlueMap-freie Inhalte der Webkarte: Farben, Beschriftungen, Symbole. */
public final class WebMapContent {

    private WebMapContent() {
    }

    /** Fraktionsfarbe als RGB (wie Partikel/Scanner). */
    public static int rgb(Faction f) {
        return switch (f) {
            case LUMEN -> 0x4cdc3a;
            case WAECHTER -> 0x3c6eff;
            case NEUTRAL -> 0x9a9a9a;
        };
    }

    public static String hex(Faction f) {
        return String.format("#%06x", rgb(f));
    }

    public static String label(Portal p) {
        return "#" + p.id() + " " + p.name();
    }

    /** HTML für das Detailfenster eines Portals (BlueMap zeigt es beim Anklicken). */
    public static String detail(Portal p) {
        String owner = p.owner() == Faction.NEUTRAL ? "neutral" : p.owner().displayName();
        return "<div style=\"min-width:10em\"><b>#" + p.id() + " " + escape(p.name()) + "</b><br>"
                + "<span style=\"color:" + hex(p.owner()) + "\">&#9679;</span> " + escape(owner)
                + (p.owner() == Faction.NEUTRAL ? "" : ", Stufe " + p.level()
                        + ", " + p.resonatorCount() + "/" + Portal.SLOTS + " Resonatoren")
                + "<br><small>" + p.core().x() + " / " + p.core().y() + " / " + p.core().z() + "</small></div>";
    }

    /** Rundes Symbol in Fraktionsfarbe mit Stufe als Data-URI (keine Dateien im Webroot nötig). */
    public static String icon(Portal p) {
        String text = p.owner() == Faction.NEUTRAL ? "" : Integer.toString(p.level());
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22' viewBox='0 0 22 22'>"
                + "<circle cx='11' cy='11' r='9' fill='" + hex(p.owner()) + "' fill-opacity='0.85'"
                + " stroke='#111' stroke-width='2'/>"
                + "<text x='11' y='15' font-family='sans-serif' font-size='11' font-weight='bold'"
                + " text-anchor='middle' fill='#fff'>" + text + "</text></svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    public static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '&' -> b.append("&amp;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
