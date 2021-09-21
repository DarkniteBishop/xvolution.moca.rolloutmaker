package com.redprairie.rollout;

import java.text.MessageFormat;
import java.util.logging.Logger;

public class RolloutMaker {	
	
	
	public static void main (String[] args) throws java.lang.Exception{
		
			System.out.println();
			Rollout r= new Rollout();
			for(String arg: args) {
				if(arg.equals("-version")|| arg.equals("-VERSION")) {					
					System.out.println(r.version);					
					System.out.println(r.author);
					return;
				}
			}
			
			String msg = "";
			if(r.createRollout()) 
				msg = MessageFormat.format("Success: Rollout {0} created in Rollouts directory",r.Name);
			else 
				msg = MessageFormat.format("Failed: Check Rollouts/{0}.log for more details",r.Name);
			System.out.println(msg);			
		
	}
	
}
