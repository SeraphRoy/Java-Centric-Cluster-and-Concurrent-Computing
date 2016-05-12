package api;
import system.*;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.List;
import java.rmi.RemoteException;
import javax.swing.JLabel;

/**
 *
 * @author Peter Cappello, Yanxi Chen
 * The client should override spawn(), spawnNext(), generateArgument(), and needToCompute() for general tasks,
 * and only the last two for compose tasks.
 */
public abstract class Task implements Serializable, Runnable{

    public static boolean useA = false;

    final protected Space space;

    final protected List<Argument> argumentList;

    protected Continuation cont;

    protected int argc;

    public long id;

    public Task(Space space, List<Argument> list, Continuation cont){
        this.space = space;
        this.argumentList = list;
        this.cont = cont;
        this.id = java.util.UUID.randomUUID().getLeastSignificantBits();
    }

    public Task(Space space, List<Argument> list){
        this.space = space;
        this.argumentList = list;
        this.cont = null;
        this.id = java.util.UUID.randomUUID().getLeastSignificantBits();
    }

    public void run(){
        if(needToCompute()){
            Object o = generateArgument();
            try{
                space.sendArgument(cont, o);
            }
            catch(Exception e){
                System.err.println("ERROR IN SENDING ARGUMENT");
            }
        }
        else{
            try{
                SpawnResult result  = spawn();
                space.putWaiting(result.successor);

                if(Task.useA){
                    for(int i = 1; i < result.subTasks.size(); i++){
                        Continuation cont = generateCont(i, result.successor);
                        result.subTasks.get(i).cont = cont;
                        space.putReady(result.subTasks.get(i));
                    }
                    Continuation cont = generateCont(0, result.successor);
                    Task task = result.subTasks.get(0);
                    task.cont = cont;
                    task.run();
                }
                else{
                    for(int i = 0; i < result.subTasks.size(); i++){
                        Continuation cont = generateCont(i, result.successor);
                        result.subTasks.get(i).cont = cont;
                        space.putReady(result.subTasks.get(i));
                    }
                }
            }
            catch(Exception e){
                System.err.println("ERROR IN PRODUCING SUBTASKS");
            }
        }
    }


    public JLabel viewResult(Object result){
        System.err.println("You shouldn't reach this point");
        return new JLabel();
    }

    public SpawnResult spawn() throws RemoteException, InterruptedException{
        System.err.println("You shouldn't reach this point");
        return null;
    }

    public Continuation generateCont(int slot, Task t){
        return new Continuation(t.id, slot);
    }

    public abstract Object generateArgument();

    //default is true for compose tasks
    //normal tasks NEED override this
    public boolean needToCompute(){
        return true;
    }

    public List<Argument> getArgumentList(){return argumentList;}

    public Continuation getCont(){return cont;}

    public Space getSapce(){return space;}

    public int getArgc(){return argc;}

    public class SpawnResult{
        public Task successor;
        // the tasks should be ordered according to the slot#
        protected List<Task> subTasks;

        public SpawnResult(Task successor, List<Task> subTasks){
            this.successor = successor;
            this.subTasks = subTasks;
        }
    }
}