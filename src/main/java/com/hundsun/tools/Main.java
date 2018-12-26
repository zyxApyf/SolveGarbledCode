package com.hundsun.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hundsun.match.ThostFtdcUserApiStruct;
import com.hundsun.module.StructModule;

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnicodeDetector;

/**
 * 替换文件指定内容
 * 
 * @author zhangyx25316
 *
 */
public class Main {
	
	private static ThostFtdcUserApiStruct thostFtdcUserApiStruct = null;
	
	public static void main(String[] args) {
		String filePath = "F:\\文件内容替换工具\\ctp_wrap.cxx";
		//String filePath = System.getProperty("user.dir") + "\\ctp_wrap.cxx";
		thostFtdcUserApiStruct = new ThostFtdcUserApiStruct();
		replaceFileContext(filePath);
	}
	
	/**
	 * 替换文件中特定内容
	 * 
	 * @param filePath 文件所在路径
	 */
	public static void replaceFileContext(String filePath) {
		File file = new File(filePath);
		FileInputStream fileInputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;
		PrintWriter printWriter = null;
		try {
			//获取文件编码
            String enCode = getFileEncode(file.getAbsolutePath());  
            if("void".equals(enCode)){  
            	enCode="UTF-8";  
            }
            if("windows-1252".equals(enCode)){  
                enCode="GBK";  
            }
			fileInputStream = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(fileInputStream, enCode);
			bufferedReader = new BufferedReader(inputStreamReader);
			//get方法中的关键字
			List<StructModule> getMethodKeyWord = new ArrayList<>();
			//set方法中的关键字
			List<StructModule> setMethodKeyWord = new ArrayList<>();
			StringBuffer stringBuffer = new StringBuffer();
			//头文件
			String headLine = "#include \"util.h\"";
			stringBuffer.append(headLine);
			//行与行之间的分割
            stringBuffer.append(System.getProperty("line.separator"));
            int index = 0;
            
			String line = null;
			while((line = bufferedReader.readLine()) != null) {
				String getterSign = "SWIGEXPORT jstring JNICALL Java_com_ctp_ctpJNI_";
				String setterSign = "SWIGEXPORT void JNICALL Java_com_ctp_ctpJNI_";
				//通过 SWIGEXPORT jstring JNICALL Java_com_ctp_ctpJNI_ 关键点匹配get方法中需要替换的行的所需关键字
				Pattern getKeyWordPattern = Pattern.compile("^" + getterSign + ".*");
				String getKeyWordString = "";
				Matcher getKeyWordMatcher = getKeyWordPattern.matcher(line);
				//通过 NewStringUTF 关键点匹配get方法中需要替换的行
				Pattern getReplacePattern = Pattern.compile(".*NewStringUTF.*");
				String getReplaceString = "";
				Matcher getReplaceMatcher = getReplacePattern.matcher(line);
				
				//通过 jstring jarg2 关键点匹配set方法中需要替换的行的所需关键字
				Pattern setkeyWordPattern = Pattern.compile(".*jstring jarg2\\).*");
				String setkeyWordString = "";
				Matcher setKeyWordMatcher = setkeyWordPattern.matcher(line);
				//通过 if (jarg2) { 关键点匹配set方法中第一处需要替换的行
				Pattern firstSetReplacePattern = Pattern.compile(".*if\\s+\\(jarg2\\)\\s+\\{.*");
				String firstSetReplaceString = "";
				Matcher firstSetReplaceMatcher = firstSetReplacePattern.matcher(line);
				//通过 strncpy((char*)arg1 关键点匹配set方法中第二处需要替换的行
				Pattern secondSetReplacePattern = Pattern.compile(".*strncpy\\(\\(char\\*\\)arg1.*");
				String secondSetReplaceString = "";
				Matcher secondSetReplaceMatcher = secondSetReplacePattern.matcher(line);
				
				//结构体关键字
				String structKeyWord = "";
				//结构体参数关键字
				String fieldKeyWord = "";
				//遍历get方法所在行，截取两个关键字
				while(getKeyWordMatcher.find()){
					//get方法一整行信息
					getKeyWordString += getKeyWordMatcher.group(0);
					StructModule structModule = new StructModule();
					structKeyWord = getKeyWordString.split(getterSign)[1]
		 					 .split("_1")[0];
					if(getKeyWordString.split(getterSign)[1].split("_1").length > 2) {
						fieldKeyWord = getKeyWordString.split(getterSign)[1]
							 						.split("_1")[1];
					} else {
						fieldKeyWord = getKeyWordString.split(getterSign)[1]
													.split("_1")[1]
													.split("\\(")[0];
					}
					structModule.setKey(structKeyWord);
					structModule.setValue(fieldKeyWord);
					getMethodKeyWord.add(structModule);
				}
				//遍历get方法中需要替换的行
				while(getReplaceMatcher.find()){
					getReplaceString += getReplaceMatcher.group(0);
					//替换get方法中需要替换的部分内容
					Map<String, String> map = new LinkedHashMap<>();
					String replaceGetMethodContext = "";
					//对应的数组大小
					index = ThostFtdcUserApiStruct.specifyQuery(getMethodKeyWord.get(getMethodKeyWord.size() - 1).getKey(), 
																	getMethodKeyWord.get(getMethodKeyWord.size() - 1).getValue());
					if(index != 0) {
						int capacity = index * 2 + 1;
						replaceGetMethodContext = "  char result_utf8[" + capacity + "] = { 0 };\n" + 
								"  if (result) {\n" + 
								"    unsigned int inlength = strlen(result);\n" + 
								"    int rsp = code_convert(\"gb2312\", \"utf-8\", result, inlength, result_utf8, " + capacity + ");\n" + 
								"    if (rsp != -1) jresult = jenv->NewStringUTF((const char *)result_utf8);\n" + 
								"  }";
					} 
					map.put("NewStringUTF", replaceGetMethodContext);
					Set<Map.Entry<String, String>> entries = map.entrySet();
					for (Map.Entry<String, String> mapKey : entries) {  
	                	//判断当前行是否存在想要替换掉的字符 -1表示存在
	                    if(line.indexOf(mapKey.getKey()) != -1 && index != 0){ 
	                    	//替换为你想替换的内容
	                        line = line.replace(getReplaceString, mapKey.getValue());
	                    }
	                }
				}
				
				//遍历set方法所在行，截取两个关键字
				while(setKeyWordMatcher.find()){
					//set方法的一整行信息
					setkeyWordString += setKeyWordMatcher.group(0);
					StructModule structModule = new StructModule();
					if(setkeyWordString.contains(setterSign)) {
						structKeyWord = setkeyWordString.split(setterSign)[1]
			 					 .split("_1")[0];
						if(setkeyWordString.split(setterSign)[1].split("_1").length > 2) {
							fieldKeyWord = setkeyWordString.split(setterSign)[1]
								 						.split("_1")[1];
						} else {
							fieldKeyWord = setkeyWordString.split(setterSign)[1]
														.split("_1")[1]
														.split("\\(")[0];
						}
					}
					structModule.setKey(structKeyWord);
					structModule.setValue(fieldKeyWord);
					setMethodKeyWord.add(structModule);	
				}
				//遍历set方法中第一处需要替换的行
				while(firstSetReplaceMatcher.find()) {
					firstSetReplaceString += firstSetReplaceMatcher.group(0);
					
					String subLine = null;
					Label:while((subLine = bufferedReader.readLine()) != null) {
						Pattern endPattern = Pattern.compile(".*}.*");
						Matcher endMatcher = endPattern.matcher(subLine);
						while(endMatcher.find()) {
							break Label;
						}
					}
					//替换set方法中需要替换的部分内容
					String replaceSetMethodContext = "";
					index = ThostFtdcUserApiStruct.specifyQuery(setMethodKeyWord.get(setMethodKeyWord.size() - 1).getKey(), 
																	setMethodKeyWord.get(setMethodKeyWord.size() - 1).getValue());
					if(index != 0) {
						int capacity = index * 2 + 1;
						replaceSetMethodContext = "  char result_gb2312[" + capacity + "] = { 0 };\n" + 
								"  if (jarg2) {\n" + 
								"	arg2 = (char *)jenv->GetStringUTFChars(jarg2, 0);\n" + 
								"	int length = strlen(arg2);\n" + 
								"	int rsp = code_convert(\"utf-8\", \"gb2312\", arg2, length, result_gb2312, " + capacity + ");\n" + 
								"	if (!arg2) return ;\n" + 
								"  }";
					} else {
						replaceSetMethodContext = "  if (jarg2) {\n" + 
								"    arg2 = (char *)jenv->GetStringUTFChars(jarg2, 0);\n" + 
								"    if (!arg2) return ;\n" + 
								"  }";
					}
					Map<String, String> map = new LinkedHashMap<>();
					map.put("if (jarg2) {", replaceSetMethodContext);
					Set<Map.Entry<String, String>> entries = map.entrySet();
					for (Map.Entry<String, String> mapKey : entries) {  
	                	//判断当前行是否存在想要替换掉的字符 -1表示存在
	                    if(line.indexOf(mapKey.getKey()) != -1){ 
	                    	//替换为你想替换的内容
	                        line = line.replace(firstSetReplaceString, mapKey.getValue());
	                    }
	                }
				}
				//遍历set方法中第二处需要替换的行
				while(secondSetReplaceMatcher.find()) {
					secondSetReplaceString += secondSetReplaceMatcher.group(0);
					Map<String, String> argMap = new LinkedHashMap<>();
					argMap.put("arg2", "result_gb2312");
					
					Set<Map.Entry<String, String>> entries = argMap.entrySet();
					for (Map.Entry<String, String> entry : entries) {  
	                	//判断当前行是否存在想要替换掉的字符 -1表示存在
	                    if(line.indexOf(secondSetReplaceString) != -1){ 
	                    	//替换为你想替换的内容
	                        line = line.replace(entry.getKey(), entry.getValue());
	                    }
	                }
				}
                stringBuffer.append(line);
                //行与行之间的分割
                stringBuffer.append(System.getProperty("line.separator"));
			}
			String newFilePath = "F:\\文件内容替换工具\\new_ctp_wrap.cxx";
			//String newFilePath = System.getProperty("user.dir") + "\\new_ctp_wrap.cxx";
			File newFile = new File(newFilePath);
			if(!newFile.exists()) {
				newFile.createNewFile();
			} 
			//替换后输出的文件位置
			printWriter = new PrintWriter(newFile);
            printWriter.write(stringBuffer.toString().toCharArray());
            printWriter.flush();
            
		}catch (FileNotFoundException e) {
        	e.printStackTrace();
        	System.out.println("文件不存在！");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("读取文件失败！");
        } catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(printWriter != null) {
				printWriter.close();
			}
		}
	}
	
	/**
	 * 检查文件类型
	 * 
	 * @param filePath 文件所在路径
	 * @return 文件类型
	 */
	public static String getFileEncode(String filePath) {  
        /* 
         * detector是探测器，它把探测任务交给具体的探测实现类的实例完成。 
         * cpDetector内置了一些常用的探测实现类，这些探测实现类的实例可以通过add方法 加进来，如ParsingDetector、 
         * JChardetFacade、ASCIIDetector、UnicodeDetector。 
         * detector按照“谁最先返回非空的探测结果，就以该结果为准”的原则返回探测到的 
          * 字符集编码。使用需要用到三个第三方JAR包：antlr.jar、chardet.jar和cpdetector.jar 
         * cpDetector是基于统计学原理的，不保证完全正确。 
         */  
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();  
        /* 
         * ParsingDetector可用于检查HTML、XML等文件或字符流的编码,构造方法中的参数用于 
          * 指示是否显示探测过程的详细信息，为false不显示。 
         */  
        detector.add(new ParsingDetector(false));  
        /* 
         * JChardetFacade封装了由Mozilla组织提供的JChardet，它可以完成大多数文件的编码 
         * 测定。所以，一般有了这个探测器就可满足大多数项目的要求，如果你还不放心，可以 
         * 再多加几个探测器，比如下面的ASCIIDetector、UnicodeDetector等。 
         */  
        // ASCIIDetector用于ASCII编码测定  
        detector.add(ASCIIDetector.getInstance());  
        // UnicodeDetector用于Unicode家族编码的测定  
        detector.add(UnicodeDetector.getInstance());  
        java.nio.charset.Charset charset = null;  
        File f = new File(filePath);  
        try {  
            charset = detector.detectCodepage(f.toURI().toURL());  
        } catch (Exception ex) {  
            ex.printStackTrace();  
        }  
        if (charset != null)  
            return charset.name();  
        else  
            return null;  
    } 
}
