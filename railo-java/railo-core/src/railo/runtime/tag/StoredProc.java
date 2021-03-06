package railo.runtime.tag;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;

import railo.commons.io.IOUtil;
import railo.commons.lang.StringUtil;
import railo.commons.sql.SQLUtil;
import railo.runtime.db.CFTypes;
import railo.runtime.db.DataSourceImpl;
import railo.runtime.db.DataSourceManager;
import railo.runtime.db.DatasourceConnection;
import railo.runtime.db.ProcMeta;
import railo.runtime.db.ProcMetaCollection;
import railo.runtime.db.SQLCaster;
import railo.runtime.db.SQLImpl;
import railo.runtime.db.SQLItemImpl;
import railo.runtime.debug.Debugger;
import railo.runtime.exp.DatabaseException;
import railo.runtime.exp.PageException;
import railo.runtime.ext.tag.BodyTagTryCatchFinallySupport;
import railo.runtime.op.Caster;
import railo.runtime.tag.util.DeprecatedUtil;
import railo.runtime.type.Array;
import railo.runtime.type.ArrayImpl;
import railo.runtime.type.Collection.Key;
import railo.runtime.type.KeyImpl;
import railo.runtime.type.QueryImpl;
import railo.runtime.type.Struct;
import railo.runtime.type.StructImpl;
import railo.runtime.type.dt.DateTime;
import railo.runtime.type.dt.DateTimeImpl;
import railo.runtime.type.dt.TimeSpan;






public class StoredProc extends BodyTagTryCatchFinallySupport {
	private static final int PROCEDURE_CAT=1;        
	private static final int PROCEDURE_SCHEM=2;
	private static final int PROCEDURE_NAME=3;
	private static final int COLUMN_NAME=4;
	private static final int COLUMN_TYPE=5;
	private static final int DATA_TYPE=6;
	private static final int TYPE_NAME=7;
	//|PRECISION|LENGTH|SCALE|RADIX|NULLABLE|REMARKS|SEQUENCE|OVERLOAD|DEFAULT_VALUE
	

	private static final railo.runtime.type.Collection.Key KEY_SC = KeyImpl.getInstance("StatusCode");
	
	private static final railo.runtime.type.Collection.Key COUNT = KeyImpl.getInstance("count_afsdsfgdfgdsfsdfsgsdgsgsdgsasegfwef");
	// TODO attr debug
	private static final ProcParamBean STATUS_CODE;
	
	static{
		STATUS_CODE = new ProcParamBean();
		STATUS_CODE.setType(Types.INTEGER);
		STATUS_CODE.setDirection(ProcParamBean.DIRECTION_OUT);
		STATUS_CODE.setVariable("cfstoredproc.statusCode");
	}
	
	
	private List params=new ArrayList();
	private Array results=new ArrayImpl();

	private String procedure;
	private String datasource;
	private String username;
	private String password;
	private int blockfactor=-1;
	private int timeout=-1;
	private boolean debug=true;
	private boolean returncode;
	private String result="cfstoredproc";
	
	private boolean clearCache;
	private DateTimeImpl cachedbefore;
	private String cachename="";
	private DateTime cachedafter;
	private ProcParamBean returnValue=null;
	//private Map<String,ProcMetaCollection> procedureColumnCache;
	
	public void release() {
		params.clear();
		results.clear();
		returnValue=null;
		procedure=null;
		datasource=null;
		username=null;
		password=null;
		blockfactor=-1;
		timeout=-1;
		debug=true;
		returncode=false;
		result="cfstoredproc";
		

		clearCache=false;
		cachedbefore=null;
		cachedafter=null;
		cachename="";
		
		super.release();
	}
	
	


	/** set the value cachedafter
	*  This is the age of which the query data can be
	* @param cachedafter value to set
	**/
	public void setCachedafter(DateTime cachedafter)	{
		//railo.print.ln("cachedafter:"+cachedafter);
		this.cachedafter=cachedafter;
	}

	/** set the value cachename
	*  This is specific to JTags, and allows you to give the cache a specific name
	* @param cachename value to set
	**/
	public void setCachename(String cachename)	{
		this.cachename=cachename;
	}

