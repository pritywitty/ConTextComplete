/* last update : 12/11/2021
Tokenizer class - tokenizes the document
 */
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {
    String document;
    List<Integer> wordStarts = new ArrayList<>(); // indexes of start of each word token
    List<Integer> paragraphStarts = new ArrayList<>(); // indexes of start of each paragraph
    List<Integer> sentenceStarts = new ArrayList<>();
    List<Integer> paragraphEnds = new ArrayList<>();
    List<Integer> sentenceEnds = new ArrayList<>();
    List<Word> allWords = new ArrayList<>();
    HashMap<String, ArrayList<Word>> words = new HashMap<>();

    /** @param d text of entire document
     *          should be called when document is locked, so tokenization occurs while user enters search keys
     */
    Tokenizer(String d) {

        document = d;

        Matcher wordMatcher = Pattern.compile("\\S+").matcher(document);

        //find each word token and add indexes to list
        while (wordMatcher.find()) { wordStarts.add(wordMatcher.start()); }

        Matcher paragraphMatcher = Pattern.compile("[^\\n\\r]+").matcher(document);

        //add first element of each paragraph to list
        while (paragraphMatcher.find()) {
            paragraphStarts.add(paragraphMatcher.start());
            paragraphEnds.add(paragraphMatcher.end()-1);
        }

        getTokens();

    }

    void getTokens() {

        boolean endOfSentence = true, punctuation = false;
        int wordIndex = 0, paragraphIndex = 0, wordStart = 0, wordEnd = 0, sentenceNumber = 0;
        String wordText;

        Word word;
        ArrayList<Word> sameWords;
        Scanner scanner = new Scanner(document);

        //for each found word
        while (scanner.hasNext()) {

            //add index for beginning of sentence and turn off flag
            if (endOfSentence) {
                sentenceStarts.add(wordStarts.get(wordIndex));
                endOfSentence = false;
            }

            wordText = scanner.next(); //get next token

            //if token is end of sentence, flag end of sentence and shorten word to not include punctuation
            if (wordText.endsWith(".") || wordText.endsWith("?") || wordText.endsWith("!")) {
                wordText = wordText.substring(0, wordText.length() - 1);
                endOfSentence = true;
                punctuation = true;
            }

            //if token includes comma, remove comma
            else if (wordText.endsWith(",")) {
                wordText = wordText.substring(0, wordText.length() - 1);
                punctuation = true;
            }

            //get starting and ending positions of word
            if(wordIndex < wordStarts.size()) {
                wordStart = wordStarts.get(wordIndex);
                wordEnd = wordText.length() + wordStart;
            }

            word = new Word(wordText, paragraphIndex, wordStart, wordEnd, sentenceNumber, punctuation); //create word instance

            //set ArrayList of words to new or existing list of instances of the word
            sameWords = (words.containsKey(wordText) ? words.get(wordText) : new ArrayList<>());
            sameWords.add(word); //add current word to list
            words.put(wordText, sameWords); //add list back into document word-map
            allWords.add(word);
            word.setIndex(allWords.size());

            if(punctuation){ wordEnd++; } //add character to end of word index for sentence / paragraph indexes

            //increment paragraph index if need be
            while (!paragraphEnds.isEmpty() //if list of paragraph end indexes is populated (exception prevention)
                    && paragraphIndex < paragraphEnds.size() //and the current index is not the last index
                    && (wordEnd) >= paragraphEnds.get(paragraphIndex)) { //and this word is at the end of the paragraph
                if(paragraphIndex + 1 < paragraphStarts.size() &&
                        paragraphEnds.get(paragraphIndex) + 1 < paragraphStarts.get(paragraphIndex + 1)){
                    paragraphEnds.set(paragraphIndex, paragraphStarts.get(paragraphIndex + 1) - 1);
                }
                //paragraphEnds.set(paragraphIndex, paragraphEnds.get(paragraphIndex) + 1);
                paragraphIndex++;
            }

            //increment sentence index and add position to list if end of sentence
            if (endOfSentence) {
                sentenceEnds.add(wordEnd);
                sentenceNumber++;
            }

            wordIndex++;

        }

        scanner.close();
    }

    public static class Word {
        String textOfWord;
        int paragraph, positionStart, positionEnd, documentIndex, sentenceIndex;
        boolean highlightFlag = false, punctuation;
        Color highlightColor = Color.white;

        /** @param word  text of word
         * @param p      index of paragraph
         * @param pStart start index of word
         * @param pEnd   end index of word
         * @param sI     index of sentence in document
         * @param punc   flag for words ending in punctuation
         */
        Word(String word, int p, int pStart, int pEnd, int sI, boolean punc) {
            textOfWord = word;
            paragraph = p;
            positionStart = pStart;
            positionEnd = pEnd;
            sentenceIndex = sI;
            punctuation = punc;
        }

        /** @param i number of word in chronological order within document */
        void setIndex(int i){ documentIndex = i; }

    } // Word class

    /**@param keyword search term
     * @param e exact match flag
     * @param m match case flag
     * */
    List<Word> found(String keyword, boolean e, boolean m){
        List<Word> foundWords = new ArrayList<>();
        for(String key : words.keySet()){
            if(m && e){ if (key.equals(keyword)) { foundWords.addAll(words.get(key)); } }
            else if(m){ if (key.contains(keyword)) { foundWords.addAll(words.get(key)); } }
            else if(e) { if (key.equalsIgnoreCase(keyword)) { foundWords.addAll(words.get(key)); } }
            else {
                if (key.toLowerCase().contains(keyword.toLowerCase(Locale.ROOT))) { foundWords.addAll(words.get(key)); }
            }
        }
        return foundWords;
    }

    /**@param keptWord previous word
     * @param b next word
     * @param e exact match flag
     * @param m match case flag
     * */
    boolean nextWordMatch(Word keptWord, String b, boolean e, boolean m){

        int wordIndex = keptWord.documentIndex;
        String wordB = allWords.get(wordIndex).textOfWord;

        if(m && e){return wordB.equals(b);}
        else if(m){ return wordB.contains(b); }
        else if(e) {return wordB.equalsIgnoreCase(b);}
        else { return wordB.toLowerCase().contains(b.toLowerCase()); }
    }

    /** @param a    current word - returns the word after 'a' */
    Word nextWord(Word a){ return allWords.get(a.documentIndex); }

    /** @param a    current word - returns the word before 'a' */
    Word wordBefore(Word a){ return allWords.get(a.documentIndex - 2); }
}
