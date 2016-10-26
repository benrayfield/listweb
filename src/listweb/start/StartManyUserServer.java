package listweb.start;
import static humanaicore.common.CommonFuncs.*;
import occamserver.MapFunc;
import occamserver.Occamserver;
import occamserver.WrapMapFuncInHttpBytesFunc;
import occamsjsonds.JsonDS;
import humanaicore.common.Files;
import humanaicore.common.Text;
import humanaicore.common.Time;
import humanaicore.err.Err;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class StartManyUserServer{
	private StartManyUserServer(){}
	
	public static void main(String[] args){
		lg(StartManyUserServer.class.getName());
		lg("TODO after fix some shortcuts taken to sync easier");
	}

}
