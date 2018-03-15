
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
    static HashMap delimeters;
    static String dbldelimeters[] = {"==", "!=", "&&", "\\|\\|", "<=", ">="};
    static String dbldelimetersencode[] = {"@", "#", "&", "~", "$", "^"};
        
    public static HashMap readTokens() throws FileNotFoundException{
        HashMap tokenslbl = new HashMap<String,String>();
        Scanner sc = new Scanner(new FileReader("tokens.txt"));
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            String key = line.split(" ")[1], value = line.split(" ")[0];
            tokenslbl.put(key, value);
            System.out.println(key+ "  " + value);
        }
        sc.close();
        return tokenslbl;
    }
    
    public static void readCode() throws FileNotFoundException, UnsupportedEncodingException{
        Scanner sc = new Scanner(new FileReader("code.txt"));
        PrintWriter writer = new PrintWriter("result.txt", "UTF-8");
        while(sc.hasNext()) {
            String token = sc.next();
            split(token, writer);
        }
        sc.close();
        writer.close();
    }
    
    public static boolean isID(String token){
        String regex = "^[a-zA-Z][a-zA-Z0-9]*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(token);
        if (matcher.find()) return true;
        return false;
    }
    
    public static void split(String token, PrintWriter writer) throws FileNotFoundException, UnsupportedEncodingException{
        String regex = "+|-|*|/|%|>|<|>=|<=|\\{|\\}|\\(|\\)|\\[|\\]|,|;|\\.|!|=|==|!=|&&|\\|\\||";
        String label = (String) tokensLabels.get(token);
        if (label == null){
            if (isID(token))   label = "ID";
            else    label = "Error unknown token";
        }
        writer.println("< "+label+" > : -"+token+"-");
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
    
    /*
    String regex = "^[a-zA-Z][a-zA-Z0-9]*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(token);
        if (matcher.find()) return true;
        return false;
    */
    
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
    
    public static ArrayList<String> combine(String [] str){
        ArrayList<String> arr = new ArrayList<>();
        for(int i=1; i < str.length; i++){
            String s = str[i-1];
            if (str[i].equals("*")&&str[i-1].equals("/")){
                s = "/*";
                for(int j = i+2; j < str.length; j++){
                    if (str[j].equals("/") && str[j-1].equals("*")){
                        s += "*/";
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
               
                if (i == str.length-1)
                    arr.add(str[str.length-1]);       
                if (!s.equals(" "))
                    arr.add(s);
            }
        }
        return arr;
    }
    
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        // dont forget .(DOT) >> itterate and shift then split . and text not numbers and not functions
       
        tokensLabels = readTokens();
        readCode();
        String token = "private || <= => static void mina(int x){int x == 8; System.out.println(\"+48  \");}";
        token = encode(token);
        String regex = "(?<= |\\=|\\;|\\t|\\(|\\)|\\{|\\}|\\+|\\-|\\/|\\*|\\!|\\,|\\[|\\]|\\@|\\#|\\&|\\~|\\$|\\^|\"|\\%|\\<|\\>)"
                     + "|(?= |\\=|\\;|\\t|\\(|\\)|\\{|\\}|\\+|\\-|\\/|\\*|\\!|\\,|\\[|\\]|\\@|\\#|\\&|\\~|\\$|\\^|\"|\\%|\\<|\\>)";
        String arr[] = token.split(regex);
      
        ArrayList<String> al = decode(combine(arr));
        
        for(String s : al){
            System.out.println(s); 
        }
        
    }
    
}
