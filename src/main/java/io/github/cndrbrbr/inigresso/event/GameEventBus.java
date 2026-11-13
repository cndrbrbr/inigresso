package io.github.cndrbrbr.inigresso.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Einfacher synchroner Ereignisbus (architecture.md §3). Nur auf dem Server-Thread benutzen. */
public final class GameEventBus {

    private record Handler<T extends GameEvent>(Class<T> type, Consumer<? super T> consumer) {
        void accept(GameEvent e) {
            if (type.isInstance(e)) {
                consumer.accept(type.cast(e));
            }
        }
    }

    private final List<Handler<?>> handlers = new ArrayList<>();
    private final Logger logger;

    public GameEventBus(Logger logger) {
        this.logger = logger;
    }

    public <T extends GameEvent> void subscribe(Class<T> type, Consumer<? super T> consumer) {
        handlers.add(new Handler<>(type, consumer));
    }

    /** Ein fehlerhafter Empfänger hält die anderen nicht auf. */
    public void publish(GameEvent event) {
        for (Handler<?> h : List.copyOf(handlers)) {
            try {
                h.accept(event);
            } catch (RuntimeException ex) {
                logger.log(Level.SEVERE, "Fehler bei Verarbeitung von " + event.getClass().getSimpleName(), ex);
            }
        }
    }
}
