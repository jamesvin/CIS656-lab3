package edu.gvsu.cis.cis656.lab2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientService extends Remote{
	
	 void sendPrivateMessage(String message,  String currentUser) throws RemoteException;
	 
	 void broadcastMessage(String message) throws RemoteException;

	 boolean startClient(PresenceService ps, String username, String host, int port) throws RemoteException, MalformedURLException, IOException;
}
