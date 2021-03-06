package railo.commons.io.res.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import railo.commons.io.DevNullOutputStream;
import railo.commons.io.IOUtil;
import railo.commons.io.SystemUtil;
import railo.commons.io.res.ContentType;
import railo.commons.io.res.ContentTypeImpl;
import railo.commons.io.res.Resource;
import railo.commons.io.res.ResourcesImpl;
import railo.commons.io.res.filter.ExtensionResourceFilter;
import railo.commons.io.res.filter.ResourceFilter;
import railo.commons.io.res.filter.ResourceNameFilter;
import railo.commons.lang.StringUtil;
import railo.runtime.PageContext;
import railo.runtime.PageSource;
import railo.runtime.config.Config;
import railo.runtime.exp.ExpressionException;
import railo.runtime.exp.PageException;
import railo.runtime.type.List;

public final class ResourceUtil {
	
	/**
     * Field <code>FILE_SEPERATOR</code>
     */
    public static final char FILE_SEPERATOR=File.separatorChar; 
    /**
     * Field <code>FILE_ANTI_SEPERATOR</code>
     */
    public static final char FILE_ANTI_SEPERATOR=(FILE_SEPERATOR=='/')?'\\':'/';
    
    /**
     * Field <code>TYPE_DIR</code>
     */
    public static final short TYPE_DIR=0;
    
    /**
     * Field <code>TYPE_FILE</code>
     */
    public static final short TYPE_FILE=1;

    /**
     * Field <code>LEVEL_FILE</code>
     */
    public static final short LEVEL_FILE=0;
    /**
     * Field <code>LEVEL_PARENT_FILE</code>
     */
    public static final short LEVEL_PARENT_FILE=1;
    /**
     * Field <code>LEVEL_GRAND_PARENT_FILE</code>
     */
    public static final short LEVEL_GRAND_PARENT_FILE=2;
    
    
    private static boolean isUnix=SystemUtil.isUnix();
	

    //private static Magic mimeTypeParser; 
	
    /**
     * cast a String (argumet destination) to a File Object, 
     * if destination is not a absolute, file object will be relative to current position (get from PageContext)
     * file must exist otherwise throw exception
     * @param pc Page Context to et actuell position in filesystem
     * @param path relative or absolute path for file object
     * @return file object from destination
     * @throws ExpressionException
     */
    public static Resource toResourceExisting(PageContext pc ,String path) throws ExpressionException {
    	return toResourceExisting(pc, path,pc.getConfig().allowRealPath());
    }
    public static Resource toResourceExisting(PageContext pc ,String path,boolean allowRealpath) throws ExpressionException {
    	path=path.replace('\\','/');
    	Resource res = pc.getConfig().getResource(path);
        
        // not allow realpath
        if(!allowRealpath){
        	if(res.exists()) return res;
        	throw new ExpressionException("file or directory "+path+" not exist");  
        }
        
    	if(res.isAbsolute() && res.exists()) {
            return res;
        }
    	
        //if(allowRealpath){
	        if(StringUtil.startsWith(path,'/')) {
	        	res = pc.getPhysical(path,true);
	            if(res!=null && res.exists()) return res;
	        }
	        res=ResourceUtil.getCanonicalResourceEL(pc.getCurrentPageSource().getPhyscalFile().getParentResource().getRealResource(path));
	        if(res.exists()) return res;
    	//}
        
        throw new ExpressionException("file or directory "+path+" not exist");      
    }
    
    public static Resource toResourceExisting(Config config ,String path) throws ExpressionException {
    	path=path.replace('\\','/');
    	Resource res = config.getResource(path);
        
        if(res.exists()) return res;
        throw new ExpressionException("file or directory "+path+" not exist");   
    }
    
    public static Resource toResourceNotExisting(Config config ,String path) {
    	Resource res;
        path=path.replace('\\','/');  
    	res=config.getResource(path);
    	return res;
    }
    
    

    /**
     * cast a String (argumet destination) to a File Object, 
     * if destination is not a absolute, file object will be relative to current position (get from PageContext)
     * at least parent must exist
     * @param pc Page Context to et actuell position in filesystem
     * @param destination relative or absolute path for file object
     * @return file object from destination
     * @throws ExpressionException
     */

    public static Resource toResourceExistingParent(PageContext pc ,String destination) throws ExpressionException {
    	return toResourceExistingParent(pc, destination, pc.getConfig().allowRealPath());
    }
    
