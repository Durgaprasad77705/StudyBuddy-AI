package com.aicoach.service;

import com.aicoach.dto.JobSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class JobsService {
    private final OllamaService ollamaService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public JobsService(OllamaService ollamaService) { this.ollamaService = ollamaService; }

    public String analyzeJob(String resumeText, String jobDescription) {
        String prompt = """
                You are an expert technical recruiter.
                CANDIDATE RESUME:\n%s
                JOB DESCRIPTION:\n%s
                Return Match Score /100, Matching Skills, Missing Skills, Education Match, Project Match, Experience Match, Technical Skill Match, Important Keywords, Interview Topics, Recommended Preparation, Strengths, Weaknesses and Final Recommendation. Do not invent information.
                """.formatted(resumeText, jobDescription);
        return ollamaService.askAI(prompt);
    }

    public JobSearchResponse liveSearch(String query, String location, String experience) {
        String q = (query == null || query.isBlank()) ? "software developer" : query.trim();
        String loc = location == null ? "" : location.trim();
        List<Map<String,Object>> jobs = new ArrayList<>();

        // Use multiple public feeds so a Java/Python/any-role search does not return the same small set of companies.
        fetchRemotive(q, loc, jobs);
        fetchJobicy(q, loc, jobs);
        fetchArbeitnow(q, loc, jobs);

        // De-duplicate by title + company and keep the newest listings first.
        Map<String, Map<String,Object>> unique = new LinkedHashMap<>();
        for (Map<String,Object> job : jobs) {
            String key = (String.valueOf(job.get("title")) + "|" + String.valueOf(job.get("company"))).toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, job);
        }
        jobs = new ArrayList<>(unique.values());
        jobs.sort((a,b) -> String.valueOf(b.getOrDefault("published",""))
                .compareTo(String.valueOf(a.getOrDefault("published",""))));
        if (jobs.size() > 20) jobs = new ArrayList<>(jobs.subList(0,20));

        if (jobs.isEmpty()) {
            Map<String,Object> fallback = new LinkedHashMap<>();
            fallback.put("title", q + " live job search");
            fallback.put("company", "LinkedIn Job Search");
            fallback.put("location", loc.isBlank() ? "All locations" : loc);
            fallback.put("type", experience == null ? "" : experience);
            fallback.put("url", linkedinUrl(q, loc));
            fallback.put("published", Instant.now().toString());
            fallback.put("description", "Open the live LinkedIn search to view current openings for this role.");
            fallback.put("descriptionLines", descriptionLines(String.valueOf(fallback.get("description")), q, "LinkedIn", String.valueOf(fallback.get("location")), String.valueOf(fallback.get("type"))));
            jobs.add(fallback);
        }

        List<Map<String,String>> resources = resources(q, loc);
        return new JobSearchResponse(true, q, loc, jobs, resources,
                "Live job discovery combines public feeds (Remotive, Jobicy and Arbeitnow) and provides a direct LinkedIn live-search link. Listing freshness depends on each source.");
    }

    private void fetchRemotive(String q, String loc, List<Map<String,Object>> jobs) {
        try {
            String url = "https://remotive.com/api/remote-jobs?search=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
            JsonNode arr = mapper.readTree(restTemplate.getForObject(url, String.class)).path("jobs");
            for (JsonNode n : arr) {
                if (jobs.size() >= 30) break;
                String title=n.path("title").asText("");
                String company=n.path("company_name").asText("");
                String jobLoc=n.path("candidate_required_location").asText("Remote");
                if (!matchesLocation(jobLoc, loc)) continue;
                addJob(jobs,title,company,jobLoc,n.path("job_type").asText(""),n.path("url").asText("https://remotive.com/remote-jobs"),n.path("publication_date").asText(""),strip(n.path("description").asText("")),"Remotive");
            }
        } catch(Exception ignored) {}
    }

    private void fetchJobicy(String q, String loc, List<Map<String,Object>> jobs) {
        try {
            String url="https://jobicy.com/api/v2/remote-jobs?count=30&tag="+URLEncoder.encode(q,StandardCharsets.UTF_8);
            JsonNode arr=mapper.readTree(restTemplate.getForObject(url,String.class)).path("jobs");
            for(JsonNode n:arr){
                String title=n.path("jobTitle").asText("");
                String company=n.path("companyName").asText("");
                String jobLoc=n.path("jobGeo").asText("Anywhere");
                if(!matchesLocation(jobLoc,loc)) continue;
                String type=n.path("jobType").isArray()?joinArray(n.path("jobType")):n.path("jobType").asText("");
                String desc=strip(n.path("jobDescription").asText(n.path("jobExcerpt").asText("")));
                addJob(jobs,title,company,jobLoc,type,n.path("url").asText("https://jobicy.com/jobs"),n.path("pubDate").asText(""),desc,"Jobicy");
            }
        }catch(Exception ignored){}
    }

    private void fetchArbeitnow(String q, String loc, List<Map<String,Object>> jobs) {
        try {
            String url="https://www.arbeitnow.com/api/job-board-api";
            JsonNode arr=mapper.readTree(restTemplate.getForObject(url,String.class)).path("data");
            String[] terms=q.toLowerCase(Locale.ROOT).split("\\s+");
            for(JsonNode n:arr){
                String title=n.path("title").asText("");
                String company=n.path("company_name").asText("");
                String desc=strip(n.path("description").asText(""));
                String hay=(title+" "+company+" "+desc).toLowerCase(Locale.ROOT);
                boolean match=true;
                for(String term:terms){ if(term.length()>2 && !hay.contains(term)){match=false;break;} }
                if(!match) continue;
                String jobLoc=n.path("location").asText(n.path("company_location").asText(""));
                if(!matchesLocation(jobLoc,loc)) continue;
                addJob(jobs,title,company,jobLoc,n.path("job_types").toString(),n.path("url").asText("https://www.arbeitnow.com/"),n.path("created_at").asText(""),desc,"Arbeitnow");
                if(jobs.size()>=40) break;
            }
        }catch(Exception ignored){}
    }

    private void addJob(List<Map<String,Object>> jobs,String title,String company,String location,String type,String url,String published,String description,String source){
        if(title.isBlank() && company.isBlank()) return;
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("title",title);m.put("company",company);m.put("location",location);m.put("type",type);m.put("url",url);m.put("published",published);m.put("source",source);m.put("description",description);m.put("descriptionLines",descriptionLines(description,title,company,location,type));
        jobs.add(m);
    }

    private boolean matchesLocation(String jobLoc,String wanted){
        if(wanted==null || wanted.isBlank()) return true;
        String a=jobLoc==null?"":jobLoc.toLowerCase(Locale.ROOT);
        String b=wanted.toLowerCase(Locale.ROOT);
        return a.contains(b) || a.contains("remote") || a.contains("anywhere") || a.contains("worldwide");
    }

    private String joinArray(JsonNode n){ List<String> x=new ArrayList<>();n.forEach(v->x.add(v.asText("")));return String.join(", ",x); }

    private List<Map<String,String>> resources(String q,String loc){
        String enc=URLEncoder.encode(q,StandardCharsets.UTF_8);
        String location=loc==null?"":loc.trim();
        List<Map<String,String>> r=new ArrayList<>();
        r.add(res("LinkedIn Live Jobs",linkedinUrl(q,location),"Live Jobs"));
        r.add(res("Indeed Live Jobs","https://www.indeed.com/jobs?q="+enc+(location.isBlank()?"":"&l="+URLEncoder.encode(location,StandardCharsets.UTF_8)),"Jobs"));
        r.add(res("Jobicy Live Feed","https://jobicy.com/api/v2/remote-jobs?count=20&tag="+enc,"Live Jobs"));
        r.add(res("YouTube Interview Videos","https://www.youtube.com/results?search_query="+URLEncoder.encode(q+" interview preparation",StandardCharsets.UTF_8),"Video"));
        r.add(res("PDF Guides","https://www.google.com/search?q="+URLEncoder.encode(q+" interview preparation filetype:pdf",StandardCharsets.UTF_8),"PDF"));
        r.add(res("LeetCode Practice","https://leetcode.com/problemset/?search="+enc,"Coding"));
        return r;
    }

    private String linkedinUrl(String q,String loc){return "https://www.linkedin.com/jobs/search/?keywords="+URLEncoder.encode(q,StandardCharsets.UTF_8)+(loc==null||loc.isBlank()?"":"&location="+URLEncoder.encode(loc,StandardCharsets.UTF_8));}
    private Map<String,String> res(String title,String url,String type){Map<String,String> m=new LinkedHashMap<>();m.put("title",title);m.put("url",url);m.put("type",type);return m;}

    private List<String> descriptionLines(String description,String title,String company,String location,String type){
        String base=description==null?"":description.replaceAll("\\s+"," ").trim();
        List<String> parts=new ArrayList<>();
        parts.add("Role: "+title+" at "+company+".");
        parts.add("Location: "+(location==null||location.isBlank()?"Not specified":location)+".");
        parts.add("Employment type: "+(type==null||type.isBlank()?"Not specified":type)+".");
        if(!base.isBlank()){for(String sentence:base.split("(?<=[.!?])\\s+")){String x=sentence.trim();if(x.length()>20)parts.add(x);if(parts.size()>=10)break;}}
        String[] defaults={"Work on role-related projects and deliverables.","Collaborate with cross-functional team members.","Use the required technical and communication skills.","Follow quality, testing and documentation practices.","Prepare for role-specific interview questions.","Review the original employer listing before applying."};
        for(String d:defaults){if(parts.size()>=10)break;parts.add(d);} return new ArrayList<>(parts.subList(0,Math.min(10,parts.size())));
    }

    public String precision(String query,String location,String experience){
        String role=(query==null||query.isBlank())?"Software Developer":query.trim();
        String prompt="""
                You are an expert recruiter and AI interview coach.
                Analyze the job search target below. It may be ANY role or skill such as Java, Python, QA, Data Analyst, DevOps, Marketing, Finance, or another job.
                Target: %s\nLocation: %s\nExperience: %s
                Return: 1. Role summary 2. Top 10 responsibilities 3. Required technical/domain skills 4. Soft skills 5. ATS keywords 6. Expected interview topics 7. Preparation priorities 8. 10 likely interview questions 9. Resume/project suggestions 10. Final recommendation.
                Do not invent a specific employer, salary, vacancy, or qualification.
                """.formatted(role,location==null||location.isBlank()?"Any":location,experience==null||experience.isBlank()?"Any":experience);
        return ollamaService.askAI(prompt);
    }

    private String strip(String s){return s.replaceAll("<[^>]*>"," ").replaceAll("\\s+"," ").trim();}
}
