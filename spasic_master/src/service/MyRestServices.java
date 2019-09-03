package service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("api")
public class MyRestServices extends ResourceConfig {

	public MyRestServices() {
        packages("com.fasterxml.jackson.jaxrs.json");
        packages("service");
    }
	

public static void main(String[] args)
{
	try {
		URL url = new URL("http://m-lab.etf.unibl.org:8080/amadeos_umlcdlayouter/services/layout");
		URLConnection conn = url.openConnection();
		DataOutputStream is = new DataOutputStream(conn.getOutputStream());
		
        String content = new String(Files.readAllBytes(Paths.get("C://cdm.uml")));
        
        conn.setDoOutput( true );
        //conn.setInstanceFollowRedirects( false );
        conn.setRequestProperty( "Content-Type", "multipart/form-data"); 
        conn.setRequestProperty( "charset", "utf-8");
        conn.setRequestProperty( "Content-Length", new Integer(content.length()).toString());
        
        conn.setUseCaches( false );
        //is.write( c );
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
        outputStreamWriter.write(content);
        outputStreamWriter.flush();
        
        /*BufferedReader bufferedReader = new BufferedReader(
        		 new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
        	 System.out.println(line);
        	 }
        outputStreamWriter.close();
        bufferedReader.close();*/
        
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
}

}