    public static Resource toResourceExistingParent(PageContext pc ,String destination, boolean allowRealpath) throws ExpressionException {
    	destination=destination.replace('\\','/');
        Resource res=pc.getConfig().getResource(destination);
        
        // not allow realpath
        if(!allowRealpath){
        	if(res.exists() || parentExists(res))
        		return res;
        	throw new ExpressionException("parent directory "+res.getParent()+"  for file "+destination+" doesn't exist");
            
        }
        
        // allow realpath
        if(res.isAbsolute() && (res.exists() || parentExists(res))) {
        	return res;
        }
        //if(allowRealpath){
	        if(StringUtil.startsWith(destination,'/')) {
	            res = pc.getPhysical(destination,true);
	            if(res!=null && (res.exists() || parentExists(res))) return res;
	        }
	    	res=ResourceUtil.getCanonicalResourceEL(pc.getCurrentPageSource().getPhyscalFile().getParentResource().getRealResource(destination));
	        if(res!=null && (res.exists() || parentExists(res))) return res;
        //}
    
        throw new ExpressionException("parent directory "+res.getParent()+"  for file "+destination+" doesn't exist");
           
    }
    
    /**
     * cast a String (argument destination) to a File Object, 
     * if destination is not a absolute, file object will be relative to current position (get from PageContext)
     * existing file is prefered but dont must exist
     * @param pc Page Context to et actuell position in filesystem
     * @param destination relative or absolute path for file object
     * @return file object from destination
     */

    public static Resource toResourceNotExisting(PageContext pc ,String destination) {
    	return toResourceNotExisting(pc ,destination,pc.getConfig().allowRealPath());
    }
    
    public static Resource toResourceNotExisting(PageContext pc ,String destination,boolean allowRealpath) {
    	Resource res;
        destination=destination.replace('\\','/');  
    	
    	if(!allowRealpath){
    		res=pc.getConfig().getResource(destination);
    		return res;
    	}
    	
    	boolean isUNC;
        if(!(isUNC=isUNCPath(destination)) && StringUtil.startsWith(destination,'/')) {
        	Resource res2 = pc.getPhysical(destination,SystemUtil.isWindows());
            if(res2!=null) return res2;
            //res2 = pc.getPhysical(destination,true);
            //if(res2!=null && res2.exists()) return res2;
        }
        if(isUNC) {
        	res=pc.getConfig().getResource(destination.replace('/','\\'));
        }
        else res=pc.getConfig().getResource(destination);
        if(res.isAbsolute()) return res;
        
        
        try {
        	return pc.getCurrentPageSource().getPhyscalFile().getParentResource().getRealResource(destination).getCanonicalResource();
        } 
        catch (IOException e) {}
        return res;
    }
    
	

    private static boolean isUNCPath(String path) {
        return SystemUtil.isWindows() && path.startsWith("//") ;
	}
    
    /**
     * transalte the path of the file to a existing file path by changing case of letters
     * Works only on Linux, becasue 
     * 
     * Example Unix:
     * we have a existing file with path "/usr/virtual/myFile.txt"
     * now you call this method with path "/Usr/Virtual/myfile.txt"
     * the result of the method will be "/usr/virtual/myFile.txt"
     * 
     * if there are more file with rhe same name but different cases
     * Example:
     *  /usr/virtual/myFile.txt
     *  /usr/virtual/myfile.txt
     *  /Usr/Virtual/myFile.txt
     *  the nearest case wil returned
     * 
     * @param res
     * @return file
     */
    public static Resource toExactResource(Resource res) {
        res=getCanonicalResourceEL(res);
        if(isUnix) {
            if(res.exists()) return res;
            return _check(res);
            
        }
        return res;
    }
    private static Resource _check(Resource file) {
    	// todo cascade durch while ersetzten
        Resource parent=file.getParentResource();
        if(parent==null) return file;
        
        if(!parent.exists()) {
            Resource op=parent;
            parent=_check(parent);
            if(op==parent) return file;
            if((file = parent.getRealResource(file.getName())).exists()) return file;
        }
        
        String[] files = parent.list();
        if(files==null) return file;
        String name=file.getName();
        for(int i=0;i<files.length;i++) {
            if(name.equalsIgnoreCase(files[i]))
                return parent.getRealResource(files[i]);
        }
        return file;
    }
    
    /**
     * create a file if possible, return file if ok, otherwise return null 
     * @param res file to touch 
     * @param level touch also parent and grand parent
     * @param type is file or directory
     * @return file if exists, otherwise null
     */
    public static Resource createResource(Resource res, short level, short type) {
        
        boolean asDir=type==TYPE_DIR;
        // File
        if(level>=LEVEL_FILE && res.exists() && ((res.isDirectory() && asDir)||(res.isFile() && !asDir))) {
            return getCanonicalResourceEL(res);
        }
        
        // Parent
        Resource parent=res.getParentResource();
        if(level>=LEVEL_PARENT_FILE && parent!=null && parent.exists() && canRW(parent)) {
            if(asDir) {
                if(res.mkdirs()) return getCanonicalResourceEL(res);
            }
            else {
                if(createNewResourceEL(res))return getCanonicalResourceEL(res);
            }
            return getCanonicalResourceEL(res);
        }    
        
        // Grand Parent
        if(level>=LEVEL_GRAND_PARENT_FILE && parent!=null) {
            Resource gparent=parent.getParentResource();
            if(gparent!=null && gparent.exists() && canRW(gparent)) {
                if(asDir) {
                    if(res.mkdirs())return getCanonicalResourceEL(res);
                }
                else {
                    if(parent.mkdirs() && createNewResourceEL(res))
                        return getCanonicalResourceEL(res);
                }
            }        
        }
        return null;
    }
    
