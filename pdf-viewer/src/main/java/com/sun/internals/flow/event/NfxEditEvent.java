package com.sun.internals.flow.event;

import com.sun.internals.flow.NfxListView;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author XDSSWAR
 * Created on 06/25/2024
 */
public class NfxEditEvent<T> extends Event {
    /**
     * Event type for any edit event.
     */
    public static final EventType<NfxEditEvent<?>> ANY;

    /**
     * Event type for the start of an edit event.
     */
    public static final EventType<NfxEditEvent<?>> NFX_EDIT_START;

    /**
     * Event type for the cancellation of an edit event.
     */
    public static final EventType<NfxEditEvent<?>> NFX_EDIT_CANCEL;

    /**
     * Event type for the commitment of an edit event.
     */
    public static final EventType<NfxEditEvent<?>> NFX_EDIT_COMMIT;

    /**
     * The new value being set during an edit event.
     */
    private final T newVal;

    /**
     * The index of the item being edited.
     */
    private final int index;

    /**
     * The source of the edit event.
     */
    private final NfxListView<T> source;

    /**
     * Constructs an NfxEditEvent.
     * @param source the source of the event
     * @param eventType the type of the event
     * @param newVal the new value being set
     * @param index the index of the item being edited
     */
    public NfxEditEvent(NfxListView<T> source, EventType<? extends NfxEditEvent<?>> eventType, T newVal, int index) {
        super(source, Event.NULL_SOURCE_TARGET, eventType);
        this.source = source;
        this.index = index;
        this.newVal = newVal;
    }


    /**
     * Returns the new value being set during the edit event.
     * @return the new value
     */
    public T getNewVal() {
        return newVal;
    }

    /**
     * Returns the index of the item being edited.
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the source of the edit event.
     * @return the source
     */
    @Override
    public NfxListView<T> getSource() {
        return source;
    }


    @Override
    public String toString() {
        return "NfxEditEvent{" +
                "newVal=" + newVal +
                ", index=" + index +
                ", source=" + source +
                '}';
    }


    static {
        ANY = new EventType<>(Event.ANY, "NFX_LIST_VIEW_EDIT");
        NFX_EDIT_START = new EventType<>(ANY, "NFX_EDIT_START");
        NFX_EDIT_CANCEL = new EventType<>(ANY, "NFX_EDIT_CANCEL");
        NFX_EDIT_COMMIT = new EventType<>(ANY, "NFX_EDIT_COMMIT");
    }
}
