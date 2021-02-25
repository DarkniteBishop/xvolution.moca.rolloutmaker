package com.redprairie.rollout;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JOptionPane;

import org.apache.commons.io.*;


public class Rollout{
	
	String RollPath;
	String RemoteBranch;
	String Name;
	Process proc;
	Runtime rt;
	BufferedReader stdInput=null;
	BufferedReader stdError=null;
	String diffCommand="git diff --name-only {0}...{1}"; //command to get the differences between two commits
	String getFstCommitCmd = "git log --grep=\"innitial Commit for {branch}\" -i --pretty=%H"; //command get first commit of the given branch based on a regexp patter
	String getLstCommitCmd = "git log -n 1 --pretty=%H"; //command to get the last commit of the given branch
	String getBranchName="git rev-parse --abbrev-ref HEAD"; //Command to get the current branch name
	Logger logger;
	ArrayList<String> removed;
	
	//Constructor
	public Rollout(){		
		removed=new ArrayList<String>();
		rt=Runtime.getRuntime();	
	}
	
	/** Main method 
		Returns true if the Rollout was created successfully otherwise false		**/
	public boolean createRollout(){		
		try{
			createRolloutFiles();
			mainFileMaker();
			return true;
		}catch(Exception e){			
			e.printStackTrace();
			writeLog(e.getMessage());
			JOptionPane.showMessageDialog(null, e.getMessage());
			return false;
		}
	}
	
	/** Validates the differences between branches and invokes copy file method for each file 
	 * @throws Exception **/
	private void createRolloutFiles() throws Exception{		
		//Getting the current branch name
		String branch= executeCommand(getBranchName);		
		branch = branch.trim();
		createLog(branch);
		this.Name=branch;
		this.RollPath="Rollouts/"+branch+"/";
		if(branch.length() == 0) {			
			throw new Exception("It was not possible to get the branch name, make sure this is the right path or repository");			
		}
		System.out.println("Current branch is: "+ branch);
		
		
		//Gettin the first branch commit		
		getFstCommitCmd = getFstCommitCmd.replace("{branch}", branch.trim());
		writeLog("git command: "+ getFstCommitCmd);
		String initialCommit = executeCommand(getFstCommitCmd);	
		if(initialCommit.length() == 0) {			
			throw new Exception("It was not possible to get the initial commit, make sure this is the right path");			
		}		
		writeLog(MessageFormat.format("Initial commit: {0}", initialCommit.trim()));
		
		//Getting the last commit		
		writeLog("git command: "+ getLstCommitCmd);
		String LastCommit = executeCommand(getLstCommitCmd);
		if(LastCommit.length() == 0) {			
			throw new Exception("It was not possible to get the last commit, make sure you this is the right path");			
		}		
		writeLog(MessageFormat.format("Last commit: {0}", LastCommit.trim()));		
		
				
		//Getting modified and new files
		diffCommand = MessageFormat.format(diffCommand, initialCommit.trim(),LastCommit.trim());		
		writeLog("git command: "+ diffCommand);		
		String files = executeCommand(diffCommand);
		String diff[] =files.split("\\r?\\n");
		
		if(diff.length <=0) {
			System.out.println("No changes detected");
			writeLog("No changes detected");
			throw new Exception("No changes detected");			
		}
		
		
		
		
		System.out.println("Modified/New files: {numfiles}".replace("{numfiles}", Integer.toString(diff.length)));	
		writeLog("Modified/New files: {numfiles}".replace("{numfiles}", Integer.toString(diff.length)));
		
		//Checks if the rollout dir already exists, if so, then it deletes it
		File rollo = new File(this.RollPath);
		if(rollo.exists()) { //if the rollout already exist then we have to delete it and put the new files		
			System.out.println("Removing existing rollout: "+ rollo.getPath().toString());		
			Files.walkFileTree(rollo.toPath(), new SimpleFileVisitor<Path>() {
				@Override				
		         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
		             throws IOException
		         {
					//Removing files
		             Files.delete(file);
		             return FileVisitResult.CONTINUE;
		         }
		         @Override
		         public FileVisitResult postVisitDirectory(Path dir, IOException e)
		             throws IOException
		         {
		        	 //Removing directories
		             if (e == null) {
		                 Files.delete(dir);
		                 return FileVisitResult.CONTINUE;
		             } else {
		                 // directory iteration failed
		                 throw e;
		             }
		         }
		     });
		}		
		
		//Creating the rollout
		System.out.println("Creating rollout: "+this.RollPath);		
		writeLog("Creating rollout files");
		System.out.println("Copying files**********************************");
		for(int i=0;i<diff.length;i++)
			copyFile(diff[i].trim(),RollPath+"pkg/"+diff[i].trim());	
		System.out.println("Files copied**********************************");
		File perlfile = new File ("Rollouts\\rollout.pl");
		
		if(perlfile.exists()) {
			//Files.copy(perlfile.toPath(), rollo.toPath());
			copyFile("Rollouts/rollout.pl","Rollouts/"+this.Name+"/rollout.pl");
		}
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
			//writeLog("Executed: "+command);
			stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			if(stdError.readLine()!=null)
				System.out.println(stdError.toString());			
			else{
				commandRes="";
				String line=null;
				while((line=stdInput.readLine())!=null)
					commandRes=commandRes+line+"\n";	
			}	
		} catch (IOException ex) {
			//writeLog("Failed to execute command: "+command);
			//writeLog("ERROR: "+ex.getMessage());
			ex.printStackTrace();
		}			
		return commandRes;	
	}
	
	private void copyFile(String source,String dest) {
		File srcFile=new File(source);
		File destFile =new File(dest);		
		try {			
			Files.createDirectories(destFile.toPath().getParent());			
			if(Files.exists(srcFile.toPath())){
				writeLog("Copying file "+source+ "   TO   "+dest);
				System.out.println("Copying file "+source+ "   TO   "+dest);
				//FileUtils.copyFile(srcFile,destFile);				
				Files.copy(srcFile.toPath(), destFile.toPath());
			}else{
				System.out.println("No existe el archivo " + source);
				writeLog("Removed file "+source);
				removed.add(source);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block			
			System.out.println(e.getMessage());
			e.printStackTrace();
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