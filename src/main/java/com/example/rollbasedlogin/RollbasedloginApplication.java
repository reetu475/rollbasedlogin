package com.example.rollbasedlogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RollbasedloginApplication {

	public static void main(String[] args) {
		SpringApplication.run(RollbasedloginApplication.class, args);
	}

}




// ⭐ Full Commands Together
// git init
// git add .
// git commit -m "Initial commit"
// git remote add origin <your-repo-url>
// git push -u origin main

// ❗ If you get "main branch not found"

// Create the branch and push:

// git branch -M main
// git push -u origin main

// ❗ If project already has commits

// Skip git init and only do:

// git add .
// git commit -m "Updated code"
// git push