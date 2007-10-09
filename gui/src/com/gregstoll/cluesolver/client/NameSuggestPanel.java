package com.gregstoll.cluesolver.client;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestionEvent;
import com.google.gwt.user.client.ui.SuggestionHandler;
import com.google.gwt.user.client.ui.Widget;
import java.util.HashSet;
import java.util.Set;

public class NameSuggestPanel extends FlowPanel {
    private SuggestBox suggestBox = null;
    private static MultiWordSuggestOracle nameSuggestOracle = null;
    public NameSuggestPanel(String name, int index, ClueSolver cs) {
        super();
        add(new HTML("Name: "));
        if (nameSuggestOracle == null) {
            initNameSuggestOracle();
        }
        suggestBox = new SuggestBox(nameSuggestOracle);
        if (name != null) {
            suggestBox.setText(name);    
        }
        final int indexFinal = index;
        final ClueSolver csFinal = cs;
        suggestBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
                csFinal.changePlayerName(indexFinal, ((SuggestBox) widget).getText());
            }
        });
        suggestBox.addEventHandler(new SuggestionHandler() {
            public void onSuggestionSelected(SuggestionEvent ev) {
                csFinal.changePlayerName(indexFinal, ev.getSelectedSuggestion().getReplacementString());
            }
        });
        add(suggestBox);
    }

    private void initNameSuggestOracle() {
        nameSuggestOracle = new MultiWordSuggestOracle();
        Set names = new HashSet();
        // TODO - add real names
        names.add("Greg");
        names.add("Graham");
        names.add("David");
        nameSuggestOracle.addAll(names);
    }
}
