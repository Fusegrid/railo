<cfset error.message="">
<cfset error.detail="">
<cfif structKeyExists(session,"passwordserver")>
	<cfdirectory 
		directory="#expandPath('dbdriver')#" 
		action="list" name="dbdriver" filter="*.cfc" serverpassword="#session.passwordserver#"><!---  --->
<cfelse>
	<cftry>
		<cfdirectory 
			directory="#expandPath('dbdriver')#" 
			action="list" name="dbdriver" filter="*.cfc">
		<cfcatch type="security">
			<cfadmin 
				action="getDatasourceDriverList"
				type="#request.adminType#"
				password="#session["password"&request.adminType]#"
				returnVariable="dbdriver">			
		</cfcatch>		
	</cftry>
</cfif>
<cfset drivers=struct()>
<cfoutput query="dbdriver">
	<cfset n=listFirst(dbdriver.name,".")>
	<cfif n NEQ "Driver" and n NEQ "IDriver">
		<cfset drivers[n]=createObject("component","dbdriver."&n)>
	</cfif>
</cfoutput>

<cffunction name="getTypeName">
	<cfargument name="className" required="true">
	<cfargument name="dsn" required="true">
	<cfset var key="">
    
	<cfloop collection="#drivers#" item="key">
		<cfif drivers[key].equals(arguments.className,arguments.dsn)>
			<cfreturn drivers[key].getName()>
		</cfif>
	</cfloop>
    
    <cfreturn drivers['other'].getName()>
</cffunction>

<cffunction name="getType">
	<cfargument name="className" required="true">
	<cfargument name="dsn" required="true">
	<cfset var key="">
	<cfloop collection="#drivers#" item="key">
		<cfif drivers[key].equals(arguments.className,arguments.dsn)>
			<cfreturn key>
		</cfif>
	</cfloop>
	<cfreturn "other">
</cffunction>

<!--- 
Defaults --->
<cfparam name="url.action2" default="list">
<cfparam name="form.mainAction" default="none">
<cfparam name="form.subAction" default="none">
<cfadmin 
	action="securityManager"
	type="#request.adminType#"
	password="#session["password"&request.adminType]#"
	returnVariable="access"
	secType="datasource">
	
<cfif access EQ "yes">
	<cfset access=-1>	
<cfelseif access EQ "none" or access EQ "no">
	<cfset access=0>
</cfif>
	
	
<cfswitch expression="#url.action2#">
	<cfcase value="list"><cfinclude template="services.datasource.list.cfm"/></cfcase>
	<cfcase value="create"><cfinclude template="services.datasource.create.cfm"/></cfcase>

</cfswitch>