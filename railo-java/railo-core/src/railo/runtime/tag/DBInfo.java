package railo.runtime.tag;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import railo.commons.sql.SQLUtil;
import railo.runtime.db.DataSourceManager;
import railo.runtime.db.DatasourceConnection;
import railo.runtime.exp.ApplicationException;
import railo.runtime.exp.DatabaseException;
import railo.runtime.exp.PageException;
import railo.runtime.ext.tag.TagImpl;
import railo.runtime.timer.Stopwatch;
import railo.runtime.type.Array;
import railo.runtime.type.ArrayImpl;
import railo.runtime.type.Collection.Key;
import railo.runtime.type.KeyImpl;
import railo.runtime.type.QueryImpl;
import railo.runtime.type.QueryPro;
import railo.runtime.type.SVArray;
import railo.runtime.type.Struct;
import railo.runtime.type.StructImpl;

/**
* Handles all interactions with files. The attributes you use with cffile depend on the value of the action attribute. 
*  For example, if the action = "write", use the attributes associated with writing a text file.
*
*
*
**/
public final class DBInfo extends TagImpl {

	private static final Key TABLE_NAME = KeyImpl.getInstance("TABLE_NAME");
	private static final Key COLUMN_NAME = KeyImpl.getInstance("COLUMN_NAME");
	private static final Key IS_PRIMARYKEY = KeyImpl.getInstance("IS_PRIMARYKEY");
	private static final Key IS_FOREIGNKEY = KeyImpl.getInstance("IS_FOREIGNKEY");
	private static final Key COLUMN_DEF = KeyImpl.getInstance("COLUMN_DEF");
	private static final Key COLUMN_DEFAULT_VALUE = KeyImpl.getInstance("COLUMN_DEFAULT_VALUE");
	private static final Key REFERENCED_PRIMARYKEY = KeyImpl.getInstance("REFERENCED_PRIMARYKEY");
	private static final Key REFERENCED_PRIMARYKEY_TABLE = KeyImpl.getInstance("REFERENCED_PRIMARYKEY_TABLE");
	private static final Key USER = KeyImpl.getInstance("USER");
	private static final Key TABLE_SCHEM = KeyImpl.getInstance("TABLE_SCHEM");
	
	
	
	private static final int TYPE_NONE=0;
	private static final int TYPE_DBNAMES=1;
	private static final int TYPE_TABLES=2;
	private static final int TYPE_TABLE_COLUMNS = 3;
	private static final int TYPE_VERSION = 4;
	private static final int TYPE_PROCEDURES = 5;
	private static final int TYPE_PROCEDURE_COLUMNS = 6;
	private static final int TYPE_FOREIGNKEYS = 7;
	private static final int TYPE_INDEX = 8;
	private static final int TYPE_USERS = 9;
	private static final int TYPE_TERMS = 10;
	
	//private static final String[] ALL_TABLE_TYPES = {"TABLE", "VIEW", "SYSTEM TABLE", "SYNONYM"};
	
	private String datasource;
	private String name;
	private int type;
	
	
	private String dbname;
	private String password;
	private String pattern;
	private String table;
	private String procedure;
	private String username;
	private String strType;
	
	
	/**
	* @see javax.servlet.jsp.tagext.Tag#release()
	*/
	public void release()	{
		super.release();
		datasource=null;
		name=null;
		type=TYPE_NONE;
		dbname=null;
		password=null;
		pattern=null;
		table=null;
		procedure=null;
		username=null;
		
		
		
	}

