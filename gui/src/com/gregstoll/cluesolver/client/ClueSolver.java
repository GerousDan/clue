package com.gregstoll.cluesolver.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class ClueSolver implements EntryPoint {

  public static final String[][] internalNames = {{"ProfessorPlum", "ColonelMustard", "MrGreen", "MissScarlet", "MsWhite", "MrsPeacock"},
                                    {"Knife", "Candlestick", "Revolver", "LeadPipe", "Rope", "Wrench"},
                                    {"Hall", "Conservatory", "DiningRoom", "Kitchen", "Study", "Library", "Ballroom", "Lounge", "BilliardRoom"}};
  public static final String[][] externalNames = {{"Professor Plum", "Colonel Mustard", "Mr. Green", "Miss Scarlet", "Ms. White", "Mrs. Peacock"},
                                    {"Knife", "Candlestick", "Revolver", "Lead Pipe", "Rope", "Wrench"},
                                    {"Hall", "Conservatory", "Dining Room", "Kitchen", "Study", "Library", "Ballroom", "Lounge", "Billiard Room"}};

  public String[] playerNames = {"Player 1", "Player 2", "Player 3", "Player 4", "Player 5", "Player 6"};
  public VerticalPanel namesPanel = null;
  private static class TestPopup extends PopupPanel {
    public TestPopup(String s) {
        super(true);
        HTML contents = new HTML(s);
        contents.setWidth("128px");
        setWidget(contents);

        setStyleName("ks-popups-Popup");
    }
  }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    final Button button = new Button("Click me");
    final Label label = new Label();

    button.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        if (label.getText().equals(""))
          label.setText("Hello World!");
        else
          label.setText("");
      }
    });
    ClueStateWidget.solver = this;

    VerticalPanel playerInfoPanel = new VerticalPanel();
    playerInfoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    playerInfoPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    playerInfoPanel.add(new HTML("Number of players:"));
    FlowPanel radioPanel = new FlowPanel();
    for (int i = 2; i <= 6; ++i) {
        RadioButton cur = new RadioButton("numPlayers", new Integer(i).toString());
        final int iFinal = i;
        /*cur.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                TestPopup tp = new TestPopup("You clicked button " + iFinal);
                int left = sender.getAbsoluteLeft() + 10;
                int top = sender.getAbsoluteTop() + 10;
                tp.setPopupPosition(left, top);
                tp.show();
            }
        });*/
        cur.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                setNumberOfPlayers(iFinal);
            }
        });
        if (i == 6) {
            cur.setChecked(true);
        }
        radioPanel.add(cur);
    }
    playerInfoPanel.add(radioPanel);
    namesPanel = new VerticalPanel();
    namesPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    namesPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    for (int i = 0; i < playerNames.length; ++i) {
        NameSuggestPanel nsp = new NameSuggestPanel(playerNames[i], i, this);
        namesPanel.add(nsp);
    }
    playerInfoPanel.add(namesPanel);

    VerticalPanel gameInfoPanel = new VerticalPanel();
    gameInfoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    gameInfoPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    gameInfoPanel.add(new HTML("TODO"));
    TabPanel tabs = new TabPanel();
    tabs.add(playerInfoPanel, "Player Info");
    tabs.add(gameInfoPanel, "Game Info");
    tabs.selectTab(0);
    RootPanel.get().add(tabs);
    /*for (int i = 0; i < 6; ++i) {
        RootPanel.get("staticSuspect" + (i + 1)).add(new Label(externalNames[0][i]));
        RootPanel.get("suspect" + (i + 1)).add(new ClueStateWidget());
    }
    for (int i = 0; i < 6; ++i) {
        RootPanel.get("staticWeapon" + (i + 1)).add(new Label(externalNames[1][i]));
        RootPanel.get("weapon" + (i + 1)).add(new ClueStateWidget());
    }
    for (int i = 0; i < 9; ++i) {
        RootPanel.get("staticRoom" + (i + 1)).add(new Label(externalNames[2][i]));
        RootPanel.get("room" + (i + 1)).add(new ClueStateWidget());
    }

    getStateWidget("weapon1").setState(ClueStateWidget.STATE_OWNED_BY_CASEFILE, -1);
    getStateWidget("weapon2").setState(ClueStateWidget.STATE_OWNED_BY_PLAYER, 1);*/
     
     
    // Assume that the host HTML has elements defined whose
    // IDs are "slot1", "slot2".  In a real app, you probably would not want
    // to hard-code IDs.  Instead, you could, for example, search for all 
    // elements with a particular CSS class and replace them with widgets.
    //
    //RootPanel.get("table").add(g);
    
  }

  public void setNumberOfPlayers(int numP) {
    int curNumP = playerNames.length;
    String[] newPlayerNames = new String[numP];
    if (curNumP > numP) {
        for (int i = 0; i < numP; ++i) {
            newPlayerNames[i] = playerNames[i];
        }
        while (curNumP > numP) {
            namesPanel.remove(namesPanel.getWidgetCount() - 1);
            --curNumP;
        }
    } else if (curNumP < numP) {
        for (int i = 0; i < curNumP; ++i) {
            newPlayerNames[i] = playerNames[i];
        }
        while (curNumP < numP) {
            newPlayerNames[curNumP] = "Player " + new Integer(curNumP + 1).toString();
            namesPanel.add(new NameSuggestPanel(newPlayerNames[curNumP], curNumP, this));
            ++curNumP;
        }
    }
    playerNames = newPlayerNames;
  }

  public ClueStateWidget getStateWidget(String id) {
      return ((ClueStateWidget)RootPanel.get(id).getWidget(0));
  }
}
