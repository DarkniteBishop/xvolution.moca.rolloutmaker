package com.redprairie.rollout;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JOptionPane;

public class RolloutFileMaker {
	
	 String Rpath;
	 ArrayList<String> Removed;
    public void generateMainFile(String roll_path,ArrayList<String> removed) {
        FileWriter script = null; 
        FileWriter readme = null;
        PrintWriter pw = null;
        String path = "";
        String extension_name = "";
        File dir;
        Calendar fecha;
        int error_script = 0;
        int error_readme = 0;
       this.Removed=removed;
        try
        {	
            dir = new File(roll_path);
            extension_name = dir.getAbsolutePath().toString().substring(dir.getAbsolutePath().toString().replace("\\.", "").lastIndexOf("\\") + 1, dir.getAbsolutePath().toString().replace("\\.", "").length());
            //System.out.println("FM Script: "+roll_path+ extension_name );
			script = new FileWriter(roll_path+extension_name);			
            pw = new PrintWriter(script);
            path = roll_path+"pkg/";
            dir = new File(path);
            Rpath=roll_path;
            //Escritura del Encabezado
            pw.print("# Extension " + extension_name + " \n"
                    + "# \n"
                    + "# This script has been built automatically using RolloutFileMaker. \n"
                    + "# Please check the actions taken by the script as they may not be entirely correct. \n"
                    + "# Also check the order of the actions taken if any dependencies might be \n"
                    + "# encountered \n" + "# \n");

            //Escritura de la Seccion de REPLACE            
            pw.println("\n# Replacing affected files.");
            replaceSection(dir,path, pw);
            
            pw.println("# Removed Files");
            removeSection(removed,pw);
            
            //Escritura Seccion SQL y MSQL
            pw.println("\n# Run any SQL, MSQL, and other scripts.");
            scriptSection(dir, path, pw);
            
            //Escritura Seccion Cargar Data de CSV
            pw.print("\n# Load any data affected.  NOTE the assumption is that\n"
                    + "# the control file will be in the db/data/load directory.\n");
            loadDataSectionByCSV(dir, path, pw);
            
            //Escritura Seccion Integrator
            pw.println("\n# Import any Seamles data affected.");
            seamlesSection(dir, path, pw);
            
            //Escritura del pie de Archivo
            pw.print("\n# Rebuilding C makefiles if necessary.\n"
                    + "#RUNSCRIPT perl -S create_c_makefiles.pl\n\n"
                    + "# Perform any environment rebuilds if necessary.\n"
                    + "MBUILD \n" 
                    + "#REBUILD LES \n\n"
                    + "# Thanks for using RolloutFileMaker\n"
                    + "# END OF AUTO-GENERATED SCRIPT.");
 
        } catch (Exception e) {
           // e.printStackTrace();
            error_script = 1;
           // JOptionPane.showMessageDialog(null, "Ha Ocurrido un error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
           try {
           if (null != script)
              script.close();
           } catch (Exception e2) {
              e2.printStackTrace();
           }
        }
        
        try {
            readme = new FileWriter(roll_path+"README.txt");
            pw = new PrintWriter(readme);
            
            fecha = Calendar.getInstance();
            String annio = Integer.toString(fecha.get(Calendar.YEAR));
            String mes = Integer.toString(fecha.get(Calendar.MONTH) + 1);
            String dia = Integer.toString(fecha.get(Calendar.DATE));
            
            if(mes.length() == 1)
                mes = "0" + mes;
            
            if(dia.length() == 1)
                dia = "0" + dia;
            
            pw.print("================================================================================\n"
                     + "Extension: " + extension_name ); 
            
            int resto = 59 - extension_name.length();
            
            if(resto < 59)            
                for(int i= 0; i < resto; i++)
                    pw.print(" ");
            
            pw.print(annio + "-" + mes + "-" + dia + "\n");
            pw.print("================================================================================\n\n"
                    + "Issue(s):\n"
                    + extension_name + ": ==[Write here the issues solved with the rollout]==\n\n\n"
                    + "Affected Files:\n");
            
           // path = ".\\pkg";
            dir = new File(path);
            
            affectedFilesSection(dir, path, pw);
            
            pw.print("\nRelease Notes:\n\n\n");
            
            pw.print("================================================================================\n"
                   + "               W I N D O W S   I N S T A L L A T I O N   N O T E S              \n"
                   + "================================================================================\n\n"
                   + "    1.  Start a Windows command prompt as an Administrator user\n\n"
                   + "    2.  Set Visual C++ environment variables.\n\n"
                   + "        You will first have to change to the Visual C++ bin directory if it \n"
                   + "        isn't in your search path.\n\n"
                   + "        vcvars32.bat\n\n"
                   + "    3.  Set RedPrairie environment variables.\n\n"
                   + "        cd %LESDIR%\\data\n"
                   + "        ..\\moca\\bin\\servicemgr /env=<environment name> /dump\n"
                   + "        env.bat\n\n"
                   + "        Note: If you know your env.bat file is current you can omit this step,\n"
                   + "              if you are not sure then rebuild one.\n\n"
                   + "    4.  Shutdown the RedPrairie instance:\n\n"
                   + "        NON-CLUSTERED Environment\n\n"
                   + "        *** IMPORTANT ***\n"
                   + "        If you are on a production system, make sure the development system\n"
                   + "        whose drive has been mapped to the system being modified has also been\n"
                   + "        shutdown to avoid sharing violations.\n\n"
                   + "        net stop moca.<environment name>\n\n"
                   + "        (Or use the Windows Services snap-in to stop the RedPrairie service.\n\n"
                   + "        CLUSTERED Environment\n\n"
                   + "        If you are running under a Windows Server Cluster, you must use the\n"
                   + "        Microsoft Cluster Administrator to stop the RedPrairie Service.\n\n"
                   + "    5.  Copy the rollout distribution file into the environment's rollout\n"
                   + "        directory.\n\n"
                   + "        cd -d %LESDIR%\\rollouts\n"
                   + "        copy <SOURCE_DIR>\\" + extension_name + ".\n\n"
                   + "    6.  Uncompress the distribution file using your preferred unzip utility\n\n"
                   + "        Make sure you extract all the files to a folder called " + extension_name + ".\n\n"
                   + "    7.  Install the rollout.\n\n"
                   + "        perl -S rollout.pl " + extension_name + "\n\n"
                   + "    8.  Start up the RedPrairie instance:\n\n"
                   + "        NON-CLUSTERED Environment\n\n"
                   + "        net start moca.<environment name>\n\n"
                   + "        (Or use the Windows Services snap-in to restart the RedPrairie service.\n\n"
                   + "        CLUSTERED Environment\n\n"
                   + "        If you are running under a Windows Server Cluster, you must use the\n"
                   + "        Microsoft Cluster Administrator to start the RedPrairie Service.\n\n\n"
                   + "================================================================================\n"
                   + "                 U N I X   I N S T A L L A T I O N   N O T E S                  \n"
                   + "================================================================================\n\n"
                   + "    1.  Login as the Logistics Suite environment's administrator.\n\n"
                   + "        ssh <user>@<hostname>\n\n"
                   + "    2.  Shutdown the RedPrairie instance:\n\n"
                   + "        rp stop\n\n"
                   + "    3.  Copy the rollout distribution file into the environment's rollout\n"
                   + "        directory.\n\n"
                   + "        cd $LESDIR/rollouts\n"
                   + "        cp <SOURCE_DIR>/" + extension_name + ".zip .\n\n"
                   + "    4.  Uncompress and untar the rollout archive file.\n\n"
                   + "    5.  Install the rollout.\n\n"
                   + "        perl -S rollout.pl " + extension_name + "\n\n"
                   + "    6.  Start up the RedPrairie instance:\n\n"
                   + "        rp start\n\n"
                   + "================================================================================");
            
        }
        catch (Exception e) {
           // e.printStackTrace();
            error_readme = 1;
           // JOptionPane.showMessageDialog(null, "Ha Ocurrido un error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            try {
                if (null != readme) readme.close();
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        
        if((error_script == 1 || error_readme == 1))
			System.out.println("Something went wrong...Failed to generate Rollout file");
        JOptionPane.showMessageDialog(null, "Rollout created successfully!!");
    }
	
	private void removeSection(ArrayList<String> removed,PrintWriter pw){
		for(int i=0;i<removed.size();i++) {
			String r_file=removed.get(i);
			String ext=r_file.substring(r_file.lastIndexOf(".")+1,r_file.length());
			if(ext.equals("mcmd")||ext.equals("mtrg")||ext.equals("c")||ext.equals("java")) {
				r_file="REMOVE $LESDIR/"+r_file;
				pw.println(r_file);
			}	
		}
	}
	
	// Replace files string
    public  void replaceSection(File dir,String path, PrintWriter pw) {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    replaceSection(listFile[i], path, pw);
                } else {
                	String replace="REPLACE " + listFile[i].toString().replaceAll("\\\\", "/");
                	replace=replace.replace(Rpath, "");
                	String les=" $LESDIR/" + listFile[i].toString().replaceAll("\\\\", "/");
                	les=les.replace("pkg/" , "").replace("/"+listFile[i].getName(), "").replace(Rpath,"");
                    pw.println(replace+les);                    
                }
            }
        }
    }
    
	//Runsql string 
    public  void scriptSection(File dir, String path, PrintWriter pw) {
        String extension;
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    scriptSection(listFile[i], path, pw);
                } else {
                    extension = listFile[i].toString().substring(listFile[i].toString().lastIndexOf(".") + 1, listFile[i].toString().length());
                    if(extension.equals("sql")) {
                        pw.println("RUNSQL " + listFile[i].toString().replace("\\", "/").replace(Rpath+"pkg", "$LESDIR"));
                    }
                }
            }
        }
    }
    
    //PorArchivoCTL
    public  void loadDataSection(File dir, String path, PrintWriter pw) {
        String extension;
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    loadDataSection(listFile[i], path, pw);
                } else {
                    extension = listFile[i].toString().substring(listFile[i].toString().lastIndexOf(".") + 1, listFile[i].toString().length());
                    if(extension.equals("ctl")) {
                        String csv_path = listFile[i].toString().replace("." + extension, "");
                        String ctl_file = "LOADDATA " + listFile[i].toString().replace("\\", "/").replace(Rpath+"pkg", "$LESDIR");
                        loadDataCSV(new File(csv_path),ctl_file, csv_path, pw);
                    }
                }
            }
        }
    }
    
    public void loadDataCSV(File path, String ctl_file, String csv_path, PrintWriter pw) {
        File listFile[] = path.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                pw.println(ctl_file + " " + listFile[i].toString().replace(csv_path + "\\", ""));
            }
        }
    }
    
    //PorArchivo CSV
    public  void loadDataSectionByCSV(File dir, String path, PrintWriter pw) {
        String extension;
        String ctl_path;
        String file_name;
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    loadDataSectionByCSV(listFile[i], path, pw);
                } else {
                    extension = listFile[i].toString().substring(listFile[i].toString().lastIndexOf(".") + 1, listFile[i].toString().length());
                    if(extension.equals("csv")) {
                        ctl_path = "LOADDATA " + listFile[i].toString().replace("\\", "/").replace(Rpath+"pkg", "$LESDIR").replace("." + extension, "");
                        file_name = ctl_path.substring(ctl_path.lastIndexOf("/"));
                        ctl_path = ctl_path.replace(file_name, "") + ".ctl";
                        file_name = file_name.replace("/", "") + ".csv";
                        pw.println(ctl_path + " " + file_name);
                    }
                }
            }
        }
    }
       
    public void seamlesSection(File dir, String path, PrintWriter pw) {
        String extension;
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    seamlesSection(listFile[i], path, pw);
                } else {
                    extension = listFile[i].toString().substring(listFile[i].toString().lastIndexOf(".") + 1, listFile[i].toString().length());
                    if(extension.equals("slexp")) {
                        pw.println("IMPORTSLDATA " + listFile[i].toString().replace("\\", "/").replace(Rpath+"pkg" , "$LESDIR"));
                    }
                }
            }
        }
    }
    
    public void affectedFilesSection(File dir, String path, PrintWriter pw) {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    affectedFilesSection(listFile[i], path, pw);
                } else {
                    pw.println("� " + listFile[i].toString().replace(Rpath+"" , "$LESDIR"));
                }
            }
        }
    }
	
}