	public static void setAttribute(Resource res,String attributes) throws IOException {
		/*if(res instanceof File && SystemUtil.isWindows()) {
			if(attributes.length()>0) {
				attributes=ResourceUtil.translateAttribute(attributes);
				Runtime.getRuntime().exec("attrib "+attributes+" " + res.getAbsolutePath());
	    	}
		}
		else {*/
			short[] flags = strAttrToBooleanFlags(attributes);
			
			if(flags[READ_ONLY]==YES)res.setWritable(false);
			else if(flags[READ_ONLY]==NO)res.setWritable(true);
			
			if(flags[HIDDEN]==YES)		res.setAttribute(Resource.ATTRIBUTE_HIDDEN, true);//setHidden(true);
			else if(flags[HIDDEN]==NO)	res.setAttribute(Resource.ATTRIBUTE_HIDDEN, false);//res.setHidden(false);
			
			if(flags[ARCHIVE]==YES)		res.setAttribute(Resource.ATTRIBUTE_ARCHIVE, true);//res.setArchive(true);
			else if(flags[ARCHIVE]==NO)	res.setAttribute(Resource.ATTRIBUTE_ARCHIVE, false);//res.setArchive(false);
			
			if(flags[SYSTEM]==YES)		res.setAttribute(Resource.ATTRIBUTE_SYSTEM, true);//res.setSystem(true);
			else if(flags[SYSTEM]==NO)	res.setAttribute(Resource.ATTRIBUTE_SYSTEM, false);//res.setSystem(false);
			
		//}
	}

	//private static final int NORMAL=0;
	private static final int READ_ONLY=1;
	private static final int HIDDEN=2;
	private static final int ARCHIVE=3;
	private static final int SYSTEM=4;

	//private static final int IGNORE=0;
	private static final int NO=1;
	private static final int YES=2;
	
	

    private static short[] strAttrToBooleanFlags(String attributes) throws IOException {
        
        String[] arr;
		try {
			arr = List.toStringArray(List.listToArrayRemoveEmpty(attributes.toLowerCase(),','));
		} 
		catch (PageException e) {
			arr=new String[0];
		}
        
        boolean hasNormal=false;
        boolean hasReadOnly=false;
        boolean hasHidden=false;
        boolean hasArchive=false;
        boolean hasSystem=false;
        
        for(int i=0;i<arr.length;i++) {
           String str=arr[i].trim().toLowerCase();
           if(str.equals("readonly") || str.equals("read-only") || str.equals("+r")) hasReadOnly=true;
           else if(str.equals("normal") || str.equals("temporary")) hasNormal=true;
           else if(str.equals("hidden") || str.equals("+h")) hasHidden=true;
           else if(str.equals("system") || str.equals("+s")) hasSystem=true;
           else if(str.equals("archive") || str.equals("+a")) hasArchive=true;
           else throw new IOException("invalid attribute definition ["+str+"]");
        }
        
        short[] flags=new short[5];
        
        if(hasReadOnly)flags[READ_ONLY]=YES;
        else if(hasNormal)flags[READ_ONLY]=NO;
        
        if(hasHidden)flags[HIDDEN]=YES;
        else if(hasNormal)flags[READ_ONLY]=NO;
        
        if(hasSystem)flags[SYSTEM]=YES;
        else if(hasNormal)flags[SYSTEM]=NO;
        
        if(hasArchive)flags[ARCHIVE]=YES;
        else if(hasNormal)flags[ARCHIVE]=NO;

        return flags;
    }
	
	
	/**
     * sets attributes of a file on Windows system
     * @param file
     * @param attributes
     * @throws PageException
     * @throws IOException
     */
    public static String translateAttribute(String attributes) throws IOException {
        short[] flags = strAttrToBooleanFlags(attributes);
       
        StringBuilder sb=new StringBuilder();
        if(flags[READ_ONLY]==YES)sb.append(" +R");
        else if(flags[READ_ONLY]==NO)sb.append(" -R");
        
        if(flags[HIDDEN]==YES)sb.append(" +H");
        else if(flags[HIDDEN]==NO)sb.append(" -H");
        
        if(flags[SYSTEM]==YES)sb.append(" +S");
        else if(flags[SYSTEM]==NO)sb.append(" -S");
        
        if(flags[ARCHIVE]==YES)sb.append(" +A");
        else if(flags[ARCHIVE]==NO)sb.append(" -A");

        return sb.toString();
    }

