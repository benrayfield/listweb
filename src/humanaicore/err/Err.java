package humanaicore.err;

public class Err extends RuntimeException{
	
	public Err(){}
	
	public Err(String message){
		super(message);
	}
	
	public Err(String message, Throwable t){
		super(message, t);
	}
	
	public Err(Throwable t){
		super(t);
	}

}