	/** set the value cachedwithin
	*  
	* @param cachedwithin value to set
	**/
	public void setCachedwithin(TimeSpan cachedwithin)	{
		if(cachedwithin.getMillis()>0)
			this.cachedbefore=new DateTimeImpl(pageContext,System.currentTimeMillis()+cachedwithin.getMillis(),false);
		else clearCache=true;
	}

	/**
	 * @param blockfactor The blockfactor to set.
	 */
	public void setBlockfactor(double blockfactor) {
		this.blockfactor = (int) blockfactor;
	}
	
	/**
	 * @param blockfactor
	 * @deprecated replaced with setBlockfactor(double)
	 */
	public void setBlockfactor(int blockfactor) {
		DeprecatedUtil.tagAttribute(pageContext,"storedproc","blockfactor");
		this.blockfactor = blockfactor;
	}

	/**
	 * @param datasource The datasource to set.
	 */
	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	/**
	 * @param username The username to set.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @param password The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @param debug The debug to set.
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * @param procedure The procedure to set.
	 */
	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	/**
	 * @param result The result to set.
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * @param returncode The returncode to set.
	 */
	public void setReturncode(boolean returncode) {
		this.returncode = returncode;
	}

	/**
	 * @param dbvarname the dbvarname to set
	 */
	public void setDbvarname(String dbvarname) {
		DeprecatedUtil.tagAttribute(pageContext,"storedproc","dbvarname");
	}
	public void setDbtype(String dbtype) {
		DeprecatedUtil.tagAttribute(pageContext,"storedproc","dbtype");
	}

	public void addProcParam(ProcParamBean param) {
		params.add(param);
	}

	public void addProcResult(ProcResultBean result) {
		results.setEL(result.getResultset(),result);
	}

	/**
	 * @see javax.servlet.jsp.tagext.Tag#doStartTag()
	 */
	public int doStartTag() throws JspException {
		
		return EVAL_BODY_INCLUDE;
	}

	private void returnValue(DatasourceConnection dc) throws PageException {
		Connection conn = dc.getConnection();
		
		
		if(SQLUtil.isOracle(conn)){
			String name=this.procedure.toUpperCase();
			int index=name.lastIndexOf('.');
			
			String pack=null;
			if(index!=-1){
				pack=name.substring(0,index);
				name=name.substring(index+1);
			}
			
			try {
				DatabaseMetaData md = conn.getMetaData();
				
				//if(procedureColumnCache==null)procedureColumnCache=new ReferenceMap();
				//ProcMetaCollection coll=procedureColumnCache.get(procedure);
				DataSourceImpl d = ((DataSourceImpl)dc.getDatasource());
				long cacheTimeout = d.getMetaCacheTimeout();
				Map<String, ProcMetaCollection> procedureColumnCache = d.getProcedureColumnCache();
				ProcMetaCollection coll=procedureColumnCache.get(procedure);
				
				
				
				if(coll==null || (cacheTimeout>=0 && (coll.created+cacheTimeout)<System.currentTimeMillis())) {
					ResultSet res = md.getProcedureColumns(pack, null, name, null);
					coll=createProcMetaCollection(res);
					procedureColumnCache.put(procedure,coll);
				}
				index=-1;
				//int inOutCount=0;
				for(int i=0;i<coll.metas.length;i++) { 
					index++;
					
					// Return
					if(coll.metas[i].columnType==DatabaseMetaData.procedureColumnReturn) {
						index--;
						ProcResultBean result= getFirstResult();
						ProcParamBean param = new ProcParamBean();
						
						param.setType(coll.metas[i].dataType);
						param.setDirection(ProcParamBean.DIRECTION_OUT);
						if(result!=null)param.setVariable(result.getName());
						returnValue=param;
						
					}	
					else if(coll.metas[i].columnType==DatabaseMetaData.procedureColumnOut || coll.metas[i].columnType==DatabaseMetaData.procedureColumnInOut) {
						//inOutCount++;
						if(coll.metas[i].dataType==CFTypes.CURSOR){
							ProcResultBean result= getFirstResult();
							
							ProcParamBean param = new ProcParamBean();
							param.setType(coll.metas[i].dataType);
							param.setDirection(ProcParamBean.DIRECTION_OUT);
							if(result!=null)param.setVariable(result.getName());
							params.add(index, param);
						}
						else {								
							ProcParamBean param=(ProcParamBean) params.get(index);
							if(coll.metas[i].dataType!=Types.OTHER && coll.metas[i].dataType!=param.getType()){
								param.setType(coll.metas[i].dataType);
							}
						}
					}	
					else if(coll.metas[i].columnType==DatabaseMetaData.procedureColumnIn) {	
						//inOutCount++;
						ProcParamBean param=(ProcParamBean) params.get(index);
						if(coll.metas[i].dataType!=Types.OTHER && coll.metas[i].dataType!=param.getType()){
							param.setType(coll.metas[i].dataType);
						}
					}	
				}
				contractTo(params,index+1);
				
				//if(res!=null)print.out(new QueryImpl(res,"columns").toString());
			} 
			catch (SQLException e) {
			    throw new DatabaseException(e,dc);
			}
			
			
		}
		
		// return code
		if(returncode) {
			returnValue=STATUS_CODE;
		}
	}

