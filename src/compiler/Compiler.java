
package compiler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Compiler {

    static HashMap tokensLabels;
    static String dbldelimeters[] = {"==", "!=", "&&", "\\|\\|", "<=", ">="};
    static String dbldelimetersencode[] = {"@", "#", "&", "~", "$", "^"};
    static ArrayList<String> codebody;
    static String splitRegex = 
                "(?<= |\\=|\\;|\t|\\(|\\)|\\{|\\}|\\+|\\-|\\/|\\*|\\!|\\,|\\[|\\]|\\@|\\#|\\&|\\~|\\$|\\^|\"|\\%|\\<|\\>)"
                + "|(?= |\\=|\\;|\t|\\(|\\)|\\{|\\}|\\+|\\-|\\/|\\*|\\!|\\,|\\[|\\]|\\@|\\#|\\&|\\~|\\$|\\^|\"|\\%|\\<|\\>)";
    static String regexID = "^[a-zA-Z_][_a-zA-Z0-9]*$";
    static String regexInt = "[0-9]+";
    static String regexFloat = "^[0-9]+[.][0-9]+$";
    static String regexMComment = "\\/\\*.*\\*/";
    static String regexSComment = "\\/\\/.*";
    static String regexString = "^\".+\"$";
    static String regexChar = "^\'.\'$";
    static String regexDotBeg = "^[.][a-zA-Z_][a-zA-Z_0-9]+$";
    static String regexDot = "^[a-zA-Z][a-zA-Z_0-9]+[.][a-zA-Z][a-zA-Z_0-9]+$";
    
    public static HashMap readTokens(String tokensfilename) throws FileNotFoundException{
        HashMap tokenslbl = new HashMap<String,String>();
        Scanner sc = new Scanner(new FileReader(tokensfilename));
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            tokenslbl.put(line.split(" ")[1], line.split(" ")[0]);
        }
        sc.close();
        return tokenslbl;
    }
    
    public static ArrayList<String> readCode(String codefilename) throws FileNotFoundException{
        ArrayList<String> arr = new ArrayList<String>();
        Scanner sc = new Scanner(new FileReader(codefilename));
        while(sc.hasNext())    arr.add(encode(sc.nextLine().replaceAll("\t ", "").replaceAll("^\\s+","").replaceAll("\\s+$","")));
        sc.close();
        return arr;
    }
    
    public static ArrayList<String> fixMComments(String arr []){
        ArrayList<String> fixed = new ArrayList<String>();
        for(int i=0;i<arr.length-1;i++){
            if (arr[i].equals("/") && arr[i+1].equals("*")){
                fixed.add("/*");
                i++;
            }
            else if (arr[i].equals("*") && arr[i+1].equals("/")){
                fixed.add("*/");
                i++;
            }
            else { 
                if (match(arr[i], regexDotBeg)){ fixed.add("."); fixed.add(arr[i].substring(1, arr[i].length())); } 
                else if (match(arr[i], regexDot)){
                    int u = 0;
                    for(int o = 0 ; o < arr[i].length(); o++)   if (arr[i].charAt(o) == '.') u = o;
                    fixed.add(arr[i].substring(0, u));
                    fixed.add("."); fixed.add(arr[i].substring(u+1, arr[i].length()));
                }
                else fixed.add(arr[i]); 
            }
        }
        if (!arr[arr.length-1].equals(" ") && !arr[arr.length-1].equals("*") && !arr[arr.length-1].equals("/")) fixed.add(arr[arr.length-1]);
        return fixed;
    }
    
    public static void run(String codefilename, String resultfilename, String tokensfilename) throws FileNotFoundException, UnsupportedEncodingException{
        ArrayList<String> code = readCode(codefilename);
        ArrayList<String> detcode = new ArrayList<String>();
        for(int i=0; i < code.size(); i++){
            ArrayList<String> fixedCode = fixMComments(code.get(i).split(splitRegex));
            for(int j = 0 ; j < fixedCode.size(); j++)    detcode.add(fixedCode.get(j));
        }
        detcode = decode(combine(detcode));
        printCodeInfo(detcode, resultfilename, tokensfilename);        
    }
    
    // dont forget .(DOT) >> itterate and shift then split . and text not numbers and not functions
    // EOL
    // split */ from (*) (/)
    public static void printCodeInfo(ArrayList<String> codebody, String resultfilename, String tokensfilename) throws FileNotFoundException, UnsupportedEncodingException{
        tokensLabels = readTokens(tokensfilename);
        PrintWriter writer = new PrintWriter(resultfilename, "UTF-8");
        for(String token : codebody){
            if (token.length() == 0) {writer.println("< EOL > : End of line"); continue;}
            if (match(token, regexSComment)) {writer.println("< \"S_COMMENTS\" > : -"+token+"-"); continue;}
            String label = (String) tokensLabels.get(token);
            if (label == null){
                if (match(token, regexID))   label = "ID";
                else if(match(token, regexString)) label = "STRING_LITERAL";
                else if(match(token, regexChar)) label = "A_CHAR";
                else if(match(token, regexFloat)) label = "FLOAT_LITERAL";
                else if(match(token, regexInt)) label = "INTEGRAL_LITERAL";
                else if(match(token, regexMComment)) label = "M_COMMENTS";
                else    label = "Error unknown token";
            }
            writer.println("< "+label+" > : -"+token+"-");
            if (label.equals("SEMICOLON") || label.equals("RIGHT_CURLY_B")){
                writer.println("< EOL > : End of line");
            }
        }
        writer.close();
    }
    
    public static boolean match(String token, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(token);
        if (matcher.find()) return true;
        return false;
    }
    
    public static String encode(String token){
        for(int i = 0; i < dbldelimeters.length; i++)
            token = replace(token, dbldelimetersencode[i], dbldelimeters[i]);
        return token;
    }
    
    public static ArrayList<String> decode(ArrayList<String> tokens){
        for(int i = 0; i < tokens.size(); i++)
            tokens.set(i, replace2(tokens.get(i)));
        return tokens;
    }
    
    public static String replace(String str, String rep, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find())
        {
            str = str.substring(0, matcher.start()) + rep + str.substring(matcher.end(), str.length());
            matcher = pattern.matcher(str);
        }
        return str;
    }
    
    public static String replace2(String str){
        if(!str.equals("~")){
            for(int i = 0 ; i< dbldelimetersencode.length ; i++){
                if (str.equals(dbldelimetersencode[i])){
                    str = dbldelimeters[i];
                    break;
                }
            }
        }
        else str = "||";
        return str;
    }
    
    public static ArrayList<String> combine(ArrayList<String> arr){
        return combineComments(combineStrings(arr));
        /*ArrayList<String> arr = new ArrayList<>();
        for(int i=1; i < str.length; i++){
            String s = str[i-1];
            if (str[i].equals("*")&&str[i-1].equals("/")){
                s = "/*";
                for(int j = i+2; j < str.length; j++){
                    if (str[j].equals("/") && str[j-1].equals("*")){
                        s += "*//*";
                        i = j+2;
                        arr.add(s);
                        break;
                    }
                    s += str[j-1];
                }
                s = str[i-1];
                arr.add(s);
            }
            else if (str[i-1].equals("\"")){
                s = "\"";
                for(int j = i+1; j < str.length; j++){
                    if (str[j].equals("\"")){
                        s = s + str[j-1] + "\"";
                        i = j;
                        arr.add(s);
                        break;
                    }
                    s += str[j-1];
                }
            }
            else{
                if (!s.equals(" "))
                    arr.add(s);
                if (i == str.length-1)
                    arr.add(str[str.length-1]);  
            }
        }
        return arr;*/
    }
    
    public static ArrayList<String> combineStrings(ArrayList<String> arr){
        ArrayList<String> combinedCode = new ArrayList<>();
        for(int i=0; i < arr.size(); i++){
            if (arr.get(i).equals("\"")){
                String s = "\"";
                for(int j = i+1; j < arr.size(); j++){
                    if (arr.get(j).equals("\"")){
                        s = s + "\"";
                        i = j;
                        combinedCode.add(s);
                        break;
                    }
                    s += arr.get(j);
                }
            }
            else { if (!arr.get(i).equals(" ")) combinedCode.add(arr.get(i)); }
        }
        return combinedCode;
    }
    
    public static ArrayList<String> combineComments(ArrayList<String> arr){
        int [] row1 = new int[1000];
        int [] col1 = new int[1000];
        int [] row2 = new int[1000];
        int [] col2 = new int[1000];
        boolean commentbeg = false;
        int length = 0;
        for(int i=0;i<arr.size();i++){
            for(int j=1;j<arr.get(i).length();j++){
                if(arr.get(i).charAt(j-1) == '\"') break;
                if(commentbeg == false && arr.get(i).charAt(j-1) == '/' && arr.get(i).charAt(j) == '*'){
                    row1[length] = i;   col1[length] = j-1;    commentbeg = true;
                    if (j+2 < arr.get(i).length()) j = j+2;
                }
                if (j >= arr.get(i).length())   continue;
                if(commentbeg == true && arr.get(i).charAt(j-1) == '*' && arr.get(i).charAt(j) == '/'){
                    row2[length] = i;   col2[length++] = j;   commentbeg = false;
                    if (j+1 < arr.get(i).length()) j = j+1;
                }
            }
        }
        ArrayList<String> combinedCode = new ArrayList<>();
        int cur = 0, last = -1;
        String mcomment = "";
        boolean first = true;
        for(int i=0;i<arr.size();i++){
            if(cur < length && i == row1[cur]  && (row1[cur] == row2[cur])){
                if(first == true && col1[cur] > 0 && arr.get(i).substring(0, col1[cur]).length() != 0) { combinedCode.add(arr.get(i).substring(0, col1[cur])); first = false; }
                if (last != -1 && arr.get(i).substring(last, col1[cur]).length() != 0) { combinedCode.add(arr.get(i).substring(last, col1[cur])); }
                combinedCode.add(arr.get(i).substring(col1[cur], col2[cur]+1));
                last = col2[cur] + 1;
                i--; cur++;  continue;
            }
            else if (cur < length && i == row1[cur] && (row1[cur] != row2[cur])){
                if(first == true && col1[cur] > 0 && arr.get(i).substring(0, col1[cur]).length() != 0) { combinedCode.add(arr.get(i).substring(0, col1[cur]));  first = false; }
                if (last != -1 && arr.get(i).substring(last, col1[cur]).length() != 0) { combinedCode.add(arr.get(i).substring(last, col1[cur])); }
                mcomment = arr.get(i).substring(col1[cur], arr.get(i).length())+"\n";
            }
            else if (cur < length && i > row1[cur] && i < row2[cur] && (row1[cur] != row2[cur]))
                mcomment = mcomment + arr.get(i) + "\n";
            else if (cur < length && i == row2[cur] && (row1[cur] != row2[cur])){
                mcomment = mcomment + arr.get(i).substring(0, col2[cur]+1);
                combinedCode.add(mcomment);
                last = col2[cur] + 1;
                i--;    cur++;  continue;
            }
            else{ 
                if (last != -1 && cur >= length) { combinedCode.add(arr.get(i).substring(last, arr.get(i).length())); last = -1; }
                else combinedCode.add(arr.get(i)); 
                first = true; 
            }
        }
        return combinedCode;
    }
    
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        run("code.txt", "result.txt", "tokens.txt");
    }
}
