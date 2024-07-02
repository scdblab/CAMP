package com.meetup.memcached;

import java.util.Vector;

public class QueryRegistration
{
	private Vector<String> triggers;
	private Vector<String> internal_tokens;
	public String query;
	public String exec_query;
	public Vector<Integer> param_list;
	private boolean is_template;
	
	public QueryRegistration( String query, boolean isPreparedStatement )
	{
		this.is_template = false;
		this.query = query;
		
		/* Disabled for now until prepared statements are added to PageGen
		if( isPreparedStatement )
		{
			this.param_list = new Vector<String>();
			
			// Process the prepared statement to figure out how many parameters there are
			int num_params = query.split("?").length;
			for( int i = 0; i < num_params; i++ )
			{
				this.param_list.add("NULL");
			}
		}
		else
		//*/
		{
			this.param_list = null;
		}
		
		init();
	}
	
	public QueryRegistration( String template_query, String execution_query, Integer param_id )
	{
		this.query = template_query;
		this.exec_query = execution_query;
		this.is_template = true;
		
		// Keep track of id related to this registration 
		this.param_list = new Vector<Integer>();			
		this.param_list.add(param_id);		
		init();
	}
	
	private void init()
	{
		this.triggers = new Vector<String>();
		this.internal_tokens = new Vector<String>();
	}
	
	public boolean isPreparedStatement()
	{
		return this.param_list == null;
	}
	
	public boolean isTemplate()
	{		
		return this.is_template;
	}
	
	public int getTemplateID()
	{
		return this.param_list.lastElement();
	}

	public Vector<String>  getTriggers() {
		return triggers;
	}

	public void addTrigger(String trigger) {
		this.triggers.add(trigger);
	}

	public Vector<String>  getInternal_tokens() {
		return internal_tokens;
	}

	public void addInternalToken(String internal_token) {
		this.internal_tokens.add(internal_token);
	}
}