	private void contractTo(List params, int paramCount) {
		if(params.size()>paramCount){
			for(int i=params.size()-1;i>=paramCount;i--){
				params.remove(i);
			}
		}
	}




	private ProcMetaCollection createProcMetaCollection(ResultSet res) throws SQLException {
		//print.out(new QueryImpl(res,"qry"));
		ArrayList<ProcMeta> list=new ArrayList<ProcMeta>();
		while(res.next()) {
			list.add(new ProcMeta(res.getInt(COLUMN_TYPE),getDataType(res)));
		}
		return new ProcMetaCollection(list.toArray(new ProcMeta[list.size()]));
	}




	private int getDataType(ResultSet res) throws SQLException {
		int dataType=res.getInt(DATA_TYPE);	
		if(dataType==Types.OTHER) {
			String  strDataType= res.getString(TYPE_NAME);
			if("REF CURSOR".equalsIgnoreCase(strDataType))dataType=CFTypes.CURSOR;
			if("CLOB".equalsIgnoreCase(strDataType))dataType=Types.CLOB;
			if("BLOB".equalsIgnoreCase(strDataType))dataType=Types.BLOB;
		}
		return dataType;
	}




	private ProcResultBean getFirstResult() {
		Key[] keys = results.keys();
		if(keys.length==0) return null;
			
		return (ProcResultBean) results.removeEL(keys[0]);
	}

