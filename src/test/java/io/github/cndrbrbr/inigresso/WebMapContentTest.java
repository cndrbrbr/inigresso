package io.github.cndrbrbr.inigresso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cndrbrbr.inigresso.model.BlockPos;
import io.github.cndrbrbr.inigresso.model.Faction;
import io.github.cndrbrbr.inigresso.model.Portal;
import io.github.cndrbrbr.inigresso.model.Resonator;
import io.github.cndrbrbr.inigresso.render.WebMapContent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebMapContentTest {

    private static Portal portal(String name, Faction owner) {
        return new Portal(12, name, new BlockPos("world", 10, 70, -5), owner, null, 0, null, 0);
    }

    @Test
    void escapesPortalNames() {
        String d = WebMapContent.detail(portal("<script>alert('x')</script> & Co", Faction.NEUTRAL));
        assertFalse(d.contains("<script>"));
        assertTrue(d.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; Co"));
        assertTrue(d.contains("neutral"));
    }

    @Test
    void detailShowsLevelAndResonatorsForOwnedPortal() {
        Portal p = portal("Wiesenkreuz", Faction.LUMEN);
        p.setResonator(0, Resonator.full(8, UUID.randomUUID()));
        p.setResonator(1, Resonator.full(8, UUID.randomUUID()));
        String d = WebMapContent.detail(p);
        assertTrue(d.contains("Lumen, Stufe 2, 2/8 Resonatoren"), d);
        assertTrue(d.contains("10 / 70 / -5"));
        assertEquals("#12 Wiesenkreuz", WebMapContent.label(p));
    }

    @Test
    void iconIsSvgInFactionColor() {
        String uri = WebMapContent.icon(portal("A", Faction.WAECHTER));
        assertTrue(uri.startsWith("data:image/svg+xml;base64,"));
        String svg = new String(Base64.getDecoder().decode(uri.substring(uri.indexOf(',') + 1)), StandardCharsets.UTF_8);
        assertTrue(svg.contains(WebMapContent.hex(Faction.WAECHTER)));
        assertEquals("#3c6eff", WebMapContent.hex(Faction.WAECHTER));
    }
}
