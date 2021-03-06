package railo.runtime.tag;


import java.sql.Connection;

import javax.servlet.jsp.JspException;

import railo.runtime.db.DataSourceManager;
import railo.runtime.db.DatasourceManagerImpl;
import railo.runtime.exp.DatabaseException;
import railo.runtime.exp.PageException;
import railo.runtime.ext.tag.BodyTagTryCatchFinallyImpl;

/**
 * Transaction class
 */
public final class Transaction extends BodyTagTryCatchFinallyImpl {
        
    private static final int ACTION_NONE = 0;

	private static final int ACTION_BEGIN = 1;

	private static final int ACTION_COMMIT = 2;

	private static final int ACTION_ROLLBACK = 4;

	private static final int ACTION_SET_SAVEPOINT = 8;
    
	//private boolean hasBody;
    private int isolation=Connection.TRANSACTION_NONE;
    private int action=ACTION_NONE;
    private boolean innerTag=false;
    
    /**
     * @see railo.runtime.ext.tag.BodyTagImpl#release()
     */
    public void release() {
        //hasBody=false;
        isolation=Connection.TRANSACTION_NONE;
        action=ACTION_NONE;
        innerTag=false;
        super.release();
    }

    /**
     * @param action The action to set.
     * @throws DatabaseException 
     */
    public void setAction(String strAction) throws DatabaseException {
    	strAction = strAction.trim().toLowerCase();
    	if(strAction.equals("begin")) action=ACTION_BEGIN;
        else if(strAction.equals("commit")) action=ACTION_COMMIT;
        else if(strAction.equals("rollback")) action=ACTION_ROLLBACK;
        else if(strAction.equals("setsavepoint")) action=ACTION_SET_SAVEPOINT;
        else {
            throw new DatabaseException("attribute action has a invalid value, valid values are [begin,commit,setsavepoint and rollback]",null,null,null);
        }
    	
    	
    }

    /**
     * @param isolation The isolation to set.
     * @throws DatabaseException 
     */
    public void setIsolation(String isolation) throws DatabaseException {
        isolation=isolation.trim().toLowerCase();
        if(isolation.equals("read_uncommitted"))    this.isolation=Connection.TRANSACTION_READ_UNCOMMITTED;
        else if(isolation.equals("read_committed")) this.isolation=Connection.TRANSACTION_READ_COMMITTED;
        else if(isolation.equals("repeatable_read"))this.isolation=Connection.TRANSACTION_REPEATABLE_READ;
        else if(isolation.equals("serializable"))   this.isolation=Connection.TRANSACTION_SERIALIZABLE;
        else if(isolation.equals("none"))           this.isolation=Connection.TRANSACTION_NONE;
        else throw new DatabaseException("transaction has a invalid isolation level (attribute isolation, valid values are [read_uncommitted,read_committed,repeatable_read,serializable])",null,null,null);
    }

    /**
     * @see railo.runtime.ext.tag.TagImpl#doStartTag()
     */
    public int doStartTag() throws PageException {
    	DataSourceManager manager = pageContext.getDataSourceManager();
        // first transaction
        if(manager.isAutoCommit()) {
            //if(!hasBody)throw new DatabaseException("transaction tag with no end Tag can only be used inside a transaction tag",null,null,null);
            manager.begin(isolation);
            return EVAL_BODY_INCLUDE;
        }
        // inside transaction
        innerTag=true;
        switch(action){
        case ACTION_NONE:
        	throw new DatabaseException("you can't have a nested transaction with no action defined",null,null,null);
        case ACTION_BEGIN:
            throw new DatabaseException("you can't start a transaction inside a transaction tag",null,null,null);
        case ACTION_COMMIT:
        	manager.commit();
        break;
        case ACTION_ROLLBACK:
        	manager.rollback();
        break;
        case ACTION_SET_SAVEPOINT:
        	 ((DatasourceManagerImpl)manager).savepoint();
        break;
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    /**
     * @see railo.runtime.ext.tag.BodyTagTryCatchFinallyImpl#doCatch(java.lang.Throwable)
     */
    public void doCatch(Throwable t) throws Throwable {
    	
        DataSourceManager manager = pageContext.getDataSourceManager();
        if(innerTag) throw t;
        try {
            manager.rollback();
        } catch (DatabaseException e) {
        	//print.printST(e);
        }
        throw t;
    }

    /**
     * @param hasBody
     */
    public void hasBody(boolean hasBody) {//print.out("hasBody"+hasBody);
        //this.hasBody=hasBody;
    }

    /**
     * @see railo.runtime.ext.tag.BodyTagTryCatchFinallyImpl#doFinally()
     */
    public void doFinally() {
        DataSourceManager manager = pageContext.getDataSourceManager();
        if(!innerTag) {
            manager.end();
        }
        super.doFinally();
    }

    /**
     * @see railo.runtime.ext.tag.BodyTagImpl#doAfterBody()
     */
    public int doAfterBody() throws JspException {
        DataSourceManager manager = pageContext.getDataSourceManager();
        if(!innerTag) {
            manager.commit();
        }
        return super.doAfterBody();
    }
}