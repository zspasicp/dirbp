package service;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.net.InetAddresses;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.ServletContext;


@Path("data")
public class RestService {

	@Context javax.servlet.http.HttpServletRequest request; 
	public static final String XMITYPE = "http://schema.omg.org/spec/XMI/2.1";
	Connection conn;
	String columnDataType;
	Element oaAttribute = null;
	String dataTypePackageId = "";
	byte[] byteArray;
	String payload = "";
	public String databaseName = "";
	public String publicip = "";
	
	HashMap<String, String> hmapDataTypes = new HashMap<String, String>();
	HashMap<String, String> hmapClassName = new HashMap<String, String>();
	HashMap<String, String> hmapColumns = new HashMap<String, String>();
	HashMap<String, String> hmapColumnsDataTypes = new HashMap<String, String>();
	HashMap<String, String> hmapParentTablesDependies = new HashMap<String, String>();
	HashMap<String, String> hmapViewName = new HashMap<String, String>();
	HashMap<String, String> hmapColumnsView = new HashMap<String, String>();
	HashMap<String, String> hmapColumnsDataTypesView = new HashMap<String, String>();
	HashMap<String, String> hmapTablesDependiesView = new HashMap<String, String>();
	HashMap<String, String> hmapIndexName = new HashMap<String, String>();
	HashMap<String, String> hmapAliasView = new HashMap<String, String>();
	HashMap<String, String> hmapMySqlTablesAlias = new HashMap<String, String>();
	HashMap<String, String> hmapMySqlAliasColumns = new HashMap<String, String>();
	//HashMap<String, Connection> hmapConnections = new HashMap<String, Connection>();
	
	@GET    /*metoda vraca listu trenutno dostupnih servera za MS SQL server!*/
    @Path("servers")
    @Produces("application/json")
	public String findServers() {
	    String[] str = new String[] {"cmd.exe", "/c", "osql", "-L", 
	                            "&&", "sp_servers","&&", 
	                            "GO"     
	            };
	    Runtime rt = Runtime.getRuntime();
	    JsonArray json = new JsonArray();
	    
	    try{
	        Process p = rt.exec(str);
	        InputStream is = p.getInputStream();
	        InputStream err = p.getErrorStream();
	        InputStreamReader in = new InputStreamReader(is);
	        InputStreamReader er = new InputStreamReader(err);
	        BufferedReader buff = new BufferedReader(in);
	        String line = buff.readLine();
	        
	        while (line != null){
	        	
	            line =buff.readLine();
	            if (!line.trim().toString().equals("Servers:")) {
	            	JsonObject tmpJson = new JsonObject();
	            	tmpJson.addProperty("servername", line.trim().toString().replace("\\", "\\"));
	            	json.add(tmpJson);
	            }
	        }
	        buff.close();	        		        
	    } 
	    catch(Exception e) 
	    	{ 
	    		e.printStackTrace(); 
	    	}
		return json.toString();
	    }
		
	@POST    /*metoda vraca uspjesnost konekcije na server!*/
    @Path("connectionSqlAut")
    @Produces(MediaType.APPLICATION_JSON) 
	@Consumes(MediaType.APPLICATION_JSON)
	public String checkConnectionSql(String body) throws SQLException
	{
		JsonElement jelement = new JsonParser().parse(body);
	    JsonObject  jobject = jelement.getAsJsonObject();
		String username = jobject.get("username").getAsString();
		String password = jobject.get("password").getAsString();
		String servername = jobject.get("servername").getAsString();
		String servertype = jobject.get("servertype").getAsString();  /*0-MSSQL, 1-MySQL*/
		String auttype = jobject.get("auttype").getAsString();   /*0-SQL, 1-WIN*/
		System.out.println("PUBLIC: ");
		System.out.println("PUBLIC jobject: " + jobject.toString());
		//String publicip = jobject.get("publicip").getAsString();
		publicip = request.getRemoteHost();
		//String addr = InetAddresses.getCoercedIPv4Address(InetAddresses.forString(request.getRemoteHost()));
		//String addr = InetAddress
		
		System.out.println("PUBLIC IP: " + publicip);
		
		createConnection(servertype, auttype, servername, username, password, publicip); 
	    
		JsonArray json = new JsonArray();
		if (servertype.equals("0"))
		{
			Statement stmt = conn.createStatement();
			ResultSet rs;
			rs = stmt.executeQuery("SELECT name FROM master.dbo.sysdatabases");
			while ( rs.next() ) {
				JsonObject tmpJson = new JsonObject();
				databaseName = rs.getString("name");
				tmpJson.addProperty("databasename", databaseName);
            	json.add(tmpJson);
			}
		}
		else if (servertype.equals("1"))
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SHOW DATABASES;");
			while ( rs.next() ) {
				JsonObject tmpJson = new JsonObject();
				String databaseName = rs.getString("Database");
				System.out.println(databaseName.toString());
				tmpJson.addProperty("databasename", databaseName);
            	json.add(tmpJson);
			}
		}
		
