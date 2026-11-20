// Inigresso: Spielstand-Kasten auf der BlueMap-Webkarte. Daten schreibt das Plugin nach
// assets/inigresso/stand.json (alle 20 s); hier wird nur angezeigt.
(function () {
    "use strict";
    var URL = "assets/inigresso/stand.json";
    var box = document.createElement("div");
    box.id = "inigresso-stand";
    box.title = "Klicken zum Ein-/Ausklappen";
    document.body.appendChild(box);
    var data = null;
    var collapsed = false;
    try { collapsed = localStorage.getItem("inigresso-stand-zu") === "1"; } catch (e) { /* egal */ }
    box.addEventListener("click", function () {
        collapsed = !collapsed;
        try { localStorage.setItem("inigresso-stand-zu", collapsed ? "1" : "0"); } catch (e) { /* egal */ }
        render();
    });

    function num(v) { return Math.round(v).toLocaleString("de-DE"); }
    function esc(s) {
        return String(s).replace(/[&<>"']/g, function (c) {
            return {"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[c];
        });
    }
    function remaining(ms) {
        var min = Math.max(0, Math.floor(ms / 60000));
        var d = Math.floor(min / 1440), h = Math.floor((min % 1440) / 60), m = min % 60;
        return d > 0 ? d + " T " + h + " h" : h > 0 ? h + " h " + m + " min" : m + " min";
    }

    function render() {
        if (!data) { box.innerHTML = "<b>Inigresso</b><div class='ig-muted'>Spielstand wird geladen …</div>"; return; }
        var html = "<div class='ig-head'><b>Inigresso</b> <span class='ig-muted'>Spielstand</span>"
            + "<span class='ig-toggle'>" + (collapsed ? "▸" : "▾") + "</span></div>";
        var f = data.factions;
        // Kurzfassung immer sichtbar: Ø-Einfluss aller Fraktionen
        html += "<div class='ig-score'>";
        for (var i = 0; i < f.length; i++) {
            html += "<span style='color:" + f[i].color + "'>" + esc(f[i].name) + " " + num(f[i].average)
                + (f[i].leader ? " ★" : "") + "</span>" + (i < f.length - 1 ? " <span class='ig-muted'>·</span> " : "");
        }
        html += "</div>";
        if (!collapsed) {
            html += "<div class='ig-muted'>Zyklus: noch " + remaining(data.cycleEnd - Date.now()) + "</div>";
            html += "<table><tr><th></th>";
            for (i = 0; i < f.length; i++) { html += "<th style='color:" + f[i].color + "'>" + esc(f[i].name) + "</th>"; }
            html += "</tr>";
            var rows = [["Ø Einfluss", "average"], ["Einfluss jetzt", "influence"], ["Fläche m²", "area"],
                ["Portale", "portals"], ["Erobert", "captures"]];
            for (var r = 0; r < rows.length; r++) {
                html += "<tr><td class='ig-muted'>" + rows[r][0] + "</td>";
                for (i = 0; i < f.length; i++) { html += "<td>" + num(f[i][rows[r][1]]) + "</td>"; }
                html += "</tr>";
            }
            html += "</table>";
            html += "<div class='ig-sub'>Top-Eroberer</div>";
            if (!data.top.length) { html += "<div class='ig-muted'>noch keine</div>"; }
            for (i = 0; i < data.top.length; i++) {
                html += "<div>" + (i + 1) + ". " + esc(data.top[i].name) + " <span class='ig-muted'>"
                    + data.top[i].count + "</span></div>";
            }
            html += "<div class='ig-muted ig-small'>Stand " + new Date(data.updated).toLocaleTimeString("de-DE") + "</div>";
        }
        box.innerHTML = html;
    }

    function load() {
        fetch(URL + "?t=" + Date.now(), {cache: "no-store"})
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (d) { if (d) { data = d; render(); } })
            .catch(function () { /* nächster Versuch */ });
    }
    render();
    load();
    setInterval(load, 20000);
    setInterval(render, 60000); // Restzeit weiterzählen
})();