	/* *
	 * transalte a path in a proper form
	 * example susi\petere -> /susi/peter
	 * @param path
	 * @return path
	 * /
	public static String translatePath(String path) {
		/*path=prettifyPath(path);
		if(path.indexOf('/')!=0)path='/'+path;
		int index=path.lastIndexOf('/');
		// remove slash at the end
		if(index==path.length()-1) path=path.substring(0,path.length()-1);
		return path;* /
		return translatePath(path, true, false);
	}*/
	
	/* *
	 * transalte a path in a proper form
	 * example susi\petere -> susi/peter/
	 * @param path
	 * @return path
	 * /
	public static String translatePath2x(String path) {
		/*path=prettifyPath(path);
		if(path.indexOf('/')==0)path=path.substring(1);
		int index=path.lastIndexOf('/');
		// remove slash at the end
		if(index!=path.length()-1) path=path+'/';* /
		return translatePath(path, false, true);
	}*/
	

	public static String translatePath(String path, boolean slashAdBegin, boolean slashAddEnd) {
		path=prettifyPath(path);
		
		// begin
		if(slashAdBegin) {
			if(path.indexOf('/')!=0)path='/'+path;
		}
		else {
			if(path.indexOf('/')==0)path=path.substring(1);
		}
		
		// end
		int index=path.lastIndexOf('/');
		if(slashAddEnd) {
			if(index!=path.length()-1) path=path+'/';
		}
		else {
			if(index==path.length()-1 && index>-1) path=path.substring(0,path.length()-1);
		}
		return path;
	}
	
	
	

	/**
	 * transalte a path in a proper form and cut name away
	 * example susi\petere -> /susi/ and  peter
	 * @param path
	 * @return
	 */
	public static String[] translatePathName(String path) {
		path=prettifyPath(path);
		if(path.indexOf('/')!=0)path='/'+path;
		int index=path.lastIndexOf('/');
		// remove slash at the end
		if(index==path.length()-1) path=path.substring(0,path.length()-1);
		
		index=path.lastIndexOf('/');
		String name;
		if(index==-1) {
			name=path;
			path = "/";
		}
		else {
			name = path.substring(index+1);
			path = path.substring(0,index+1);
		}
		return new String[] {path,name};
	}
	
	public static String prettifyPath(String path) {
		path=path.replace('\\','/');
		return StringUtil.replace(path, "//", "/", false);
		// TODO /aaa/../bbb/
	}

	public static String removeScheme(String scheme, String path) {
		if(path.indexOf("://")==scheme.length() && StringUtil.startsWithIgnoreCase(path,scheme)) path=path.substring(3+scheme.length());
		return path;
	}

	/**
	 * merge to path parts to one
	 * @param parent
	 * @param child
	 * @return
	 */
	public static String merge(String parent, String child) {
		if(child.length()<=2) {
			if(child.length()==0) return parent;
			if(child.equals(".")) return parent;
			if(child.equals("..")) child="../";
		}
		
		
		
		parent=translatePath(parent, true, false);
		child=prettifyPath(child);//child.replace('\\', '/');
		
		if(child.startsWith("./"))child=child.substring(2);
		if(StringUtil.startsWith(child, '/'))return parent.concat(child);
		if(!StringUtil.startsWith(child, '.'))return parent.concat("/").concat(child);
		
		
		while(child.startsWith("../")) {
			parent=pathRemoveLast(parent);
			child=child.substring(3);
		}
		if(StringUtil.startsWith(child, '/'))return parent.concat(child);
		return parent.concat("/").concat(child);
	}
	
	private static String pathRemoveLast(String path) {
		if(path.length()==0) return "..";
		
		else if(path.endsWith("..")){
		    return path.concat("/..");
		}
		return path.substring(0,path.lastIndexOf('/'));
	}

	/**
     * Returns the canonical form of this abstract pathname.
     * @param file file to get canoncial form from it
     *
     * @return  The canonical pathname string denoting the same file or
     *          directory as this abstract pathname
     *
     * @throws  SecurityException
     *          If a required system property value cannot be accessed.
     */
    public static String getCanonicalPathEL(Resource res) {
        try {
            return res.getCanonicalPath();
        } catch (IOException e) {
            return res.toString();
        }
    }
    
    
    /**
     * Returns the canonical form of this abstract pathname.
     * @param file file to get canoncial form from it
     *
     * @return  The canonical pathname string denoting the same file or
     *          directory as this abstract pathname
     *
     * @throws  SecurityException
     *          If a required system property value cannot be accessed.
     */
    public static Resource getCanonicalResourceEL(Resource res) {
        if(res==null) return res;
    	try {
            return res.getCanonicalResource();
        } catch (IOException e) {
            return res;
        }
    }
    
