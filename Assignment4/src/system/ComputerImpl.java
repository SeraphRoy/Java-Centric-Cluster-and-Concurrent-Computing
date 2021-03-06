package system;

import api.Space;
import api.Task;
import api.Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Runtime;

public class ComputerImpl extends UnicastRemoteObject implements Computer{

    public int numTasks = 0;

    public final int coreNum;

    private BlockingQueue<Task> tasksQ = new LinkedBlockingQueue<>();

    public ComputerImpl() throws RemoteException{
        System.out.println(SpaceImpl.MULTICORE + " " + SpaceImpl.preFetchNum);
        int temp = Runtime.getRuntime().availableProcessors()/2 - 1;
        if (temp < 1)
            temp = 2;
        coreNum = SpaceImpl.MULTICORE ? temp : 1;
        for(int i = 0; i < coreNum; i++){
            new Thread(new Core(tasksQ)).start();
        }
    }

    public void Execute(Task task) throws RemoteException{
        numTasks++;
        try{
            //task.run();
            //if(tasksQ.size() == 0)
            tasksQ.put(task);
            if(tasksQ.size() > SpaceImpl.preFetchNum)
                //if(tasksQ.size() > 30)
                synchronized(tasksQ){
                    tasksQ.wait();
                }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void Execute(List<Task> tasks) throws RemoteException{
        for(Task t : tasks){
            Execute(t);
        }
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException{
        SpaceImpl.MULTICORE = Boolean.parseBoolean(args[0]);
        SpaceImpl.preFetchNum = Integer.parseInt(args[1]);
        final String domainName = "localhost";
        System.setSecurityManager( new SecurityManager() );
        final String url = "rmi://" + domainName + ":" + Space.PORT + "/" + Space.SERVICE_NAME;
        final Space space = (Space) Naming.lookup(url);
        ComputerImpl computer = new ComputerImpl();
        try{
            space.register(computer);
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        System.out.println( "Computer running." );
    }

    /**
     * Terminate the JVM.
     * @throws RemoteException - always!
     */
        @Override
        public void exit() throws RemoteException{
            Logger.getLogger( this.getClass().getName() )
                .log(Level.INFO, "Computer: on exit, # completed [0] tasks:", numTasks );
            System.exit( 0 );
        }
}
