package org.xava.jfx2swing.webview;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.Beans;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.PromptData;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.util.Callback;
import netscape.javascript.JSException;

/**
 * A JPanel that ports JavaFX WebView widget to Swing based applications.
 * <p>
 * This JPanel contains and manages a WebView instance porting it from JavaFX
 * library, thus the first thing to consider is to have JavaFX library
 * (jfxrt.jar) present in the class-path if you're using any Java version that
 * is less than 1.7 Update 2.
 * <p>
 * This component provides an API as a proxy to the enclosed WebView widget and
 * it's internal WebEngine instance. The API removes most of the complications
 * caused by the cross-thread method calls and event dispatching which raise
 * between the Swing Event Dispatcher Thread and JavaFX Thread. However, this
 * component also provides the internal WebView instance because custom
 * interactions might not be fulfilled directly by the provided API.
 * Consequently, cross-threading issue will then be the responsibility of the
 * calling code.
 * <p>
 * <h3>On JavaFX initialization</h3>
 * <ul>
 * <li>
 * The actual initialization of JavaFX Thread and the WebView widget will occur
 * inside the overloaded addNotify method and only at Runtime. It will not be
 * initialized in Design-time.
 * </li>
 * <li>
 * Platform.exit() method will never be called by this component. Thus, if the
 * JavaFX must be ended at runtime, then Platform.exit() or any equivalent
 * ending call must be made explicitly.
 * </li>
 * </ul>
 *
 * @author Alaa Sarhan
 *
 * @version 1.0
 */
public class WebView extends javax.swing.JPanel {

    private Object returned_js_object = null;
    private JSException jsException = null;
    private boolean js_execution_returned = false;
    private JFXPanel fxPanel;
    private Webview_fxmlController webViewController;

    /**
     * Creates an instance of SwingFXWebView component that can be added to any
     * Swing container.
     */
    public WebView() {

        initComponents();

        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                fxPanel.setSize(e.getComponent().getSize());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        fxPanel = new JFXPanel();

        add(fxPanel);
    }

    @Override
    public void addNotify() {
        super.addNotify();

        if (!Beans.isDesignTime()) {
            initWebView(fxPanel);
        }
    }

    /**
     * Gets the internal instance of WebView JavaFX widget. You may use this
     * instance to directly access the WebView widget API for complex scenarios.
     * If any or many of the API methods provided by this class may fulfill your
     * needs, then it's recommended to use them since they are thread-safe
     * implementations.
     *
     * @return The internal instance of WebView widget.
     */
    public javafx.scene.web.WebView getWebView() {
        return this.webViewController.getWebView();
    }

