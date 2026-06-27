package xss.it.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import xss.it.ultimate.pdf.viewer.PdfViewer;

/**
 * @author XDSSWAR
 * Created on 01/23/2024
 */
public class Demo extends Application {

    /**
     * The entry point of the Java application.
     * This method calls the launch method to start a JavaFX application.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This method is called after the application has been launched.
     * Override this method to create and set up the primary stage of the application.
     *
     * @param stage The primary stage for this application, onto which
     *              the application scene can be set.
     */
    @Override
    public void start(Stage stage) {
        // The existing PDF viewer demo.
        PdfViewer viewer = new PdfViewer();
        // The viewer's toolbar uses a 10000px spacer + min=pref, so its computed
        // minimum width is huge. A Scene root hides this (the scene force-resizes
        // its root), but a TabPane hosts content in a StackPane that honors the
        // child's min - which would size the viewer to ~10000px and clip its right
        // side. Pin its min to 0 so the tab sizes it to the real area instead.
        viewer.setMinSize(0, 0);
        viewer.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                viewer.setEnableToolbar(!viewer.isEnableToolbar());
            }
        });
        viewer.setPageColumns(2);

        // Two tabs: the PDF viewer and the SVG (SvgView) showcase. This keeps the
        // single `:demo:run` entry point while demonstrating both modules.
        Tab pdfTab = new Tab("PDF Viewer", viewer);
        pdfTab.setClosable(false);
        Tab svgTab = new Tab("SVG View", new SvgShowcase());
        svgTab.setClosable(false);
        TabPane tabs = new TabPane(pdfTab, svgTab);
        tabs.getSelectionModel().select(pdfTab);

        Scene scene = new Scene(tabs, 1200, 700);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The initialization method for the application.
     * This method is called immediately after the application class is loaded and
     * constructed. An application can override this method to perform initialization
     * tasks before the application is shown.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @Override
    public void init() throws Exception {
        super.init();
    }

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     *
     * @throws Exception if an error occurs during stopping the application.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
