
package edu.gvsu.cis.cis656.lab2.client;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.Vector;
import org.zeromq.ZMQ;
import edu.gvsu.cis.cis656.lab2.PresenceService;
import edu.gvsu.cis.cis656.lab2.RegistrationInfo;


public class ChatClient extends UnicastRemoteObject implements Runnable,Serializable{
	
	protected ChatClient() throws RemoteException {
		super();
	}
	private static final long serialVersionUID = 1L;
	
	static String x;
	
	static String currentUser;
	
	public static void main(String args[]){
		currentUser = args[0];
		x = args[1];
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        
        try {        
        	String name = "PresenceService";
            Registry registry = LocateRegistry.getRegistry("127.0.0.1");
            PresenceService ps = (PresenceService) registry.lookup(name);
            
            if(args.length > 1) {
	            String[] serverDetails = args[1].split(":");
	            RegistrationInfo temp = ps.lookup(args[0]);
	            
	            if(temp!=null && args[0].equals(temp.getUserName())){
	            	System.out.println("The username already exists");
	            	System.exit(1);
	            } else{
	            	ps.register(new RegistrationInfo(args[0],serverDetails[0], Integer.parseInt(serverDetails[1]),true));
	            	System.out.println("New user registered - Welcome " + args[0]);
	                boolean connected=true;
	            	Scanner input = new Scanner(System.in);
	            	Thread t = new Thread(new ChatClient());
	            	t.start();
	            	
	            	Thread t2 = new Thread(new Runnable(){
            			@Override
            			public void run() {
            				while(true){
            					synchronized(this){
		            				ZMQ.Context context = ZMQ.context(1);
		            				ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
		            		        
		            		        subscriber.subscribe("".getBytes());
		            		        subscriber.connect("tcp://localhost:5566");	
		            		        while (!Thread.currentThread().isInterrupted()) {
		            		        	 String recdText = subscriber.recvStr();
		            		        	 String frmUser = recdText.substring(0, recdText.indexOf(":"));
		            		        	 if(!frmUser.equals(currentUser)) {
		            		        		 String msg = recdText.substring(recdText.indexOf(":")+1);
		            		        		 System.out.println("Broadcast message : "+msg);
		            		        	 }
		            		        }
            					}
            				}
            			}
            		});
        			t2.start();
        			
	            	//t.join();
	            	while(connected){
	                	String a = input.next();
	                	if(a.equals("friends")){
	                		Vector<RegistrationInfo> users = ps.listRegisteredUsers();
	            			
	            			for(RegistrationInfo user : users) {
	            				System.out.println("Name: " + user.getUserName() + " Status: " + (user.getStatus()?"Available":"Not Available"));
	            			}
	                	} else if(a.equals("talk")){
	                		
	            			synchronized(ChatClient.class){
		                		String talkTo = input.next();
		                		String text = input.nextLine();
		                		int clientPort;
		                		String clientHost;
		                		boolean clientStatus;
	                		
		                		if(ps.lookup(talkTo)!=null){
			                		clientPort = ps.lookup(talkTo).getPort();
			                		clientHost = ps.lookup(talkTo).getHost();
			                		clientStatus = ps.lookup(talkTo).getStatus();
	                		
			                		if(clientStatus==true){
				                		/*Socket cs = new Socket(clientHost,clientPort);
				                		PrintStream os = new PrintStream(cs.getOutputStream());
				                		os.println("Received message from " + args[0] + ": " + text);
				                		cs.close();*/
			                			 ZMQ.Context context = ZMQ.context(1);
			                			 ZMQ.Socket requester = context.socket(ZMQ.REQ);
			                			 requester.connect("tcp://localhost:"+clientPort);
			                			 String message = "Text from " + args[0] + ": " + text;
			                			 requester.send(message.getBytes(), 0);
			                			 requester.close();
			                			 context.term();
			                		} else System.out.println("User Unavailable");			                		
		                		}else System.out.println("Try again, This user does not exist");
	            			}
	                	} else if(a.equals("broadcast")){
	                		String text = input.nextLine();
	                		
	                		ps.broadcast(args[0] + ":" +text);
	                		
	                	} else if(a.equals("busy")){
	                    	RegistrationInfo currentUser = ps.lookup(args[0]);
	                    	if(currentUser.getStatus()==true){
	                    		currentUser.setStatus(false);
	                    		ps.updateRegistrationInfo(currentUser);
	                    	}
	                    	else System.out.println("Your status is already unavailable");
	                    	
	                	} else if(a.equals("available")){
	                    	RegistrationInfo currentUser = ps.lookup(args[0]);
	                    	if(currentUser.getStatus()==false){
	                    		currentUser.setStatus(true);
	                    		ps.updateRegistrationInfo(currentUser);
	                    	}
	                    	else System.out.println("You are already online");
	                    	
	                	} else if(a.equals("exit")){
	                		System.out.println("Exiting program");
	                    	RegistrationInfo currentUser = ps.lookup(args[0]);
	                		ps.unregister(currentUser.getUserName());
	                		System.exit(1);
	               		 	connected = false;
	                	}else
	                		System.out.println("Bad input, try again");
	            	}	
	            }         
            }else {
            	System.out.println("Try again, bad arguments");
            	System.exit(1);
            }
        }catch (Exception e) {
            System.err.println("ChatClient exception:");
            e.printStackTrace();
        }
        
	}         
	
	@Override
	public void run() {		
		while(true){
           	synchronized(this){
					String[] socketServerDetails = x.split(":");
					
					while (!Thread.currentThread().isInterrupted()) {
						ZMQ.Context context = ZMQ.context(1);
						//  Socket to talk to clients
						ZMQ.Socket responder = context.socket(ZMQ.REP);
						responder.bind("tcp://*:"+socketServerDetails[1]);
			            // Wait for next request from the client
			            byte[] request = responder.recv(0);
			            if(request != null) {
				            System.out.println("Received " + new String (request));
				            responder.close();
					        context.term();
			            }
			            
			            /*ZMQ.Context context1 = ZMQ.context(1);
        				ZMQ.Socket subscriber = context1.socket(ZMQ.SUB);
        		        
        		        subscriber.subscribe("".getBytes());
        		        subscriber.connect("tcp://localhost:5566");	
        		        while (!Thread.currentThread().isInterrupted()) {
        		        	 String recdText = subscriber.recvStr();
             				System.out.println("Broadcast message : "+recdText);
        		        }*/
			            
			        }
			        
				}
            } 
        } 
		
}

    
    