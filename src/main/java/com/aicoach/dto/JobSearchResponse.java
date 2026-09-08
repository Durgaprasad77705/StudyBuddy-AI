package com.aicoach.dto;
import java.util.List;
import java.util.Map;
public class JobSearchResponse {
 private boolean success; private String query; private String location; private List<Map<String,Object>> jobs; private List<Map<String,String>> resources; private String sourceNote;
 public JobSearchResponse(){} public JobSearchResponse(boolean s,String q,String l,List<Map<String,Object>> j,List<Map<String,String>> r,String n){success=s;query=q;location=l;jobs=j;resources=r;sourceNote=n;}
 public boolean isSuccess(){return success;} public void setSuccess(boolean v){success=v;} public String getQuery(){return query;} public void setQuery(String v){query=v;} public String getLocation(){return location;} public void setLocation(String v){location=v;} public List<Map<String,Object>> getJobs(){return jobs;} public void setJobs(List<Map<String,Object>> v){jobs=v;} public List<Map<String,String>> getResources(){return resources;} public void setResources(List<Map<String,String>> v){resources=v;} public String getSourceNote(){return sourceNote;} public void setSourceNote(String v){sourceNote=v;}
}
