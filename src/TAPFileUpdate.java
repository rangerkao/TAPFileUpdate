import java.io.BufferedOutputStream;import java.io.BufferedReader;import java.io.File;import java.io.FileInputStream;import java.io.FileNotFoundException;import java.io.FileOutputStream;import java.io.IOException;import java.io.InputStreamReader;import java.io.PrintWriter;import java.io.StringWriter;import java.sql.Connection;import java.sql.DriverManager;import java.sql.ResultSet;import java.sql.SQLException;import java.sql.Statement;import java.text.SimpleDateFormat;import java.util.ArrayList;import java.util.Date;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;import java.util.Properties;import java.util.Set;import java.util.zip.GZIPInputStream;import javax.mail.Message;import javax.mail.MessagingException;import javax.mail.Session;import javax.mail.Transport;import javax.mail.internet.InternetAddress;import javax.mail.internet.MimeMessage;import org.apache.log4j.Logger;import org.apache.log4j.PropertyConfigurator;public class TAPFileUpdate {	//file createTime	static Map fileLastDate = new HashMap();	//chargearea data	static List chargeareaconfigList = new ArrayList();	//TAPInPartner data	static List TAPInPartner = new ArrayList();	//use for dateTime	static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");	//use for runtime	static SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmss");			static Properties props=new Properties();	static Logger logger = null;	static Connection conn1 = null;	static Connection conn2 = null;		static Date startTime = null;	//program runtime	static String regularTime = null;		public static void main(String[] args) throws Exception {				if(loadProperties()){			//String workDir = "C:\\Users\\ranger.kao\\Desktop\\TAP";			String workDir = props.getProperty("workdir");			String tempDir=workDir+"/"+"tempDir";						//prefix,startNo,endNo			//args = new String []{"CDTHAASHKGBM","2764","2768"};				if(args.length==0){				logger =Logger.getLogger("TAPFileUpdate");				regularTime = props.getProperty("dayExecuteTime");				logger.info("One day execute time : "+regularTime);								boolean exit=false;				while(!exit){					Thread.sleep(1000);					if(sdf2.format(new Date()).equals(regularTime)){						mainProcess(workDir,tempDir);					}					//set every minute 0~60 when 0 print					if(0 == new Date().getSeconds()){						logger.info("Program running");					}				}								//mainProcess(workDir,tempDir);			}			if(args.length==3){				logger =Logger.getLogger("TAPFileCustomerUpdate");				String uPrefix = args[0];				int uStartNo = Integer.parseInt(args[1]);				int uEndNo = Integer.parseInt(args[2]);								cusProcess(workDir,tempDir,uPrefix,uStartNo,uEndNo);			}		}	}		private static void cusProcess(String workDir,String tempDir,String uPrefix,int uStartNo,int uEndNo){		startTime = new Date();		logger.info("customer Program start...  "+startTime);		try {			conn1 = connDB1();			conn2 = connDB2();			if(conn1 == null || conn2 == null)				throw new Exception("Connection is null");						//cancel auto commit			conn1.setAutoCommit(false);						if(!setChargeAreaConfig())				throw new Exception("setChargeAreaConfig Error!");						if(!loadTAPInPartner())				throw new Exception("loadTAPInPartner Error!");						fileLastDate.clear();						File fileDir = new File(workDir);			logger.info(workDir);			if(fileDir.isDirectory()){								File temp = new File(tempDir);				deleteFile(temp);				if(!temp.exists()){					temp.mkdir();				}												loadFile2(workDir, tempDir, uPrefix, uStartNo, uEndNo);								readFile(tempDir);								conn1.commit();								logger.info("delete temp folder!");				deleteFile(temp);			}else{				ErrorHandle("The workDir not a folder!");			}		} catch (IOException e) {			ErrorHandle("At main cusProcess got IOException!", e);		} catch (ClassNotFoundException e) {			ErrorHandle("At main cusProcess got ClassNotFoundException!", e);		} catch (SQLException e) {			ErrorHandle("At main cusProcess got SQLException!", e);		} catch (Exception e) {			ErrorHandle("At main cusProcess got Exception!", e);		}finally{			try {				if(conn1!=null){					conn1.close();				}				if(conn2!=null){					conn2.close();				}			} catch (SQLException e) {			}			logger.info("Program end... (run time = "+(new Date().getTime()-startTime.getTime())+")");		}		}		private static void mainProcess(String workDir,String tempDir){		startTime = new Date();		logger.info("Program start...  "+startTime);		try {			conn1 = connDB1();			conn2 = connDB2();			if(conn1 == null || conn2 == null)				throw new Exception("Connection is null");						//cancel auto commit			conn1.setAutoCommit(false);						if(!setChargeAreaConfig())				throw new Exception("setChargeAreaConfig Error!");						if(!loadTAPInPartner())				throw new Exception("loadTAPInPartner Error!");						fileLastDate.clear();						File fileDir = new File(workDir);			if(fileDir.isDirectory()){								File temp = new File(tempDir);				deleteFile(temp);								if(!temp.exists()){					temp.mkdir();				}								loadFile(workDir,tempDir);								readFile(tempDir);								if(!updateTAPInPartner())					throw new Exception("updateTAPInPartner Error!");								conn1.commit();							logger.info("delete temp folder!");				deleteFile(temp);							}else{				ErrorHandle("The workDir not a folder!");			}		} catch (ClassNotFoundException e) {			ErrorHandle("At main process got ClassNotFoundException!", e);		} catch (SQLException e) {			ErrorHandle("At main process got SQLException!", e);		} catch (Exception e) {			ErrorHandle("At main process got Exception!", e); 		}finally{			try {				if(conn1!=null){					conn1.close();				}				if(conn2!=null){					conn2.close();				}			} catch (SQLException e) {			}			logger.info("Program end... (run time = "+(new Date().getTime()-startTime.getTime())+")");		}		}	private static void loadFile2(String workDir,String tempDir,String uPrefix,int uStartNo,int uEndNo) throws IOException{		logger.info("loadFile2...  ");		File fileDir = new File(workDir);				int length = 0;		for(Iterator map = TAPInPartner.iterator();map.hasNext();){			Map m = (Map) map.next();			String prefix = (String) m.get("prefix");			if(uPrefix.equalsIgnoreCase(prefix)){				length = Integer.parseInt((String) m.get("length"));				break;			}		}				File[] fileList = fileDir.listFiles();		for( int i=0;i<fileList.length;i++){			File f = fileList[i];			if(f.isFile()){				String fName = f.getName();				//System.out.println(fName);								if(fName.indexOf(uPrefix)!=-1){					//nameProccess					int dot = fName.indexOf(".");					//System.out.println(dot);					String n = fName.substring(0, dot);					//System.out.println(n);															String sNo = n.substring(n.length()-length);					//System.out.println(sNo);					int iNo = Integer.parseInt(sNo);					//System.out.println(iNo);					if(uStartNo<=iNo && iNo<=uEndNo){						logger.info("Extract file "+fName);						unGZIP(workDir,fName,tempDir);					}				}			}		}				}	private static void loadFile(String workDir,String tempDir) throws IOException{		logger.info("loadFile...  ");		File fileDir = new File(workDir);		File[] fileList = fileDir.listFiles();		for( int i=0;i<fileList.length;i++){			File f = fileList[i];			if(f.isFile()){				String fName = f.getName();				//System.out.println(fName);								//nameProccess				int dot = fName.indexOf(".");				//System.out.println(dot);				String n = fName.substring(0, dot);				//System.out.println(n);								for(Iterator map = TAPInPartner.iterator();map.hasNext();){					Map m = (Map) map.next();					String prefix = (String) m.get("prefix");										if(n.indexOf(prefix)!=-1){						int length = Integer.parseInt((String) m.get("length"));						String sNo = n.substring(n.length()-length);						//System.out.println(sNo);						int iNo = Integer.parseInt(sNo);						//System.out.println(iNo);												int pNo =Integer.parseInt((String) m.get("number"));												if(pNo<iNo){							System.out.println("Extract file "+fName);							unGZIP(workDir,fName,tempDir);							int last = Integer.parseInt((String) m.get("temp"));							if(iNo>last){								m.put("temp", String.valueOf(iNo));							}						}					}				}			}		}				}	private static boolean loadTAPInPartner(){		logger.info("loadTAPInPartner...");		TAPInPartner.clear();				Statement st = null;		ResultSet rs = null;				String sql = "SELECT A.PARTNERID,A.NAME,A.FILEPREFIX,A.FILENUMBERLEN,A.LASTPROCESSFILENO "				+ "FROM TAPINPARTNER A ";				try {			st = conn1.createStatement();			logger.info("Execute query TAPInPartner:"+sql);			rs = st.executeQuery(sql);						while(rs.next()){				Map m = new HashMap();				m.put("id", rs.getString("PARTNERID"));				m.put("name", rs.getString("NAME"));				m.put("prefix", rs.getString("FILEPREFIX"));				m.put("length", rs.getString("FILENUMBERLEN"));				m.put("number", rs.getString("LASTPROCESSFILENO"));				m.put("temp", "0");				TAPInPartner.add(m);			}						return true;					} catch (SQLException e) {			ErrorHandle("At getTAPFileID got SQLException",e);			return false;		}finally{						try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static boolean updateTAPInPartner(){				for(Iterator map = TAPInPartner.iterator();map.hasNext();){			Map m = (Map) map.next();			int oNo = Integer.parseInt((String) m.get("number"));			int lNo = Integer.parseInt((String) m.get("temp"));						if(oNo<lNo)				m.put("number", String.valueOf(lNo));		}						Statement st = null;		ResultSet rs = null;		try {						st = conn1.createStatement();			for(Iterator map = TAPInPartner.iterator();map.hasNext();){				Map m = (Map) map.next();				String sql = "UPDATE TAPINPARTNER A "						+ "SET A.LASTPROCESSFILENO = "+m.get("number")+",A.PROCESSTIME = TO_DATE('"+sdf.format(startTime)+"','yyyyMMddhh24miss') "						+ "WHERE A.NAME ='"+m.get("name")+"' ";				logger.info("Execute update TAPInPartner:"+sql);				st.executeUpdate(sql);			}			return true;					} catch (SQLException e) {			ErrorHandle("At getTAPFileID got SQLException",e);			return false;		}finally{						try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static void unGZIP(String workDir,String fName,String tempDir) throws IOException{		String fileName = fName;		File file = new File(workDir,fName);		fileLastDate.put(fName.substring(0,fName.length()-3), sdf.format(new Date(file.lastModified())));		//System.out.println("put "+fName.substring(0,fName.length()-3)+" date = "+fileLastDate.get(fName.substring(0,fName.length()-3)));				GZIPInputStream gzi = new GZIPInputStream(new FileInputStream(file));	    int to = fileName.lastIndexOf('.');        String toFileName = fileName.substring(0, to);        //System.out.println(tempDir);        File targetFile = new File(tempDir,toFileName);        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));        int b;        byte[] d = new byte[1024];        try {            while ((b = gzi.read(d)) > 0) {                bos.write(d, 0, b);            }        } catch (Exception err) {        }        gzi.close();        bos.close();       /* File file = new File(toFileName);        FileReader fileReader = new FileReader(file);        BufferedReader buffferedReader = new BufferedReader(fileReader);        String line = null;        while ((line = buffferedReader.readLine()) != null) {            System.out.println(line);        }        buffferedReader.close();        fileReader.close();		*/	}	private static boolean loadProperties(){		System.out.println("initial Log4j, property !");		String path=TAPFileUpdate.class.getResource("").toString().replaceAll("file:", "")+"/log4j.properties";			try {				props.load(new   FileInputStream(path));				PropertyConfigurator.configure(props);				return true;			} catch (FileNotFoundException e) {				ErrorHandle("At Propertiese got FileNotFoundException",e);				return false;			} catch (IOException e) {				ErrorHandle("At Propertiese got IOException",e);				return false;			}				}	public static void ErrorHandle(String cont){		ErrorHandle(cont,null);	}	public static void ErrorHandle(String cont,Exception e){		String errorMsg="";				if(logger!=null){			if(e!=null){				logger.error(cont, e);				StringWriter s = new StringWriter();				e.printStackTrace(new PrintWriter(s));				//send mail				errorMsg=s.toString();			}else{				logger.error(cont);			}		}else{			System.out.println(cont);			e.printStackTrace();		}						sendMail(cont+"\n"+errorMsg+"\n\n"+new Date());	}		private static void deleteFile(File f){		if(f.exists()){			if(f.isDirectory()){				File[] fileList = f.listFiles();								for( int i=0;i<fileList.length;i++){					File sf = fileList[i];					deleteFile(sf);				}			}			f.delete();		}	}		public static void sendMail(String content){		sendMail("TAPFileUpdate Error",content);	}	public static void sendMail(String subject,String content){		final String host=props.getProperty("mail.smtp.host");						final String username = props.getProperty("mail.username");		final String passwd = props.getProperty("mail.password");						String sender = props.getProperty("mail.Sender");			String receiver = props.getProperty("mail.Receiver");					Session session = javax.mail.Session.getInstance(props);		try {			Message message = new MimeMessage(session);			message.setFrom(new InternetAddress(sender));			message.setRecipients(Message.RecipientType.TO,	InternetAddress.parse(receiver));			message.setSubject(subject);			message.setText(content);			Transport transport = session.getTransport("smtp");		    transport.connect(host,username,passwd);		    transport.sendMessage(message, message.getAllRecipients());		} catch (MessagingException e) {			throw new RuntimeException(e);		}			}					private static void readFile(String dir) throws Exception{		logger.info("readFile...");		File fileDir = new File(dir);					if(fileDir.isDirectory()){			File [] fileList = fileDir.listFiles();			for( int i=0;i<fileList.length;i++){				File file = fileList[i];				logger.info("Processing File "+file.getName()+"...");				BufferedReader reader = null;				try {										String processTime = sdf.format(startTime);										String fileCreateTime = (String) fileLastDate.get(file.getName());										if(fileCreateTime==null)						throw new Exception("file:"+file.getName()+" is null!");											//Find Partner ID					String partnerId = null;					String fileName = file.getName();					for( Iterator map = TAPInPartner.iterator() ; map.hasNext();){						Map m = (Map) map.next();						String pName = (String) m.get("name");						if(fileName.indexOf(pName)!=-1){							partnerId = (String) m.get("id");							break;						}					}										if(partnerId == null)						throw new Exception("Can't find partner Id!");										Map TAPFileDetail = new HashMap();					reader = new BufferedReader(new InputStreamReader(new FileInputStream(file))); // 					String str;					int CDRcount = 0;					while ((str = reader.readLine()) != null) {							String datas[] = str.split(",");														String data66 = null,data128 = null,data129 = null,data12 = null,data13 = null,data36 = null,data37 = null,data1010 = null;														int usage = 0;							int totalsec = 0;							long updata = 0;							long downdata = 0;							double amount = 0D;														data66 = foundTag(datas,66);							String type = data66;														data128 = foundTag(datas,128);//outgoing							data129 = foundTag(datas,129);//incoming														if((data128!=null && data12 !=null)||(data128==null &&data129==null)){								throw new Exception("Not correct data!(tag 128 and tag129)");							}														data12 = foundTag(datas,12);							if(data12==null){								throw new Exception("No data!(tag 12)");							}							String startDate = data12.substring(0,8);														String VLR = data128;							String direction = "128";							if(VLR == null){ 								VLR = data129;								direction = "129";							}																					if(!"0".equals(data66)&&!"3".equals(data66)&&!"53".equals(data66)){								throw new Exception("Found error type!");							}														Map VLRType = new HashMap();							if(TAPFileDetail.containsKey(VLR)){								VLRType = (Map) TAPFileDetail.get(VLR);							}														Map detaType = new HashMap();							if(VLRType.containsKey(direction)){								detaType = (Map) VLRType.get(direction);							}									Map cdrDate = new HashMap();							if(detaType.containsKey(type)){								cdrDate = (Map) detaType.get(type);							}														Map detail = new HashMap();							int typeCount = 0;							if(cdrDate.containsKey(startDate)){								detail = (Map) cdrDate.get(startDate);								typeCount = Integer.parseInt((String) detail.get("count"));							}							CDRcount++;							typeCount++;							if("0".equals(data66)){//voice								//data12 = foundTag(datas,12);								data13 = foundTag(datas,13);								if(data12==null||data13==null){									throw new Exception("Not correct data!(tag 12 and tag13)");								}								usage =  (detail.get("usage")==null? 0:Integer.parseInt(detail.get("usage").toString()));								totalsec = (detail.get("totalsec")==null? 0:Integer.parseInt((String) detail.get("totalsec")));								//System.out.println("min="+min);																double sec = ((double)sdf.parse(data13).getTime()-sdf.parse(data12).getTime())/1000;								double min = sec/60;																usage += (int)Math.ceil(min);								totalsec += sec;																//System.out.println("sec="+sec+"("+(sdf.parse(data13).getTime())+"-"+(sdf.parse(data12).getTime())+"="+(sdf.parse(data13).getTime()-sdf.parse(data12).getTime())+")/1000");								//System.out.println("min="+min);								//System.out.println("unit usage="+(int)Math.ceil(min));								//System.out.println("usage="+usage);															}else if("3".equals(data66)){//sms								int countSMS = (detail.get("usage")==null? 0:Integer.parseInt((String) detail.get("usage")));								countSMS++;								usage = countSMS;																//System.out.println("countSMS="+countSMS);							}else if("53".equals(data66)){//data								data36 = foundTag(datas,36);								data37 = foundTag(datas,37);																if(data36==null||data37==null){									throw new Exception("Not correct data!(tag 36 and tag37)");								}																usage = (detail.get("usage")==null? 0:Integer.parseInt((String) detail.get("usage")));								updata = (detail.get("updata")==null? 0l:Long.parseLong((String) detail.get("updata")));								downdata = (detail.get("downdata")==null? 0l:Long.parseLong((String) detail.get("downdata")));																double up = Double.parseDouble(data36);								double down = Double.parseDouble(data37);								double sum = (up+down)/1024;																usage += (int) Math.ceil(sum);								updata += up;								downdata += down;																//System.out.println("up="+up+"("+Double.parseDouble(data36)+"/1024)");								//System.out.println("down="+down+"("+Double.parseDouble(data37)+"/1024)");								//System.out.println("sum="+(up+down));								//System.out.println("usage="+usage);							}														data1010 = foundTag(datas,1010);							if(data1010==null){								throw new Exception("Can't fine tag data1010!");							}							amount = (detail.get("amount")==null? 0D:Double.parseDouble((String) detail.get("amount")));							amount += Double.parseDouble(data1010);																											String areaReference = "";														int arCount = 0;							for(int vi = VLR.length() ; vi > 0; vi--){																for(Iterator map = chargeareaconfigList.iterator() ; map.hasNext();){									Map m = (Map) map.next();									String chargeAreaCode = (String) m.get("CHARGEAREACODE");									if(chargeAreaCode!=null && chargeAreaCode.equals(VLR.substring(0,vi))){										arCount ++;										areaReference = (String) m.get("AREAREFERENCE");										//System.out.println("VLR="+VLR+",chargeAreaCode="+chargeAreaCode+",areaReference="+areaReference);									}								}								if(arCount>0) break;							}														if(arCount == 0 || areaReference == null)								throw new Exception("Can't find areaReference by VLR="+VLR+".");							if(arCount > 1)								throw new Exception("Found more than one  areaReference by VLR="+VLR+".");							//data put							detail.put("count", String.valueOf(typeCount));							detail.put("usage",String.valueOf(usage));							detail.put("amount", String.valueOf(amount));							detail.put("areaReference", String.valueOf(areaReference));							//new request							detail.put("updata",String.valueOf(updata));							detail.put("downdata", String.valueOf(downdata));							detail.put("totalsec", String.valueOf(totalsec));														cdrDate.put(startDate, detail);							detaType.put(type, cdrDate);							VLRType.put(direction, detaType);							TAPFileDetail.put(VLR, VLRType);														/*System.out.println(count+"\t"									+ "66="+data66+"\t"									+ "128="+data128+"\t"									+ "129="+data129+"\t"									+ "12="+data12+"\t"									+ "13="+data13+"\t"									+ "36="+data36+"\t"									+ "37="+data37+"\t"									+ "1010="+data1010+".");*/							//XXX Test use							//if(CDRcount == 3000)break;					}					//get FileID					String tapFileId = getTAPFileID();					if(tapFileId == null)						throw new Exception("Can't get TAPFile ID!");										updateTapFile(tapFileId,fileName,CDRcount,processTime,partnerId,fileCreateTime);					updateTAPFileDetail(tapFileId,TAPFileDetail);									} catch (IOException e) {					ErrorHandle("At readFile got IOException",e);				} catch (Exception e) {					ErrorHandle("At readFile got IOException",e);				}finally {					try {						if(reader!=null)							reader.close();					} catch (IOException e) {					}				}			}		}else{			throw new Exception("Error read file path "+dir+" !");		}	}		private static String getTAPFileID(){		Statement st = null;		ResultSet rs = null;				String sql = "SELECT TAPFILE_ID.NEXTVAL TAPFILE_ID "				+ "FROM DUAL";				try {			st = conn1.createStatement();			logger.info("Execute query  TAPFileID:"+sql);			rs = st.executeQuery(sql);					String tapFileId = null ;						while(rs.next()){				tapFileId = rs.getString("TAPFILE_ID");			}						return tapFileId;					} catch (SQLException e) {			ErrorHandle("At getTAPFileID got SQLException",e);			return null;		}finally{						try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static void updateTapFile(String tapFileId,String fileName,int CDRCount,String processTime,String pid,String FileCreateTime){		logger.info("updateTapFile ...");		Statement st = null;		ResultSet rs = null;				String sql = "INSERT INTO TAPFILE(FILEID,FILENAME,CDRCOUNT,PROCESSTIME,PARTNERID,FILECREATETIME) "				+ "VALUES("+tapFileId+",'"+fileName+"',"+CDRCount+", to_date('"+processTime+"','yyyyMMddhh24miss'),"+pid+",to_date('"+FileCreateTime+"','yyyyMMddhh24miss'))";				try {			st = conn1.createStatement();			logger.info("Execute insert TapFile :"+sql);			st.executeUpdate(sql);		} catch (SQLException e) {			ErrorHandle("At updateTapFile got SQLException",e);		}finally{			try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static void updateTAPFileDetail(String tapFileId,Map TAPFileDetail){		logger.info("updateTAPFileDetail ...");		Statement st = null;		ResultSet rs = null;				String sql = null;				try {			st = conn1.createStatement();			Set VLRKey = TAPFileDetail.keySet();			for(Iterator i = VLRKey.iterator(); i.hasNext();){				String VLR = (String) i.next();				Map VLRMap = (Map) TAPFileDetail.get(VLR);				Set directionKey = VLRMap.keySet();				for(Iterator j = directionKey.iterator();j.hasNext();){					String direction = (String) j.next();					Map directionMap = (Map) VLRMap.get(direction);					Set typeKey = directionMap.keySet();					for(Iterator z = typeKey.iterator();z.hasNext();){						String type = (String) z.next();						Map typeMap = (Map) directionMap.get(type);						Set dateKey = typeMap.keySet();						for(Iterator k = dateKey.iterator();k.hasNext();){							String startDate = (String) k.next();							Map dateMap = (Map) typeMap.get(startDate);														sql = "INSERT INTO TAPFILEDETAIL(FILEID,VLR,TYPE,DIRECTION,CDRStartDate,CDRCOUNT,TOTALUSAGE,TOTALAMOUNT,AREAREFERENCE"									+ ",ORIGINALUPLOAD,ORIGINALDOWNLOAD,ORIGINALDURATION"									+ ") "									+ "VALUES("+tapFileId+",'"+VLR+"',"+type+"," + direction+ "," + startDate									+ "," + dateMap.get("count")									+ "," + dateMap.get("usage")									+ "," + dateMap.get("amount")									+ "," + dateMap.get("areaReference")									+ "," + dateMap.get("updata")									+ "," + dateMap.get("downdata")									+ "," + dateMap.get("totalsec")									+ ")";							logger.info("Execute insert TapFileDetail :"+sql);							st.executeUpdate(sql);						}					}				}			}		} catch (SQLException e) {			ErrorHandle("At updateTAPFileDetail got SQLException",e);		}finally{			try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static boolean setChargeAreaConfig(){		logger.info("setChargeAreaConfig...");		chargeareaconfigList.clear();				Statement st = null;		ResultSet rs = null;				String sql = "SELECT CHARGEAREACODE,AREAREFERENCE "				+ "FROM CHARGEAREACONFIG A ";				try {			st = conn2.createStatement();			logger.info("Execute query  CHARGEAREACONFIG:"+sql);			rs = st.executeQuery(sql);						while(rs.next()){				Map map = new HashMap();				map.put("CHARGEAREACODE", rs.getString("CHARGEAREACODE"));				map.put("AREAREFERENCE", rs.getString("AREAREFERENCE"));				chargeareaconfigList.add(map);			}						return true;					} catch (SQLException e) {			ErrorHandle("At setChargeAreaConfig got SQLException",e);			return false;		}finally{						try {				if(st!=null){					st.close();				}				if(rs!=null){					rs.close();				}			} catch (SQLException e) {			}		}	}		private static String foundTag(String [] datas,int tag){		String result = null;				//directive search		for(int i = 0 ;i<datas.length;i++){			String data[] = datas[i].split("/");			if(data.length==2 && String.valueOf(tag).equals(data[0])){				result = data[1];				return result;			}		}		//by other tag		for(int i=tag-1 ; i>0 ; i--){			for(int j = 0 ;j<datas.length;j++){				String data[] = datas[j].split("/");				if(data.length==2 && String.valueOf(i).equals(data[0]) &&  j+(tag-i)<datas.length){					String subdata[] = datas[j+(tag-i)].split("/");					if(subdata.length==1){						result = subdata[0];						return result;					}else{						//found null						return result;					}				}			}		}		return result;	}		/**	 * connect to DB	 */	public static Connection connDB1() throws ClassNotFoundException, SQLException {				String url = props.getProperty("Oracle.URL")				.replaceAll("\\{\\{Host\\}\\}", props.getProperty("Oracle.Host"))				.replaceAll("\\{\\{Port\\}\\}", props.getProperty("Oracle.Port"))				.replaceAll("\\{\\{ServiceName\\}\\}", (props.getProperty("Oracle.ServiceName")!=null?props.getProperty("Oracle.ServiceName"):""))				.replaceAll("\\{\\{SID\\}\\}", (props.getProperty("Oracle.SID")!=null?props.getProperty("Oracle.SID"):""));				String DriverClass = props.getProperty("Oracle.DriverClass");		String UserName = props.getProperty("Oracle.UserName");		String PassWord = props.getProperty("Oracle.PassWord");				Class.forName(DriverClass);		return DriverManager.getConnection( url, UserName, PassWord);	}	public static Connection connDB2() throws ClassNotFoundException, SQLException {		String url = props.getProperty("mBOSS.URL")				.replaceAll("\\{\\{Host\\}\\}", props.getProperty("mBOSS.Host"))				.replaceAll("\\{\\{Port\\}\\}", props.getProperty("mBOSS.Port"))				.replaceAll("\\{\\{ServiceName\\}\\}", (props.getProperty("mBOSS.ServiceName")!=null?props.getProperty("mBOSS.ServiceName"):""))				.replaceAll("\\{\\{SID\\}\\}", (props.getProperty("mBOSS.SID")!=null?props.getProperty("mBOSS.SID"):""));				String DriverClass = props.getProperty("mBOSS.DriverClass");		String UserName = props.getProperty("mBOSS.UserName");		String PassWord = props.getProperty("mBOSS.PassWord");				Class.forName(DriverClass);		return DriverManager.getConnection( url, UserName, PassWord);	}}