package humanaicore.err;

/** Thrown when theres more code to write */
public class Todo extends Err{
	
	public Todo(){}
	
	public Todo(String message){
		super(message);
	}
	
	public Todo(String message, Throwable t){
		super(message, t);
	}
	
	public Todo(Throwable t){
		super(t);
	}

}
