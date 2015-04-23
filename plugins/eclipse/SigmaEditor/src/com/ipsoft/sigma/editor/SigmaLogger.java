package com.ipsoft.sigma.editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

public class SigmaLogger {
	public static PrintWriter out;
	
	public static PrintWriter getPW(){
		if(out==null)
			try {
				File f=new File(System.getenv("HOME")+"/Documents/Sigmalog.log");
				if(!f.exists()) f.createNewFile();
				out=new PrintWriter(f);
			}
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return out;
	}
	
	public static void log(Object s){
		PrintWriter out=getPW();
		out.println(s.toString());
		out.flush();
	}
	public static void main(String[] args){
		Map<String,String> map=System.getenv();
		for(Entry<String,String> e:map.entrySet()){
			System.out.println(e.getKey()+"     :    "+e.getValue());
		}
	}
}
