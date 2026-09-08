# StudyBuddy AI - Advanced Real-Time Setup

## 1. Start the backend

```bat
mvn clean package
mvn spring-boot:run
```

Application URL:

`http://localhost:8099/`

## 2. Google Login

Create a Google OAuth 2.0 Web Application client and add this exact redirect URI:

`http://localhost:8099/login/oauth2/code/google`

Set Windows environment variables before starting Maven:

```bat
set GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
set GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
mvn spring-boot:run
```

The login button opens:

`http://localhost:8099/oauth2/authorization/google`

If Google shows `Error 401: invalid_client`, the client ID/secret or Google OAuth configuration is incorrect or still set to the placeholder values. This is a Google credential configuration issue, not a frontend link issue.

## 3. GitHub Login

Create a GitHub OAuth App and use this callback URL:

`http://localhost:8099/login/oauth2/code/github`

Then:

```bat
set GITHUB_CLIENT_ID=YOUR_GITHUB_CLIENT_ID
set GITHUB_CLIENT_SECRET=YOUR_GITHUB_CLIENT_SECRET
mvn spring-boot:run
```

The login button opens:

`http://localhost:8099/oauth2/authorization/github`

## 4. Forgot Password

The login screen has **Forgot password?**.

For this local build, the backend generates a 15-minute reset token and displays a development reset link. Production deployment should replace this with an SMTP/email provider.

## 5. Resume Analysis

Upload:

- PDF
- DOCX
- TXT

The backend extracts the text and analyzes:

- Candidate Summary
- Education
- Skills
- Projects
- Experience
- Certifications
- Strengths
- Skill Gaps
- Improvements

It also returns scores for:

- Skills
- Education
- Projects
- Experience
- Summary
- Certifications
- Overall score

If Ollama is available, the analysis is AI-powered. If Ollama is stopped, a local fallback parser still produces a live scored result instead of a connection-refused page.

Start Ollama for full AI analysis:

```bat
ollama serve
ollama list
```

Make sure `llama3.2:3b` is installed.

## 6. Coding Interview

The coding page now has:

- AI problem generation
- Java/Python/C++ selection
- Arrays, Strings, Linked List, Trees, Graphs and DP
- Candidate code submission
- AI evaluation
- Score and improvement result
- Direct LeetCode Login link
- Direct LeetCode Problems link

LeetCode authentication is intentionally opened in LeetCode's own tab. The application does not attempt to collect or proxy LeetCode passwords.

LeetCode login:

`https://leetcode.com/accounts/login/`

LeetCode problems:

`https://leetcode.com/problemset/`


## GitHub Login — required configuration

The GitHub button is implemented with Spring Security OAuth2. You must create/configure a GitHub OAuth App and set:

- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`

For local development on port 8099, configure this callback URL in GitHub:

`http://localhost:8099/login/oauth2/code/github`

For production, replace the host with your deployed HTTPS host.

The application requests `read:user,user:email`. GitHub's `user:email` scope is the permission that allows an OAuth app to read private email addresses. Do not put the client secret in frontend JavaScript or commit it to GitHub.
