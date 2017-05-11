package com.ott;

import java.io.File;

public class FileManager 
{
	
	public File[] getAllFiles(String directory) 
	{
		File files = new File(directory);
		return files.listFiles();
	}
	
	public File getFile(String filename) 
	{
		File file = new File(filename);
		return file;
	}
	
}
