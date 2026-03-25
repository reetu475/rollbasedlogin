package com.example.rollbasedlogin.util;



import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static final String SECRET = "thisisaverylongsecretkeyformyjwtandmustbeatleast32chars";

    private final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(SECRET.getBytes())
    ));

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour
                .signWith(this.key)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(this.key).build().parseClaimsJws(token).getBody();
    }

    public String getEmailFromToken(String token) {
        return this.extractClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) this.extractClaims(token).get("role");
    }

    public boolean isTokenValid(String token) {
        try {
            this.extractClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}


// ✅ Full Method:
// java
// Copy
// Edit
// public String generateToken(String email, String role) {
//     return Jwts.builder()
//             .setSubject(email)
//             .claim("role", role)
//             .setIssuedAt(new Date())
//             .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour
//             .signWith(key)
//             .compact();
// }
// 🔍 Explanation Line-by-Line:
// 🟩 public String generateToken(String email, String role) {
// This is the method signature.

// It accepts user info: email (to identify the user) and role (to manage authorization).

// It returns a signed JWT string.

// 🟩 return Jwts.builder()
// Creates a new JWT builder.

// Jwts.builder() is from the io.jsonwebtoken library.

// Think of this as: “Start building a JWT.”

// 🟩 .setSubject(email)
// Sets the subject claim in the JWT (standard claim).

// email is used as the subject to uniquely identify the user (like username).

// Example in JWT payload:

// "sub": "user@example.com"
// 🟩 .claim("role", role)
// Adds a custom claim to the JWT called "role".

// This allows you to store user roles like "ADMIN" or "USER".

// Example in JWT payload:

// "role": "ADMIN"
// 🟩 .setIssuedAt(new Date())
// Sets the "issued at" (iat) timestamp.

// This marks the exact time when the token was generated.

// Helps with time-based validations.

// Example:

// json
// Copy
// Edit
// "iat": 1719339000
// 🟩 .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000))
// Sets the expiration time for the token.

// System.currentTimeMillis() gives the current time in milliseconds.

// Adding 3600 * 1000 = 1 hour (in milliseconds).

// So the token expires in 1 hour from now.

// Example:

// json
// Copy
// Edit
// "exp": 1719342600
// 🟩 .signWith(key)
// This is where the JWT is digitally signed using your secret key.

// key is a SecretKey that you created earlier.

// This ensures:

// Integrity (token can't be modified).

// Authenticity (you can verify who created it).

// 🟩 .compact();
// Final step: turns the built JWT object into a compact JWT string:

// php-template
// Copy
// Edit
// <header>.<payload>.<signature>
// ✅ Sample Output of the JWT
// Looks like this (example):

// Copy
// Edit
// eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNjg2NjYxODAwLCJleHAiOjE2ODY2NjU0MDB9.abc123...
// 🔵 Header: algorithm info

// 🟡 Payload: email, role, issuedAt, expiry

// 🔴 Signature: created using your secret key

// 🧠 Summary in Telugu-English Mix:
// text
// Copy
// Edit
// • subject lo email pettam -> identify cheyyadaniki
// • role ane custom claim pettam -> authorization kosam
// • iat lo ippudu time pettam
// • exp lo 1 hour varaku token valid ani cheppam
// • key to sign chesi token ni compact chesam
// Let me know if you want to implement token validation and extract claims from this JWT also!












// Tools


