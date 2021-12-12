/* last update : 12/11/2021
ErrorClass - displays messages to the user for errors or data collection
 */

import javax.swing.JOptionPane;
import java.util.UUID;

public class ErrorClass {

    enum ErrorType { FILE_ERROR, UNSUPPORTED_FILE_TYPE, UNKNOWN_ERROR, INFO, GET_TEXT}
    String errorMessage = "", errorTitle, returnString = "";
    ErrorType errorType;
    boolean reDo = false;

    /** @param e error type
     * @param t title
     * @param m message
     */
    ErrorClass(ErrorType e, String t, String m ){
        errorType = e;
        errorTitle = t;

        //call appropriate class for each error type
        switch (e){
            case INFO:
                errorMessage = m;
                informationOnly();
                break;
            case UNKNOWN_ERROR:
                errorMessage = "An unknown error has occurred, please contact your local sys admin. Reference: " + m;
                informationOnly();
                break;
            case UNSUPPORTED_FILE_TYPE:
                errorMessage = "The filetype selected is not currently supported";
                informationOnly();
                break;
            case FILE_ERROR:
                errorMessage = m + " Would you like to try another file?";
                fileError();
                break;
            case GET_TEXT:
                errorMessage = m;
                getTextDialog();
                break;
        }
    } // ErrorClass constructor

    public void informationOnly(){
            JOptionPane.showMessageDialog(new JOptionPane(), errorMessage, errorTitle, JOptionPane.INFORMATION_MESSAGE);
    } // informationOnly

    public void fileError() {
        reDo = JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(new JOptionPane(),
                errorMessage, errorTitle, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,
                null, null, JOptionPane.CANCEL_OPTION);
    } // fileError

    public void getTextDialog(){
        int option = JOptionPane.showOptionDialog(new JOptionPane(), errorMessage,
                errorTitle, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, null, JOptionPane.CANCEL_OPTION);

        if(JOptionPane.NO_OPTION == option){ returnString = UUID.randomUUID().toString(); }

        else if(JOptionPane.YES_OPTION == option){
            returnString = JOptionPane.showInputDialog(new JOptionPane(), errorMessage);
        }

        else { returnString = "-1"; } //indicate error

    } // getTextDialog

} // ErrorClass