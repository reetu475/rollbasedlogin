package com.example.rollbasedlogin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username cannot be empty")
    @Column(nullable = false)
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be empty")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Role is required")
    @Column(nullable = false)
    private String role;

    @Column(nullable = true)  // Optional field
    private String phoneNumber;

    // New fields for job portal
    @Column(nullable = true)
    private String fullName;

    @Column(nullable = true)
    private String education;

    @Column(nullable = true)
    private String skills;

    @Column(nullable = true)
    private String resumeUrl;

    @Column(nullable = true)
    private String experience;

    @Column(nullable = true)
    private String company;

    @Column(nullable = true)
    private String jobTitle;

    // Getters & Setters
    public Long getId() { return this.id; }

    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return this.email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return this.password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return this.role; }
    public void setRole(String role) { this.role = role; }

    public String getPhoneNumber() { return this.phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFullName() { return this.fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEducation() { return this.education; }
    public void setEducation(String education) { this.education = education; }

    public String getSkills() { return this.skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getResumeUrl() { return this.resumeUrl; }
    public void setResumeUrl(String resumeUrl) { this.resumeUrl = resumeUrl; }

    public String getExperience() { return this.experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getCompany() { return this.company; }
    public void setCompany(String company) { this.company = company; }

    public String getJobTitle() { return this.jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
}

















// package com.example.rollbasedlogin.model;



// import jakarta.persistence.Column;
// import jakarta.persistence.Entity;
// import jakarta.persistence.GeneratedValue;
// import jakarta.persistence.GenerationType;
// import jakarta.persistence.Id;

// @Entity
// public class User {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     private String username;

//     @Column(unique = true)
//     private String email;

//     private String password;

//     private String role;


//    public String getEmail()
//     {
//         return this.email;
//     }
    
//    public void setPassword(String p)
//     {
//         this.password=p;
//     }
//     public void setUsername(String u)
//     {
//         this.username=u;
//     }

//     public void setEmail(String e)
//     {
//         this.email=e;
//     }
//     public void setRole(String r)
//     {
//         this.role=r;
//     }

//     public String getPassword() {
//        return this.password;
//     }
//     public String getRole() {
//        return this.role;
//     }

    
// }
