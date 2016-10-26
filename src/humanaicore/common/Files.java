/** Ben F Rayfield offers this "common" software to everyone opensource GNU LGPL */
package humanaicore.common;
import static humanaicore.common.CommonFuncs.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import occamserver.Occamserver;

public class Files{
	private Files(){}

	public static byte[] read(File file){
		try{
			if(file.isDirectory()) return new byte[0];
			InputStream fileIn = null;
			try{
				fileIn = new FileInputStream(file);
				byte fileBytes[] = new byte[fileIn.available()];
				int bytesRead = fileIn.read(fileBytes);
				if(bytesRead != fileBytes.length) throw new IOException(
					"Tried to write "+fileBytes.length+" bytes but did write "+bytesRead+" bytes.");
				return fileBytes;
			}finally{
				if(fileIn!=null) fileIn.close();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static void overwrite(byte data[], File file){
		write(data, file, false);
	}
	
	public static void append(byte data[], File file){
		write(data, file, true);
	}
	
	/** Creates dirs under file if not exist already, unless names of those dirs already exist as files */
	protected static void write(byte data[], File file, boolean append){
		lg("Saving "+file);
		try{
			file.getParentFile().mkdirs();
			OutputStream fileOut = null;
			try{
				if(!file.exists()){
					File parent = file.getParentFile();
					if(parent!=null) parent.mkdirs();
					file.createNewFile();
				}
				fileOut = new FileOutputStream(file, append);
				fileOut.write(data, 0, data.length);
				fileOut.flush();
			}finally{
				if(fileOut!=null) fileOut.close();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static final File dirWhereThisProgramStarted =
		new File(System.getProperty("user.dir")).getAbsoluteFile();
	
	public static boolean bytesEqual(byte x[], byte y[]){
		if(x.length != y.length) return false;
		for(int i=0; i<x.length; i++){
			if(x[i] != y[i]) return false;
		}
		return true;
	}
	
	public static byte[] readFileRel(String relPath){
		//TODO merge duplicate code
		String r = relPath.startsWith("\\")||relPath.startsWith("/") ? relPath.substring(1) : relPath;
		File f = new File(r);
		if(f.exists()){ //avoid cache problems with using file url
			System.out.println("Reading file "+f);
			InputStream in = null;
			try{
				in = new FileInputStream(f);
				if(Integer.MAX_VALUE < f.length()) throw new RuntimeException("File too big: "+f);
				byte b[] = new byte[(int)f.length()];
				in.read(b);
				return b;
			}catch(IOException e){
				throw new RuntimeException(e);
			}finally{
				if(in != null) try{ in.close(); }catch(IOException e){}
			}
		}else{
			//System.out.println("Reading Class.getResourceAsStream "+relPath);
			InputStream in = Files.class.getResourceAsStream(relPath);
			try{
				return Occamserver.readFully(in, 1<<28, 5, true);
			}catch(IOException e){
				throw new RuntimeException(e);
			}finally{
				if(in != null) try{ in.close(); }catch(IOException e){}
			}
		}
	}

}