		return json.toString();
	}
	
	@POST
	@Path("generateAll")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Consumes(MediaType.APPLICATION_JSON)
	public String generateALL(String body) 
	{
		JsonElement jelement = new JsonParser().parse(body);
	    JsonObject  jobject = jelement.getAsJsonObject();
	    String servername = jobject.get("servername").getAsString();
	    String username = jobject.get("username").getAsString();
		String password = jobject.get("password").getAsString();
		databaseName = jobject.get("databasename").getAsString();
		String filename = jobject.get("filename").getAsString();
		String servertype = jobject.get("servertype").getAsString(); 
		String auttype = jobject.get("auttype").getAsString();  
		
		publicip = request.getRemoteHost();
		
		
		createConnection(servertype, auttype, servername, username, password, publicip);
		
		try
		{
			if(conn == null) { System.out.println("fail"); }
			else if(conn.isClosed()) { System.out.println("closedfail"); }
			else { System.out.println("super"); }
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElementNS("http://www.eclipse.org/uml2/3.0.0/UML", "uml:Model");
			
			Attr attr = doc.createAttribute("xmlns:uml");	    
			attr.setValue("http://www.eclipse.org/uml2/3.0.0/UML");
			rootElement.setAttributeNodeNS(attr);
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			rootElement.setAttributeNode(attr);

			attr = doc.createAttributeNS(XMITYPE, "xmi:version");
			attr.setValue("2.1");
			rootElement.setAttributeNode(attr);		
			doc.appendChild(rootElement);
			
			createDataTypePackage(rootElement, doc, attr, databaseName, servertype, conn);
			createClassPackages(rootElement, doc, attr, databaseName, servertype);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			String location = "";
						
			//String path = System.getProperty("user.home") + File.separator + "Downloads";		
			
			String path = request.getServletContext().getRealPath("./repo");
			File customDir = new File(path);

            String basePath = customDir.getCanonicalPath() + File.separator + filename;
            File finalFile = new File(basePath);
			StreamResult result = new StreamResult(finalFile);
			
			transformer.transform(source, result);
			
			ResponseBuilder response = Response.ok((Object) finalFile);
	        
			callWebServis(basePath);
			String resultfinal = new String( byteArray, java.nio.charset.Charset.forName("UTF-8") );
			
			System.out.print(payload);
			//finalFile.delete();
			return payload;			
			
		}
		catch(Exception e) 
		{
			e.printStackTrace();
			return null;
		}		
	}
	
		
	public String callWebServis(String basePath) 
	{
		try {	
			
		 URL url = new URL("http://m-lab.etf.unibl.org:8080/amadeos_umlcdlayouter/services/layout/json");
		 HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		 final String boundary = Strings.repeat("-", 15) + Long.toHexString(System.currentTimeMillis());
         
		 conn.setDoOutput(true);
	     conn.setDoInput(true);
	     conn.setUseCaches(false);
	     conn.setRequestMethod("POST");
	     conn.setRequestProperty("Connection", "Keep-Alive");
	     conn.setRequestProperty("Cache-Control", "no-cache");
         conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
         
         File file = new File(basePath);         
         InputStream targetStream = new FileInputStream(file);
         
         
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         final int BUF_SIZE = 1024;
         byte[] buffer = new byte[BUF_SIZE];
         int bytesRead = -1;
         while ((bytesRead = targetStream.read(buffer)) > -1) {
             out.write(buffer, 0, bytesRead);
         }
         targetStream.close();
         byteArray = out.toByteArray();
         
         OutputStream os = conn.getOutputStream();
         PrintWriter body = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);  
         body.append("\r\n");
         body.append("--").append(boundary).append("\r\n");
         body.append("Content-Disposition: form-data; name=\"" + "input" + "\"; filename=\"" + basePath + "\"").append("\r\n");
         body.append("Content-Type: application/octed-stream").append("\r\n");
         body.append("Content-Transfer-Encoding: binary").append("\r\n");
         body.append("\r\n");
         body.flush();
         
         os.write(byteArray);
         os.flush();
         
         body.append("\r\n");
         body.flush();
         
         body.append("--").append(boundary).append("--").append("\r\n");
         body.flush();
         
         int responseCode = conn.getResponseCode();
         String responseMessage = conn.getResponseMessage();         
         payload = CharStreams.toString(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        
         conn.disconnect();         
         return payload;
		}
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	@POST    /*metoda vraca kreiran fajl!*/
    @Path("generateFile")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response generateFile(String body) 
	{
		String finalresult = "";
		JsonElement jelement = new JsonParser().parse(body);
	    JsonObject  jobject = jelement.getAsJsonObject();
	    String servername = jobject.get("servername").getAsString();
	    String username = jobject.get("username").getAsString();
		String password = jobject.get("password").getAsString();
		String databasename = jobject.get("databasename").getAsString();
		String filename = jobject.get("filename").getAsString();
		String servertype = jobject.get("servertype").getAsString();  /*0-MSSQL, 1-MySQL*/
		String auttype = jobject.get("auttype").getAsString();   /*0-SQL, 1-WIN*/
		
		publicip = request.getRemoteHost();
		createConnection(servertype, auttype, servername, username, password, publicip);
		/*for(Map.Entry entry: hmapConnections.entrySet()){
			if("1".equals(entry.getKey())) {
				conn = (Connection)entry.getValue();
			}
		}*/
		
		
		
		try {			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElementNS("http://www.eclipse.org/uml2/3.0.0/UML", "uml:Model");
			
			Attr attr = doc.createAttribute("xmlns:uml");	    
			attr.setValue("http://www.eclipse.org/uml2/3.0.0/UML");
			rootElement.setAttributeNodeNS(attr);
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			rootElement.setAttributeNode(attr);

			attr = doc.createAttributeNS(XMITYPE, "xmi:version");
			attr.setValue("2.1");
			rootElement.setAttributeNode(attr);		
			doc.appendChild(rootElement);
			
			/*datatype package*/
			createDataTypePackage(rootElement, doc, attr, databasename, servertype, conn);
			/*classes packages*/
			createClassPackages(rootElement, doc, attr, databasename, servertype);
			/*viewsPackages*/
			generateViews(rootElement, doc, attr, databasename, servertype);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			String location = "";
						
			String path = System.getProperty("user.home") + File.separator + "Downloads";
			
            File customDir = new File(path);
            if (!customDir.exists()) {
                customDir.mkdir();
            }
            String basePath = customDir.getCanonicalPath() + File.separator + filename;
			StreamResult result = new StreamResult(new File(basePath));			
			transformer.transform(source, result);
			
		    File fileDownload = new File(basePath);
		    byte[] fileContent = Files.readAllBytes(fileDownload.toPath());
		    ResponseBuilder response = Response.ok((Object) fileDownload);
		    response.header("Content-Disposition", "attachment;filename=\\" + filename + "\\");
			    
		    return response.build();			
			
		}
		catch(Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}					
	}
	
	private void createClassPackages(Element packageElement, Document doc, Attr attr, String databasename, String servertype) throws SQLException, TransformerConfigurationException
	{
		Element packageClasses = doc.createElement("packagedElement");
		attr = doc.createAttributeNS(XMITYPE, "xmi:type");
		attr.setValue("uml:Package");
		packageClasses.setAttributeNode(attr);

		packageClasses.setAttribute("name", databaseName);

		attr = doc.createAttributeNS(XMITYPE, "xmi:id");
		attr.setValue(UUID.randomUUID().toString());
		packageClasses.setAttributeNode(attr);
		packageElement.appendChild(packageClasses);
		
		/*package import datatypes*/
		Element peImport = doc.createElement("packageImport");
		attr = doc.createAttributeNS(XMITYPE, "xmi:type");
		attr.setValue("uml:PackageImport");
		peImport.setAttributeNode(attr);
		
		attr = doc.createAttributeNS(XMITYPE, "xmi:id");
		attr.setValue("_packageImport.0");
		peImport.setAttributeNode(attr);
		
		attr = doc.createAttribute("importedPackage");
		attr.setValue(dataTypePackageId);
		peImport.setAttributeNode(attr);
		
		packageClasses.appendChild(peImport);
		
		/*Classes*/
		Statement stmtClass = conn.createStatement();
		ResultSet rsClass;
		String sqlQueryTable = "";
		if(servertype.equals("0")) {
			sqlQueryTable += "use [" + databasename + "] select DISTINCT TABLE_NAME from information_schema.TABLES where TABLE_TYPE = 'BASE TABLE' and table_name != 'sysdiagrams' ";
		} else {
			sqlQueryTable += "select DISTINCT TABLE_NAME from information_schema.TABLES where TABLE_TYPE = 'BASE TABLE' and table_schema = '"+ databasename + "';";
		}
		rsClass = stmtClass.executeQuery(sqlQueryTable);
		while (rsClass.next() ) {
			String className = rsClass.getString("TABLE_NAME");
			String classKey = UUID.randomUUID().toString();
			hmapClassName.put(classKey, className);
		}
		Iterator itClass = hmapClassName.entrySet().iterator();
		while(itClass.hasNext()) {  
			HashMap.Entry pairClass = (HashMap.Entry)itClass.next();

			Element peClass = doc.createElement("packagedElement");
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");
			attr.setValue("uml:Class");
			peClass.setAttributeNode(attr);
			
			peClass.setAttribute("type", "Table");

			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(pairClass.getKey().toString());
			peClass.setAttributeNode(attr);
			peClass.setAttribute("name", pairClass.getValue().toString());
			
			String pairClassName = "";
			pairClassName = pairClass.getValue().toString();
			
			/*tableColumnsNames*/
			getAllTableColumns(packageElement, doc, attr, databasename, servertype, pairClassName, peClass);
			/*primaryKeys*/
			getAllPrimaryKeys(packageElement, doc, attr, databasename, servertype, pairClassName, peClass);
			/*foreignKeys*/
			getAllForeignKeys(packageElement, doc, attr, databasename, servertype, pairClassName, peClass);
			/*indexes*/
			getAllIndexes(packageElement, doc, attr, databasename, servertype, pairClassName, peClass);
			/*relations*/
			getAllRelations(packageClasses, doc, attr, databasename, servertype, pairClassName, peClass);
			
			String dependencyAttribute = "";
			for(Map.Entry entry: hmapParentTablesDependies.entrySet()){
				if(pairClass.getValue().equals(entry.getValue())) {
					dependencyAttribute = dependencyAttribute + entry.getKey() + " ";
				}
			}
			if (dependencyAttribute.trim()!="") {
				peClass.setAttribute("clientdependency", dependencyAttribute.trim());
			}
			packageClasses.appendChild(peClass);
			
		} 		
		generateViews(packageClasses, doc, attr, databaseName, servertype);		
	}
	
	public void generateViews(Element packageElement, Document doc, Attr attr, String databasename, String servertype) 
	{
		try
		{
			Statement stmtView = conn.createStatement();
			ResultSet rsView;
			if (servertype.equals("0")) {
				rsView = stmtView.executeQuery("use [" + databasename + "] select distinct name from sys.views ");
			} else {
				rsView = stmtView.executeQuery("select distinct table_name as name from information_schema.VIEWS where TABLE_SCHEMA = '" + databasename + "' ");	
			}
			while ( rsView.next() ) {
				String viewName = rsView.getString("name");
				String viewKey = UUID.randomUUID().toString();
				hmapViewName.put(viewKey, viewName);
			}
			Iterator itView = hmapViewName.entrySet().iterator();
			while(itView.hasNext()) {
				HashMap.Entry pairView = (HashMap.Entry)itView.next();

				Element peView = doc.createElement("packagedElement");
				attr = doc.createAttributeNS(XMITYPE, "xmi:type");
				attr.setValue("uml:Class");
				peView.setAttributeNode(attr);
				
				peView.setAttribute("type", "View");

				attr = doc.createAttributeNS(XMITYPE, "xmi:id");
				attr.setValue(pairView.getKey().toString());
				peView.setAttributeNode(attr);

				peView.setAttribute("name", pairView.getValue().toString());
				//get all columns names
				Statement stmtColumns = conn.createStatement();
				ResultSet rsColumns;
				String sqlQuery = "";
				if (servertype.equals("0")) {
					sqlQuery += "use [" + databasename + "] SELECT c.COLUMN_NAME, c.DATA_TYPE FROM sys.views vv left join sys.objects oo on vv.object_id = oo.object_id left join sys.columns cc on oo.object_id = cc.object_id left join INFORMATION_SCHEMA.COLUMNS c on c.COLUMN_NAME = cc.name and c.TABLE_NAME = vv.name where vv.name = '" + pairView.getValue().toString() + "'";
				} else {
					sqlQuery += "SELECT column_name, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + databasename + "' AND TABLE_NAME = '" + pairView.getValue().toString() + "';";	
				}
				rsColumns = stmtColumns.executeQuery(sqlQuery);
				Element oaAttribute = null;
				while ( rsColumns.next() ) {
					String columnName = rsColumns.getString("COLUMN_NAME");
					String columnKey = UUID.randomUUID().toString();
					columnDataType = rsColumns.getString("DATA_TYPE");            

					hmapColumnsView.put(columnKey, columnName);
					hmapColumnsDataTypesView.put(columnKey, columnDataType);

					oaAttribute = doc.createElement("ownedAttribute"); 
					attr = doc.createAttribute("type");
					String key = "";
					for(Map.Entry entry: hmapDataTypes.entrySet()){
						if(columnDataType.equals(entry.getValue())) {
							key = entry.getKey().toString();
						}
					}
					attr.setValue(key);
					oaAttribute.setAttributeNode(attr);

					attr = doc.createAttributeNS(XMITYPE, "xmi:id");
					attr.setValue(UUID.randomUUID().toString());
					oaAttribute.setAttributeNode(attr);
					
					attr = doc.createAttributeNS(XMITYPE, "xmi:type");
					attr.setValue("uml:Property");
					oaAttribute.setAttributeNode(attr);
					
					oaAttribute.setAttribute("name", columnName.toString());        		

					peView.appendChild(oaAttribute);
				}
				Statement stmtRelations = conn.createStatement();
				ResultSet rsRelations;
				String sqlRelations = "";
				String parentTable = "";
				String associationId = "";

				String clientdependency = "";
				if (servertype.equals("0")) {
					sqlRelations += "use [" + databasename + "] select distinct tbl.name from sys.views as vws  ";
					sqlRelations += "join sys.sysdepends as dep on dep.id = vws.object_id join sys.tables as tbl on dep.depid = tbl.object_id ";
					sqlRelations += "join sys.columns as col on dep.depid = col.object_id and dep.depNumber = col.column_ID  ";
					sqlRelations += "where vws.name = '" + pairView.getValue().toString() + "'";
					rsRelations = stmtRelations.executeQuery(sqlRelations);
					while(rsRelations.next()) {    
						String veznaTabela = rsRelations.getString("name");
						for(Map.Entry entry: hmapClassName.entrySet()){
							if(rsRelations.getString("name").equals(entry.getValue())) {
								clientdependency = clientdependency + entry.getKey() + " ";
							}
						}
						if (clientdependency.trim()!="") {
							peView.setAttribute("clientdependency", clientdependency.trim());
						}
						packageElement.appendChild(peView);
						
						//dependencies
						Element packageDependency = doc.createElement("packagedElement");
						attr = doc.createAttributeNS(XMITYPE, "xmi:type");
						attr.setValue("uml:Dependency");
						packageDependency.setAttributeNode(attr);

						packageDependency.setAttribute("name", pairView.getValue().toString()+veznaTabela);

						attr = doc.createAttributeNS(XMITYPE, "xmi:id");
						attr.setValue(UUID.randomUUID().toString());
						packageDependency.setAttributeNode(attr);

						attr = doc.createAttribute("supplier");
						String supplierName = "";
						for(Map.Entry entry: hmapClassName.entrySet()){
							if(veznaTabela.equals(entry.getValue())) {
								supplierName = entry.getKey().toString();
							}
						}
						attr.setValue(supplierName);
						packageDependency.setAttributeNode(attr);

						attr = doc.createAttribute("client");
						attr.setValue(pairView.getKey().toString());
						packageDependency.setAttributeNode(attr);

						packageElement.appendChild(packageDependency);
					}
				} else {    //mysql
					String sqlParseView = "";
					sqlParseView += "SELECT replace(replace(replace(SUBSTRING(view_definition, locate('from', replace(view_definition, 'FROM', 'from')) ";
					sqlParseView += ", LENgth(view_definition)-instr('from', replace(view_definition, 'FROM', 'from'))), '(', ' '), ')', ' '), char(10), ' ') AS OutputString ";
					sqlParseView += "from information_schema.views where table_schema = '" + databasename + "' ";
					sqlParseView += "and table_name = '" + pairView.getValue().toString() + "' ";
					
					System.out.println("VIEW " + sqlParseView);
					ResultSet rsParseView = stmtRelations.executeQuery(sqlParseView);
					while(rsParseView.next()) {
						String outputViewDef = rsParseView.getString("OutputString");
						String[] niz = outputViewDef.trim().replaceAll("\\s{2,}", " ").replaceAll("`", "").split(" ");
						Array tableArray = null;
						for(int i = 0; i < niz.length; i++) {
							int count = niz[i].length() - niz[i].replace(".","").length();
							if (niz[i].startsWith(databasename+".") && count == 1) {
								String alias = "";
								if (niz[i+1] != null) {
								if (niz[i+1].toLowerCase().equals("as")) { alias = niz[i+2]; }
								else if (!niz[i+1].toLowerCase().equals("join") && !niz[i+1].toLowerCase().equals("left") && !niz[i+1].toLowerCase().equals("right") && !niz[i+1].toLowerCase().equals("on") && !niz[i+1].toLowerCase().equals("cross") && !niz[i+1].toLowerCase().equals("outer") && !niz[i+1].toLowerCase().equals("=")) 
								{ alias = niz[i+1]; } else { alias = niz[i]; }
								
								hmapMySqlTablesAlias.put(niz[i].replace(databasename+".", ""), alias);								
								}
							}
						}
						
						for(Map.Entry entry: hmapMySqlTablesAlias.entrySet()){
							String veznaTabela = entry.getKey().toString();
							for(Map.Entry entryClass: hmapClassName.entrySet()){
								if(entry.getKey().equals(entryClass.getValue())) {
									clientdependency = clientdependency + entryClass.getKey() + " ";
								}
							}
							if (clientdependency.trim()!="") {
								peView.setAttribute("clientdependency", clientdependency.trim());
							}
							packageElement.appendChild(peView);

							//dependencies
							Element packageDependency = doc.createElement("packagedElement");
							attr = doc.createAttributeNS(XMITYPE, "xmi:type");
							attr.setValue("uml:Dependency");
							packageDependency.setAttributeNode(attr);

							packageDependency.setAttribute("name", pairView.getValue().toString()+veznaTabela);

							attr = doc.createAttributeNS(XMITYPE, "xmi:id");
							attr.setValue(UUID.randomUUID().toString());
							packageDependency.setAttributeNode(attr);

							attr = doc.createAttribute("supplier");
							String supplierName = "";
							for(Map.Entry entryClass: hmapClassName.entrySet()){
								if(veznaTabela.equals(entryClass.getValue())) {
									supplierName = entryClass.getKey().toString();
								}
							}
							attr.setValue(supplierName);
							packageDependency.setAttributeNode(attr);

							attr = doc.createAttribute("client");
							attr.setValue(pairView.getKey().toString());
							packageDependency.setAttributeNode(attr);

							packageElement.appendChild(packageDependency);
						}
					}}
				
				//kreiranje operacija u view
				Statement stmtViewOperations = conn.createStatement();
				ResultSet rsViewOperations;
				String sqlViewOperations = "";
				if (servertype.equals("0")) {
					sqlViewOperations += " IF OBJECT_ID('tempdb..#temp') IS NOT NULL DROP TABLE #temp  IF OBJECT_ID('tempdb..#temp2') IS NOT NULL DROP TABLE #temp2 ";
					sqlViewOperations += " select  vt.VIEW_SCHEMA+'.'+vt.TABLE_NAME as ViewTableName, case when ltrim((select SUBSTRING(ltrim(rtrim(value)) ";
					sqlViewOperations += " , patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) from sys.objects o ";
					sqlViewOperations += " join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '"+ pairView.getValue().toString() +"') and o.type = 'V'), ',') )) like 'on%' ";
					sqlViewOperations += " OR ltrim((select SUBSTRING(ltrim(rtrim(value)) , patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '" + pairView.getValue().toString() + "') ";
					sqlViewOperations += " and o.type= 'V'), ',') )) like 'where%' then NULL ";
					sqlViewOperations += " WHEN ltrim((select SUBSTRING(ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '" + pairView.getValue().toString() + "') ";
					sqlViewOperations += " and o.type = 'V'), ',') )) like 'as%' ";
					sqlViewOperations += " THEN ltrim(stuff(ltrim((select SUBSTRING(ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '" + pairView.getValue().toString() + "') ";
					sqlViewOperations += " and o.type = 'V'), ',') )), charindex('as', ltrim((select SUBSTRING(ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '" + pairView.getValue().toString() +"') and o.type = 'V'), ',') ))), len('as'), '')) ";
					sqlViewOperations += " ELSE (select SUBSTRING(ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME) ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(value)), patindex(('%' +vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME + '%'), value)+LEN(vt.TABLE_SCHEMA+'.'+vt.TABLE_NAME)+1)) ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('FROM', definition), LEN(definition)-CHARINDEX('FROM', definition)) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '" + pairView.getValue().toString() + "') and o.type = 'V'), ',') ) end as OutputView ";
					sqlViewOperations += " INTO #temp from INFORMATION_SCHEMA.VIEW_TABLE_USAGE vt ";
					sqlViewOperations += " select ViewTableName, isnull(case when SUBSTRING(LTRIM(OutputView) , 0, PATINDEX('% %', ltrim(outputview))) like '%' +CHAR(10)+'%' ";
					sqlViewOperations += " then replace(replace(replace(ltrim(rtrim(SUBSTRING(SUBSTRING(LTRIM(OutputView) , 0, PATINDEX('%'+CHAR(10)+'%', ltrim(outputview))), 0, PATINDEX('%' + CHAR('10') + '%', ltrim(outputview))))) ";
					sqlViewOperations += " , CHAR(10), ''),CHAR(13),''),CHAR(9),'') else ltrim(rtrim(SUBSTRING(LTRIM(OutputView) , 0, PATINDEX('% %', ltrim(outputview))))) end, ViewTableName) as Alias ";
					sqlViewOperations += " into #temp2 from #temp t ";
					sqlViewOperations += " select  reverse(left(reverse(value), charindex(' ', reverse(value)) -1)) as AliasKolona ";
					sqlViewOperations += " , REPLACE(SUBSTRING(LTRIM(rtrim(value)), 1, charindex(reverse(left(reverse(value), charindex(' ', reverse(value)) -1)), LTRIM(rtrim(value)), 1)-1), ' as ', '') as Izraz ";
					sqlViewOperations += " , case when REPLACE(SUBSTRING(LTRIM(rtrim(value)), 1, charindex(reverse(left(reverse(value), charindex(' ', reverse(value)) -1)), LTRIM(rtrim(value)), 1)-1), ' as ', '') like '%' + tt.Alias + '.%' ";
					sqlViewOperations += " THEN REPLACE(REPLACE(SUBSTRING(LTRIM(rtrim(value)), 1, charindex(reverse(left(reverse(value), charindex(' ', reverse(value)) -1)), LTRIM(rtrim(value)), 1)-1), ' as ', '')  ";
					sqlViewOperations += " , tt.Alias+'.', (select t.ViewTableName from #temp2 t where t.Alias=tt.Alias)+'.') ELSE REPLACE(SUBSTRING(LTRIM(rtrim(value)), 1, charindex(reverse(left(reverse(value) ";
					sqlViewOperations += " , charindex(' ', reverse(value)) -1)), LTRIM(rtrim(value)), 1)-1), ' as ', '') end as Operacija, tt.ViewTableName, tt.Alias ";
					sqlViewOperations += " from STRING_SPLIT((SELECT SUBSTRING(definition, CHARINDEX('select', definition)+len('select'), CHARINDEX('from',definition) - CHARINDEX('select', definition) - Len('select')) ";
					sqlViewOperations += " from sys.objects o join sys.sql_modules m on m.object_id = o.object_id where o.object_id = object_id( '"+ pairView.getValue().toString() + "') and o.type= 'V'), ',') ";
					sqlViewOperations += " left join #temp2 tt on SUBSTRING(ltrim(rtrim(REPLACE(REPLACE(REPLACE(ltrim(rtrim((value))), CHAR(13),''), CHAR(10),''), CHAR(9), ''))), 1 ";
					sqlViewOperations += " , CHARINDEX(' ', ltrim(rtrim(REPLACE(REPLACE(REPLACE(ltrim(rtrim((value))), CHAR(13),''), CHAR(10),''), CHAR(9), ''))))) like '%' + ltrim(rtrim(tt.Alias)) + '.%'  ";
					sqlViewOperations += " where CHARINDEX(' ', ltrim(rtrim(REPLACE(REPLACE(REPLACE(ltrim(rtrim((value))), CHAR(13),''), CHAR(10),''), CHAR(9), '')))) > 0 ";
					rsViewOperations = stmtViewOperations.executeQuery(sqlViewOperations);
					while(rsViewOperations.next()) {
						String aliasKolona = rsViewOperations.getString("AliasKolona");
						String izrazView = rsViewOperations.getString("Izraz");
						String operacija = rsViewOperations.getString("Operacija");
						String viewTableName = rsViewOperations.getString("ViewTableName");
						String alias = rsViewOperations.getString("Alias");
						if (!hmapAliasView.containsKey(alias) && alias != null) {
							hmapAliasView.put(alias, viewTableName);
						}

						Element oOperationView = doc.createElement("ownedOperation");
						attr = doc.createAttributeNS(XMITYPE, "xmi:id");
						attr.setValue(UUID.randomUUID().toString());
						oOperationView.setAttributeNode(attr);									

						oOperationView.setAttribute("name", aliasKolona.replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", ""));
						
						attr = doc.createAttributeNS(XMITYPE, "xmi:type");
						attr.setValue("uml:Operation");
						oOperationView.setAttributeNode(attr);

						Element oParameter = doc.createElement("ownedParameter");
						attr = doc.createAttributeNS(XMITYPE, "xmi:id");
						attr.setValue(UUID.randomUUID().toString());
						oParameter.setAttributeNode(attr);

						String operacijaFinal = operacija;
						for(java.util.Map.Entry<String, String> entry : hmapAliasView.entrySet()) {
							if(operacija.contains(entry.getKey().toString()+'.')) {
								operacijaFinal = operacijaFinal.replace(entry.getKey().toString()+'.', entry.getValue().toString()+'.');
							}
						}

						oParameter.setAttribute("name", operacijaFinal);
						
						/*set data type of view column*/
						attr = doc.createAttribute("type");
						String keyColumn = "", keyDataTypeColumn = "", key = ""; 
						String nameViewColumn = aliasKolona.replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "");
						for(Map.Entry entry: hmapColumnsView.entrySet()){
							if(nameViewColumn.equals(entry.getValue())) {
								keyColumn = entry.getKey().toString();
							}
						}

						for(Map.Entry entry: hmapColumnsDataTypesView.entrySet()){
							if(keyColumn.equals(entry.getKey())) {
								keyDataTypeColumn = entry.getValue().toString();
							}
						}

						for(Map.Entry entry: hmapDataTypes.entrySet()){
							if(keyDataTypeColumn.equals(entry.getValue())) {
								key = entry.getKey().toString();
							}
						}

						attr.setValue(key);            		
						oParameter.setAttributeNode(attr);   
						
						oOperationView.appendChild(oParameter);

						peView.appendChild(oOperationView);
					}
					
			} else if (servertype.equals("1")) 
			{
				String sqlParseColumns = "";
				sqlParseColumns += "SELECT substring_index(substring_index(view_definition, 'select', -1), 'from', 1) as OutParse ";
				sqlParseColumns += "from information_schema.views where table_schema = '" + databasename + "' ";
				sqlParseColumns += "and table_name = '" + pairView.getValue().toString() + "';";
				rsViewOperations = stmtViewOperations.executeQuery(sqlParseColumns);
				while(rsViewOperations.next()) 
				{
					String outputViewDef = rsViewOperations.getString("OutParse");
					String[] niz = outputViewDef.trim().replaceAll("\\s{2,}", " ").replaceAll("`", "").split(",");
					Array tableArray = null;
					for(int i = 0; i < niz.length; i++) {                    		
						String fullColumnName = niz[i].substring(0, niz[i].indexOf(" AS")).trim();
						String fullAliasName = niz[i].substring(niz[i].indexOf(" AS")+3, niz[i].length());
						String columnNameShort = fullColumnName.substring(fullColumnName.indexOf(".")+1, fullColumnName.length()).trim();
						
						if(!columnNameShort.toUpperCase().equals(fullAliasName.toUpperCase().trim()))
						{
							hmapMySqlAliasColumns.put(fullColumnName.trim(), fullAliasName.trim());
							
							Element oOperationView = doc.createElement("ownedOperation");
							attr = doc.createAttributeNS(XMITYPE, "xmi:id");
							attr.setValue(UUID.randomUUID().toString());
							oOperationView.setAttributeNode(attr);
							
							attr = doc.createAttributeNS(XMITYPE, "xmi:type");
							attr.setValue("uml:Operation");
							oOperationView.setAttributeNode(attr);

							oOperationView.setAttribute("name", fullAliasName.trim().replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", ""));

							Element oParameter = doc.createElement("ownedParameter");
							attr = doc.createAttributeNS(XMITYPE, "xmi:id");
							attr.setValue(UUID.randomUUID().toString());
							oParameter.setAttributeNode(attr);
										
							/*set data type of view column*/
							attr = doc.createAttribute("type");
							String keyColumn = "", keyDataTypeColumn = "", key = ""; 
							String nameViewColumn = fullAliasName.replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "");
							for(Map.Entry entry: hmapColumnsView.entrySet()){
								if(nameViewColumn.trim().equals(entry.getValue().toString().trim())) {
									keyColumn = entry.getKey().toString();
								}
							}

							for(Map.Entry entry: hmapColumnsDataTypesView.entrySet()){
								if(keyColumn.equals(entry.getKey())) {
									keyDataTypeColumn = entry.getValue().toString();
								}
							}

							for(Map.Entry entry: hmapDataTypes.entrySet()){
								if(keyDataTypeColumn.equals(entry.getValue())) {
									key = entry.getKey().toString();
								}
							}

							attr.setValue(key);            		
							oParameter.setAttributeNode(attr);   
							
							String operacijaFinal = "";
							operacijaFinal = columnNameShort;
							for(java.util.Map.Entry<String, String> entrys : hmapMySqlTablesAlias.entrySet()) {
								if (entrys.getValue() != null) {
									if(fullColumnName.toString().contains(entrys.getValue().toString()+'.')) {
										operacijaFinal = fullColumnName.replace(entrys.getValue().toString()+'.', entrys.getKey().toString()+'.');
									}
								}
							}
							oParameter.setAttribute("name", operacijaFinal);
							oOperationView.appendChild(oParameter);

							peView.appendChild(oOperationView);
						}
					}
				}

			}
			packageElement.appendChild(peView);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void getAllRelations(Element packageElement, Document doc, Attr attr, String databasename, String servertype, String pairClassName, Element peClass) throws SQLException
	{
		Statement stmtRelations = conn.createStatement();
		ResultSet rsRelations;
		String sqlRelations = "";
		if(servertype.equals("0")) {
			sqlRelations += "use [" + databasename + "] SELECT fk.name as FKName,tp.name as Parenttable, cp.name, cp.column_id, cp.object_id,  ";
			sqlRelations += "tr.name 'Refrencedtable',cr.name,fk.update_referential_action_desc [UpdateAction], fk.delete_referential_action_desc [DeleteAction], isnull(ic.index_id,999) as index_id ";
			sqlRelations += "FROM sys.foreign_keys fk INNER JOIN sys.tables tp ON fk.parent_object_id = tp.object_id ";
			sqlRelations += "INNER JOIN sys.tables tr ON fk.referenced_object_id = tr.object_id INNER JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id ";
			sqlRelations += "INNER JOIN sys.columns cp ON fkc.parent_column_id = cp.column_id AND fkc.parent_object_id = cp.object_id ";
			sqlRelations += "INNER JOIN sys.columns cr ON fkc.referenced_column_id = cr.column_id AND fkc.referenced_object_id = cr.object_id ";
			sqlRelations += "left JOIN sys.index_columns AS ic ON  cp.OBJECT_ID = ic.OBJECT_ID and cp.column_id = ic.column_id ";
			sqlRelations += "where tp.name = '" + pairClassName + "'";
		} else {
			sqlRelations += "SELECT tc.constraint_name as FKName,tc.table_name as Parenttable,kcu.column_name as name,1 as column_id,1 as object_id ";
			sqlRelations += ",kcu.referenced_table_name as Refrencedtable,kcu.referenced_column_name as name,rc.update_rule as UpdateAction,rc.delete_rule as DeleteAction, 1 as index_id ";
			sqlRelations += "FROM information_schema.table_constraints tc inner JOIN information_schema.key_column_usage kcu ";
			sqlRelations += "ON tc.constraint_catalog = kcu.constraint_catalog AND tc.constraint_schema = kcu.constraint_schema ";
			sqlRelations += "AND tc.constraint_name = kcu.constraint_name AND tc.table_name = kcu.table_name ";
			sqlRelations += "LEFT JOIN information_schema.referential_constraints rc ON tc.constraint_catalog = rc.constraint_catalog ";
			sqlRelations += "AND tc.constraint_schema = rc.constraint_schema AND tc.constraint_name = rc.constraint_name ";
			sqlRelations += " AND tc.table_name = rc.table_name where tc.constraint_schema = '" + databasename + "' and tc.constraint_type = 'FOREIGN KEY'";
			sqlRelations += " and tc.table_name = '" + pairClassName + "'";
		}
		rsRelations = stmtRelations.executeQuery(sqlRelations);
		String parentTable = "";
		String associationId = "";
		
		while(rsRelations.next())
		{
			parentTable = rsRelations.getString("Parenttable");
			associationId = UUID.randomUUID().toString();
			hmapParentTablesDependies.put(associationId, parentTable);

			String associationName = rsRelations.getString("FKName");                	
			String memberStart = UUID.randomUUID().toString();
			String memberEnd = UUID.randomUUID().toString();

			String referencedTable = rsRelations.getString("Refrencedtable");
			int index = rsRelations.getInt("index_id");

			Element packageAssociation = doc.createElement("packagedElement");
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");
			attr.setValue("uml:Dependency");
			packageAssociation.setAttributeNode(attr);

			packageAssociation.setAttribute("name", associationName);

			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(associationId);
			packageAssociation.setAttributeNode(attr);
			
			attr = doc.createAttribute("supplier");
			String supplierName = "";
			for(Map.Entry entry: hmapClassName.entrySet()){
				if(referencedTable.equals(entry.getValue())) {
					supplierName = entry.getKey().toString();
				}
			}
			attr.setValue(supplierName);
			packageAssociation.setAttributeNode(attr);

			attr = doc.createAttribute("client");
			String clientName = "";
			for(Map.Entry entry: hmapClassName.entrySet()){
				if(parentTable.equals(entry.getValue())) {
					clientName = entry.getKey().toString();
				}
			}
			attr.setValue(clientName);
			packageAssociation.setAttributeNode(attr);
			
			/*unijeti referencijalni integritet*/
			oaAttribute = doc.createElement("ownedRule");
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			String idOwnedRule =UUID.randomUUID().toString(); 
			attr.setValue(idOwnedRule);
			oaAttribute.setAttributeNode(attr);
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:type"); 
			attr.setValue("uml:Rule");
			oaAttribute.setAttributeNode(attr);

			oaAttribute.setAttribute("name", associationName);
			oaAttribute.setAttribute("constrainedElement", clientName);
			peClass.appendChild(oaAttribute);

			Element elementComent = doc.createElement("ownedComment");
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			elementComent.setAttributeNode(attr);

			elementComent.setAttribute("annotatedElement", idOwnedRule);
			oaAttribute.appendChild(elementComent);

			Element bodyComment = doc.createElement("body");
			String onUpdateAction = rsRelations.getString("UpdateAction");
			String onDeleteAction = rsRelations.getString("DeleteAction");
			bodyComment.setTextContent("ON  UPDATE " + onUpdateAction + ";ON DELETE " + onDeleteAction );
			elementComent.appendChild(bodyComment);

			packageElement.appendChild(packageAssociation);			
		}
	}
	
	private void getAllIndexes(Element packageElement, Document doc, Attr attr, String databasename, String servertype, String pairClassName, Element peClass) throws SQLException
	{
		Statement stmtIndex = conn.createStatement();
		ResultSet rsIndex;   
		String sqlIndex = "";
		if (servertype.equals("0")) {
			sqlIndex += "use [" + databasename + "] select distinct a.name as IndexName, case type_desc when 'CLUSTERED' then 'CLUSTER' else 'NOCLUSTER' end as type_desc from sys.indexes a where a.object_id = (select object_id from sys.objects where name = '" + pairClassName + "')";
		} else {
			sqlIndex += "SELECT DISTINCT INDEX_NAME as IndexName, case when INDEX_NAME='PRIMARY' THEN 'CLUSTER' ELSE 'NOCLUSTER' END AS type_desc FROM INFORMATION_SCHEMA.STATISTICS ";
			sqlIndex += " WHERE TABLE_SCHEMA = '" + databasename + "' and table_name = '" + pairClassName + "'";
		}
		rsIndex = stmtIndex.executeQuery(sqlIndex);
		ResultSetMetaData rsmdIndex = rsIndex.getMetaData();
		hmapIndexName.clear();
		String indexType = "";
		if (rsmdIndex.getColumnCount() > 0) {
			while(rsIndex.next()) {
				String indexName = rsIndex.getString("IndexName");
				String indexKey = UUID.randomUUID().toString();
				indexType = rsIndex.getString("type_desc");
				hmapIndexName.put(indexKey, indexName);
			}
			Element oOperationIndex = null;
			Iterator itIndex = hmapIndexName.entrySet().iterator();
			while(itIndex.hasNext()) {
				HashMap.Entry pairIndex = (HashMap.Entry)itIndex.next();   

				oOperationIndex = doc.createElement("ownedOperation");
				attr = doc.createAttributeNS(XMITYPE, "xmi:id");
				attr.setValue(UUID.randomUUID().toString());
				oOperationIndex.setAttributeNode(attr);
				
				attr = doc.createAttributeNS(XMITYPE, "xmi:type");
				attr.setValue("uml:Operation");
				oOperationIndex.setAttributeNode(attr);

				oOperationIndex.setAttribute("name", "INDEX_" + indexType + "_" + pairIndex.getValue());
				peClass.appendChild(oOperationIndex);

				//odredjivanje broja kolona koje ulaze u index
				Statement stIndexCol = conn.createStatement();
				ResultSet rsIndexColumn;
				String sqlIndexColumn = "";
				if (servertype.equals("0")) {
					sqlIndexColumn += "USE [" + databasename + "] select a.name as IndexName, c.name as ColumnIndexName, case b.is_descending_key when 1 then 'DESC' else 'ASC' end AS OrderType from sys.indexes a ";
					sqlIndexColumn += " left join sys.index_columns b on a.object_id = b.object_id and a.index_id = b.index_id ";
					sqlIndexColumn += " join sys.columns c on b.object_id = c.object_id and b.column_id = c.column_id ";
					sqlIndexColumn += " where a.object_id = (select object_id from sys.objects where name = '" + pairClassName + "') ";
					sqlIndexColumn += " and a.name = '" + pairIndex.getValue().toString() + "'";
				} else {
					sqlIndexColumn += "SELECT index_name as IndexName, COLUMN_NAME as ColumnIndexName, 'ASC' AS OrderType ";
					sqlIndexColumn += "FROM INFORMATION_SCHEMA.STATISTICS  WHERE TABLE_SCHEMA = '" + databasename + "' and table_name = '" + pairClassName + "' ";
					sqlIndexColumn += " AND index_name = '" + pairIndex.getValue().toString() + "'";
				}
				rsIndexColumn = stIndexCol.executeQuery(sqlIndexColumn);        	        	
				while(rsIndexColumn.next()) {
					String columnIndexName = rsIndexColumn.getString("ColumnIndexName");
					String dataOrderType = rsIndexColumn.getString("OrderType");

					Element oParameter = doc.createElement("ownedParameter");
					attr = doc.createAttributeNS(XMITYPE, "xmi:id");
					attr.setValue(UUID.randomUUID().toString());
					oParameter.setAttributeNode(attr);
					oParameter.setAttribute("name", columnIndexName);

					attr = doc.createAttribute("type");
					String keyColumn = "", keyDataTypeColumn = "", key = "";
					
					for(Map.Entry entry: hmapDataTypes.entrySet()){
						if(dataOrderType.equals(entry.getValue())) {
							key = entry.getKey().toString();
						}
					}

					attr.setValue(key);            		
					oParameter.setAttributeNode(attr);            		
					oOperationIndex.appendChild(oParameter);
				}
			}
		}     
		
	}
	
	private void getAllForeignKeys(Element packageElement, Document doc, Attr attr, String databasename, String servertype, String pairClassName, Element peClass) throws SQLException
	{
		Statement stmtFk = conn.createStatement();
		ResultSet rsFkey;
		String sqlFkey = "";
		if (servertype.equals("0")) {
			sqlFkey += "use [" + databasename + "] select t.name as TableWithForeignKey, c.name as ForeignKeyColumn,tl.name as ReferencedTable ";
			sqlFkey += "from sys.foreign_key_columns as fk inner join sys.tables as t on fk.parent_object_id = t.object_id ";
			sqlFkey += "inner join sys.columns as c on fk.parent_object_id = c.object_id and fk.parent_column_id = c.column_id ";
			sqlFkey += " inner join sys.tables as tl on fk.referenced_object_id = tl.object_id ";
			sqlFkey += " where t.name = '" + pairClassName + "'";
		} else {
			sqlFkey += "SELECT TABLE_NAME as TableWithForeignKey,COLUMN_NAME as ForeignKeyColumn,REFERENCED_TABLE_NAME as ReferencedTable ";
			sqlFkey += " FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE where table_name = '" + pairClassName + "' ";
			sqlFkey += " and REFERENCED_TABLE_NAME is not null; ";
		}
		rsFkey = stmtFk.executeQuery(sqlFkey);
		int counter = 0;
		Element oOperation = null;
		
		while(rsFkey.next()) {
			String fkColumnName = rsFkey.getString("ForeignKeyColumn");
			String fkColumnKey = UUID.randomUUID().toString();	
			String fkNameT = rsFkey.getString("ReferencedTable");

			oOperation = doc.createElement("ownedOperation");
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			oOperation.setAttributeNode(attr);
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");
			attr.setValue("uml:Operation");
			oOperation.setAttributeNode(attr);

			oOperation.setAttribute("name", "FK_" + fkNameT.toString());
			peClass.appendChild(oOperation);
			counter++;
			
			Element oParameter = doc.createElement("ownedParameter");
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			oParameter.setAttributeNode(attr);

			oParameter.setAttribute("name", fkColumnName);

			attr = doc.createAttribute("type");
			String keyColumn = "", keyDataTypeColumn = "", key = "";
			for(Map.Entry entry: hmapColumns.entrySet()){
				if(fkColumnName.equals(entry.getValue())) {
					keyColumn = entry.getKey().toString();
				}
			}

			for(Map.Entry entry: hmapColumnsDataTypes.entrySet()){
				if(keyColumn.equals(entry.getKey())) {
					keyDataTypeColumn = entry.getValue().toString();
				}
			}

			for(Map.Entry entry: hmapDataTypes.entrySet()){
				if(keyDataTypeColumn.equals(entry.getValue())) {
					key = entry.getKey().toString();
				}
			}

			attr.setValue(key);            		
			oParameter.setAttributeNode(attr);            		
			oOperation.appendChild(oParameter);            		
		}
	}	
	
	private void getAllPrimaryKeys(Element packageElement, Document doc, Attr attr, String databasename, String servertype, String pairClassName, Element peClass) throws SQLException
	{
		Statement stmtPk = conn.createStatement();
		ResultSet rsPk;
		String sqlPk = "";
		if (servertype.equals("0")) {            
			sqlPk += "use [" + databasename + "] SELECT  i.name AS IndexName,COL_NAME(ic.OBJECT_ID,ic.column_id) AS ColumnName ";
			sqlPk += "FROM    sys.indexes AS i INNER JOIN sys.index_columns AS ic ON  i.OBJECT_ID = ic.OBJECT_ID ";
			sqlPk += "AND i.index_id = ic.index_id WHERE   i.is_primary_key = 1 and OBJECT_NAME(ic.OBJECT_ID) = '" + pairClassName + "'";
		} else {
			sqlPk += "select TT.CONSTRAINT_SCHEMA, TT.TABLE_NAME, TT.CONSTRAINT_TYPE, KC.COLUMN_NAME as ColumnName ";
			sqlPk += " from information_schema.TABLE_CONSTRAINTS TT ";
			sqlPk += " JOIN information_schema.KEY_COLUMN_USAGE KC ON TT.CONSTRAINT_NAME = KC.CONSTRAINT_NAME AND TT.TABLE_NAME = KC.TABLE_NAME ";
			sqlPk += " where TT.CONSTRAINT_SCHEMA = '" + databasename + "' and TT.table_name = '" + pairClassName + "' and TT.CONSTRAINT_TYPE = 'PRIMARY KEY';";
		}
		rsPk = stmtPk.executeQuery(sqlPk);
		ResultSetMetaData rsmd = rsPk.getMetaData();
		
		if (rsmd.getColumnCount() > 0) {
			Element oOperation = doc.createElement("ownedOperation");
			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			oOperation.setAttributeNode(attr);

			oOperation.setAttribute("name", "PK");
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");
			attr.setValue("uml:Operation");
			oOperation.setAttributeNode(attr);
			
			peClass.appendChild(oOperation);

			while(rsPk.next()) {
				String pkColumnName = rsPk.getString("ColumnName");
				String pkColumnKey = UUID.randomUUID().toString();	

				Element oParameter = doc.createElement("ownedParameter");
				attr = doc.createAttributeNS(XMITYPE, "xmi:id");
				attr.setValue(UUID.randomUUID().toString());
				oParameter.setAttributeNode(attr);

				oParameter.setAttribute("name", pkColumnName);						

				attr = doc.createAttribute("type");
				String keyColumn = "", keyDataTypeColumn = "", key = "";
				for(Map.Entry entry: hmapColumns.entrySet()){
					if(pkColumnName.equals(entry.getValue())) {
						keyColumn = entry.getKey().toString();
					}
				}

				for(Map.Entry entry: hmapColumnsDataTypes.entrySet()){
					if(keyColumn.equals(entry.getKey())) {
						keyDataTypeColumn = entry.getValue().toString();
					}
				}

				for(Map.Entry entry: hmapDataTypes.entrySet()){
					if(keyDataTypeColumn.equals(entry.getValue())) {
						key = entry.getKey().toString();
					}
				}
				attr.setValue(key);
				oParameter.setAttributeNode(attr);

				oOperation.appendChild(oParameter);
			}      		        		
		}		
	}
	
	private void getAllTableColumns(Element packageElement, Document doc, Attr attr, String databasename, String servertype, String pairClassName, Element peClass) throws SQLException
	{
		Statement stmtColumns = conn.createStatement();
		ResultSet rsColumns;
		String sqlQuery = "";
		if (servertype.equals("0")) {
			sqlQuery += "use [" + databasename + "] select COLUMN_NAME, DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = '" + pairClassName + "'";
		} else {
			sqlQuery += "select COLUMN_NAME, DATA_TYPE from INFORMATION_SCHEMA.COLUMNS where TABLE_NAME = '" + pairClassName + "';";
		}		
		rsColumns = stmtColumns.executeQuery(sqlQuery);		
		while ( rsColumns.next() ) {
			String columnName = rsColumns.getString("COLUMN_NAME");
			String columnKey = UUID.randomUUID().toString();
			columnDataType = rsColumns.getString("DATA_TYPE");            

			hmapColumns.put(columnKey, columnName);
			hmapColumnsDataTypes.put(columnKey, columnDataType);

			oaAttribute = doc.createElement("ownedAttribute"); 
			attr = doc.createAttribute("type");
			String key = "";
			for(Map.Entry entry: hmapDataTypes.entrySet()){
				if(columnDataType.equals(entry.getValue())) {
					key = entry.getKey().toString();
				}
			}
			attr.setValue(key);
			oaAttribute.setAttributeNode(attr);

			oaAttribute.setAttribute("name", columnName.toString());        		

			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(UUID.randomUUID().toString());
			oaAttribute.setAttributeNode(attr);     
			
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");/*dodala Dragana za definisanje Property*/
			attr.setValue("uml:Property");
			oaAttribute.setAttributeNode(attr);
			
			peClass.appendChild(oaAttribute);
		}
		
	
	}
	
	private void createDataTypePackage(Element packageElement, Document doc, Attr attr, String databasename, String servertype, Connection conn) throws SQLException
	{
		Statement stmt = conn.createStatement();
		ResultSet rs;
		if (servertype.equals("0")) {
			rs = stmt.executeQuery("use [" + databasename + "] select DISTINCT DATA_TYPE from information_schema.columns where TABLE_NAME != 'sysdiagrams'  union ALL SELECT 'ASC' UNION ALL SELECT 'DESC'");
		} else {
			rs = stmt.executeQuery("SELECT distinct DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS where table_schema = '" + databasename + "' union ALL SELECT 'ASC' UNION ALL SELECT 'DESC'; ");
		}
		while ( rs.next() ) {
			String dataTypeName = rs.getString("DATA_TYPE");
			String dataTypeKey = UUID.randomUUID().toString();
			hmapDataTypes.put(dataTypeKey, dataTypeName);
		}		
		Element packageDataTypes = doc.createElement("packagedElement");
		attr = doc.createAttributeNS(XMITYPE, "xmi:type");
		attr.setValue("uml:Package");
		packageDataTypes.setAttributeNode(attr);

		//packageDataTypes.setAttribute("name", "ICM_PT");
		packageDataTypes.setAttribute("name", "DataTypes");
		attr = doc.createAttributeNS(XMITYPE, "xmi:id");
		dataTypePackageId = UUID.randomUUID().toString();
		attr.setValue(dataTypePackageId);
		packageDataTypes.setAttributeNode(attr);

		packageElement.appendChild(packageDataTypes);
		
		Iterator it = hmapDataTypes.entrySet().iterator();
		while (it.hasNext()) {
			HashMap.Entry pair = (HashMap.Entry)it.next();
			Element peDataType = doc.createElement("packagedElement");
			attr = doc.createAttributeNS(XMITYPE, "xmi:type");
			attr.setValue("uml:PrimitiveType");
			peDataType.setAttributeNode(attr);

			String dataTypeName = "";
			if (pair.getValue().equals("int")) { dataTypeName = "Integer"; }
			else if (pair.getValue().equals("varchar") || pair.getValue().equals("nchar") || pair.getValue().equals("char")) { dataTypeName = "String"; }
			else if (pair.getValue().equals("datetime") || pair.getValue().equals("date")) { dataTypeName = "DateTime"; }
			else if (pair.getValue().equals("ASC")) { dataTypeName = "ASC"; }
			else if (pair.getValue().equals("DESC")) { dataTypeName = "DESC"; }
			else if (pair.getValue().equals("blob")) { dataTypeName = "Blob"; }
			else if (pair.getValue().equals("smallint")) { dataTypeName = "Short"; }
			peDataType.setAttribute("name", dataTypeName);

			attr = doc.createAttributeNS(XMITYPE, "xmi:id");
			attr.setValue(pair.getKey().toString());
			System.out.println("PAR " + pair.getKey().toString());
			
			peDataType.setAttributeNode(attr);

			packageDataTypes.appendChild(peDataType);
		}		
	}
	
	
	private Connection createConnection(String servertype, String auttype, String servername, String username, String password, String publicip) 
	{
		if (servertype.toString().equals("0"))
		{
			if (auttype.equals("1"))
			{
				try 
				{
					Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
					String conString = "";			
					if (servername != "" && servername != null) {
						conString = "jdbc:sqlserver://;serverName=" + servername + ";username=" + username + ";password=" + password;
					} else {
						conString = "jdbc:sqlserver://;serverName=" + publicip + ";username=" + username + ";password=" + password;
					}
					conn = DriverManager.getConnection(conString);
					
					//hmapConnections.put("1", conn);
										
					System.out.println("Konekcija je uspjesna!");
				}
				catch(Exception e) {
					e.printStackTrace();
					System.out.println("Konekcija nije uspjesna!");
				}
			}
			else if (auttype.equals("0"))
			{
				try {
					Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
					String connString = "";
					
					String inetAddress = request.getRemoteHost();
					
					if (servername != "" && servername != null) {
						connString = "jdbc:sqlserver://;serverName=" + servername + ";integratedSecurity=true;";
					} else {					
						connString = "jdbc:sqlserver://;serverName=" + publicip + ";integratedSecurity=true;";
					}
					System.out.println("MOJA KONEKCIJA " + connString);
					conn = DriverManager.getConnection(connString);
					//hmapConnections.put("1", conn);
				}
				catch(Exception e) {
					e.printStackTrace();
					System.out.println("Konekcija nije uspjesna!");
				}
			}
		}
		else if (servertype.equals("1")) /*MYSQL*/
		{
			Properties connectionProps = new Properties();
			connectionProps.put("user", username);
	        connectionProps.put("password", password);
			String conString = "";
			if(servername.equals("")) {
				conString = "jdbc:mysql://address=(protocol=tcp)(host=" + publicip + ")(port=3306)/";
				System.out.println(conString);
				
			} else {
				conString = "jdbc:mysql://address=(protocol=tcp)(host=" + servername + ")(port=3306)/";
			}
			
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();;
				conn = DriverManager.getConnection(conString, connectionProps);
				System.out.println("Konekcija je uspjesna MYSQL!");
				//hmapConnections.put("1", conn);
			}
			catch(Exception e) {
				System.out.println("Konekcija nije uspjesna MYSQL!");
				e.printStackTrace();
			}
		}
		return conn;
	}
	
	
	
}
