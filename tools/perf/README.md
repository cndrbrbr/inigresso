# Lasttest

Erzeugt 200 Portale (Raster 20 × 10, beide Fraktionen), alle nicht kreuzenden Links (≈ 509) und
Felder (≈ 315) im Speicher und misst die Teile, die regelmäßig je Spieler laufen. Ohne Server.

```
mvn -q -B package -DskipTests        # target/classes bauen
API=~/.m2/repository/org/spigotmc/spigot-api/26.3-R0.1-SNAPSHOT/spigot-api-26.3-R0.1-SNAPSHOT.jar
GUAVA=$(ls ~/.m2/repository/com/google/guava/guava/*/guava-*.jar | tail -1)
javac -cp target/classes:$API -d /tmp/perf tools/perf/Perf.java
java -Djava.awt.headless=true -cp /tmp/perf:target/classes:$API:$GUAVA Perf
```

Ergebnis 0.9.3 siehe ADMIN_GUIDE.md §7.
