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
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JOptionPane;

import org.apache.commons.io.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;


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
	String version = "GitRolloutMaker 1.3.0";
	String author = "Author: Yarib Hernandez and Julio C. Diaz";
	
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
	
	/** Validates the differences between initial commit and last commit and invokes copy file method for each file,  
	 * @throws Exception **/	
	public void createRolloutFiles() throws Exception {
		
		FileRepositoryBuilder builder = new FileRepositoryBuilder();		
		File f = new File("");
		Iterable<RevCommit> thecommit;
		RevCommit initialCommit, lastCommit;
		List<DiffEntry> filesList = null;		
		
		try(Repository repo = builder.setGitDir(f.getParentFile()).readEnvironment().findGitDir().build()){ //Opens the existing repository
			
			Git git = new Git(repo);
			
			this.Name = repo.getBranch().trim();			
			this.RollPath = "Rollouts/"+ this.Name +"/";
			createLog(this.Name);
			
			writeLog(this.version);
			
			writeLog("Current branch: " + this.Name);			
			
			//Get the last commit
			writeLog("Getting last commit...");
			thecommit = git.log().add(repo.resolve(repo.getBranch())).setMaxCount(1).call();
			if((lastCommit = thecommit.iterator().next()) == null) {
				writeLog("Last commit not found");
				throw new Exception("Last commit not found");				
			}	
			
			writeLog("Last commit id: " + lastCommit.getName());
			
				
			//Get the first commit,
			writeLog("Getting last commit...");
			thecommit = git.log().add(repo.resolve(repo.getBranch())).setRevFilter(MessageRevFilter.create("Initial Commit for " + this.Name)) .call();
			if((initialCommit = thecommit.iterator().next()) == null) {
				thecommit = git.log().add(repo.resolve(repo.getBranch())).setRevFilter(MessageRevFilter.create("Innitial Commit for " + this.Name)) .call();
				if((initialCommit = thecommit.iterator().next()) == null) {
					writeLog("Initial commit not found");	
					throw new Exception("Initial commit not found");	
				}
			}				
			
			writeLog("Initial commit id: " + initialCommit.getName());
			
			
			//Gets the files with differences
			writeLog("Getting the files between commits: " + initialCommit.getName() + " -> " + lastCommit.getName() + "...");			
			filesList = listDiff(repo, git,
					initialCommit.getName(),
	                lastCommit.getName());	
			
			writeLog("Found: " + filesList.size() + " differences");
			
			if(filesList.size() == 0)				
				throw new Exception("No changes detected");
			
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			writeLog(e.getMessage().toString());
			throw new Exception(e);
		} catch (RevisionSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			writeLog(e.getMessage());
			throw new Exception(e.getMessage());
		} catch (NoHeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			writeLog(e.getMessage());
			throw new Exception(e.getMessage());
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			writeLog(e.getMessage());
			throw new Exception(e.getMessage());
		}
		
		
		//Checks if the rollout dir already exists, if so, then it deletes it so that it rebuilds it
		File rollo = new File(this.RollPath);		
		
		if(rollo.exists()) { //if the rollout already exist then we have to delete it and put the new files
			writeLog("Removing existing rollout: "+ this.RollPath);
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
		
		//Loops over the file list and copy the files to the rollout dir
		writeLog("Generating new rollout " + this.RollPath + "...");
		for (DiffEntry diff : filesList) {			
			//System.out.println("Diff: " + diff.getChangeType() + ": " + (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath())); ////diff.getChangeType()  returns the type of change which could be DELETE, ADD, MODIFY
			if(diff.getChangeType().equals(ChangeType.DELETE)) 				
				removed.add(diff.getOldPath());			
			else
				copyFile(diff.getNewPath() , RollPath+"pkg/" + diff.getNewPath());
            
		}
		
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
	
	private ArrayList<String> executeCommand(String command) throws IOException{
		//String commandRes=null;
		ArrayList<String> commandRes = new ArrayList<String>();
		Process proc = null;
		try {
			proc = rt.exec(command);
			//writeLog("Executed: "+command);
			stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			if(stdError.readLine()!=null)
				System.out.println(stdError.toString());			
			else{
				//commandRes="";
				String line=null;
				while((line=stdInput.readLine())!=null)
					commandRes.add(line);
					//commandRes=commandRes+line+"\n";	
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
				//writeLog("Copying file "+source+ "   TO   "+dest);
				//System.out.println("Copying file "+source+ "   TO   "+dest);
				//FileUtils.copyFile(srcFile,destFile);				
				Files.copy(srcFile.toPath(), destFile.toPath());
			}else{
				//System.out.println("No existe el archivo " + source);
				//writeLog("Removed file "+source);
				removed.add(source);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block			
			writeLog(e.getMessage());
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
		System.out.println(logLine);
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
	
	private List<DiffEntry> listDiff(Repository repository, Git git, String oldCommit, String newCommit) throws GitAPIException, IOException {
		
        List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldCommit))
                .setNewTree(prepareTreeParser(repository, newCommit))
                .call();

        return diffs;
        
        /*for (DiffEntry diff : diffs) {
            System.out.println("Diff: " + diff.getChangeType() + ": " + (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath())); 
        }*/
	}
	private AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }
	
}