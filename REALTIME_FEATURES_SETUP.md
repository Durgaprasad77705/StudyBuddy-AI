# Real-Time Advanced Features Setup

## 1. LinkedIn Profile Analyzer

The application now supports **Sign in with LinkedIn using OpenID Connect**.

Set these Windows environment variables before starting Spring Boot:

```bat
set LINKEDIN_CLIENT_ID=YOUR_LINKEDIN_CLIENT_ID
set LINKEDIN_CLIENT_SECRET=YOUR_LINKEDIN_CLIENT_SECRET
```

In the LinkedIn Developer Portal, add this redirect URL:

```text
http://localhost:8099/login/oauth2/code/linkedin
```

The app uses the current OpenID Connect scopes:

```text
openid,profile,email
```

Important: LinkedIn's self-service OpenID Connect profile data is intended for authenticated identity information (name/headline/photo/email). It does not automatically provide every Experience, Education or Skills field. The analyzer therefore lets the member paste the full profile content for section-by-section scoring and role prediction instead of scraping LinkedIn.

## 2. Company Interview

The Company Interview page is now an interactive interview loop:

1. Select company, role, experience and interview type.
2. Start interview.
3. AI generates one question.
4. Candidate types or speaks the answer.
5. AI evaluates the answer.
6. Candidate clicks Next Question.
7. Timer tracks the interview.

Chrome Speech Recognition is used for microphone input. Ollama is used when available, with a local fallback if Ollama is stopped.

Start Ollama:

```bat
ollama serve
ollama list
```

Required model:

```text
llama3.2:3b
```

## 3. Smart Job Preparation

The Smart Job Preparation page now searches a public remote-job feed when available and also provides live resource links for:

- YouTube interview videos
- PDF interview guides
- LinkedIn jobs
- Indeed jobs
- LeetCode practice

The public Remotive feed is not guaranteed to be instant; its documentation states that public listings can be delayed by up to 24 hours. The LinkedIn/Indeed/YouTube/PDF links open live search pages.

## 4. Build

From the project root:

```bat
mvn clean package
mvn spring-boot:run
```

Open:

```text
http://localhost:8099/
```