	/**
	 * @param procedure the procedure to set
	 */
	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	/**
	 * @param datasource the datasource to set
	 */
	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param type the type to set
	 * @throws ApplicationException 
	 */
	public void setType(String strType) throws ApplicationException {
		this.strType=strType;
		strType=strType.toLowerCase().trim();
		
		if("dbnames".equals(strType)) 			this.type=TYPE_DBNAMES;
		else if("dbname".equals(strType)) 		this.type=TYPE_DBNAMES;
		else if("tables".equals(strType)) 		this.type=TYPE_TABLES;
		else if("table".equals(strType)) 		this.type=TYPE_TABLES;
		else if("columns".equals(strType)) 		this.type=TYPE_TABLE_COLUMNS;
		else if("column".equals(strType)) 		this.type=TYPE_TABLE_COLUMNS;
		else if("version".equals(strType)) 		this.type=TYPE_VERSION;
		else if("procedures".equals(strType)) 	this.type=TYPE_PROCEDURES;
		else if("procedure".equals(strType)) 	this.type=TYPE_PROCEDURES;
		

		else if("table_columns".equals(strType))	this.type=TYPE_TABLE_COLUMNS;
		else if("table_column".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		else if("column_table".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		else if("column_tables".equals(strType))	this.type=TYPE_TABLE_COLUMNS;
		
		else if("tablecolumns".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		else if("tablecolumn".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		else if("columntable".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		else if("columntables".equals(strType))		this.type=TYPE_TABLE_COLUMNS;
		
		else if("procedure_columns".equals(strType))	this.type=TYPE_PROCEDURE_COLUMNS;
		else if("procedure_column".equals(strType))		this.type=TYPE_PROCEDURE_COLUMNS;
		else if("column_procedure".equals(strType))		this.type=TYPE_PROCEDURE_COLUMNS;
		else if("column_procedures".equals(strType))	this.type=TYPE_PROCEDURE_COLUMNS;
		
		else if("procedurecolumns".equals(strType))	this.type=TYPE_PROCEDURE_COLUMNS;
		else if("procedurecolumn".equals(strType))		this.type=TYPE_PROCEDURE_COLUMNS;
		else if("columnprocedure".equals(strType))		this.type=TYPE_PROCEDURE_COLUMNS;
		else if("columnprocedures".equals(strType))	this.type=TYPE_PROCEDURE_COLUMNS;
		
		else if("foreignkeys".equals(strType)) 	this.type=TYPE_FOREIGNKEYS;
		else if("foreignkey".equals(strType)) 	this.type=TYPE_FOREIGNKEYS;
		else if("index".equals(strType)) 		this.type=TYPE_INDEX;
		else if("users".equals(strType)) 		this.type=TYPE_USERS;
		else if("user".equals(strType)) 		this.type=TYPE_USERS;
		
		else if("term".equals(strType)) 	this.type=TYPE_TERMS;
		else if("terms".equals(strType)) 	this.type=TYPE_TERMS;
		
		else throw new ApplicationException("invalid value for attribute type ["+strType+"]",
				"valid values are [dbname,tables,columns,version,procedures,foreignkeys,index,users]");
		
	}

	/**
	 * @param dbname the dbname to set
	 */
	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param pattern the pattern to set
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * @param table the table to set
	 */
	public void setTable(String table) {
		this.table = table;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}


	/**
	* @see javax.servlet.jsp.tagext.Tag#doStartTag()
	*/
	public int doStartTag() throws PageException	{
		
		DataSourceManager manager = pageContext.getDataSourceManager();
		DatasourceConnection dc=manager.getConnection(pageContext,datasource, username, password);
		try {
			
			if(type==TYPE_TABLE_COLUMNS)			typeColumns(dc.getConnection().getMetaData());
			else if(type==TYPE_DBNAMES)		typeDBNames(dc.getConnection().getMetaData());
			else if(type==TYPE_FOREIGNKEYS)	typeForeignKeys(dc.getConnection().getMetaData());
			else if(type==TYPE_INDEX)		typeIndex(dc.getConnection().getMetaData());
			else if(type==TYPE_PROCEDURES)	typeProcedures(dc.getConnection().getMetaData());
			else if(type==TYPE_PROCEDURE_COLUMNS)typeProcedureColumns(dc.getConnection().getMetaData());
			else if(type==TYPE_TERMS)		typeTerms(dc.getConnection().getMetaData());
			else if(type==TYPE_TABLES)		typeTables(dc.getConnection().getMetaData());
			else if(type==TYPE_VERSION)		typeVersion(dc.getConnection().getMetaData());
			else if(type==TYPE_USERS)		typeUsers(dc.getConnection().getMetaData());
			
		}
		catch(SQLException sqle) {
			throw new DatabaseException(sqle,dc);
		}
		finally {
			manager.releaseConnection(pageContext,dc);
		}
		
		
								
			 
		return SKIP_BODY;
	}

	
	
	private void typeColumns(DatabaseMetaData metaData) throws PageException, SQLException {
		required("table",table);
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
		checkTable(metaData);
		
        ResultSet columns = metaData.getColumns(dbname, null, table, pattern);
        //ResultSet primaries = metaData.getPrimaryKeys(dbname, null, null);
        QueryPro qry = new QueryImpl(columns,"query");
        
		
		qry.rename(COLUMN_DEF,COLUMN_DEFAULT_VALUE);
        
		// add is primary
		int len=qry.getRecordcount();
		Map primaries = new HashMap();
		String tblName;
		Array isPrimary=new ArrayImpl();
		Set set;
		for(int i=1;i<=len;i++) {
			set=(Set) primaries.get(tblName=(String) qry.getAt(TABLE_NAME, i));
			if(set==null) {
				set=toSet(metaData.getPrimaryKeys(dbname, null, tblName),"COLUMN_NAME");
				primaries.put(tblName,set);
			}
			isPrimary.append(set.contains(qry.getAt(COLUMN_NAME, i))?"YES":"NO"); 
		}
		qry.addColumn(IS_PRIMARYKEY, isPrimary);
		
		// add is foreignkey
		Map foreigns = new HashMap();
		Array isForeign=new ArrayImpl();
		Array refPrim=new ArrayImpl();
		Array refPrimTbl=new ArrayImpl();
		Map map,inner;
		for(int i=1;i<=len;i++) {
			map=(Map) foreigns.get(tblName=(String) qry.getAt(TABLE_NAME, i));
			if(map==null) {
				map=toMap(metaData.getCrossReference(dbname, null,tblName, dbname, null, tblName),"FKCOLUMN_NAME",new String[]{"PKCOLUMN_NAME","PKTABLE_NAME"});
				foreigns.put(tblName, map);
			}
			inner=(Map) map.get(qry.getAt(COLUMN_NAME, i));
			if(inner!=null) {
				isForeign.append("YES");
				refPrim.append(inner.get("PKCOLUMN_NAME")); 
				refPrimTbl.append(inner.get("PKTABLE_NAME"));
			}
			else {
				isForeign.append("NO"); 
				refPrim.append("N/A"); 
				refPrimTbl.append("N/A");   	 
			}
		}
		qry.addColumn(IS_FOREIGNKEY, isForeign);
		qry.addColumn(REFERENCED_PRIMARYKEY, refPrim);
		qry.addColumn(REFERENCED_PRIMARYKEY_TABLE, refPrimTbl);
		
		
		qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private Map toMap(ResultSet result,String columnName,String[] additional) throws SQLException {
		HashMap map=new HashMap();
		HashMap inner;
		String col;
		SVArray item;
		while(result.next()){
			col=result.getString(columnName);
			inner=(HashMap) map.get(col);
			if(inner!=null) {
				for(int i=0;i<additional.length;i++) {
					item=(SVArray) inner.get(additional[i]);
					item.add(result.getString(additional[i]));
					item.setPosition(item.size());
				}
			}
			else {
				inner=new HashMap();
				map.put(col, inner);
				for(int i=0;i<additional.length;i++) {
					item=new SVArray();
					item.add(result.getString(additional[i]));
					inner.put(additional[i], item);
				}
			}
		}
		return map;
	}
	
	private Set toSet(ResultSet result,String columnName) throws SQLException {
		HashSet set = new HashSet();
		while(result.next()){
			set.add(result.getString(columnName)); 
		}
		return set;
	}

	private void typeDBNames(DatabaseMetaData metaData) throws PageException, SQLException {

		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
        railo.runtime.type.Query catalogs = new QueryImpl(metaData.getCatalogs(),"query");
        railo.runtime.type.Query scheme = new QueryImpl(metaData.getSchemas(),"query");
        
        Pattern p=null;
        if(pattern!=null && !"%".equals(pattern)) 
        	p=SQLUtil.pattern(pattern, true);
		
        
        
        String[] columns=new String[]{"database_name","type"};
		String[] types=new String[]{"VARCHAR","VARCHAR"};
		railo.runtime.type.Query qry=new QueryImpl(columns,types,0,"query");
		int row=1,len=catalogs.getRecordcount();
		String value;
		// catalog
		for(int i=1;i<=len;i++) {
			value=(String) catalogs.getAt("TABLE_CAT", i);
			if(!matchPattern(value,p)) continue;
			qry.addRow();
			qry.setAt("database_name", row, value);
			qry.setAt("type", row, "CATALOG");
			row++;
		}
		// scheme
		len=scheme.getRecordcount();
		for(int i=1;i<=len;i++) {
			value=(String) scheme.getAt("TABLE_SCHEM", i);
			if(!matchPattern(value,p)) continue;
			qry.addRow();
			qry.setAt("database_name", row, value);
			qry.setAt("type", row, "SCHEMA");
			row++;
		}
		
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void typeForeignKeys(DatabaseMetaData metaData) throws PageException, SQLException {
		required("table",table);
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
		checkTable(metaData);
		
		ResultSet columns = metaData.getCrossReference(dbname, null, table, null, null, null);// TODO ist das ok
		railo.runtime.type.Query qry = new QueryImpl(columns,"query");
		
		
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void checkTable(DatabaseMetaData metaData) throws SQLException, ApplicationException {
		ResultSet tables =null;
		try {
			tables = metaData.getTables(null, null, table, null);
			if(!tables.next()) throw new ApplicationException("there is no table that match the following pattern ["+table+"]");
		}
		finally {
			if(tables!=null) tables.close();
		}
	}

	private void typeIndex(DatabaseMetaData metaData) throws PageException, SQLException {
		required("table",table);
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
		checkTable(metaData);
		
        ResultSet tables = metaData.getIndexInfo(dbname, null, table, true, true);
        railo.runtime.type.Query qry = new QueryImpl(tables,"query");
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void typeProcedures(DatabaseMetaData metaData) throws SQLException, PageException {
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
        ResultSet tables = metaData.getProcedures(dbname, null, pattern);
        railo.runtime.type.Query qry = new QueryImpl(tables,"query");
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}
	
	private void typeProcedureColumns(DatabaseMetaData metaData) throws SQLException, PageException {
		required("procedure",procedure);
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
		
		ResultSet tables = metaData.getProcedureColumns(dbname, null, procedure, pattern);
		
		railo.runtime.type.Query qry = new QueryImpl(tables,"query");
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void typeTerms(DatabaseMetaData metaData) throws SQLException, PageException {
		Struct sct=new StructImpl();
		sct.setEL("procedure", metaData.getProcedureTerm());
		sct.setEL("catalog", metaData.getCatalogTerm());
		sct.setEL("schema", metaData.getSchemaTerm());
		
		pageContext.setVariable(name, sct);
	}

	private void typeTables(DatabaseMetaData metaData) throws PageException, SQLException {

		
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
        ResultSet tables = metaData.getTables(dbname, null, pattern, null);
        railo.runtime.type.Query qry = new QueryImpl(tables,"query");
        
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void typeVersion(DatabaseMetaData metaData) throws PageException, SQLException {

		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();

		String dbproductname="DATABASE_PRODUCTNAME";
		String dbversion="DATABASE_VERSION";
		String drname="DRIVER_NAME";
		String drversion="DRIVER_VERSION";
		String major="JDBC_MAJOR_VERSION";
		String minor="JDBC_MINOR_VERSION";
		
		String[] columns=new String[]{dbproductname,dbversion,drname,drversion,major,minor};
		String[] types=new String[]{"VARCHAR","VARCHAR","VARCHAR","VARCHAR","DOUBLE","DOUBLE"};
		
		railo.runtime.type.Query qry=new QueryImpl(columns,types,1,"query");

		qry.setAt(dbproductname,1,metaData.getDatabaseProductName());
		qry.setAt(dbversion,1,metaData.getDatabaseProductVersion());
		qry.setAt(drname,1,metaData.getDriverName());
		qry.setAt(drversion,1,metaData.getDriverVersion());
		qry.setAt(major,1,new Double(metaData.getJDBCMajorVersion()));
		qry.setAt(minor,1,new Double(metaData.getJDBCMinorVersion()));
		
		
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}
	
	private void typeUsers(DatabaseMetaData metaData) throws PageException, SQLException {
		
		Stopwatch stopwatch=new Stopwatch();
		stopwatch.start();
        
		checkTable(metaData);
		ResultSet result = metaData.getSchemas();
		QueryPro qry = new QueryImpl(result,"query");
		
		
		qry.rename(TABLE_SCHEM,USER);
        
        qry.setExecutionTime(stopwatch.time());
        
        
		pageContext.setVariable(name, qry);
	}

	private void required(String name, String value) throws ApplicationException {
		if(value==null)
			throw new ApplicationException("Missing attribute ["+name+"]. The type ["+strType+"] requires the attribute [" + name + "].");
	}

	private static  boolean matchPattern(String value, Pattern pattern) {
		if(pattern==null) return true;
		return SQLUtil.match(pattern, value);
	}

	/**
	* @see javax.servlet.jsp.tagext.Tag#doEndTag()
	*/
	public int doEndTag()	{
		return EVAL_PAGE;
	}

}