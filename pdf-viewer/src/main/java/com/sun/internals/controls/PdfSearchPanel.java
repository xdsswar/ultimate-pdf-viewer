/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static xss.it.ultimate.pdf.viewer.Assets.load;

/**
 * @author XDSSWAR
 * Created on 01/30/2024
 */
public final class PdfSearchPanel extends AnchorPane {
    private final VBox header;
    private final TextField searchField;
    private final HBox hb1;
    private final CheckBox caseBox;
    private final AnchorPane liner1;
    private final AnchorPane center;
    private final AbstractViewer abstractViewer;

    public PdfSearchPanel(AbstractViewer abstractViewer) {
        this.abstractViewer = abstractViewer;
        header = new VBox();
        searchField = new TextField();
        hb1 = new HBox();
        caseBox = new CheckBox();
        liner1 = new AnchorPane();
        center = new AnchorPane();

        /*
         * Initialize method call.
         */
        initialize();
    }

    private void initialize(){
        setMinWidth(0);
        getStylesheets().add(getUserAgentStylesheet());
        getStyleClass().add("pdf-search-panel");
        AnchorPane.setLeftAnchor(header, 20.0);
        AnchorPane.setRightAnchor(header, 20.0);
        AnchorPane.setTopAnchor(header, 20.0);

        caseBox.setMnemonicParsing(false);
        caseBox.setText("Case sensitive");
        VBox.setMargin(hb1, new Insets(10.0, 0.0, 0.0, 0.0));

        liner1.getStyleClass().add("horizontal-divider");
        VBox.setMargin(liner1, new Insets(10.0, 0.0, 0.0, 0.0));

        AnchorPane.setBottomAnchor(center, 5.0);
        AnchorPane.setLeftAnchor(center, 20.0);
        AnchorPane.setRightAnchor(center, 20.0);
        AnchorPane.setTopAnchor(center, 110.0);

        header.getChildren().add(searchField);
        hb1.getChildren().add(caseBox);
        header.getChildren().add(hb1);
        header.getChildren().add(liner1);
        getChildren().add(header);
        getChildren().add(center);
    }

    @Override
    public String getUserAgentStylesheet() {
        return load("/xss/it/ultimate/pdf/viewer/css/search.css").toExternalForm();
    }
}
