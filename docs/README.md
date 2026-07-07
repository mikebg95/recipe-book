# Recipe Book - Architecture & Diagrams

This repository contains the software architecture blueprints for the Recipe Book ecosystem, including the **C4 Model** (via **Structurizr**) and the **database schema** (via **DBML**).

## Repository Structure

```
recipe-book-architecture/
├── README.md
├── .gitignore
├── c4/
│   └── workspace.dsl        # C4 architecture model (source of truth)
└── database/
    ├── schema.dbml          # Database schema (source of truth)
    └── schema.png           # Rendered schema diagram
```

## C4 Architecture Diagrams

The architecture is written as code using the Structurizr DSL (`c4/workspace.dsl`). You can run a local development server using Docker to view and interact with the live diagrams.

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### Steps to Run

1. Open your terminal and navigate to the C4 folder:

   ```bash
   cd recipe-book-architecture/c4
   ```

2. Run the following Docker command to spin up Structurizr Lite (this mounts the current folder containing the `workspace.dsl` file):

   ```bash
   docker run -it --rm -p 9090:8080 -v "$(pwd)":/usr/local/structurizr structurizr/structurizr local
   ```

3. Open your web browser and navigate to **http://localhost:9090**

### Available Views

Once the browser UI loads, you can toggle between three conceptual layers:

1. **System Context** — High-level overview showing how the user interacts with the Recipe Book system.
2. **Container View** — Zoomed-in look at the Frontend (Angular), Backend API (Spring Boot), and Database (PostgreSQL).
3. **Component View** — Deep-dive into the Backend API's internal structure (Controllers, Services, DAOs).

## Database Schema

The database schema is defined as code using **DBML** in `database/schema.dbml`, which is the source of truth. A rendered image is available at `database/schema.png`.

To view or edit the schema, open [dbdiagram.io](https://dbdiagram.io) and paste in the contents of `schema.dbml`. The diagram updates live as you edit, and can be re-exported to PNG or to PostgreSQL DDL.

## Repository Contents

- `c4/workspace.dsl` — The C4 architecture-as-code source file.
- `database/schema.dbml` — The database schema-as-code source file.
- `database/schema.png` — Rendered database schema diagram.
- `.gitignore` — Configured to ignore auto-generated layout caching (`workspace.json`).
