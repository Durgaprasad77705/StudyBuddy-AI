# Advanced Practice Center - Main Integration

## What was changed
- Removed the secondary Advanced/Coding/Resume/etc. navigation bar from the Dashboard.
- Removed the dashboard's duplicate optional practice grid.
- Removed `advanced-practice.css` from `index.html` so the Advanced page CSS cannot override the Dashboard styles.
- Kept `Advanced` as a main navigation item: Dashboard -> New Interview -> Live Interview -> Chatbot -> Advanced -> History -> Progress -> Badges -> Profile -> Logout.
- Advanced Practice remains the main feature center at `/pages/AdvancedPractice.html`.
- Added `api.js` to Advanced Practice so authentication/JWT is shared with the Spring Boot backend.
- Coding preparation now calls `POST /api/future/coding` through the authenticated `Api.post()` helper instead of a separate `/api/chat` request.
- Existing Voice, Video, Company, Group, Resume, LinkedIn and Jobs pages continue to use the `/api/future/*` backend endpoints.

## Run
1. Open a PowerShell terminal in the folder containing `pom.xml`.
2. Run: `mvn clean package -DskipTests`
3. Run on port 8099: `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8099"`
4. Open: `http://localhost:8099/`
5. Log in, then click **Advanced** in the main navigation.

Do not open the Dashboard through a separate static file server; Spring Boot should serve `src/main/resources/static`.
