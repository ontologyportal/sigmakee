
import java.io.*;
import java.util.*;

public class test {

    public void testme() {
    
        String PL_PATH = "C:\\Program Files\\pl-5.2.10\\bin";  
        String CELT_PATH = "C:\\PEASE\\CELT-ACE\\latestDemo\\May29";  
        StringBuffer kif = new StringBuffer();

        try {            
            Process proc = Runtime.getRuntime().exec(PL_PATH + File.separator + "plcon.exe" + " " + CELT_PATH + File.separator + "Startup.pl");
            InputStream stderr = proc.getErrorStream();
            InputStreamReader isrerror = new InputStreamReader(stderr);
            BufferedReader brerror = new BufferedReader(isrerror);
            
            InputStream stdout = proc.getInputStream();
            InputStreamReader isrout = new InputStreamReader(stdout);
            BufferedReader brout = new BufferedReader(isrout);

            OutputStream stdin = proc.getOutputStream();
            OutputStreamWriter oswin = new OutputStreamWriter(stdin);
            BufferedWriter brin = new BufferedWriter(oswin);

            String erline = null;
            String line = null;
            do {
                if (brerror.ready()) {
                    erline = brerror.readLine();
                    //System.out.println("error: " + erline);
                }
                else if (brout.ready()) {
                    line = brout.readLine();
                    System.out.println("line: " + line);
                }
                else {
                    line = null;
                    erline = null;
                }
                synchronized (this) {
                    this.wait(100);
                }
            } while (line == null || !line.equalsIgnoreCase("Done initializing."));
            
            brin.write("xml_eng2log('John enters the bank.',X),format('~w',X).\n\n\n",0,56);
            brin.flush();

            boolean inKIF = false;
            boolean fail = false;
            do {
                if (brerror.ready()) {
                    erline = brerror.readLine();
                    //System.out.println("error: " + erline);
                }
                else if (brout.ready()) {
                    line = brout.readLine();
                    //System.out.println("line: " + line);
                }
                else {
                    line = null;
                    erline = null;
                }
                if (line.equalsIgnoreCase("</KIF>")) {
                    inKIF = false;
                }
                else if (inKIF)
                    kif = kif.append(line+"\n");
                if (line.indexOf("Could not parse this sentence:") == 0) 
                    fail = true;
                if (line.equalsIgnoreCase("<KIF>")) 
                    inKIF = true;
                synchronized (this) {
                    this.wait(100);
                }
            } while (line == null || line.indexOf("</translation>") != 0);
            if (fail) 
                System.out.println("Failed to parse.");
            else {
                System.out.println("Parse successful.");
                System.out.println(kif.toString());
            }
        } 
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        test t = new test();
        t.testme();
    }
}
        

