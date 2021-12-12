/* last update : 12/11/2021
Export class - converts displayed data into external documents
called by Main
calls ErrorClass to display error messages or get user input for document name
 */

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class Export {

    private final Tokenizer tokenizer;
    private final String htmlContent;
    private final String originalContent;

    /** @param t tokenizer object instantiated from Main
     * @param fileName title to apply to HTML file
     * @param o original text from display area
     */
    public Export(Tokenizer t, String fileName, String o) {

        tokenizer = t;

        htmlContent = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta http-equiv='X-UA-Compatible' "
                + "content='IE=edge'><link rel='stylesheet' "
                + "href='https://cdn.jsdelivr.net/npm/bootstrap@4.6.1/dist/css/bootstrap.min.css' "
                + "integrity='sha384-zCbKRCUGaJDkqS1kPbPd7TveP5iyJE0EjAuZQTgFLD2ylzuqKfdKlfG/eSrtxUkn' "
                + "crossorigin='anonymous'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>" + fileName + "</title></head><body><div class='container mt-4'></p>";

        originalContent = o;

        try {
            //create the temp file and directory
            String path =
                    "./tmp/" + new ErrorClass(ErrorClass.ErrorType.GET_TEXT,
                            "Export File Name", "User defined file name?").returnString + ".html";
            if (!path.equals("./tmp/-1.html")) {
                File tempFile = new File(path);

                //after the directory/files are created, generate the html
                String content = getHtml();
                FileOutputStream fos = new FileOutputStream(tempFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                byte[] data = content.getBytes(StandardCharsets.UTF_8);

                bos.write(data);
                bos.close();
                fos.close();

                Desktop.getDesktop().open(new File(path));
            }
        } catch (IOException e) {
            new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "WARNING", e.getMessage());
        }
    }

    /**@param textOfFile text taken from displayed text area to be exported */
    public Export(String textOfFile){
        tokenizer = new Tokenizer("");
        htmlContent = "";
        originalContent = textOfFile;
        try {

            //name the file
            String path = "./tmp/" + new ErrorClass(ErrorClass.ErrorType.GET_TEXT,
                    "Export File Name", "User defined file name?").returnString + ".txt";

            if (!path.equals("./tmp/-1.txt")) {

                FileWriter fileWriter = new FileWriter(path);
                fileWriter.write(originalContent);
                fileWriter.close();

                Desktop.getDesktop().open(new File(path));

            }
        } catch (IOException e) {
            new ErrorClass(ErrorClass.ErrorType.UNKNOWN_ERROR, "WARNING", e.getMessage());
        }

    }

    /** @return  string of displayed text with HTML tags for highlights*/
    public String getHtml() {
        final StringBuilder contentBuilder = new StringBuilder(originalContent);

        int currentParagraph =
                (tokenizer.allWords.size() > 0 ? tokenizer.allWords.get(tokenizer.allWords.size()-1).paragraph : 0);

        for(int i = tokenizer.allWords.size()-1; i >= 0; i--){

            Tokenizer.Word word = tokenizer.allWords.get(i);

            if(currentParagraph > word.paragraph){

                int wordLength = word.positionEnd-word.positionStart;
                if(word.punctuation){ wordLength++; }

                contentBuilder.insert(word.positionStart + wordLength, "<br /><br />");
                currentParagraph = word.paragraph;
            }

            if (word.highlightFlag){
                if(word.highlightColor == null) { word.highlightColor = Color.white; }
                String color = String.format("<span style='background-color:#%02x%02x%02x'>",
                        word.highlightColor.getRed(), word.highlightColor.getGreen(), word.highlightColor.getBlue()
                );

                contentBuilder.insert(word.positionEnd, "</span>").insert(word.positionStart, color);

            }
        }

        contentBuilder.insert(0, htmlContent).append("</div></body></html>");
        return contentBuilder.toString();
    }


}
