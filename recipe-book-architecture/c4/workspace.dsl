workspace "Recipe Book" "A personal administrative tool to save, edit, and browse recipes." {
    model {
        user = person "User" "A home cook who saves, edits, and browses their personal recipes."

        recipeBook = softwareSystem "Recipe Book" "Allows users to store, browse, and manage recipes, including their ingredients and cooking steps." {
            frontend = container "Frontend Web Application" "Provides the user interface for browsing, creating, editing, and deleting recipes." "Angular"
            backend = container "Backend API" "Handles recipe CRUD operations and manages ingredients across shared recipes." "Java, Spring Boot, JPA" {
                controller = component "Recipe Controller" "Exposes REST endpoints for recipe and ingredient operations." "Spring MVC Rest Controller"
                service = component "Recipe Service" "Implements business logic for managing recipes, their ordered steps, and ingredients shared across recipes." "Spring Service"
                repository = component "Recipe Repository" "Provides data access for recipes, steps, and ingredients." "JPA/Hibernate, EntityManager"
            }
            database = container "Database" "Stores recipes, ingredients, and cooking steps." "PostgreSQL" "Database"
        }

        user -> frontend "Browses, creates, edits, and deletes recipes using" "HTTPS"
        frontend -> controller "Makes API calls to" "JSON/HTTPS"
        controller -> service "Delegates and ingredient operations to"
        service -> repository "Uses to persist and retrieve data"
        repository -> database "Reads from and writes to" "JDBC"
    }

    views {
        systemContext recipeBook "SystemContext" {
            include *
            autolayout lr
        }

        container recipeBook "Containers" {
            include *
            autolayout lr
        }

        component backend "Component" {
            include *
            autolayout lr
        }

        styles {
            element "Person" {
                background #08427b
                color #ffffff
                shape Person
            }
            element "Software System" {
                background #1168bd
                color #ffffff 
            }    
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Database" {
                shape Cylinder
            }
        }
    }
}
