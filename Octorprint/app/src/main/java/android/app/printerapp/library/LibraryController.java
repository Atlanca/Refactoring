package android.app.printerapp.library;

import android.annotation.SuppressLint;
import android.app.printerapp.ListContent;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.model.ModelFile;
import android.database.Cursor;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;


/**
 * This class will handle the file storage architecture and retrieval in a static way
 * so every class can access these methods from wherever
 * @author alberto-baeza
 *
 */
@SuppressLint("DefaultLocale")
public class LibraryController {

	private static ArrayList<File> mFileList = new ArrayList<File>();
    private static ArrayList<ListContent.DrawerListItem> mHistoryList = new ArrayList<ListContent.DrawerListItem>();
	private static File mCurrentPath;

    public static final String TAB_ALL = "all";
    public static final String TAB_CURRENT = "current";
    public static final String TAB_PRINTER = "printer";
    public static final String TAB_FAVORITES = "favorites";
	
	public LibraryController(){
		mFileList.clear();
		//Retrieve normal files
		retrieveFiles( getParentFolder(), false);
	
	}
	
	public static ArrayList<File> getFileList(){
		return mFileList;
	}
	
	/**
	 * Retrieve files from the provided path
	 * If it's recursive, also search inside folders
	 * @param path
	 * @param recursive
	 */
	public static void retrieveFiles(File path, boolean recursive){
			
		File[] files = path.listFiles();

        if (files !=null)
		for (File file : files){
			
			//If folder
			if (file.isDirectory()){	

                //exclude files from the temporary folder
                if (!file.getAbsolutePath().equals(getParentFolder() + "/temp")){

                    //If project
                    if (isProject(file)){


                        //Create new project
                        ModelFile m = new ModelFile(file.getAbsolutePath(), "Internal storage");

                        addToList(m);

                        //Normal folder
                    } else {

                        if (recursive) {

                            //Retrieve files for the folder
                            retrieveFiles(new File(file.getAbsolutePath()),true);

                        } else addToList(file);


                    }


                }

				
			//TODO this will eventually go out
			} else {
				
				//Add only stl and gcode
				if ((hasExtension(0, file.getName())) || (hasExtension(1, file.getName()))){
					addToList(file);
				}
				
				
			}

		}
		
		//Set new current path
		mCurrentPath = path;
	}
	
	//Retrieve main folder or create if doesn't exist
	//TODO: Changed main folder to FILES folder.
	public static File getParentFolder(){
		String parentFolder = Environment.getExternalStorageDirectory().toString();
		File mainFolder = new File(parentFolder + "/PrintManager");
		mainFolder.mkdirs();
		File temp_file = new File(mainFolder.toString());

		return temp_file;
	}
	
	
	//Create a new folder in the current path
	//TODO add dialogs maybe
	public static void createFolder(String name){
			
		File newFolder = new File(mCurrentPath + "/" + name);
		
		if (!newFolder.mkdir()){
			
		} else {
			
			addToList(newFolder);
		}
			
		
	}
	
	
	//Retrieve a certain element from file storage
	public static String retrieveFile(String name, String type){
		
		String result = null;
		File folder = new File(name + "/" + type + "/");
		try {
			
			String file = folder.listFiles()[0].getName();
			
			result = folder + "/" + file; 
		
			
			//File still in favorites
		} catch (Exception e){
			//e.printStackTrace();
		}
		
		
		return result;	
		
	}
	
	//Reload the list with another path
	public static void reloadFiles(String path){
		
		mFileList.clear();
		
		//Retrieve every single file by recursive search
		//TODO Not retrieving printers
		if (path.equals(TAB_ALL)){

            File parent = new File(getParentFolder() + "/Files/");
			retrieveFiles(parent, true);
			mCurrentPath = parent;
		}
				
	}
	
	//Check if a folder is also a project
	//TODO return individual results according to the amount of elements found
	public static boolean isProject(File file){
		
		FilenameFilter f = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				
				return filename.endsWith("thumb");
			}
		};
		
		if (file.exists()){
            if (file.list(f).length > 0) return true;
        }


        return false;
	}
	
	/**
	 * Method to check if a file is a proper .gcode or .stl
	 * @param type 0 for .stl 1 for .gcode
	 * @param name name of the file
	 * @return true if it's the desired type
	 */

	public static boolean hasExtension(int type, String name){
		
		switch (type){
			
		case 0: if (name.toLowerCase().endsWith(".stl"))  return true;
			break;
		case 1: if ((name.toLowerCase().endsWith(".gcode")) || (name.toLowerCase().endsWith(".gco")) || (name.toLowerCase().endsWith(".g"))) return true;
			break;
		}
		
		return false;
	}
	
	public static void addToList(File m ){
		mFileList.add(m);
	}

	public static File getCurrentPath(){
		return mCurrentPath;
	}
    public static void setCurrentPath(String path) {
        mCurrentPath = new File(path);
    }

    /**
     * Delete files recursively
     * @param file
     */
    public static void deleteFiles(File file){



        if (file.isDirectory()){

            if (DatabaseController.isPreference(DatabaseController.TAG_FAVORITES, file.getName())) {
                DatabaseController.handlePreference(DatabaseController.TAG_FAVORITES, file.getName(), null, false);
            }

            for (File f : file.listFiles()){

                deleteFiles(f);

            }

        }
        file.delete();

    }

    //Check if a file already exists in the current folder
    public static boolean fileExists(String name){

        for (File file : mFileList){

            String nameFinal = name.substring(0,name.lastIndexOf('.'));

            if (nameFinal.equals(file.getName())) {
                return true;
            }

        }

        return false;

    }

}
