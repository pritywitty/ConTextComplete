/* last update : 12/11/2021
Main Class - builds the GUI, handles file opening, and some of the search logic
calls Tokenizer to tokenize document into word instances
calls Search to sort search data and identify found words
calls Export to convert displayed data into TXT or HTML document
calls ErrorClass to display messages or get input from user
*/

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.BoxLayout;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.BorderFactory;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {
    private final List<CreateSearchWidget> searches = new ArrayList<>();
    public Tokenizer tokenizer;
    private final CreateMenu createMenu;
    public final TextEditor textEditor;
    public final SearchWidgetsPanel searchWidgetsPanel;
    public final StatusBar statusBar;

    //makes main GUI frame
    Main() {
        new ErrorClass(ErrorClass.ErrorType.INFO, "Legal Notice",
                "By clicking OK, you acknowledge understanding of Terms of Use " +
                "https://sites.google.com/view/context-document-highlighter/terms-of-service");

        //make the logo
        URL url = getClass().getResource("ConTextLogo.JPG");
        ImageIcon imageIcon;
        if(url != null) {
            imageIcon = new ImageIcon(url);
            setIconImage(imageIcon.getImage());
        }
        setTitle("ConText");

        int W_WIDTH = 900, W_HEIGHT = 500, SEARCH_WIDTH;
        JPanel mainContentPanel, contentOverStatus;

        setSize(new Dimension(W_WIDTH, W_HEIGHT));

        createMenu = new CreateMenu(); //menu bar
        setJMenuBar(createMenu);
        createMenu.openSubMenu.setEnabled(true);

        mainContentPanel = new JPanel(); //container for the text area and search controls
        mainContentPanel.setSize(new Dimension(W_WIDTH-25, W_HEIGHT-25));
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.X_AXIS));

        searchWidgetsPanel = new SearchWidgetsPanel();
        SEARCH_WIDTH = searchWidgetsPanel.getWidth();
        searchWidgetsPanel.setMaximumSize(new Dimension(SEARCH_WIDTH, 900));

        textEditor = new TextEditor(W_WIDTH-SEARCH_WIDTH-100, W_HEIGHT-50);
        textEditor.jTextPane.setEditable(true);

        mainContentPanel.add(textEditor);
        mainContentPanel.add(searchWidgetsPanel);
        contentOverStatus = new JPanel();
        contentOverStatus.setLayout(new BoxLayout(contentOverStatus, BoxLayout.Y_AXIS));

        statusBar = new StatusBar(W_WIDTH);
        statusBar.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        contentOverStatus.add(mainContentPanel);
        contentOverStatus.add(statusBar);

        add(contentOverStatus);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e){
                super.windowStateChanged(e);
                packFrame();
            }
        });

        setVisible(true);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                int w = getWidth()-SEARCH_WIDTH-75, h=getHeight()-50;
                textEditor.setDimensions(w, h);
            }
        });

    } // Main

        //when components are added or removed, sets frame size to larger of user-adjusted or space required
        private void packFrame(){
        int h = getHeight(), w = getWidth();
        pack();
        int newW = Math.max(w, getWidth()), newH = Math.max(h, getHeight());
        setSize(new Dimension(newW, newH));

    }

    //creates the File menu to open/ export/ lock/ unlock document
    private class CreateMenu extends JMenuBar {
        JMenu jMenu, exportSubMenu;
        JMenuItem openSubMenu, expHTML, expTXT, help;

        CreateMenu(){
            jMenu = new JMenu("File");
            jMenu.setMnemonic(KeyEvent.VK_F);

            openSubMenu = new JMenuItem("Open...");
            openSubMenu.setMnemonic(KeyEvent.VK_O);
            openSubMenu.addActionListener(a -> OpenFile()); //actions in method
            jMenu.add(openSubMenu);

            exportSubMenu = new JMenu("Export as...");
            exportSubMenu.setMnemonic(KeyEvent.VK_E);

            expHTML = new JMenuItem("HTML");
            expHTML.setMnemonic(KeyEvent.VK_H);
            expHTML.addActionListener(e -> {
                Export exporter = new Export(tokenizer, "Exported Document", textEditor.jTextPane.getText());
                //returns false if there is an error.
                //todo - notify user if there is an error
                exporter.exportHtml();
            });

            expTXT = new JMenuItem("TXT");
            expTXT.setMnemonic(KeyEvent.VK_T);
            expTXT.addActionListener(e -> {
                Export exporter = new Export(textEditor.jTextPane.getText());
                exporter.exportText();
            });

            exportSubMenu.add(expHTML);
            exportSubMenu.add(expTXT);

            jMenu.add(exportSubMenu);

            help = new JMenuItem("Help...");
            help.setMnemonic(KeyEvent.VK_P);
            help.addActionListener(h -> new ErrorClass(ErrorClass.ErrorType.INFO, "Visit Help", "User Guide available at " +
                    "https://sites.google.com/view/context-document-highlighter/documentation"));

            jMenu.add(help);

            add(jMenu);
        }

    } // CreateMenu

        //opens new document
        private void OpenFile() {
        JFileChooser fileChooser;
        File selectedFile = null;
        boolean newFile;

        do {
            fileChooser = new JFileChooser(FileSystemView.getFileSystemView());
            int o = fileChooser.showOpenDialog(this);

            if (o == JFileChooser.APPROVE_OPTION) { selectedFile = fileChooser.getSelectedFile(); }

            if (selectedFile != null) {

                //OPEN PDF FILE
                if (selectedFile.toString().endsWith("pdf")) {
                    String parsedPDF = "";

                    try {
                        PDFParser pdfParser = new PDFParser(new RandomAccessFile(selectedFile, "r"));
                        pdfParser.parse();
                        COSDocument cosDocument = pdfParser.getDocument();
                        PDDocument pdDocument = new PDDocument(cosDocument);
                        PDFTextStripper pdfTextStripper = new PDFTextStripper();

                        File tempFile = new File("temp.txt");
                        PrintWriter out = new PrintWriter(tempFile);
                        out.println(pdfTextStripper.getText(pdDocument));

                        pdDocument.close();
                        out.close();

                        selectedFile = tempFile;

                    } catch (IOException e) {
                        newFile = new ErrorClass(ErrorClass.ErrorType.FILE_ERROR,
                                "File Error", "There was an error opening the file. " + e).reDo;
                    }
                    textEditor.jTextPane.setText(parsedPDF);
                }

                //OPEN TXT FILE
                if (selectedFile.toString().endsWith("txt")) {
                    try {
                        StringBuilder text = new StringBuilder();

                        List<String> lines = Files.readAllLines(selectedFile.toPath());
                        for (String x : lines) { text.append(x).append("\n"); }
                        textEditor.jTextPane.setText(text.toString());
                        newFile = false;

                    } catch (IOException ex) {
                        newFile = new ErrorClass(ErrorClass.ErrorType.FILE_ERROR,
                                "File Error", "There was an error opening the file." + ex).reDo;
                    }

                }

                else {
                    newFile = new ErrorClass(ErrorClass.ErrorType.FILE_ERROR,
                            "File Error", "This file format is not currently supported.").reDo;
                }

            }

            else { newFile = false; }

        } while (newFile);

    } // OpenFile

    //create area for document to be displayed as original and highlighted
    private static class TextEditor extends JPanel {
        JTextPane jTextPane;
        JScrollPane jScrollPane;
        Highlighter highlighter;

        /** @param editorWidth width of text display area
         * @param editorHeight height of text display area
         */
        TextEditor(int editorWidth, int editorHeight){
            setPreferredSize(new Dimension(editorWidth, editorHeight));

            jTextPane = new JTextPane();
            jTextPane.setText("Highlight World!");

            jScrollPane = new JScrollPane(jTextPane);
            jScrollPane.setPreferredSize(new Dimension(editorWidth-25, editorHeight-25));

            highlighter = jTextPane.getHighlighter();
            try {
                highlighter.addHighlight(0, 8, new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));
            }
            catch (BadLocationException e) {
                new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "Highlight Error", e.toString());
            }

            add(jScrollPane);
        }

        /** @param w new width of text display area
         * @param h new height of text display area
         */
        void setDimensions(int w, int h){ jScrollPane.setPreferredSize(new Dimension(w-50, h-50)); }

    } // CreateTextEditor

    //create panel for search boxes and display
    private class SearchWidgetsPanel extends JPanel {

        CreateSearchWidget initialSearchWidget;
        JPanel widgetContainer, widgetControls, leftControls, rightControls;
        JButton addSearch, highlightButton, clearHighlightsButton,
                lockDocButton, sentenceDocButton, paragraphDocButton;

        SearchWidgetsPanel(){
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setSize(new Dimension(200, 400));

            widgetContainer = new JPanel();
            widgetContainer.setSize(new Dimension(200,300));
            //widgetContainer.setPreferredSize(new Dimension(200,300));
            widgetContainer.setLayout(new BoxLayout(widgetContainer, BoxLayout.Y_AXIS));

            initialSearchWidget = new CreateSearchWidget();
            initialSearchWidget.removeSearchButton.setVisible(false);

            //LIST OF SEARCHES
            searches.add(initialSearchWidget);
            widgetContainer.add(initialSearchWidget);

            widgetControls = new JPanel();
            addSearch = new JButton("Add Search");
            addSearch.addActionListener(a -> {
                //ADD SEARCH BUTTON LIMIT
                if (searches.size() < 8) {
                    CreateSearchWidget addWidget = new CreateSearchWidget();
                    searches.add(addWidget);
                    addWidget.removeSearchButton.addActionListener(b -> {
                        widgetContainer.remove(addWidget);
                        searches.remove(addWidget);
                        packFrame();
                    });
                    widgetContainer.add(addWidget);
                    packFrame();
                }

                else { new ErrorClass(ErrorClass.ErrorType.INFO,"ERROR", "Limit 8 searches"); }

            });

            clearHighlightsButton = new JButton("Clear Highlights");
            clearHighlightsButton.addActionListener(c -> {
                textEditor.highlighter.removeAllHighlights(); //clear highlight
                if(tokenizer != null) {
                    for (Tokenizer.Word word : tokenizer.allWords) { word.highlightFlag = false; } //clear flags
                }
            });

            //HIGHLIGHT DOCUMENT BUTTON
            highlightButton = new JButton("Highlight");
            highlightButton.addActionListener(h -> highlightAction()); // highlight button action

            sentenceDocButton = new JButton("Sentences Only");
            sentenceDocButton.addActionListener(s -> {
                List<Integer> removeSentences = removalList(true, false);

                int from, length;
                try {
                    for (int i = removeSentences.size() - 1; i >= 0; i--) {
                        from = tokenizer.sentenceStarts.get(removeSentences.get(i));
                        length = tokenizer.sentenceEnds.get(removeSentences.get(i)) - from;
                        textEditor.jTextPane.getDocument().remove(from, length);
                    }
                }
                catch (BadLocationException e) {
                    new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "Removal Error", e.toString());
                }
                removeSentences.clear();

                tokenizer = new Tokenizer(textEditor.jTextPane.getText());
                highlightAction();
            });

            paragraphDocButton = new JButton("Paragraphs Only");
            paragraphDocButton.addActionListener(p -> {
                List<Integer> removeParagraphs = removalList(false, true);

                int from, length;
                try {
                    for (int i = removeParagraphs.size() - 1; i >= 0; i--) {
                        from = tokenizer.paragraphStarts.get(removeParagraphs.get(i));
                        length = tokenizer.paragraphEnds.get(removeParagraphs.get(i)) - from;
                        textEditor.jTextPane.getDocument().remove(from, length);

                        //take out extra line otherwise left by removed paragraph
                        if(textEditor.jTextPane.getDocument().getLength() > from + 1){
                            textEditor.jTextPane.getDocument().remove(from, 1);
                        }

                    }
                }
                catch (BadLocationException e) {
                    new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "Removal Error", e.toString());
                }

                removeParagraphs.clear();

                tokenizer = new Tokenizer(textEditor.jTextPane.getText());
                highlightAction();

            });

            //LOCK DOCUMENT BUTTON
            lockDocButton = new JButton("Lock to Highlight");
            highlightButton.setEnabled(false);
            sentenceDocButton.setEnabled(false);
            paragraphDocButton.setEnabled(false);
            lockDocButton.addActionListener(l -> {

                //if document is unlocked
                if(lockDocButton.getText().equals("Lock to Highlight")) {
                    lockDocButton.setText("Unlock to Edit");

                    //lock document to prevent editing and enable highlighting buttons
                    textEditor.jTextPane.setEditable(false);
                    highlightButton.setEnabled(true);
                    sentenceDocButton.setEnabled(true);
                    paragraphDocButton.setEnabled(true);
                    createMenu.openSubMenu.setEnabled(false);

                    tokenizer = new Tokenizer(textEditor.jTextPane.getText()); //tokenize document

                }

                //if document is locked
                else{
                    lockDocButton.setText("Lock to Highlight");

                    //unlock document to allow editing and disable highlighting buttons
                    highlightButton.setEnabled(false);
                    sentenceDocButton.setEnabled(false);
                    paragraphDocButton.setEnabled(false);
                    createMenu.openSubMenu.setEnabled(true);
                    textEditor.jTextPane.setEditable(true);
                }

            }); //lockDoc button action

            leftControls = new JPanel();
            rightControls = new JPanel();
            leftControls.setLayout(new BoxLayout(leftControls, BoxLayout.Y_AXIS));
            rightControls.setLayout(new BoxLayout(rightControls, BoxLayout.Y_AXIS));

            leftControls.add(addSearch);
            leftControls.add(clearHighlightsButton);
            leftControls.add(lockDocButton);

            rightControls.add(highlightButton);
            rightControls.add(sentenceDocButton);
            rightControls.add(paragraphDocButton);

            widgetControls.add(leftControls);
            widgetControls.add(rightControls);

            add(widgetContainer);
            add(widgetControls);

        } //SearchWidgetsPanel

        //actions taken by the highlight button
        private void highlightAction(){

            List<Search> paragraphSearches = new ArrayList<>(),
                    sentenceSearches = new ArrayList<>(),
                    individualSearches = new ArrayList<>();

            searches.forEach(search -> {

                //DATA FROM SEARCH BOX
                String textValue = search.searchQuery.getText();
                int searchTypeSelected = search.searchType.getSelectedIndex();
                Color color = search.highlightColor;
                SearchType type;
                boolean exact = search.exactMatch.isSelected(), caseMatch = search.matchCase.isSelected();

                //ADD SEARCHES TO SEARCH TYPE LISTS
                switch (searchTypeSelected) {
                    case 1:
                        type = SearchType.PARAGRAPH;
                        paragraphSearches.add(new Search(tokenizer, textValue, type, color, exact, caseMatch));
                        break;
                    case 2:
                        type = SearchType.SENTENCES;
                        sentenceSearches.add(new Search(tokenizer, textValue, type, color, exact, caseMatch));
                        break;
                    default:
                        type = SearchType.WHOLE_DOCUMENT;
                        individualSearches.add(new Search(tokenizer, textValue, type, color, exact, caseMatch));
                        break;
                }
            });

            //GET LISTS OF ALL WORDS IN MAP
            paragraphSearches.forEach(Search::wordListInMap);
            sentenceSearches.forEach(Search::wordListInMap);
            individualSearches.forEach(Search::wordListInMap);

            //PARAGRAPH SEARCH
            if(!paragraphSearches.isEmpty()) { paragraphSearch(paragraphSearches); }

            //SENTENCE SEARCH
            //if there are sentence searches
            if(!sentenceSearches.isEmpty()) { sentenceSearch(sentenceSearches); }

            //INDIVIDUAL SEARCH
            //set highlight flag and color for each word for each individual search
            if(!individualSearches.isEmpty()) {
                for (Search individualSearch : individualSearches) { individualSearch.setHighlightI(); }
            }

            //SET HIGHLIGHTS
            //for all words- if highlight flag is set, highlight word with that color
            int i = 0;
            for(Tokenizer.Word word : tokenizer.allWords){

                if(word.highlightFlag){
                    i++;
                    try {
                        textEditor.highlighter.addHighlight(word.positionStart, word.positionEnd,
                                new DefaultHighlighter.DefaultHighlightPainter(word.highlightColor));
                    } catch (BadLocationException e) {
                        new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "Highlight Error", e.toString());
                    }
                }
                else{ word.highlightColor = Color.white; }

            }

            statusBar.foundCount.setText("" + i);
            statusBar.wordCount.setText("" + (tokenizer.allWords.size()));

            StringBuilder notice = new StringBuilder();
            if (tokenizer.allWords.size() < 1) { notice.append(" ... | no text to search | ... "); }
            if(i < 1){ notice.append(" | no matches found | ... "); }

            statusBar.noticeLabel.setText(notice.toString());

        } // highlightAction

    } // CreateSearchParameterMenu

    //creates instances of keyword search boxes
    private static class CreateSearchWidget extends JPanel {

        JPanel leftPanel, rightPanel;
        JTextField searchQuery;
        JCheckBox exactMatch, matchCase;
        JComboBox<String> searchType;
        Color highlightColor;
        JColorChooser colorPicker;
        JButton removeSearchButton, colorButton;

        CreateSearchWidget() {
            leftPanel = new JPanel();
            rightPanel = new JPanel();

            leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

            searchQuery = new JTextField();
            searchQuery.setPreferredSize(new Dimension(200, 25));
            searchQuery.setToolTipText("Enter keyword(s) here");

            exactMatch = new JCheckBox("Exact Match");
            matchCase = new JCheckBox("Match Case");

            searchType = new JComboBox<>(Search.searchTypes);
            searchType.setPreferredSize(new Dimension(200, 25));
            searchType.setToolTipText("Search for individual keyword or by keywords in the same sentence or paragraph");

            colorButton = new JButton("Color...");
            colorPicker = new JColorChooser();
            colorButton.addActionListener(b -> {
                highlightColor = JColorChooser.showDialog(colorPicker, "Highlight color", Color.yellow);
                colorButton.setBackground(highlightColor);
            });

            removeSearchButton = new JButton("X");
            removeSearchButton.setBackground(Color.red);

            leftPanel.add(searchQuery);
            leftPanel.add(searchType);
            leftPanel.add(colorButton);
            leftPanel.add(new JLabel(" "));

            rightPanel.add(exactMatch);
            rightPanel.add(matchCase);
            rightPanel.add(removeSearchButton);

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(leftPanel);
            add(rightPanel);
            setMaximumSize(new Dimension(350, 100));

        }

    } // CreateSearchWidget

    //creates status bar at the bottom of the window to notify user of document statistics and search results
    public static class StatusBar extends JPanel{

        JLabel wordCountLabel = new JLabel("Word Count"), foundWordsLabel = new JLabel("Found Count"),
                noticeLabel = new JLabel("");
        JTextField wordCount = new JTextField("2"), foundCount = new JTextField("1");

        /** @param width preset width of window */
        StatusBar(int width){

            setPreferredSize(new Dimension(width, 30));
            setMaximumSize(new Dimension(1200,30));

            wordCount.setEditable(false);
            wordCount.setPreferredSize(new Dimension(100, 20));
            foundCount.setEditable(false);
            foundCount.setPreferredSize(new Dimension(100, 20));

            add(noticeLabel);
            add(wordCountLabel);
            add(wordCount);
            add(foundWordsLabel);
            add(foundCount);

        }
    } // StatusBar

        //execute search for words that match in same sentence
        /** @param sentenceSearches all searches that require the context of same sentence*/
        void sentenceSearch(List<Search> sentenceSearches){

            List<Integer> includeSentences, holdSentenceList = new ArrayList<>();

            //if the first search's list of sentences is empty, return with no highlights
            if (sentenceSearches.get(0).sentences.isEmpty()) { return; }

            //if the first search's list of sentences is not empty,
            // put the list of sentences from the first search in the list of included sentences
            else{ includeSentences = new ArrayList<>(sentenceSearches.get(0).sentences); }

            //if the list of sentences is not empty,
            if (!includeSentences.isEmpty()) {
                //for each subsequent search
                for (int i = 1; i < sentenceSearches.size() && !includeSentences.isEmpty(); i++) {

                    //and for each sentence in that search's sentence list
                    for( int sentenceNumber : sentenceSearches.get(i).sentences) {

                        //if the sentence in the search's list is in the include list
                        if (includeSentences.contains(sentenceNumber)){

                            //add the sentence to the hold list
                            holdSentenceList.add(sentenceNumber);
                        }
                    }

                    if(holdSentenceList.isEmpty()){ return; } //if the hold list is empty, return with no highlights


                    else { //if the hold list is not empty
                        includeSentences.clear(); //clear the keep list
                        includeSentences.addAll(holdSentenceList); //put the hold list in the keep list
                        holdSentenceList.clear();//clear the hold list
                    }

                }

                //if the keep list is populated, set highlight flag and color for each word for each search
                if (!includeSentences.isEmpty()) {
                    for (Search sS : sentenceSearches) { sS.setHighlightS(includeSentences); }
                }
            }

        } // sentenceSearch

        //execute search for words that match in same paragraph
        /** @param paragraphSearches all searches that require the context of same paragraph */
        void paragraphSearch(List<Search> paragraphSearches){

            List<Integer> includeParagraphs, holdParagraphList = new ArrayList<>();

            //if the first search's list of paragraphs is empty, return with no highlights
            if (paragraphSearches.get(0).paragraphs.isEmpty()) { return; }

            //if the first search's list of paragraphs is not empty, include the first search's paragraphs
            else{ includeParagraphs = new ArrayList<>(paragraphSearches.get(0).paragraphs); }

            //if the list of paragraphs is not empty,
            if (!includeParagraphs.isEmpty()) {
                //for each subsequent search
                for (int i = 1; i < paragraphSearches.size() && !includeParagraphs.isEmpty(); i++) {

                    //and for each paragraph in that search's paragraph list
                    for( int paragraphNumber : paragraphSearches.get(i).paragraphs) {

                        //if the paragraph in the search's list is in the include list
                        if (includeParagraphs.contains(paragraphNumber)){

                            //add the paragraph to the hold list
                            holdParagraphList.add(paragraphNumber);
                        }
                    }

                    if(holdParagraphList.isEmpty()){ return; } //if the hold list is empty, return with no highlights

                    else { //if the hold list is not empty
                        includeParagraphs.clear(); //clear the keep list
                        includeParagraphs.addAll(holdParagraphList); //put the hold list in the keep list
                        holdParagraphList.clear();//clear the hold list
                    }

                }

                //if the keep list is populated, set highlight flag and color for each word for each search
                if (!includeParagraphs.isEmpty()) {
                    for (Search pS : paragraphSearches) { pS.setHighlightP(includeParagraphs); }
                }
            }

        } // paragraphSearch

        /** @param sentence true if getting list of sentences to remove
         * @param paragraph true if getting list of paragraphs to remove
         * @return list of sentence or paragraph index numbers to be removed based on tokenizer indexes
         */
        public List<Integer> removalList(boolean sentence, boolean paragraph){
            List<Integer> removals = new ArrayList<>();
            int len;
            if(sentence){
                len = tokenizer.sentenceStarts.size();
                for(int i = 0; i < len; i++){ removals.add(i); }
                tokenizer.allWords.forEach(w ->{
                    if(removals.contains(w.sentenceIndex) && w.highlightFlag){
                        removals.remove(removals.indexOf(w.sentenceIndex));
                    }
                });
                statusBar.noticeLabel.setText("Removing " + removals.size() + "sentences... ");
            }

            else if(paragraph){
                len = tokenizer.paragraphStarts.size();
                for(int i = 0; i < len; i++){ removals.add(i); }
                tokenizer.allWords.forEach(w ->{
                    if(removals.contains(w.paragraph) && w.highlightFlag){
                        removals.remove(removals.indexOf(w.paragraph));
                    }
                });
                statusBar.noticeLabel.setText("Removing " + removals.size() + "paragraphs... ");
            }

            return removals;

        } // removalList

        //main method
        public static void main(String[] args) { new Main(); } // main

} // Main
