/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thespheres.betula.web;

import java.util.ArrayList;

/**
 *
 * @author boris.heithecker
 * @param <D>
 */
public class TabArrayList<D extends AbstractData> {

    private int activeTabIndex = -1;
    private int activeTabIndexBefore = -1;
    private final ArrayList<D> tabs = new ArrayList<>();

    public TabArrayList() {
    }

    public ArrayList<D> getTabs() {
        return tabs;
    }

    public String getActiveTabIndex() {
        return Integer.toString(activeTabIndex);
    }

    public void setActiveTabIndex(String value) {
        int index = Integer.valueOf(value);
        if (index != this.activeTabIndex) {
            activeTabIndexBefore = this.activeTabIndex;
            this.activeTabIndex = index;
//            UIComponent uic = FacesContext.getCurrentInstance()..getViewRoot();
            if (activeTabIndexBefore != -1 && activeTabIndexBefore < tabs.size()) {
                tabs.get(activeTabIndexBefore).setActiveTab(false);
            }
            if (this.activeTabIndex != -1 && this.activeTabIndex < tabs.size()) {
                tabs.get(this.activeTabIndex).setActiveTab(true);
            }
        }
    }
    
}
