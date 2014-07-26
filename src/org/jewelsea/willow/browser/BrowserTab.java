/*
 * Copyright 2013 John Smith
 *
 * This file is part of Willow.
 *
 * Willow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Willow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Willow. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact details: http://jewelsea.wordpress.com
 */

package org.jewelsea.willow.browser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import jnetention.Core;
import jnetention.run.WebBrowser;
import org.apache.commons.math3.stat.Frequency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tab associated with a browser window.
 */
public class BrowserTab extends UITab<WebView> {
    private WebEngine engine;
    private final TabManager tabManager;
    private final WebView view;
    /**
     * @return The browser window associated with this tab
     */
    public BrowserWindow getBrowser() {
        return browser;
    }

    private BrowserWindow browser;

    public BrowserTab(Core c, TabManager tabManager) {
        super(c, new WebView());
                
        this.view = content();
        this.tabManager = tabManager;
        

        init();
        
    }
    
    public String getLocation() {
        return browser.getLocField().getText();
    }

    protected void init() {
        System.out.println("BrowserTab start" + (System.currentTimeMillis() - WebBrowser.start));   
        
        browser = new BrowserWindow(view);
        
        final WebView view = browser.getView();
        view.setFontSmoothingType(FontSmoothingType.GRAY);
        
        
        // set the new browser to open any pop-up windows in a new tab.
        view.getEngine().setCreatePopupHandler(popupFeatures -> {
            final BrowserTab browserTab = new BrowserTab(core, tabManager);
            tabManager.addTab(browserTab);
            return browserTab.browser.getView().getEngine();
        });

        // put some dummy invisible content in the tab otherwise it doesn't show because it has no dimensions.
        Pane spacer = new StackPane();
        spacer.setMinWidth(TabManager.TAB_PANE_WIDTH + 35);
        spacer.setMaxWidth(TabManager.TAB_PANE_WIDTH + 35);
        setContent(spacer);

        engine = getBrowser().getView().getEngine();
        engine.loadContent(getDefaultContent());
        
        // add the tab
        graphicProperty().bind(getBrowser().faviconProperty());
        engine.titleProperty().addListener((observableValue, oldValue, newTitle) -> {
            // todo we already have a listener for the title, might want to repurpose it...
            // todo I wonder if the title would be reset correctly if the page has no title.
            if (newTitle != null && !"".equals(newTitle)) {
                setText(newTitle);
            }
        });
        
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {

            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                if (newValue == State.SUCCEEDED) {
                    onPageLoaded();
                }
            }
            
        });
        
        System.out.println("BrowserTab finish" + (System.currentTimeMillis() - WebBrowser.start));   

    }
    
    static String prevLocation = null;
    
    public void extractAttributes(NamedNodeMap n, Map<String,String> s, String prefix) {
        
        for (int i = 0; i < n.getLength(); i++) {
            Node x = n.item(i);
            
            s.put(prefix + x.getNodeName(), x.getTextContent());
        }
    }
    
    public Map<String,String> extractNodeFeatures(HashMap<String,String> m, Node n) {
        extractAttributes(n.getAttributes(), m, "");
        
        /*
        for (int i = 0; i < n.getChildNodes().getLength(); i++) {
            Node x = n.getChildNodes().item(i);            
            extractAttributes(x.getAttributes(), m, x.getNodeName() + ".");            
        }
        */
        
        if (n.getTextContent()!=null)
            if (!n.getTextContent().isEmpty())
                m.put("_", n.getTextContent());

        return m;
    }
    
    protected void onPageLoaded() {
        String location = engine.getLocation();
        if (!(location.startsWith("http://") || location.startsWith("https://")))
            return;
        
        String title = engine.getTitle();
        String metaKeywords = title + "";
        Document doc = engine.getDocument();
        
        Element e = doc.getDocumentElement();
        
        NodeList n = e.getElementsByTagName("head");
        
        if (n.getLength() > 0) {
            n.item(0).normalize();
            n = n.item(0).getChildNodes();
            
            for (int i = 0; i < n.getLength(); i++) {
                Node child = n.item(i);   
                String name = child.getNodeName();
                if (name.equalsIgnoreCase("meta"))  {
                    HashMap<String,String> m = new HashMap();
                    extractNodeFeatures(m, child);           
                    if (m.containsKey("name") && m.containsKey("content")) {
                        if (m.get("name").equalsIgnoreCase("description") 
                                || m.get("name").equalsIgnoreCase("keywords") )
                            metaKeywords += " " + m.get("content") ;
                        
                    }
                }
            }
        }
        
        Frequency f = Core.tokenBag(metaKeywords, 3, 16);
        
        
        
        try {
            URL u = new URL(location);
            String host = u.getHost();
            core.knowInherit(location, "_interest", 1.0, 0.99, 0.9);
            core.knowProduct(location, host, "_hostname", 1.0, 0.99, 0.8);
            
            
            int maxKeytokens = 32;
            Iterator<Comparable<?>> vi = f.valuesIterator();
            int i = 0;
            while ((vi.hasNext() && (i < maxKeytokens))) {
                String t = vi.next().toString();
                double p = f.getPct(t);
                p = 0.5 + (p/2.0);
                core.knowProduct(location, t, "_keyword", 1.0, p, p);
                i++;
            }
            
            if (prevLocation!=null) {
                //core.knowProduct(prevLocation, host, "NextBrowserVisit", 1.0, 0.9, 0.8);
                String v = "$0.95$ <" + core.n(prevLocation) + " =\\> " + core.n(location) + ">. %1.00;1.00%";             
                core.logic.addInput(v);
                core.think();
            }
            
            prevLocation = location;
            
            
        } catch (MalformedURLException ex) {
        }
        
    }

    public String getDefaultContent() {
        return "";
    }

}

