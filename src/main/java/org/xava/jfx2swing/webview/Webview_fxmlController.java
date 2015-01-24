package org.xava.jfx2swing.webview;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;

/**
 * FXML Controller class
 *
 * @author Alaa
 */
public class Webview_fxmlController implements Initializable {

    @FXML
    private WebView webView;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public WebView getWebView() {
        return webView;
    }
}