    /**
     * creates a new File
     * @param file
     * @return was successfull
     */
    public static boolean createNewResourceEL(Resource res) {
        try {
            res.createFile(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean exists(Resource res) {
        return res!=null && res.exists();
    }
    

    /**
     * check if file is read and writable
     * @param file
     * @return is or not
     */
    public static boolean canRW(Resource res) {
        return res.isReadable() && res.isWriteable();
    }
    

    /**
     * similat to linux bash fuction toch, create file if not exists oherwise change last modified date
     * @param res
     * @throws IOException
     */
    public static void touch(Resource res) throws IOException {
    	
        if(res.exists()) {
            res.setLastModified(System.currentTimeMillis());
        }
        else {
            res.createFile(true);
        }
    }
    

    /**
     * return the mime type of a file, dont check extension
     * @param res
     * @param defaultValue 
     * @return mime type of the file
     */
    public static String getMymeType(Resource res, String defaultValue) {
        
    	PrintStream out = System.out;
        try {
        	System.setOut(new PrintStream(DevNullOutputStream.DEV_NULL_OUTPUT_STREAM));
            MagicMatch match = Magic.getMagicMatch(IOUtil.toBytes(res));
            return match.getMimeType();
        } 
        catch (Exception e) {
            return defaultValue;
        }
        finally {
        	System.setOut(out);
        }
    }

    /**
     * return the mime type of a file, dont check extension
     * @param barr
     * @return mime type of the file
     * @throws IOException 
     */
    public static String getMymeType(byte[] barr) throws IOException {
        //if(mimeTypeParser==null)mimeTypeParser=new Magic();
    	PrintStream out = System.out;
        try {
        	System.setOut(new PrintStream(DevNullOutputStream.DEV_NULL_OUTPUT_STREAM));
            MagicMatch match = Magic.getMagicMatch(barr);
            return match.getMimeType();
        } 
        catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        finally {
        	System.setOut(out);
        }
    }

    /**
     * return the mime type of a file, dont check extension
     * @param barr
     * @param defaultValue 
     * @return mime type of the file
     */
    public static String getMymeType(byte[] barr, String defaultValue) {
    	PrintStream out = System.out;
        try {
        	System.setOut(new PrintStream(DevNullOutputStream.DEV_NULL_OUTPUT_STREAM));
            MagicMatch match = Magic.getMagicMatch(barr);
            return match.getMimeType();
        } 
        catch (Exception e) {
            return defaultValue;
        }
        finally {
        	System.setOut(out);
        }
    }
    
	/**
	 * check if file is a child of given directory
	 * @param file file to search
	 * @param dir directory to search
	 * @return is inside or not
	 */
	public static boolean isChildOf(Resource file, Resource dir) {
	    if(file==null) return false;
	    if(file.equals(dir)) return true;
	    return isChildOf(file.getParentResource(),dir);
	}
	/**
	 * return diffrents of one file to a other if first is child of second otherwise return null
	 * @param file file to search
	 * @param dir directory to search
	 */
	public static String getPathToChild(Resource file, Resource dir) {
		boolean isFile=file.isFile();
		String str="/";
		while(file!=null) {
			//print.out("- "+file+".equals("+dir+"):"+file.equals(dir));
			if(file.equals(dir)) {
				if(isFile)
					return str.substring(0,str.length()-1);
				return str;
			}
			str="/"+file.getName()+str;
			file=file.getParentResource();
		}
		return null;
	}
	
    /**
     * get the Extension of a file
     * @param res
     * @return extension of file
     */
    public static String getExtension(Resource res) {
        return getExtension(res.getName());
    }

    /**
     * get the Extension of a file
     * @param strFile
     * @return extension of file
     */
    public static String getExtension(String strFile) {
        int pos=strFile.lastIndexOf('.');
        if(pos==-1)return null;
        return strFile.substring(pos+1);
    }
    
    public static String getName(String strFileName) {
        int pos=strFileName.lastIndexOf('.');
        if(pos==-1)return strFileName;
        return strFileName.substring(0,pos);
    }
    
    /**
     * split a FileName in Parts
     * @param fileName
     * @return new String[]{name[,extension]}
     */
    public static String[] splitFileName(String fileName) {
        int pos=fileName.lastIndexOf('.');
        if(pos==-1) {
            return new String[]{fileName};
        }
        return new String[]{fileName.substring(0,pos),fileName.substring(pos+1)};
    }
    
    /**
     * change extesnion of file and return new file
     * @param file
     * @param newExtension
     * @return  file with new Extension
     */
    public static Resource changeExtension(Resource file, String newExtension) {
        String ext=getExtension(file);
        if(ext==null) return file.getParentResource().getRealResource(file.getName()+'.'+newExtension);
        //new File(file.getParentFile(),file.getName()+'.'+newExtension);
        String name=file.getName();
        return file.getParentResource().getRealResource(name.substring(0,name.length()-ext.length())+newExtension);
        //new File(file.getParentFile(),name.substring(0,name.length()-ext.length())+newExtension);
    }
    
    /**
     * @param res delete the content of a directory
     */

    public static void deleteContent(Resource src,ResourceFilter filter) {
    	_deleteContent(src, filter,false);
    }
    public static void _deleteContent(Resource src,ResourceFilter filter,boolean deleteDirectories) {
    	if(src.isDirectory()) {
        	Resource[] files=filter==null?src.listResources():src.listResources(filter);
            for(int i=0;i<files.length;i++) {
            	_deleteContent(files[i],filter,true);
            	if(deleteDirectories)src.delete();
            }
            
        }
        else if(src.isFile()) {
        	src.delete();
        }
    }
    

    /**
     * copy a file or directory recursive (with his content)
     * @param file file or directory to delete
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public static void copyRecursive(Resource src,Resource trg) throws IOException {
		copyRecursive(src, trg,null);
	}
    
    
    /**
     * copy a file or directory recursive (with his content)
     * @param src
     * @param trg
     * @param filter
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public static void copyRecursive(Resource src,Resource trg,ResourceFilter filter) throws IOException {
    	//print.out(src);
    	//print.out(trg);
        if(!src.exists()) return ;
        if(src.isDirectory()) {
        	if(!trg.exists())trg.createDirectory(true);
        	Resource[] files=filter==null?src.listResources():src.listResources(filter);
            for(int i=0;i<files.length;i++) {
            	copyRecursive(files[i],trg.getRealResource(files[i].getName()),filter);
            }
        }
        else if(src.isFile()) {
        	touch(trg);
        	IOUtil.copy(src,trg);
        }
    }
    
	public static void copy(Resource src, Resource trg) throws IOException {
		if(src.equals(trg)) return;
		ResourceUtil.checkCopyToOK(src,trg);
		IOUtil.copy(src,trg);
	}
    
    
    /**
     * return if parent file exists 
     * @param res file to check
     * @return parent exists?
     */
    private static boolean parentExists(Resource res) {
        res=res.getParentResource();
        return res!=null && res.exists();
    }

	public static void removeChildren(Resource res) throws IOException {
		removeChildren(res, (ResourceFilter)null);
	}

	public static void removeChildren(Resource res,ResourceNameFilter filter) throws IOException {
		Resource[] children = filter==null?res.listResources():res.listResources(filter);
		if(children==null) return;
		
		for(int i=0;i<children.length;i++) {
			children[i].remove(true);
		}
	}
	
	public static void removeChildren(Resource res,ResourceFilter filter) throws IOException {
		Resource[] children = filter==null?res.listResources():res.listResources(filter);
		if(children==null) return;
		
		for(int i=0;i<children.length;i++) {
			children[i].remove(true);
		}
	}

	public static void removeChildrenEL(Resource res,ResourceNameFilter filter) {
		try {
			removeChildren(res,filter);
		}
		catch(Throwable e) {}
	}

	public static void removeChildrenEL(Resource res,ResourceFilter filter) {
		try {
			removeChildren(res,filter);
		}
		catch(Throwable e) {}
	}
	
	public static void removeChildrenEL(Resource res) {
		try {
			removeChildren(res);
		}
		catch(Throwable e) {}
	}

	public static void removeEL(Resource res, boolean force) {
		try {
			res.remove(force);
		} 
		catch (Throwable t) {}
	}

	public static void createFileEL(Resource res, boolean force) {
		try {
			res.createFile(force);
		} 
		catch (IOException e) {}
	}

	public static void createDirectoryEL(Resource res, boolean force) {
		try {
			res.createDirectory(force);
		} 
		catch (IOException e) {}
	}

	public static ContentType getContentType(Resource resource) {
		InputStream is=null;
		try {
			is = resource.getInputStream();
			return new ContentTypeImpl(is);
		}
		catch(IOException e) {
			return ContentTypeImpl.APPLICATION_UNKNOW;
		}
		finally {
			IOUtil.closeEL(is);
		}
	}
	
	public static void moveTo(Resource src, Resource dest) throws IOException {
		ResourceUtil.checkMoveToOK(src, dest);
		
		if(src.isFile()){
			if(!dest.exists()) dest.createFile(false);
			IOUtil.copy(src,dest);
			src.remove(false);
		}
		else {
			if(!dest.exists()) dest.createDirectory(false);
			Resource[] children = src.listResources();
			for(int i=0;i<children.length;i++){
				moveTo(children[i],dest.getRealResource(children[i].getName()));
			}
			src.remove(false);
		}
		dest.setLastModified(System.currentTimeMillis());
	}

	/**
	 * return the size of the Resource, other than method length of Resource this mthod return the size of all files in a directory
	 * @param collectionDir
	 * @return
	 */
	public static long getRealSize(Resource res) {
		return getRealSize(res,null);
	}
	
	/**
	 * return the size of the Resource, other than method length of Resource this mthod return the size of all files in a directory
	 * @param collectionDir
	 * @return
	 */
	public static long getRealSize(Resource res, ResourceFilter filter) {
		if(res.isFile()) {
			return res.length();
		}
		else if(res.isDirectory()) {
			long size=0;
			Resource[] children = filter==null?res.listResources():res.listResources(filter);
			for(int i=0;i<children.length;i++) {
				size+=getRealSize(children[i]);
			}
			return size;
		}
		
		return 0;
	}


	/**
	 * return if Resource is empty, means is directory and has no children or a empty file,
	 * if not exists return false.
	 * @param res
	 * @return
	 */
	public static boolean isEmpty(Resource res) {
		return isEmptyDirectory(res) || isEmptyFile(res);
	}

	public static boolean isEmptyDirectory(Resource res) {
		if(res.isDirectory()) {
			String[] children = res.list();
			return children==null || children.length==0;
		}
		return false;
	}
	
	public static boolean isEmptyFile(Resource res) {
		if(res.isFile()) {
			return res.length()==0;
		}
		return false;
	}

	public static Resource toResource(File file) {
		return ResourcesImpl.getFileResourceProvider().getResource(file.getPath());
	}


	/**
	 * list childrn of all given resources
	 * @param resources
	 * @return
	 */
	public static Resource[] listResources(Resource[] resources,ResourceFilter filter) {
		int count=0;
		Resource[] children;
		ArrayList list=new ArrayList();
		for(int i=0;i<resources.length;i++) {
			children=filter==null?resources[i].listResources():resources[i].listResources(filter);
			if(children!=null){
				count+=children.length;
				list.add(children);
			}
			else list.add(new Resource[0]);
		}
		Resource[] rtn=new Resource[count];
		int index=0;
		for(int i=0;i<resources.length;i++) {
			children=(Resource[]) list.get(i);
			for(int y=0;y<children.length;y++) {
				rtn[index++]=children[y];
			}
		}
		//print.out(rtn);
		return rtn;
	}


	public static Resource[] listResources(Resource res,ResourceFilter filter) {
		return filter==null?res.listResources():res.listResources(filter);
	}
	

	public static void deleteFileOlderThan(Resource res, long date, ExtensionResourceFilter filter) {
		if(res.isFile()) {
			if(res.lastModified()<=date) res.delete();
		}
		else if(res.isDirectory()) {
			Resource[] children = filter==null?res.listResources():res.listResources(filter);
			for(int i=0;i<children.length;i++) {
				deleteFileOlderThan(children[i],date,filter);
			}
		}
	}
	
	/**
	 * check if directory creation is ok with the rules for the Resource interface, to not change this rules.
	 * @param resource
	 * @param createParentWhenNotExists
	 * @throws IOException
	 */
	public static void checkCreateDirectoryOK(Resource resource, boolean createParentWhenNotExists) throws IOException {
		if(resource.exists()) {
			if(resource.isFile()) 
				throw new IOException("can't create directory ["+resource.getPath()+"], resource already exists as a file");
			if(resource.isDirectory()) 
				throw new IOException("can't create directory ["+resource.getPath()+"], directory already exists");
		}
		
		Resource parent = resource.getParentResource();
		// when there is a parent but the parent does not exists
		if(parent!=null) {
			if(!parent.exists()) {
				if(createParentWhenNotExists)parent.createDirectory(true);
				else throw new IOException("can't create file ["+resource.getPath()+"], missng parent directory");
			}
			else if(parent.isFile()) {
				throw new IOException("can't create directory ["+resource.getPath()+"], parent is a file");
			}
		}
	}


	/**
	 * check if file creating is ok with the rules for the Resource interface, to not change this rules.
	 * @param resource
	 * @param createParentWhenNotExists
	 * @throws IOException
	 */
	public static void checkCreateFileOK(Resource resource, boolean createParentWhenNotExists) throws IOException {
		if(resource.exists()) {
			if(resource.isDirectory()) 
				throw new IOException("can't create file ["+resource.getPath()+"], resource already exists as a directory");
			if(resource.isFile()) 
				throw new IOException("can't create file ["+resource.getPath()+"], file already exists");
		}
		
		Resource parent = resource.getParentResource();
		// when there is a parent but the parent does not exists
		if(parent!=null) {
			if(!parent.exists()) {
				if(createParentWhenNotExists)parent.createDirectory(true);
				else throw new IOException("can't create file ["+resource.getPath()+"], missng parent directory");
			}
			else if(parent.isFile()) {
				throw new IOException("can't create file ["+resource.getPath()+"], parent is a file");
			}
		}
	}

	/**
	 * check if copying a file is ok with the rules for the Resource interface, to not change this rules.
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void checkCopyToOK(Resource source, Resource target) throws IOException {
		if(!source.isFile()) {
			if(source.isDirectory())
				throw new IOException("can't copy ["+source.getPath()+"] to ["+target.getPath()+"], source is a directory");
			throw new IOException("can't copy ["+source.getPath()+"] to ["+target.getPath()+"], source file does not exists");
		}
		else if(target.isDirectory()) {
			throw new IOException("can't copy ["+source.getPath()+"] to ["+target.getPath()+"], target is a directory");
		}
	}

	/**
	 * check if moveing a file is ok with the rules for the Resource interface, to not change this rules.
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void checkMoveToOK(Resource source, Resource target) throws IOException {
		if(!source.exists()) {
			throw new IOException("can't move ["+source.getPath()+"] to ["+target.getPath()+"], source file does not exists");
		}
		if(source.isDirectory() && target.isFile())
			throw new IOException("can't move ["+source.getPath()+"] directory to ["+target.getPath()+"], target is a file");
		if(source.isFile() && target.isDirectory())
			throw new IOException("can't move ["+source.getPath()+"] file to ["+target.getPath()+"], target is a directory");
	}

	/**
	 * check if getting a inputstream of the file is ok with the rules for the Resource interface, to not change this rules.
	 * @param resource
	 * @throws IOException
	 */
	public static void checkGetInputStreamOK(Resource resource) throws IOException {
		if(!resource.exists())
			throw new IOException("file ["+resource.getPath()+"] does not exists");
		
		if(resource.isDirectory())
			throw new IOException("can't read directory ["+resource.getPath()+"] as a file");

	}

	/**
	 * check if getting a outputstream of the file is ok with the rules for the Resource interface, to not change this rules.
	 * @param resource
	 * @throws IOException
	 */
	public static void checkGetOutputStreamOK(Resource resource) throws IOException {
		if(resource.exists() && !resource.isWriteable()) {
			throw new IOException("can't write to file ["+resource.getPath()+"],file is readonly");
		}
		if(resource.isDirectory())
			throw new IOException("can't write directory ["+resource.getPath()+"] as a file");
		if(!resource.getParentResource().exists())
			throw new IOException("can't write file ["+resource.getPath()+"] as a file, missing parent directory ["+resource.getParent()+"]");
	}

	/**
	 * check if removing the file is ok with the rules for the Resource interface, to not change this rules.
	 * @param resource
	 * @throws IOException
	 */
	public static void checkRemoveOK(Resource resource) throws IOException {
		if(!resource.exists())throw new IOException("can't delete resource "+resource+", resource does not exists");
		if(!resource.canWrite())throw new IOException("can't delete resource "+resource+", no access");
		
	}
	
	public static void deleteEmptyFolders(Resource res) throws IOException {
		if(res.isDirectory()){
			Resource[] children = res.listResources();
			for(int i=0;i<children.length;i++){
				deleteEmptyFolders(children[i]);
			}
			if(res.listResources().length==0){
				res.remove(false);
			}
		}
	}
	
	// FUTURE this method should be part of pagesource in a more proper way, there should be a method getResource() inside PageSource
	public static Resource getResource(PageContext pc,PageSource ps) throws ExpressionException {
		Resource res = ps.getPhyscalFile();
		
		// there is no physical resource
		if(res==null){
        	String path=ps.getDisplayPath();
        	if(path.startsWith("ra://"))
        		path="zip://"+path.substring(5);
        	res=ResourceUtil.toResourceExisting(pc, path,false);
        }
		return res;
	}
	
	public static int directrySize(Resource dir,ResourceFilter filter) {
		if(dir==null || !dir.isDirectory()) return 0;
		if(filter==null) return dir.list().length;
		return dir.list(filter).length;
	}
	
	public static int directrySize(Resource dir,ResourceNameFilter filter) {
		if(dir==null || !dir.isDirectory()) return 0;
		if(filter==null) return dir.list().length;
		return dir.list(filter).length;
	}

}
