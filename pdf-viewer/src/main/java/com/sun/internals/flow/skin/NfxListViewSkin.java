package com.sun.internals.flow.skin;


import com.sun.internals.flow.NfxListView;
import com.sun.internals.flow.internals.BaseListView;
import com.sun.internals.flow.misc.Anima;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SkinBase;


/**
 * @author XDSSWAR
 * Created on 06/28/2024
 */
public class NfxListViewSkin<T> extends SkinBase<NfxListView<T>> {
    /**
     * The style class for the NfxListViewSkin.
     */
    private static final String STYLE_CLASS = "nfx-base";

    /**
     * The delegate BaseListView.
     */
    private final BaseListView<T> delegate;

    /**
     * Listener for tracking changes in a list of items.
     */
    private final ListChangeListener<T> LISTENER;

    /**
     * Constructs an NfxListViewSkin.
     * @param listView the NfxListView to be skinned
     * @param delegate the delegate BaseListView
     */
    public NfxListViewSkin(NfxListView<T> listView, BaseListView<T> delegate) {
        super(listView);
        this.delegate = delegate;
        LISTENER = c -> {
            while (c.next()){
                handleListPlaceHolderBasedOnItems(delegate.getItems());
            }
        };
        initialize();
    }

    /**
     * Initializes the skin.
     * Adds the style class and the delegate to the children.
     */
    private void initialize() {
        getSkinnable().getStyleClass().add(STYLE_CLASS);
        getChildren().add(delegate);
        getSkinnable().setPrefSize(200, 200);

        handleListPlaceHolderBasedOnItems(getSkinnable().getItems());
        delegate.itemsProperty().addListener((obs, o, items) -> {
            if (o != null){
                o.removeListener(LISTENER);
            }
            handleListPlaceHolderBasedOnItems(items);
            items.addListener(LISTENER);
        });
    }

    /**
     * Handles the visibility of the placeholder node based on the presence of items in the list.
     * If the list is empty or null, the placeholder is displayed; otherwise, the list content is shown.
     *
     * @param items the observable list of items to monitor
     */
    private void handleListPlaceHolderBasedOnItems(ObservableList<T> items){
        final int delay = 100;
        if (items.isEmpty() && getSkinnable().getPlaceHolder() != null){
            Timeline fo = Anima.fadeOt(delegate, delay);
            fo.setOnFinished(e->{
                getChildren().remove(delegate);
                if (!getChildren().contains(getSkinnable().getPlaceHolder())) {
                    getSkinnable().getPlaceHolder().setOpacity(0);
                    getChildren().add(getSkinnable().getPlaceHolder());
                    Anima.fadeIn(getSkinnable().getPlaceHolder(), delay).play();
                }
            });
            fo.play();
        }
        else {
            Timeline fo = Anima.fadeOt(getSkinnable().getPlaceHolder(), delay);
            fo.setOnFinished(e->{
                getChildren().remove(getSkinnable().getPlaceHolder());
                if (!getChildren().contains(delegate)) {
                    delegate.setOpacity(0);
                    getChildren().add(delegate);
                    Anima.fadeIn(delegate, delay*2).play();
                }
            });
            fo.play();
        }
    }

}
