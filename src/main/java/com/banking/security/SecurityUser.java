package com.banking.security;

import com.banking.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SecurityUser implements UserDetails {

    private final User user;
    private final Set<GrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        Set<GrantedAuthority> granted = new HashSet<>();
        user.getRoles().forEach(role -> {
            granted.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
            role.getPermissions().forEach(permission ->
                    granted.add(new SimpleGrantedAuthority(permission.getName())));
        });
        this.authorities = Set.copyOf(granted);
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Set<String> getRoleNames() {
        return user.getRoles().stream().map(r -> r.getName().name()).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

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

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
