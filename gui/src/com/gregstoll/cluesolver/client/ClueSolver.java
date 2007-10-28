package com.gregstoll.cluesolver.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public ArrayList playerNames = new ArrayList();
  public VerticalPanel namesPanel = null;
  public HashMap internalNameToClueStateWidgetMap = new HashMap();
  public boolean infoAdded = false;
  private ArrayList playerListBoxes = new ArrayList();
  private ArrayList numPlayersButtons = new ArrayList();
  public static final String scriptName = "clue.cgi";
  public String curSessionString = null;
  public Tree clauseInfoTree = new Tree();
  public Label warningLabel = null;
  private boolean validNumberOfCards = true;
  private ListBox refutingCard = null;
  private ArrayList workingLabels = new ArrayList();
  private ArrayList actionButtons = new ArrayList();
  private ListBox undoHistoryBox = null;
  private Grid simulationTable = null;
  private Label gameStateLabel = null;
  private TextBox loadGameBox = null;
  private ArrayList playerRadioButtons = null;
  public Label consistentLabel = null;

  /**
   * Aggregate the images
   */
  public interface Images extends ClueStateWidget.Images {
    AbstractImagePrototype rainbowpalette();
  }
  private Images images = (Images) GWT.create(Images.class);
  /*private static class TestPopup extends PopupPanel {
    public TestPopup(String s) {
        super(true);
        HTML contents = new HTML(s);
        contents.setWidth("128px");
        setWidget(contents);

        setStyleName("ks-popups-Popup");
    }
  }*/

  public void addActionToHistory(String description, String state) {
    undoHistoryBox.addItem(description, description + "|" + state);
  }

  private String internalToExternalName(String internalName) {
    for (int i = 0; i < internalNames.length; ++i) {
        for (int j = 0; j < internalNames[i].length; ++j) {
            if (internalName.equals(internalNames[i][j])) {
                return externalNames[i][j];
            }
        }
    }
    if (internalName == "None") {
        return "None/Unknown";
    }
    return "???(" + internalName + ")";
  }

  private String getPlayerName(int index) {
     if (index >= 0 && index < playerNames.size()) {
         return (String) playerNames.get(index);
     }
     if (index == -1) {
         return "None";
     }
     if (index == playerNames.size()) {
         return "Solution (case file)";
     }
     return "???";
  }

  private static class ConfirmDialog extends DialogBox implements ClickListener {
      private ClueSolver solver;
      private ConfirmHandler handler;
      public ConfirmDialog(String title, String text, ClueSolver _solver, ConfirmHandler _handler) {
          solver = _solver;
          handler = _handler;
          Button okButton = new Button("OK", this);
          Button cancelButton = new Button("Cancel", this);
          setText(title);
          DockPanel dock = new DockPanel();
          HorizontalPanel buttonPanel = new HorizontalPanel();
          buttonPanel.add(okButton);
          buttonPanel.add(cancelButton);
          dock.add(buttonPanel, DockPanel.SOUTH);
          dock.add(new HTML(text), DockPanel.NORTH);
          setWidget(dock);
      }
      public void onClick(Widget sender) {
          String text = ((Button) sender).getText();
          if (text.equals("OK")) {
              handler.doAction(solver);
          }
          hide();
      }
  }

  public void handleNewInfo(String body) {
    if (infoAdded == false) {
        // We have real info now, so don't change the number of players!
        infoAdded = true;
        for (int i = 0; i < numPlayersButtons.size(); ++i) {
            RadioButton button = ((RadioButton) numPlayersButtons.get(i));
            button.setEnabled(false);
        }
        // disable changing number of cards
        for (int i = 0; i < playerNames.size(); ++i) {
            ((NameSuggestPanel) namesPanel.getWidget(i)).setNumCardsEnabled(false);
        }
    }
    // Reset the refuting card to be None to try to prevent errors.
    refutingCard.setSelectedIndex(0);
    JSONObject response = JSONParser.parse(body).isObject();
    double errorStatus = response.get("errorStatus").isNumber().getValue();
    if (errorStatus != 0.0) {
        Window.alert("Internal error - error returned from script - " + response.get("errorText").isString().toString());
    } else {
        setGameState(response.get("session").isString().stringValue());
        JSONArray newInfos = response.get("newInfo").isArray();
        int numElements = newInfos.size();
        for (int i = 0; i < numElements; ++i) {
            JSONObject curInfo = newInfos.get(i).isObject();
            String card = curInfo.get("card").isString().stringValue();
            int status = (int) curInfo.get("status").isNumber().getValue();
            JSONArray ownerArray = curInfo.get("owner").isArray();
            int[] owners = new int[ownerArray.size()];
            for (int j = 0; j < owners.length; ++j) {
                owners[j] = (int) ownerArray.get(j).isNumber().getValue();
            }
            getStateWidget(card).setState(status, owners);
        }
        clauseInfoTree.removeItems();
        if (response.containsKey("clauseInfo")) {
            JSONObject clauseInfoObj = response.get("clauseInfo").isObject();
            for (int i = 0; i < playerNames.size(); ++i) {
                if (clauseInfoObj.containsKey(new Integer(i).toString())) {
                    JSONArray playerClauseArray = clauseInfoObj.get(new Integer(i).toString()).isArray();
                    TreeItem playerClauseInfo = new TreeItem(playerNames.get(i) + " has:");
                    for (int j = 0; j < playerClauseArray.size(); ++j) {
                        JSONArray curClause = playerClauseArray.get(j).isArray();
                        StringBuffer clauseBuffer = new StringBuffer();
                        for (int k = 0; k < curClause.size(); ++k) { 
                            clauseBuffer.append(internalToExternalName(curClause.get(k).isString().stringValue()));
                            // FFV - order these nicely?
                            if (k != curClause.size() - 1) {
                                clauseBuffer.append(" or ");
                            }
                        }
                        playerClauseInfo.addItem(clauseBuffer.toString());
                    }
                    clauseInfoTree.addItem(playerClauseInfo);
                    playerClauseInfo.setState(true);
                }
            }
        }
        if (response.containsKey("isConsistent")) {
            boolean isConsistent = response.get("isConsistent").isBoolean().booleanValue();
            setIsConsistent(isConsistent);
        }
    }
  }

  CgiResponseHandler newInfoHandler = new CgiResponseHandler() {
      public void onSuccess(String body) {
        handleNewInfo(body);
        setWorking(false);
      }
      public void onError(Throwable ex) {
        Window.alert("Internal error - unable to contact backend - " + ex.getMessage());
        setWorking(false);
      }
  };

  CgiResponseHandler loadHandler = new CgiResponseHandler() {
      public void onSuccess(String body) {
        // If we got here than the session is valid.
        // Set the number of players and their number of cards.
        JSONObject response = JSONParser.parse(body).isObject();
        int numPlayers = (int) response.get("numPlayers").isNumber().getValue();
        setNumberOfPlayers(numPlayers, false);
        JSONArray numCards = response.get("numCards").isArray();
        for (int i = 0; i < numPlayers; ++i) {
            ((NameSuggestPanel) namesPanel.getWidget(i)).setDefaultNumCards((int) numCards.get(i).isNumber().getValue());
        }
        handleNewInfo(body);
        // We can't undo a load so clear the history.
        undoHistoryBox.clear();
        setWorking(false);
      }
      public void onError(Throwable ex) {
        Window.alert("Internal error - unable to contact backend - " + ex.getMessage());
        setWorking(false);
      }
  };


  CgiResponseHandler simulateInfoHandler = new CgiResponseHandler() {
      public void onSuccess(String body) {
        JSONObject response = JSONParser.parse(body).isObject();
        double errorStatus = response.get("errorStatus").isNumber().getValue();
        if (errorStatus != 0.0) {
            Window.alert("Internal error - error returned from script - " + response.get("errorText").isString().toString());
        } else {
            JSONObject simulateData = response.get("simData").isObject();
            int[] offsets = {2, 9, 16};
            for (int i = 0; i < offsets.length; ++i) {
                for (int j = 0; j < internalNames[i].length; ++j) {
                    JSONArray dataArray = simulateData.get(internalNames[i][j]).isArray();
                    double total = 0.0;
                    for (int k = 0; k < dataArray.size(); ++k) {
                        total += dataArray.get(k).isNumber().getValue();
                    }
                    for (int k = 0; k < dataArray.size(); ++k) {
                        double percent;
                        if (total > 0.0) {
                            percent = (dataArray.get(k).isNumber().getValue() * 100) / total;
                        } else {
                            percent = 0.0;
                        }
                        percent = ((int) (percent * 10)) / 10.0;
                        double percentTo255 = percent * (255.0/100.0);
                        int red = 0;
                        int green = 0;
                        int blue = 0;
                        if (percentTo255 <= 64) {
                            green = (int) (percentTo255 * (255.0/64.0));
                            blue = 255;
                        } else if (percentTo255 <= 128) {
                            green = 255;
                            blue = (int) ((128 - percentTo255) * (255.0/64.0));
                        } else if (percentTo255 <= 192) {
                            red = (int) ((percentTo255 - 128) * (255.0/64.0));
                            green = 255;
                        } else {
                            red = 255;
                            blue = (int) ((255 - percentTo255) * (255.0/64.0));
                        }
                        simulationTable.setHTML(offsets[i] + j, k + 1, "<span style=\"background-color: rgb(" + red + ", " + green + ", " + blue + ")\">" + Double.toString(percent) + "%</span>");
                    }
                }
            }
        }
        setWorking(false);
      }
      public void onError(Throwable ex) {
        Window.alert("Internal error - unable to contact backend - " + ex.getMessage());
        setWorking(false);
      }
  };


  public void setValidNumberOfCards(boolean valid) {
    validNumberOfCards = valid;
    warningLabel.setVisible(!valid);
  }

  public void setIsConsistent(boolean isConsistent) {
      consistentLabel.setVisible(!isConsistent);
      // TODO - do more?
  }

  public void setWorking(boolean working) {
    if (!working) {
        for (int i = 0; i < workingLabels.size(); ++i) {
            ((Label) workingLabels.get(i)).setVisible(false);
        }
    } else {
        for (int i = 0; i < workingLabels.size(); ++i) {
            Label workingLabel = (Label) workingLabels.get(i);
            workingLabel.setVisible(true);
            workingLabel.setText("Working...");
        }
    }
    // Enable or disable the buttons.
    for (int i = 0; i < actionButtons.size(); ++i) {
        Button curButton = (Button) actionButtons.get(i);
        curButton.setEnabled(!working);
    }
  }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    ClueStateWidget.solver = this;
    playerNames.add("Player 1");
    playerNames.add("Player 2");
    playerNames.add("Player 3");
    playerNames.add("Player 4");
    playerNames.add("Player 5");
    playerNames.add("Player 6");

    VerticalPanel playerInfoPanel = new VerticalPanel();
    playerInfoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    playerInfoPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    playerInfoPanel.add(new HTML("Number of players:"));
    FlowPanel radioPanel = new FlowPanel();
    playerRadioButtons = new ArrayList();
    for (int i = 3; i <= 6; ++i) {
        RadioButton cur = new RadioButton("numPlayers", new Integer(i).toString());
        final int iFinal = i;
        cur.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                setNumberOfPlayers(iFinal, true);
            }
        });
        if (i == 6) {
            cur.setChecked(true);
        }
        playerRadioButtons.add(cur);
        radioPanel.add(cur);
        numPlayersButtons.add(cur);
    }
    playerInfoPanel.add(radioPanel);
    namesPanel = new VerticalPanel();
    namesPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    namesPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
    for (int i = 0; i < playerNames.size(); ++i) {
        NameSuggestPanel nsp = new NameSuggestPanel((String) playerNames.get(i), i, this);
        namesPanel.add(nsp);
    }
    playerInfoPanel.add(namesPanel);
    warningLabel = new Label("Total number of cards must total 18!");
    warningLabel.setStylePrimaryName("warning");
    warningLabel.setVisible(false);
    playerInfoPanel.add(warningLabel);
    final ClueSolver clueSolver = this;
    Button newGameButton = new Button("New game", new ClickListener() {
        public void onClick(Widget sender) {
            ConfirmDialog dialog = new ConfirmDialog("Confirm new game", "Are you sure you want to start a new game and wipe out all progress?<br><br>", clueSolver, new ConfirmHandler() {
                public void doAction(ClueSolver solver) {
                    solver.startNewGame();
                }
            });
            dialog.center();
        }
    });
    actionButtons.add(newGameButton);
    playerInfoPanel.add(newGameButton);
    HorizontalPanel gameStateLabelPanel = new HorizontalPanel();
    gameStateLabelPanel.add(new Label("Current game state (for loading later): "));
    gameStateLabel = new Label();
    gameStateLabelPanel.add(gameStateLabel);
    playerInfoPanel.add(gameStateLabelPanel);
    HorizontalPanel loadGamePanel = new HorizontalPanel();
    loadGamePanel.add(new Label("Load game state: "));
    loadGameBox = new TextBox();
    loadGamePanel.add(loadGameBox);
    playerInfoPanel.add(loadGamePanel);
    Button loadGameButton = new Button("Load game", new ClickListener() {
        public void onClick(Widget sender) {
            final String gameToLoad = loadGameBox.getText().trim();
            ConfirmDialog dialog = new ConfirmDialog("Confirm load", "Are you sure you want to load a game and wipe out all progress?<br><br>", clueSolver, new ConfirmHandler() {
                public void doAction(ClueSolver solver) {
                    setWorking(true);
                    CgiHelper.doRequest(RequestBuilder.GET, scriptName, "sess=" + gameToLoad + "&action=fullInfo", loadHandler);
                }
            });
            dialog.center();
        }
    });
    actionButtons.add(loadGameButton);
    playerInfoPanel.add(loadGameButton);

    VerticalPanel gameInfoPanel = new VerticalPanel();
    HorizontalPanel gameInfoMainPanel = new HorizontalPanel();
    gameInfoMainPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
    gameInfoMainPanel.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

    Tree infoTree = new Tree();
    TreeItem suspectTree = new TreeItem("Suspects");
    for (int i = 0; i < internalNames[0].length; ++i) {
        suspectTree.addItem(new ClueStateWidget(images, internalNames[0][i], externalNames[0][i]));
    }
    TreeItem weaponTree = new TreeItem("Weapons");
    for (int i = 0; i < internalNames[1].length; ++i) {
        weaponTree.addItem(new ClueStateWidget(images, internalNames[1][i], externalNames[1][i]));
    }
    TreeItem roomTree = new TreeItem("Rooms");
    for (int i = 0; i < internalNames[2].length; ++i) {
        roomTree.addItem(new ClueStateWidget(images, internalNames[2][i], externalNames[2][i]));
    }
    infoTree.addItem(suspectTree);
    infoTree.addItem(weaponTree);
    infoTree.addItem(roomTree);
    suspectTree.setState(true);
    weaponTree.setState(true);
    roomTree.setState(true);
    gameInfoMainPanel.add(infoTree);

    VerticalPanel enterInfoPanel = new VerticalPanel();
    enterInfoPanel.add(new HTML("Enter new info:")); 
    TabPanel enterInfoTabs = new TabPanel();
    VerticalPanel whoOwnsCardPanel = new VerticalPanel();
    HorizontalPanel tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Card: "));
    final ListBox whichCardOwned = makeNewCardListBox(-1, false);
    tempPanel1.add(whichCardOwned);
    whoOwnsCardPanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Owned by: "));
    final ListBox ownerOwned = makeNewPlayerListBox(false, true);
    tempPanel1.add(ownerOwned);
    whoOwnsCardPanel.add(tempPanel1);
    Button whoOwnsSubmitButton = new Button("Add info", new ClickListener() {
        public void onClick(Widget sender) {
            setWorking(true);
            addActionToHistory(internalToExternalName(listBoxValue(whichCardOwned)) + " owned by " + getPlayerName(Integer.parseInt(listBoxValue(ownerOwned))), curSessionString);
            CgiHelper.doRequest(RequestBuilder.POST, scriptName, "sess=" + curSessionString + "&action=whoOwns&owner=" + listBoxValue(ownerOwned) + "&card=" + listBoxValue(whichCardOwned), newInfoHandler);
        }
    });
    actionButtons.add(whoOwnsSubmitButton);
    whoOwnsCardPanel.add(whoOwnsSubmitButton);
    enterInfoTabs.add(whoOwnsCardPanel, "Who owns a card");
    VerticalPanel suggestionMadePanel = new VerticalPanel();
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Made by: "));
    final ListBox suggestingPlayer = makeNewPlayerListBox(false, false);
    tempPanel1.add(suggestingPlayer);
    suggestionMadePanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Suspect: "));
    final ListBox card1 = makeNewCardListBox(0, false);
    tempPanel1.add(card1);
    suggestionMadePanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Weapon: "));
    final ListBox card2 = makeNewCardListBox(1, false);
    tempPanel1.add(card2);
    suggestionMadePanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Room: "));
    final ListBox card3 = makeNewCardListBox(2, false);
    tempPanel1.add(card3);
    suggestionMadePanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Refuted by: "));
    final ListBox refutingPlayer = makeNewPlayerListBox(true, false);
    tempPanel1.add(refutingPlayer);
    suggestionMadePanel.add(tempPanel1);
    tempPanel1 = new HorizontalPanel();
    tempPanel1.add(new HTML("Refuting card: "));
    refutingCard = makeNewCardListBox(-1, true);
    tempPanel1.add(refutingCard);
    ChangeListener updateRefutingCardListener = new ChangeListener() {
        public void onChange(Widget widget) {
            int originalSelectedIndex = refutingCard.getSelectedIndex();
            // Clear out everything except for None/Unknown
            while (refutingCard.getItemCount() > 1) {
                refutingCard.removeItem(1);
            }
            // Add the possibilities to this listbox.
            String internalCard1 = listBoxValue(card1);
            refutingCard.addItem(internalToExternalName(internalCard1), internalCard1);
            String internalCard2 = listBoxValue(card2);
            refutingCard.addItem(internalToExternalName(internalCard2), internalCard2);
            String internalCard3 = listBoxValue(card3);
            refutingCard.addItem(internalToExternalName(internalCard3), internalCard3);
            refutingCard.setSelectedIndex(originalSelectedIndex);
        }
    };
    card1.addChangeListener(updateRefutingCardListener);
    card2.addChangeListener(updateRefutingCardListener);
    card3.addChangeListener(updateRefutingCardListener);
    // Trigger a change to make the refuting card show up correctly at the
    // beginning.
    updateRefutingCardListener.onChange(null);
    suggestionMadePanel.add(tempPanel1);
    Button suggestionSubmitButton = new Button("Add info", new ClickListener() {
        public void onClick(Widget sender) {
            setWorking(true);
            addActionToHistory(getPlayerName(Integer.parseInt(listBoxValue(suggestingPlayer))) + " suggested " + internalToExternalName(listBoxValue(card1)) + ", " + internalToExternalName(listBoxValue(card2)) + ", " + internalToExternalName(listBoxValue(card3)) + " - refuted by " + getPlayerName(Integer.parseInt(listBoxValue(refutingPlayer))) + " with card " + internalToExternalName(listBoxValue(refutingCard)), curSessionString);
            CgiHelper.doRequest(RequestBuilder.POST, scriptName, "sess=" + curSessionString + "&action=suggestion&suggestingPlayer=" + listBoxValue(suggestingPlayer) + "&card1=" + listBoxValue(card1) + "&card2=" + listBoxValue(card2) + "&card3=" + listBoxValue(card3) + "&refutingPlayer=" + listBoxValue(refutingPlayer) + "&refutingCard=" + listBoxValue(refutingCard), newInfoHandler);
        }
    });
    actionButtons.add(suggestionSubmitButton);
    suggestionMadePanel.add(suggestionSubmitButton);
    enterInfoTabs.add(suggestionMadePanel, "Suggestion made");

    enterInfoTabs.selectTab(0);
    enterInfoPanel.add(enterInfoTabs);
    Label workingLabel = new Label();
    workingLabel.setVisible(false);
    workingLabels.add(workingLabel);
    enterInfoPanel.add(workingLabel);
    gameInfoMainPanel.add(enterInfoPanel);

    DisclosurePanel clauseInfoPanel = new DisclosurePanel("Additional information", false);
    clauseInfoPanel.add(clauseInfoTree);

    consistentLabel = new Label("Game is no longer consistent!");
    consistentLabel.setStylePrimaryName("warning");
    consistentLabel.setVisible(false);
    gameInfoPanel.add(consistentLabel);
    gameInfoPanel.add(gameInfoMainPanel);
    gameInfoPanel.add(clauseInfoPanel);

    VerticalPanel undoHistoryPanel = new VerticalPanel();
    Label undoHistoryLabel = new Label("List of information:");
    undoHistoryPanel.add(undoHistoryLabel);
    undoHistoryBox = new ListBox();
    undoHistoryBox.setVisibleItemCount(15);
    undoHistoryBox.addChangeListener(new ChangeListener() {
        public void onChange(Widget widget) {
            // Don't let them select anything but the bottom one, lest they
            // think they can undo other things.
            undoHistoryBox.setSelectedIndex(undoHistoryBox.getItemCount() - 1);
        }
    });
    undoHistoryPanel.add(undoHistoryBox);
    Button undoButton = new Button("Undo latest information", new ClickListener() {
        public void onClick(Widget widget) {
            // Get the new status
            String descriptionAndState = undoHistoryBox.getValue(undoHistoryBox.getItemCount() - 1);
            // There could be a | in the name but not in the description, so
            // look for the last one.
            setGameState(descriptionAndState.substring(descriptionAndState.lastIndexOf('|') + 1));
            // Remove the last info from the listbox and get new status.
            undoHistoryBox.removeItem(undoHistoryBox.getItemCount() - 1);
            setWorking(true);
            CgiHelper.doRequest(RequestBuilder.GET, scriptName, "sess=" + curSessionString + "&action=fullInfo", newInfoHandler);
        }
    });
    actionButtons.add(undoButton);
    undoHistoryPanel.add(undoButton);
   
    VerticalPanel simulationPanel = new VerticalPanel();
    Button simulateButton = new Button("Simulate", new ClickListener() {
        public void onClick(Widget widget) {
            setWorking(true);
            CgiHelper.doRequest(RequestBuilder.GET, scriptName, "sess=" + curSessionString + "&action=simulate", simulateInfoHandler);
        }
    });
    actionButtons.add(simulateButton);
    simulationPanel.add(simulateButton);
    Label workingLabel2 = new Label();
    workingLabel2.setVisible(false);
    workingLabels.add(workingLabel2);
    simulationPanel.add(workingLabel2);
    simulationPanel.add(new Label("Note that this will take 1-2 minutes."));
    createSimulationTable();
    HorizontalPanel tableAndImage = new HorizontalPanel();
    tableAndImage.add(simulationTable);
    tableAndImage.add(images.rainbowpalette().createImage());
    simulationPanel.add(tableAndImage);



    TabPanel tabs = new TabPanel();
    tabs.add(playerInfoPanel, "Game Setup");
    tabs.add(gameInfoPanel, "Game Info");
    tabs.add(undoHistoryPanel, "Undo and History");
    tabs.add(simulationPanel, "Simulation");
    tabs.addTabListener(new TabListener() {
        public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
            // Don't allow a switch if we're in an invalid state.
            if (!validNumberOfCards) {
                return false;
            }
            return true;
        }
        public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
        }
    });
    tabs.selectTab(0);
    RootPanel.get("main").add(tabs);

    // Get the state of the game.
    setNumberOfPlayers(6, true);
     
  }

  public void createSimulationTable() {
    if (simulationTable == null) {
        simulationTable = new Grid(25, playerNames.size() + 2);
    } else {
        simulationTable.resize(25, playerNames.size() + 2);
    }
    simulationTable.setHTML(1, 0, "<b>Suspects</b>");
    for (int i = 0; i < externalNames[0].length; ++i) {
        simulationTable.setText(2+i, 0, externalNames[0][i]);
    }
    simulationTable.setHTML(8, 0, "<b>Weapons</b>");
    for (int i = 0; i < externalNames[1].length; ++i) {
        simulationTable.setText(9+i, 0, externalNames[1][i]);
    }
    simulationTable.setHTML(15, 0, "<b>Rooms</b>");
    for (int i = 0; i < externalNames[2].length; ++i) {
        simulationTable.setText(16+i, 0, externalNames[2][i]);
    }
    for (int i = 0; i < playerNames.size(); ++i) {
        simulationTable.setText(0, 1+i, (String) playerNames.get(i));
    }
    simulationTable.setText(0, playerNames.size() + 1, "Solution");
  }

  public void setNumberOfPlayers(int numP, boolean startNewGame) {
    // Fix the radio button if it needs fixing.
    int numPAccordingToRadioButtons = -1;
    for (int i = 0; i < playerRadioButtons.size(); ++i) {
        if (((RadioButton) playerRadioButtons.get(i)).isChecked()) {
            numPAccordingToRadioButtons = 3 + i;
        }
    }
    if (numPAccordingToRadioButtons != numP) {
        ((RadioButton) playerRadioButtons.get(numP - 3)).setChecked(true);
    }
    int curNumP = playerNames.size();
    int deltaNumP = numP - curNumP;
    if (curNumP > numP) {
        while (curNumP > numP) {
            namesPanel.remove(namesPanel.getWidgetCount() - 1);
            playerNames.remove(playerNames.size() - 1);
            --curNumP;
        }
    } else if (curNumP < numP) {
        while (curNumP < numP) {
            playerNames.add("Player " + new Integer(curNumP + 1).toString());
            namesPanel.add(new NameSuggestPanel((String) playerNames.get(curNumP), curNumP, this));
            ++curNumP;
        }
    }
    // Update the number of cards
    int[] numCards = new int[numP];
    // There are 18 cards among the players.
    int baseNumCards = 18 / numP;
    int leftovers = 18 % numP;
    for (int i = 0; i < numP; ++i) {
        numCards[i] = (i < leftovers) ? (baseNumCards + 1) : baseNumCards;
    }
    for (int i = 0; i < numP; ++i) {
        ((NameSuggestPanel) namesPanel.getWidget(i)).setDefaultNumCards(numCards[i]);
    }
    // Update the list boxes.
    for (int i = 0; i < playerListBoxes.size(); ++i) {
        ListBox listBox = (ListBox) playerListBoxes.get(i);
        int endCorrection = 0;
        int startIndex = 0;
        // See if we start with an extra item.
        if (listBox.getValue(0).equals("-1")) {
            startIndex = 1;
        }
        // See if we end with an extra item.
        if (listBox.getValue(listBox.getItemCount() - 1).equals(new Integer(numP - deltaNumP).toString())) {
            endCorrection = 1;
        }
        int currentNumInListBox = listBox.getItemCount() - startIndex;

        if (deltaNumP > 0) {
            for (int j = 0; j < deltaNumP; ++j) {
                listBox.insertItem((String) playerNames.get(j+(currentNumInListBox-1)), new Integer(j+currentNumInListBox).toString(), (listBox.getItemCount() - 1) - endCorrection);
            }
        } else if (deltaNumP < 0) {
            for (int j = 0; j > deltaNumP; --j) {
                listBox.removeItem(listBox.getItemCount() - 1 - endCorrection);
            }
        }
        // Fix the extra item (solution) value at the end, if there is one.
        if (endCorrection == 1) {
           listBox.setValue(listBox.getItemCount() - 1, new Integer(numP).toString());
        }
    }
    // Update the simulation table
    createSimulationTable();
    if (startNewGame) {
        doNewGameRequest(); 
    }
  }

  public void checkTotalNumCards() {
    int totalNumCards = 0;
    for (int i = 0; i < playerNames.size(); ++i) {
        totalNumCards += ((NameSuggestPanel) namesPanel.getWidget(i)).getNumCards();
    }
    if (totalNumCards != 18) {
        setValidNumberOfCards(false);
    } else {
        setValidNumberOfCards(true);
        doNewGameRequest();
    }

  }
  public void doNewGameRequest() {
    StringBuffer requestStringBuffer = new StringBuffer();
    requestStringBuffer.append("action=new&players=" + playerNames.size());
    for (int i = 0; i < playerNames.size(); ++i) {
        int numCards = ((NameSuggestPanel) namesPanel.getWidget(i)).getNumCards();
        requestStringBuffer.append("&numCards" + new Integer(i).toString() + "=" + numCards);
    }
    CgiHelper.doRequest(RequestBuilder.POST, scriptName, requestStringBuffer.toString(), new CgiResponseHandler() {
        public void onSuccess(String body) {
            JSONObject response = JSONParser.parse(body).isObject();
            double errorStatus = response.get("errorStatus").isNumber().getValue();
            if (errorStatus != 0.0) {
                Window.alert("Internal error - error returned from script - " + response.get("errorText").isString().toString());
            } else {
                setGameState(response.get("session").isString().stringValue());
            }
        }
        public void onError(Throwable ex) {
            Window.alert("Internal error - unable to contact backend for new session - " + ex.getMessage());
        }
    });
 
  }

  public void startNewGame() {
    undoHistoryBox.clear();
    if (infoAdded == true) {
        infoAdded = false;
        for (int i = 0; i < numPlayersButtons.size(); ++i) {
            RadioButton button = ((RadioButton) numPlayersButtons.get(i));
            button.setEnabled(true);
        }
        // enable changing number of cards
        for (int i = 0; i < playerNames.size(); ++i) {
            ((NameSuggestPanel) namesPanel.getWidget(i)).setNumCardsEnabled(true);
        }
 
    }
    setNumberOfPlayers(playerNames.size(), true);
    // Reset the widgets
    Set stateWidgetKeys = internalNameToClueStateWidgetMap.entrySet();
    for (Iterator it = stateWidgetKeys.iterator(); it.hasNext();) {
        Map.Entry curEntry = (Map.Entry) it.next(); 
        ClueStateWidget curWidget = (ClueStateWidget) curEntry.getValue();
        curWidget.setState(ClueStateWidget.STATE_UNKNOWN, null);
    }
 
  }

  public ClueStateWidget getStateWidget(String id) {
      return (ClueStateWidget) internalNameToClueStateWidgetMap.get(id);
  }

  public static String listBoxValue(ListBox lb) {
      return lb.getValue(lb.getSelectedIndex());
  }

  public ListBox makeNewCardListBox(int index, boolean includeNoneUnknown) {
      ListBox toReturn = new ListBox();
      if (includeNoneUnknown) {
          toReturn.addItem("None/Unknown", "None");
      }
      if (index == -1) {
        for (int i = 0; i < externalNames.length; ++i) {
            for (int j = 0; j < externalNames[i].length; ++j) {
                toReturn.addItem(externalNames[i][j], internalNames[i][j]);
            }
        }
      } else {
        for (int i = 0; i < externalNames[index].length; ++i) {
            toReturn.addItem(externalNames[index][i], internalNames[index][i]);
        }
      }
      return toReturn;
  }

  public ListBox makeNewPlayerListBox(boolean includeNone, boolean includeSolution) {
      ListBox toReturn = new ListBox();
      if (includeNone) {
          toReturn.addItem("None", "-1");
      }
      for (int i = 0; i < playerNames.size(); ++i) {
          toReturn.addItem((String) playerNames.get(i), new Integer(i).toString());
      }
      if (includeSolution) {
          toReturn.addItem("Solution (case file)", new Integer(playerNames.size()).toString());
      }
      playerListBoxes.add(toReturn);
      return toReturn;
  }

  public void changePlayerName(int index, String newName) {
      playerNames.set(index, newName);
      for (int i = 0; i < playerListBoxes.size(); ++i) {
        ListBox listBox = ((ListBox) playerListBoxes.get(i));
        // See if we start with an extra item.
        int curIndex = index;
        if (listBox.getValue(0).equals("-1")) {
            ++curIndex;
        }
        listBox.setItemText(curIndex, newName);
      }
      // Update the tooltips on the images in the ClueStateWidgets
      Set stateWidgetKeys = internalNameToClueStateWidgetMap.entrySet();
      for (Iterator it = stateWidgetKeys.iterator(); it.hasNext();) {
          Map.Entry curEntry = (Map.Entry) it.next(); 
          ClueStateWidget curWidget = (ClueStateWidget) curEntry.getValue();
          curWidget.setImage();
      }
      // Update the simulation table.
      simulationTable.setText(0, 1+index, newName);
  }

  public void setGameState(String sessionString) {
      curSessionString = sessionString;
      gameStateLabel.setText(sessionString);
  }

}
