package org.aquila.event;

@FunctionalInterface
public interface Listener {
	void listen(Event event);
}
