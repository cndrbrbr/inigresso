package io.github.cndrbrbr.inigresso.render;

import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/** Bukkit- und BlueMap-freie Inhalte der Webkarte: Farben, Beschriftungen, Symbole. */
public final class WebMapContent {

    private WebMapContent() {
    }

    /** Fraktionsfarbe als RGB (wie Partikel/Scanner). */
    public static int rgb(Faction f) {
        return switch (f) {
            case LUMEN -> 0x4cdc3a;
            case WAECHTER -> 0x3c6eff;
            case CLIP -> 0xe8413c;
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

    /** Spielstand als JSON für den Kasten auf der Webkarte (inigresso-stand.js). */
    public static String standingJson(Sidebar.Data d, long now) {
        Faction leader = d.leader();
        StringBuilder b = new StringBuilder("{\"updated\":").append(now)
                .append(",\"cycleEnd\":").append(now + d.remainingMs()).append(",\"factions\":[");
        List<Sidebar.Side> sides = d.sides();
        for (int i = 0; i < sides.size(); i++) {
            Sidebar.Side s = sides.get(i);
            b.append(i > 0 ? "," : "").append("{\"name\":").append(json(s.faction().displayName()))
                    .append(",\"color\":").append(json(hex(s.faction())))
                    .append(",\"leader\":").append(s.faction() == leader)
                    .append(",\"average\":").append(Math.round(s.average()))
                    .append(",\"influence\":").append(s.influence())
                    .append(",\"area\":").append(Math.round(s.area()))
                    .append(",\"portals\":").append(s.portals())
                    .append(",\"captures\":").append(s.captures()).append('}');
        }
        b.append("],\"top\":[");
        for (int i = 0; i < d.top().size(); i++) {
            Sidebar.Top t = d.top().get(i);
            b.append(i > 0 ? "," : "").append("{\"name\":").append(json(t.name()))
                    .append(",\"count\":").append(t.count()).append('}');
        }
        return b.append("]}").toString();
    }

    static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (c < 0x20 || c == '<' || c == '>') {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
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
