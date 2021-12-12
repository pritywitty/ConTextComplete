/* last update : 12/11/2021
Search class - searches for matches using data from search boxes and Tokenizer class

 */
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

enum SearchType{ WHOLE_DOCUMENT, SENTENCES, PARAGRAPH }

public class Search {
    public static String[] searchTypes = {"Individual Search", "Same Paragraph Search", "Same Sentence Search"};
    public Tokenizer tokenizer;
    public List<String> keywords;
    public Color color;
    public boolean exactMatch, caseMatch;
    SearchType searchType;
    List<Tokenizer.Word> foundWords;
    List<Integer> paragraphs = new ArrayList<>(), sentences = new ArrayList<>();

    /** @param t Tokenizer instantiated at last document lock
     * @param k keyword box text (to be broken up when Search instantiated)
     * @param s WHOLE_DOCUMENT, SENTENCES, PARAGRAPH
     * @param c color
     * @param e exact match flag
     * @param m match case flag
     * */
    Search(Tokenizer t, String k, SearchType s, Color c, boolean e, boolean m){
        searchType = s;
        tokenizer = t;
        keywords = new ArrayList<>();
        setKeywords(k);
        color = c;
        foundWords = new ArrayList<>();
        exactMatch = e;
        caseMatch = m;
    }

    /**@param text text from search box*/
    public void setKeywords(String text) {
        Scanner scanner = new Scanner(text);
        String keyword;
        while (scanner.hasNext()) {
            keyword = scanner.next();

            //save keywords without ending punctuation
            if(keyword.endsWith(".") || keyword.endsWith("?") || keyword.endsWith("!") || keyword.endsWith(",")){
                keyword = keyword.substring(0, keyword.length() - 1);
            }
            keywords.add(keyword);
        }
    }

    //finds instances of consecutive matches of keywords in word map
    void wordListInMap(){
        List<Tokenizer.Word> holderList = new ArrayList<>(), keepWords;

        //don't search if no keywords were entered
        if(keywords.size() < 1) { return; }

        else {

            //get a list of each word in map that contains the first keyword
            keepWords = new ArrayList<>(tokenizer.found(keywords.get(0), exactMatch, caseMatch));

            //if more than one keyword is entered
            if (keywords.size() > 1) {

                //for each subsequent keyword
                for (int i = 1; i < keywords.size(); i++) {
                    String nextKeyWord = keywords.get(i);

                    //and for each word in keep list
                    for (Tokenizer.Word keptWord : keepWords) {

                        //if the next word in the document matches the next keyword
                        if (tokenizer.nextWordMatch(keptWord, nextKeyWord, exactMatch, caseMatch)) {

                            //hold that next word
                            holderList.add(tokenizer.nextWord(keptWord));
                        }
                    }
                    keepWords.clear();
                    keepWords.addAll(holderList);
                    holderList.clear();
                }

                //if list of words to keep is not empty,
                if (!keepWords.isEmpty()) {
                    List<Tokenizer.Word> lastWord = new ArrayList<>(keepWords);
                    holderList.clear();

                    for (int i = 1; i < keywords.size(); i++) { // for length of list of KEY words
                        for(Tokenizer.Word last : lastWord) { // for each token in list of KEPT words
                            holderList.add(tokenizer.wordBefore(last)); // add the previous token to the pass off list
                        }
                        keepWords.addAll(holderList); //add previous word token to holder list
                        lastWord.clear(); //clear token list
                        lastWord.addAll(holderList); //repopulate token list with new tokens
                        holderList.clear();
                    }
                }
            }
        }

        if(keepWords.isEmpty()){ return; }

        else {
            keepWords.forEach(word -> {
                if(!paragraphs.contains(word.paragraph)){ paragraphs.add(word.paragraph); }
                if(!sentences.contains(word.sentenceIndex)){ sentences.add(word.sentenceIndex); }
            });
        }

        foundWords.addAll(keepWords);
    }

    //highlighter for paragraph searches
    /**@param highlights indexes of words to highlight*/
    void setHighlightP(List<Integer> highlights){
        foundWords.forEach(foundWord -> {
            if(highlights.contains(foundWord.paragraph)){
                foundWord.highlightFlag = true;
                foundWord.highlightColor = color;
            }
        });
    }

    //highlighter for same sentence searches
    /**@param highlights indexes of words to highlight*/
    void setHighlightS(List<Integer> highlights){
        foundWords.forEach(foundWord -> {
            if(highlights.contains(foundWord.sentenceIndex)){
                foundWord.highlightFlag = true;
                foundWord.highlightColor = color;
            }
        });
    }

    //highlighter for individual searches
    void setHighlightI(){
        foundWords.forEach(foundWord -> {
            foundWord.highlightFlag = true;
            foundWord.highlightColor = color;
        });
    }

}
