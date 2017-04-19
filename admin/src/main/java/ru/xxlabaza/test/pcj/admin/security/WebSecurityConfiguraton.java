/*
 * Copyright 2017 Artem Labazin <xxlabaza@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.xxlabaza.test.pcj.admin.security;


import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import ru.xxlabaza.test.pcj.admin.AppProperties;
import ru.xxlabaza.test.pcj.admin.AppProperties.User;

import java.util.UUID;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 02.04.2017
 */
@Slf4j
@EnableWebSecurity
class WebSecurityConfiguraton extends WebSecurityConfigurerAdapter {

  @Autowired
  private AppProperties appProperties;

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    User user = appProperties.getUser();

    String beholderPassword = user.getBeholder();
    if (beholderPassword == null) {
      beholderPassword = UUID.randomUUID().toString();
      log.info("\n\nBeholder password is:\n{}\n", beholderPassword);
    }

    String adminPassword = user.getAdmin();
    if (adminPassword == null) {
      adminPassword = UUID.randomUUID().toString();
      log.info("\n\nAdmin password is:\n{}\n", adminPassword);
    }

    auth.inMemoryAuthentication()
        .withUser("beholder")
        .password(beholderPassword)
        .authorities("USER")
        .and()
        .withUser("admin")
        .password(adminPassword)
        .authorities("ADMIN");
  }

  @Override
  protected void configure (HttpSecurity http) throws Exception {
    // Page with login form is served as /login.html and does a POST on /login
    http.formLogin().loginPage("/login.html").loginProcessingUrl("/login").permitAll();
    // The UI does a POST on /logout on logout
    http.logout().logoutUrl("/logout");
    // The ui currently doesn't support csrf
    http.csrf().disable();

    // Requests for the login page and the static assets are allowed
    http.authorizeRequests()
        .antMatchers("/login.html", "/**/*.css", "/img/**", "/third-party/**")
        .permitAll();
    // ... and any other request needs to be authorized
    http.authorizeRequests()
        .antMatchers(POST, "/api/**").hasAuthority("ADMIN")
        .antMatchers(PATCH, "/api/**").hasAuthority("ADMIN")
        .antMatchers(DELETE, "/api/**").hasAuthority("ADMIN")
        .antMatchers(PUT, "/api/**").hasAuthority("ADMIN")
        .antMatchers("/**").authenticated();

    // Enable so that the clients can authenticate via HTTP basic for registering
    http.httpBasic();
  }

//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//    http.authorizeRequests()
//        .antMatchers("/login",
//                     "/login.html",
//                     "/*.css",
//                     "/*.js",
//                     "/third-party/**",
//                     "/webjars/**",
//                     "/img/**",
//                     "/css/**",
//                     "/modules/**",
//                     "/turbine.stream**"
//        ).permitAll()
//        .antMatchers(POST, "/api/**").hasAuthority("ADMIN")
//        .antMatchers(PATCH, "/api/**").hasAuthority("ADMIN")
//        .antMatchers(DELETE, "/api/**").hasAuthority("ADMIN")
//        .antMatchers(PUT, "/api/**").hasAuthority("ADMIN")
//        .anyRequest().authenticated()
//        .and()
//        .csrf().disable()
//        .formLogin()
//        .loginPage("/login")
//        .failureUrl("/login?error")
//        .defaultSuccessUrl("/#/")
//        .permitAll()
//        .and()
//        .logout()
//        .permitAll();
//  }
}
