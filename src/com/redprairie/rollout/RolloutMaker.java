package com.redprairie.rollout;

public class RolloutMaker {
	
//	public static void main (String[] args) throws java.lang.Exception{
//		if(args[0]!=null||args[1]!=null){
//			Rollout r= new Rollout(args[0],args[1]);
//			String success	="Sucess: Rollout for "+args[0]+" created in Rollouts directory...";
//			String failed 	="Failed: Check Rollouts/Logs/"+args[0]+".log for more details...";
//			System.out.println(r.createRollout()?success:failed);
//		}else
//			System.out.println("Missing parameter...");	
//		
//	}
	
	public static void main (String[] args) throws java.lang.Exception{
			
		
			System.out.println();
			Rollout r= new Rollout();
			String success	="Success: Rollout created in Rollouts directory...";
			String failed 	="Failed: Check Rollouts/{BranchName}.log for more details...";
			System.out.println(r.createRollout()?success:failed);
		
	}
	
}
