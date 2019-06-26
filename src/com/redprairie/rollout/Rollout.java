package com.redprairie.rollout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.io.*;


public class Rollout{
	
	String RollPath;
	String RemoteBranch;
	String Name;
	Process proc;
	Runtime rt;
	BufferedReader stdInput=null;
	BufferedReader stdError=null;
	String diffCommand="git diff --name-only remotes/origin/{RBRANCH}...{BRANCH}";
	//String diffCommand="git diff --name-only {BRANCH}..remotes/origin/{RBRANCH}"; 
	String getBranchName="git rev-parse --abbrev-ref HEAD";
	Logger logger;
	ArrayList<String> removed;
	public Rollout(String name, String remoteBranch){
		this.Name=name;
		this.RollPath="Rollouts/"+name+"/";
		this.RemoteBranch=remoteBranch;
		removed=new ArrayList<String>();
		rt=Runtime.getRuntime();
		createLog(name);
	}
	
	/** Main method 
		Returns true if the Rollout was created successfully otherwise false		**/
	public boolean createRollout(){		
		try{
			createRolloutFiles();
			mainFileMaker();
			return true;
		}catch(Exception e){
			writeLog(e.getMessage());
			return false;
		}
	}
	
	/** Validates the differences between branches and invokes copy file method for each file **/
	private void createRolloutFiles() throws IOException{
		writeLog("Creating rollout files");
		String branch= executeCommand(getBranchName);
		diffCommand=diffCommand.replace("{RBRANCH}", RemoteBranch);
		String files = executeCommand(diffCommand.replace("{BRANCH}",branch));
		String diff[] =files.split("\\r?\\n");
		for(int i=0;i<diff.length;i++)
			copyFile(diff[i],RollPath+"pkg/"+diff[i]);	
	}
	
	private void mainFileMaker(){
		RolloutFileMaker fm=new RolloutFileMaker();
		fm.generateMainFile(RollPath,removed);
		writeLog("Rollout Instructions File Generated");
	}
	
	private String executeCommand(String command) throws IOException{
		String commandRes=null;
		Process proc = null;
		try {
			proc = rt.exec(command);
			writeLog("Executed: "+command);
			stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			if(stdError.readLine()!=null)
				writeLog(stdError, command);
			else{
				commandRes="";
				String line=null;
				while((line=stdInput.readLine())!=null)
					commandRes=commandRes+line+"\n";	
			}	
		} catch (IOException ex) {
			writeLog("Failed to execute command: "+command);
			writeLog("ERROR: "+ex.getMessage());
		}			
		return commandRes;	
	}
	
	private void copyFile(String source,String dest){
		File srcFile=new File(source);
		File destFile =new File(dest);
		try{
			if(srcFile.exists()){
				writeLog("Copying file "+source+ "   TO   "+dest);
				FileUtils.copyFile(srcFile,destFile);
			}else{
				writeLog("Removed file "+source);
				removed.add(source);
			}
		}catch(IOException ex){
			writeLog("Failed to copy file "+source);
			writeLog("ERROR: "+ex.getMessage());
		}
	}
	
	private void createLogDir(){
		File log=new File("Rollouts/Log");
		if(log.exists())
			writeLog("Log Directory Exists");
		else{
			log.mkdir();
			writeLog("Log Directory Created");
		}	
	}
	
	//creates a log file for each execution
	private void createLog(String name){
		logger=Logger.getLogger(Rollout.class.getName());
		FileHandler fh;		
		try {
			createLogDir();
			fh = new FileHandler("Rollouts/Log/"+name+".log",true);
			logger.addHandler(fh);
			logger.setUseParentHandlers(false);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter); 
		}catch (SecurityException | IOException e) {
			e.printStackTrace();
		}      
	}
	
	//Writes standard messages to log
	private void writeLog(String logLine){
		logger.info(logLine);
	}	
	
	//Writes error output in log 
	private void writeLog(BufferedReader logLine,String command) throws IOException{
		String errMsgLine=null;
		String errMsg="Error in commad: "+command+"\n";
		while((errMsgLine=logLine.readLine())!=null)
			errMsg=errMsg+errMsgLine+"\n";
		writeLog(errMsg);
	}	
	
}