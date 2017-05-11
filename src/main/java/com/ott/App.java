package com.ott;

import static spark.Spark.*;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App 
{

	private static HashMap<String, String> channelFileHashMap;
	private static HashMap<String, Double> channelTotalTimeHashMap;
	private static HashMap<String, Integer> channelTotalSegmentHashMap;
	private static HashMap<String, ArrayList<SegmentData>> channelSegmentData;
	
	public static Date serverStartTime;
	
	public static void createChannelHashMap()
	{
		
		channelFileHashMap = new HashMap<String, String>();

		File currentDir  = new File("");
		String directory = currentDir.getAbsolutePath() + "/src/playlist/";
		
		FileManager fileManager = new FileManager();
		
		File files[] = fileManager.getAllFiles(directory);
		for (File file : files) {
			channelFileHashMap.put(file.getName(), directory + file.getName());
		}
	}
	
	public static void createChannelData()
	{
		serverStartTime = new Date();
		
    	createChannelHashMap();

		channelTotalTimeHashMap    = new HashMap<String, Double>();
		channelTotalSegmentHashMap = new HashMap<String, Integer>();
		channelSegmentData         = new HashMap<String, ArrayList<SegmentData>>();
		
    	FileManager fileManager = new FileManager();
    	
    	channelFileHashMap.forEach((channel,filename)->{
			try {
				BufferedReader fileReader = new BufferedReader(new FileReader(fileManager.getFile(filename)));
				
		        String secondsPattern = "([0-9.]+)";
		        String durationText = new String();
		        double totalTime = 0;

		        ArrayList<SegmentData> tempSegmentDataArrayList = new ArrayList<SegmentData>();
		        Pattern sp = Pattern.compile(secondsPattern);
		        
		        String line;
		        while ((line = fileReader.readLine()) != null)
		        {
		        	if(line.startsWith("#EXTINF")){
		        		Matcher matcher = sp.matcher(line);
		                while(matcher.find()) {
		                	durationText = durationText + matcher.group();
		                }
		        	}else if(line.startsWith("http")){
				        SegmentData segmentDataObject = new SegmentData();
				        segmentDataObject.setDurationText(durationText);
				        segmentDataObject.setSegmentURL(line);
				        
		 		        totalTime = totalTime + Double.parseDouble(durationText);
		 		        durationText = "";

		 		        tempSegmentDataArrayList.add(segmentDataObject);
		        	}
		    	}
		    	fileReader.close();
		    	
		    	channelTotalTimeHashMap.put(channel, totalTime);
		    	channelSegmentData.put(channel, tempSegmentDataArrayList);
		    	channelTotalSegmentHashMap.put(channel, channelSegmentData.get(channel).size());
		    	
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	});
	}
	
    public static void main(String[] args)
    {
        createChannelData();

        get("/:m3u8", (request, response) -> {

        	String channel = request.params(":m3u8");
        	String extinfMain = "";
        	String extinf = "#EXTINF:";
        	
            Date currentTime   = new Date();
            double delta       = (currentTime.getTime() - serverStartTime.getTime()) / 1000.00;

        	double loops       = Math.floor( delta / channelTotalTimeHashMap.get(channel));
        	double deltaInLoop = delta - (channelTotalTimeHashMap.get(channel) * loops); 
        	int media_sequence = (int) (loops * channelTotalSegmentHashMap.get(channel));
        	int sequenceInLoop = 0;
            
        	ArrayList<SegmentData> channelSegments = channelSegmentData.get(channel);
        	ArrayList<String> segments = new ArrayList<String>();

        	for (int i = 0; i < channelSegments.size(); i++) {
        		deltaInLoop = deltaInLoop - Double.parseDouble(channelSegments.get(i).getDurationText());
        		
        		if(deltaInLoop > 0){
        			sequenceInLoop = sequenceInLoop + 1;
        		}else if(segments.size() < 10){
        			segments.add(channelSegments.get(i).getDurationText() + ",\n" + channelSegments.get(i).getSegmentURL());
        		}
        		else{
        			break;
        		}
        	}
        	
    		for (String segment : segments) {
    			extinfMain = extinfMain + extinf + segment + "\n";
    		}
    		
    		media_sequence = media_sequence + sequenceInLoop;
    		
    		response.type("application/vnd.apple.mpegurl");
    		
            return "#EXTM3U\n#EXT-X-TARGETDURATION:11\n#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:" + media_sequence + "\n" + extinfMain;
        });
    }
    
}
