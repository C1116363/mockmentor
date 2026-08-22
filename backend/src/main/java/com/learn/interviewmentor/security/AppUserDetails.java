package com.learn.interviewmentor.security;

import com.learn.interviewmentor.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The bridge between our JPA User entity and Spring Security's UserDetails.
 *
 * Spring Security only understands UserDetails. Rather than making our entity
 * implement it (which would drag security concerns into the database model),
 * we wrap it.
 */
public class AppUserDetails implements UserDetails {

    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    /** The entity behind the login, so controllers can get the real User. */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // hasRole("ADMIN") actually checks for the authority "ROLE_ADMIN".
        // This prefix is the single most common source of "why is my
        // @PreAuthorize always failing?".
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Spring calls the login identifier "username"; ours happens to be an email. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** An admin flipping User.active to false blocks login here. */
    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
