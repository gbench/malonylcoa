package gbench.util.jdbc.sql;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public class SqlFulfil {
	
	/**
	 * 
	 * @param tpl
	 * @param arg
	 * @return
	 */
	public static String fulfil(String tpl,String arg){
		return fulfil(tpl,arg.split("[,//s]+"));
	}
	
	/**
	 * 
	 * @param tpl
	 * @param args
	 * @return
	 */
	public static String fulfil(String tpl,String args[]){
		return fulfil3(tpl,Arrays.asList(args));
	}
	
	/**
	 * 
	 * @param tpl
	 * @param args
	 * @return
	 */
	public static String fulfil(final String tpl,List<String> args){
		String buffer = String.valueOf(tpl);
		for (int i = 0; i < args.size(); i++) {
			System.out.println(args.get(i));
			buffer = buffer.replaceFirst("//?", args.get(i));
		}
		
		return buffer;
	}
	
	/**
	 * 
	 * @param tpl
	 * @param args
	 * @return
	 */
	public static String fulfil2(final String tpl,List<String> args){
		String buffer = String.valueOf(tpl);
		final String markSymbol = "?";
		int i = 0;
		int n = args.size();
		while(buffer.indexOf(markSymbol)>=0){
			buffer = buffer.replaceFirst("//?", args.get(i++%n));
		}
		
		return buffer;
	}
	
	/**
	 * 
	 * @param tpl
	 * @param args
	 * @return
	 */
	public static String fulfil3(final String tpl,List<String> args){
		StringBuffer buffer = new StringBuffer();
		int n = tpl.length();
		int j = 0;
		int m = args.size();
		for(int i=0;i<n;i++){
			String s = tpl.charAt(i)+"";
			if(s.matches("\\?+"))s=args.get((j++)%m).trim();
			buffer.append(s);
		}
		
		return buffer.toString();
		
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	static List<String> readFile(String path){
		File file = new File(path);
		if(!file.isFile()||!file.exists())return null;
		List<String> ll = new LinkedList<>();
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"gbk"));
			br.lines().filter(e->!e.matches("\\s*")).forEach(ll::add);
			br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return ll;
	}
	
	public static String[] getParams(String line) {
		Matcher matcher= Pattern.compile(",*([^,\\(\\)]+)\\([^\\(\\)]+\\)").matcher(line);
		List<String> ll = new LinkedList<>();
		while(matcher.find()) {
			String s = matcher.group(1);
			
			ll.add("'"+s+"'");
		}
		
		System.out.println(ll);
		return ll.toArray(new String[0]);
	}
	
	
	
	public static void main(String args[]){
		
		List<String> tpl = readFile("\\\\gpc\\share\\linux\\sql.txt");
		//String s = "1(String), sys-msds-recomend(String), 1(String), (String), (String), (String)";
		System.out.println(fulfil(tpl.get(0),getParams(tpl.get(1))));
		
	}

}
