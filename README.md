# LinkedOut

LinkedOut is a job recruitment platform built with Java Spring Boot. It allows candidates to apply for jobs, recruiters to post and manage job offers, and admins to oversee the platform. The project features authentication, profile management, file uploads (CVs, profile images), and a modern web interface.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## Features

- User authentication (JWT-based)
- Role-based access: Admin, Recruiter, Candidate
- Job offer creation, editing, and viewing
- Candidate application management
- Profile management for recruiters and candidates
- File uploads: CVs, profile images
- Admin dashboard for platform management
- Exception handling and validation

## Tech Stack

- **Backend:** Java 17+, Spring Boot, Spring Security, JWT
- **Frontend:** HTML, CSS, JavaScript (static resources)
- **Database:** (Specify your DB, e.g., MySQL, PostgreSQL, H2)
- **Build Tool:** Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- (Your database, e.g., MySQL/PostgreSQL) running and accessible

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/LinkedOut.git
   cd LinkedOut
   ```

2. **Configure the database:**
   - Edit `src/main/resources/application.properties` with your DB credentials.

3. **Build the project:**
   ```bash
   ./mvnw clean install
   ```

### Running the Application

```bash
./mvnw spring-boot:run
```

The application will start on [http://localhost:8080](http://localhost:8080).

## Project Structure

```
src/
  main/
    java/
      ma/
        linkedout/
          linkedout/
            config/         # Spring and security configuration
            controllers/    # REST and web controllers
            dto/            # Data transfer objects
            dtos/           # Additional DTOs
            entities/       # JPA entities
            exception/      # Global exception handling
            repositories/   # Spring Data repositories
            security/       # JWT and security logic
            services/       # Service interfaces and implementations
    resources/
      static/               # Static web resources (HTML, CSS, JS)
      templates/            # Thymeleaf templates
      application.properties
  test/
    java/
      ma/
        linkedout/
          linkedout/
            # Unit and integration tests
uploads/                    # Uploaded files (CVs, profile images)
```

## Usage

- Access the web interface at [http://localhost:8080](http://localhost:8080).
- Register as a candidate or recruiter, or log in as an admin.
- Recruiters can create and manage job offers.
- Candidates can apply to job offers and manage their profiles.
- Admins can manage users and job offers.

## Contributing

Contributions are welcome! Please open issues or submit pull requests for improvements or bug fixes.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Contact

For questions or support, contact [your-email@example.com]. 