    /**
     * Unloads the contents of the WebView by setting the content to an empty
     * string.
     */
    public void unload() {
        if (Platform.isFxApplicationThread()) {
            webViewController.getWebView().getEngine().loadContent("");
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    unload();
                }
            });
        }
    }

    /**
     * Loads the given URL into the WebView.
     *
     * @param url The url to be loaded. This must be a valid URL.
     */
    public void load(final URL url) {
        if (url == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            webViewController.getWebView().getEngine()
                    .load(url.toExternalForm());
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    load(url);
                }
            });
        }
    }

    /**
     * Loads the given HTML content into the WebView
     *
     * @param html HTML content
     */
    public void loadContent(final String html) {
        if (html == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            webViewController.getWebView().getEngine()
                    .loadContent(html);
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    loadContent(html);
                }
            });
        }
    }

    /**
     * Loads the given content into the WebView
     *
     * @param content content as String
     * @param contentMimeType content mimetype
     */
    public void loadContent(final String content, final String contentMimeType) {
        if (content == null) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            webViewController.getWebView().getEngine()
                    .loadContent(content, contentMimeType);
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    loadContent(content, contentMimeType);
                }
            });
        }
    }

    /**
     * Stops any loading activity with in the WebView.
     */
    public void stop() {
        if (Platform.isFxApplicationThread()) {

            Worker worker = getWebView().getEngine().getLoadWorker();

            if (worker == null) {
                return;
            }

            worker.cancel();

        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            });
        }
    }

    /**
     * Executes the given script directly into the WebView and in the calling
     * thread. This method must be called only in the JavaFX thread.
     * <p>
     * The result of the execution is stored internally in the private variable
     * <code>returned_js_object</code> so that it can be obtained later.
     *
     * @param script the script to execute.
     */
    public synchronized void executeScript(String script) {
        try {
            if (getWebView().getEngine().getDocument() != null) {
                this.returned_js_object = getWebView().getEngine().executeScript(script);
            }
        } catch (JSException ex) {
            this.jsException = ex;
        }

        this.js_execution_returned = true;
        notifyAll();
    }

    /**
     * Executes the given script in the loaded document in the WebView within
     * the given timeout.
     *
     * @param script The script to be executed.
     * @param timeout the timeout, in milliseconds, before this method throws a
     * TimeoutException if the execution of the script didn't return.
     * @return The returned object from the WebEngine as is.
     * @throws TimeoutException
     */
    public synchronized Object executeScript(final String script, long timeout)
            throws TimeoutException, JSException {
        this.js_execution_returned = false;
        this.returned_js_object = null;
        this.jsException = null;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                executeScript(script);
            }
        });

        long started = System.currentTimeMillis();

        while (!this.js_execution_returned
                && ((timeout == 0)
                || (System.currentTimeMillis() - started) < timeout)) {
            try {
                this.wait(100);
            } catch (InterruptedException ex) {
                throw new IllegalThreadStateException("Execution interrupted");
            }
        }

        if (js_execution_returned) {
            if (this.jsException != null) {
                throw this.jsException;
            } else {
                return returned_js_object;
            }
        } else {
            throw new TimeoutException("Script execution timed out.");
        }
    }

    /**
     * Executes a script file in the loaded document in the WebView with in the
     * given timeout.
     *
     * @param file The script file to be executed
     * @param timeout the timeout, in milliseconds, before this method throws a
     * TimeoutException if the execution of the script didn't return.
     * @return The returned object from the WebEngine as is.
     * @throws FileNotFoundException
     * @throws TimeoutException
     */
    public Object executeScript(File file, long timeout)
            throws FileNotFoundException, TimeoutException, JSException {

        FileInputStream fileInputStream = new FileInputStream(file);

        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));

        return executeScript(fileInputStream, timeout);
    }

    /**
     * Executes a script file in the loaded document in the WebView with in the
     * given timeout.
     *
     * @param inStream
     * @param timeout the timeout, in milliseconds, before this method throws a
     * TimeoutException if the execution of the script didn't return.
     * @return The returned object from the WebEngine as is.
     * @throws TimeoutException
     */
    public Object executeScript(InputStream inStream, long timeout)
            throws TimeoutException, JSException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

        StringBuilder contentBuilder = new StringBuilder();

        StringBuilder contentBuidler = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                contentBuidler.append(line).append("\n\r");
            }
        } catch (IOException ex) {
            Logger.getLogger(WebView.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        if (contentBuidler.length() == 0) {
            return null;
        }

        return executeScript(contentBuidler.toString(), timeout);
    }

    /**
     * Sets a callback that will be called when the prompt(string) method is
     * called from a script within the loaded document.&nbsp;<br />This callback
     * must return string data that will be given back to the calling script.
     *
     * @param handler The prompt callback. null to unset.
     */
    public void setPromptHandler(
            final Callback<PromptData, java.lang.String> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setPromptHandler(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setPromptHandler(handler);
                }
            });
        }
    }

    /**
     * Sets the visibility changes event handler. Visibility chages when a
     * script, or code, calls the window.show() or window.hide() methods.
     *
     * @param handler the visibility changed event handler. null to unset.
     */
    public void setOnVisibilityChanged(
            final EventHandler<WebEvent<java.lang.Boolean>> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setOnVisibilityChanged(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setOnVisibilityChanged(handler);
                }
            });
        }
    }

    /**
     * Sets the handler to the WebEngine's status changing events.
     *
     * @param handler The handler to set. null to unset.
     */
    public void setOnStatusChanged(
            final EventHandler<WebEvent<java.lang.String>> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setOnStatusChanged(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setOnStatusChanged(handler);
                }
            });
        }
    }

    /**
     * Sets the handler for the javascript window resize method.
     *
     * @param handler The handler to set. null to unset.
     */
    public void setOnResized(
            final EventHandler<WebEvent<Rectangle2D>> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setOnResized(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setOnResized(handler);
                }
            });
        }
    }

    /**
     * Sets the handler for Javascript alert call. The handler will receive the
     * message text of the alert call.
     *
     * @param handler The handler to set. null to unset.
     */
    public void setOnAlert(final EventHandler<WebEvent<java.lang.String>> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setOnAlert(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setOnAlert(handler);
                }
            });
        }
    }

    /**
     * Sets whether Javascript is enabled in the WebView's engine or not.
     *
     * @param value true to enable Javascript in the WebView widget and false to
     * disable it.
     */
    public void setJavaScriptEnabled(final boolean value) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setJavaScriptEnabled(value);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setJavaScriptEnabled(value);
                }
            });
        }
    }

    /**
     * Sets the handler for Javascript popup call.
     *
     * @param handler The handler to set. null to unset.
     */
    public void setCreatePopupHandler(
            final Callback<PopupFeatures, WebEngine> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setCreatePopupHandler(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setCreatePopupHandler(handler);
                }
            });
        }
    }

    /**
     * Sets the handler for Javascript confirm calls
     *
     * @param handler The handler to set. null to unset.
     */
    public void setConfirmHandler(final Callback<String, Boolean> handler) {
        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine()
                    .setConfirmHandler(handler);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setConfirmHandler(handler);
                }
            });
        }
    }

    /**
     * Adds a progress listener to the web engine.
     *
     * @param listener The listener to add.
     */
    public void addProgressListener(final ChangeListener listener) {

        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine().getLoadWorker()
                    .progressProperty().addListener(listener);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    addProgressListener(listener);
                }
            });
        }
    }

    /**
     * Removes a progress listener from the web engine.
     *
     * @param listener The listener to remove.
     */
    public void removeProgressListener(final ChangeListener listener) {

        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine().getLoadWorker()
                    .progressProperty().removeListener(listener);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    removeProgressListener(listener);
                }
            });
        }
    }

    /**
     * Adds a state listener to the web engine.
     *
     * @param listener The listener to add.
     */
    public void addStateListener(final ChangeListener<State> listener) {

        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine().getLoadWorker()
                    .stateProperty().addListener(listener);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    addStateListener(listener);
                }
            });
        }
    }

    /**
     * Removes a state listener from the web engine.
     *
     * @param listener The listener to remove.
     */
    public void removeStateListener(final ChangeListener<State> listener) {

        if (Platform.isFxApplicationThread()) {

            webViewController.getWebView().getEngine().getLoadWorker()
                    .stateProperty().removeListener(listener);

        } else {

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    removeStateListener(listener);
                }
            });
        }
    }

    /**
     * Gets whether context menu is enabled on the WebView widget.
     *
     * @return true if WebView's context menu is enabled, false if not.
     */
    public boolean isContextMenuEnabled() {
        // This is experimental implementation not calling from JavaFX Thread.
        return getWebView().isContextMenuEnabled();
    }

    /**
     * Sets the enabled state of WebView context menu.
     *
     * @param enabled The desired enabled state.
     */
    public void setContextMenuEnabled(final boolean enabled) {
        if (Platform.isFxApplicationThread()) {
            getWebView().setContextMenuEnabled(enabled);
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setContextMenuEnabled(enabled);
                }
            });
        }
    }

    /**
     * Sets the location to the user-defined style sheet to use by the web
     * engine.
     *
     * @param cssPath
     */
    public void setUserStyleSheet(final String cssPath) {
        if (Platform.isFxApplicationThread()) {
            getWebView().getEngine().setUserStyleSheetLocation(cssPath);
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setUserStyleSheet(cssPath);
                }
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    boolean fxInitialized = false;

    /**
     * Returns whether JavaFX Thread has been initialized or not yet.
     *
     * @return Whether JavaFX Thread had been initialized or not.
     */
    public boolean isFXInitialized() {
        return fxInitialized;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * This method initializes the JavaFX WebView widget and adds it to this
     * panel
     *
     * @param fxPanel
     */
    private void initWebView(final JFXPanel fxPanel) {

        if (Platform.isFxApplicationThread()) {

            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("webview_fxml.fxml"));
            try {

                loader.load();
                webViewController = loader.getController();
                Scene scene = new Scene((Parent) loader.getRoot());
                fxPanel.setScene(scene);

                fxInitialized = true;

            } catch (IOException ex) {
                Logger.getLogger(WebView.class
                        .getName())
                        .log(Level.SEVERE, null, ex);
            }
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    initWebView(fxPanel);

                    if (Platform.isImplicitExit()) {
                        Platform.setImplicitExit(false);
                    }
                }
            });
        }
    }
}
