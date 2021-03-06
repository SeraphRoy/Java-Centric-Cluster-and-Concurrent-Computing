package api;
import system.*;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.List;
import java.rmi.RemoteException;
import javax.swing.JLabel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter Cappello, Yanxi Chen
 * The client should override spawn(), spawnNext(), generateArgument(), and needToCompute() for general tasks,
 * and only the last two for compose tasks.
 */
public abstract class Task<T> implements Serializable{

    public Computer computer;

    final protected List<Argument<T>> argumentList;

    protected Continuation cont;

    protected int argc;

    public long id;

    public Share share;

    public long parent_T_1 = 0;

    public long parent_T_Inf = 0;

    public Task(List<Argument<T>> list, Continuation cont){
        this.argumentList = list;
        this.cont = cont;
        this.id = java.util.UUID.randomUUID().getLeastSignificantBits();
        this.computer = null;
        this.share = null;
    }

    public Task(List<Argument<T>> list){
        this.argumentList = list;
        this.cont = null;
        this.id = java.util.UUID.randomUUID().getLeastSignificantBits();
        this.computer = null;
        this.share = null;
    }

    // updateComputerShare is false only when the task is executed on space
    public ResultWrapper execute(boolean updateComputerShare){
        ResultWrapper result = null;
        if(needToProceed()){
            final long taskStartTime = System.nanoTime();
            if(needToCompute()){
                T o = generateArgument();
                try{
                    result = new ResultWrapper(1, cont, o, this);
                    final long taskRunTime = ( System.nanoTime() - taskStartTime ) / 1000000;
                    result.T_1 += taskRunTime;
                    result.T_Inf += taskRunTime;
                    if(this instanceof ComposeTask){
                        result.T_1 += this.parent_T_1;
                        result.T_Inf += this.parent_T_Inf;
                        long max = -1;
                        for(Argument argument : argumentList){
                            result.T_1 += argument.T_1;
                            if(argument.T_Inf > max)
                                max = argument.T_Inf;
                        }
                        result.T_Inf += max;
                    }

                    Comparable comp = generateShareValue(o);
                    Share newShare = new Share(comp);
                    if(newShare.isBetterThan(this.share) && updateComputerShare){
                        result.needToUpdate = true;
                        computer.updateShare(newShare);
                    }
                    return result;
                }
                catch(Exception e){
                    System.err.println("ERROR IN SENDING ARGUMENT");
                    e.printStackTrace();
                }
            }
            else{
                try{
                    SpawnResult spawnResult  = spawn();
                    result = new ResultWrapper(2, spawnResult, this);
                    final long taskRunTime = ( System.nanoTime() - taskStartTime ) / 1000000;
                    spawnResult.successor.parent_T_1 += taskRunTime;
                    spawnResult.successor.parent_T_Inf += taskRunTime;
                    return result;
                }
                catch(Exception e){
                    e.printStackTrace();
                    System.err.println("ERROR IN PRODUCING SUBTASKS");
                }
            }
        }
        result = new ResultWrapper(0, cont, null, this);
        return result;
    }

    public abstract Comparable generateShareValue(T o);

    // default is based on SpaceCallable marking interface
    public boolean spaceCallable(){
        return this instanceof SpaceCallable;
    }

    public JLabel viewResult(T result){
        System.err.println("You shouldn't reach this point");
        return new JLabel();
    }

    public SpawnResult spawn() throws RemoteException, InterruptedException{
        System.err.println("You shouldn't reach this point");
        return null;
    }

    public static Continuation generateCont(int slot, Task t){
        return new Continuation(t.id, slot);
    }

    public abstract T generateArgument();

    //default is true for compose tasks
    //normal tasks NEED override this
    public boolean needToCompute(){
        return true;
    }

    //default is true for compose tasks
    //normal tasks NEED override this
    public boolean needToProceed(){
        return true;
    }

    public List<Argument<T>> getArgumentList(){return argumentList;}

    public Continuation getCont(){return cont;}

    public void setCont(Continuation cont){this.cont = cont;}

    public int getArgc(){return argc;}

}