	/**
	 * @see railo.runtime.ext.tag.TagSupport#doEndTag()
	 */
	public int doEndTag() throws JspException {
		long start=System.currentTimeMillis();

	    Struct res=new StructImpl();
		DataSourceManager manager = pageContext.getDataSourceManager();
		DatasourceConnection dc = manager.getConnection(pageContext,datasource,username,password);
		
		// create returnValue 
		long s=System.currentTimeMillis();
		returnValue(dc);
		
		// create SQL 
		StringBuffer sql=createSQL();
		

		// add returnValue to params
		if(returnValue!=null){
			params.add(0,returnValue);
		}
		
		SQLImpl _sql=new SQLImpl(sql.toString());
		CallableStatement callStat=null;
		try {
		    callStat = dc.getConnection().prepareCall(sql.toString());
		    		//ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
    				//ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY); 
		    
		    if(blockfactor>0)callStat.setFetchSize(blockfactor);
		    if(timeout>0)callStat.setQueryTimeout(timeout);
		    
	// set IN register OUT
		    Iterator it = params.iterator();
			ProcParamBean param;
			int index=1;
		    while(it.hasNext()) {
		    	param=(ProcParamBean) it.next();
		    	param.setIndex(index);
		    	_sql.addItems(new SQLItemImpl(param.getValue()));
	    		if(param.getDirection()!=ProcParamBean.DIRECTION_OUT) {
	    			SQLCaster.setValue(callStat, index, param);
		    	}
		    	if(param.getDirection()!=ProcParamBean.DIRECTION_IN) {
		    		registerOutParameter(callStat,param);
		    	}
		    	index++;
			}
		    
	// cache
		    boolean isFromCache=false;
		    boolean hasCached=cachedbefore!=null || cachedafter!=null;
		    Object cacheValue=null;
			if(clearCache) {
				hasCached=false;
				pageContext.getQueryCache().remove(_sql,datasource,username,password);
			}
			else if(hasCached) {
				cacheValue = pageContext.getQueryCache().get(_sql,datasource,username,password,cachedafter);
			}
			int count=0;
			if(cacheValue==null){
				// execute
				boolean isResult=callStat.execute();
				
			    Struct cache=hasCached?new StructImpl():null;
	
			    // resultsets
			    ProcResultBean result;
			    
			    index=1;
				do {
			    	if(isResult){
			    		ResultSet rs=callStat.getResultSet();
			    		if(rs!=null) {
							try{
								result=(ProcResultBean) results.get(index++,null);
								if(result!=null) {
									railo.runtime.type.Query q = new QueryImpl(rs,result.getMaxrows(),result.getName());	
									count+=q.getRecordcount();
									setVariable(result.getName(), q);
									if(hasCached)cache.set(result.getName(), q);
								}
							}
							finally{
								IOUtil.closeEL(rs);
							}
						}
			    	}
			    }
			    while((isResult=callStat.getMoreResults()) || (callStat.getUpdateCount() != -1));

	
			    // params
			    it = params.iterator();
			    while(it.hasNext()) {
			    	param=(ProcParamBean) it.next();
			    	if(param.getDirection()!=ProcParamBean.DIRECTION_IN){
			    		Object value;
			    		if(!StringUtil.isEmpty(param.getVariable())){
			    			value=emptyIfNull(SQLCaster.toCFType(callStat.getObject(param.getIndex())));
			    			if(param==STATUS_CODE){
			    				
			    				res.set("StatusCode", value);
			    			}
			    			else setVariable(param.getVariable(), value);
			    			if(hasCached)cache.set(param.getVariable(), value);
			    		}
			    	}
				}
			    if(hasCached){
			    	cache.set(COUNT, Caster.toDouble(count));
			    	pageContext.getQueryCache().set(_sql,datasource,username,password,cache,cachedbefore);
			    }
			    
			}
			else if(cacheValue instanceof Struct) {
				Struct sctCache = (Struct) cacheValue;
				count=Caster.toIntValue(sctCache.removeEL(COUNT),0);
				
				
				Key[] keys = sctCache.keys();
				for(int i=0;i<keys.length;i++){
					if(STATUS_CODE.getVariable().equals(keys[i].getString()))
						res.set(KEY_SC, sctCache.get(keys[i]));
					else setVariable(keys[i].getString(), sctCache.get(keys[i]));
				}
				isFromCache=true;
			}
			
		    // result
		    long exe;
		    
		    setVariable(this.result, res);
		    res.set(QueryImpl.EXECUTION_TIME,Caster.toDouble(exe=(System.currentTimeMillis()-start)));
		    res.set(QueryImpl.CACHED,Caster.toBoolean(isFromCache));
		    
		    if(pageContext.getConfig().debug() && debug) {
		    	Debugger debugger = pageContext.getDebugger();
				debugger.addQueryExecutionTime(datasource,procedure,_sql,count,pageContext.getCurrentPageSource(),(int)exe);
			}
		    
		    
		}
		catch (SQLException e) {
		    throw new DatabaseException(e,new SQLImpl(sql.toString()),dc);
		}
		finally {
		    if(callStat!=null){
			    try {
					callStat.close();
				} catch (SQLException e) {}
		    }
		    manager.releaseConnection(pageContext,dc);
		}
		return EVAL_PAGE;
	}

	private void setVariable(String name, Object value) throws PageException {
		pageContext.setVariable(name, value);
	}




	private StringBuffer createSQL() {
		StringBuffer sql=new StringBuffer();
		if(returnValue!=null)sql.append("{? = call ");
		else sql.append("{ call ");
		sql.append(procedure);
		sql.append('(');
		int incount=params.size();
		
		for(int i=0;i<incount;i++) {
			if(i==0)sql.append('?');
			else sql.append(",?");
		}
		sql.append(") }");
		return sql;
		
	}




	private Object emptyIfNull(Object object) {
		if(object==null)return "";
		return object;
	}

	private void registerOutParameter(CallableStatement proc, ProcParamBean param) throws SQLException {
		if(param.getScale()==-1)proc.registerOutParameter(param.getIndex(),param.getType());
		else proc.registerOutParameter(param.getIndex(),param.getType(),param.getScale());
	}

	/**
	 * @param b
	 */
	public void hasBody(boolean b) {
		
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	
}


