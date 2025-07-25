package com.ecclesiaflow.springsecurity;

import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.entities.Role;
import com.ecclesiaflow.springsecurity.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SpringsecurityApplication implements CommandLineRunner {

	@Autowired
	private MemberRepository memberRepository;

	public static void main(String[] args) {
		SpringApplication.run(SpringsecurityApplication.class, args);
	}

	@Override
	public void run(String... args) {
		Member adminAccount = memberRepository.findByRole(Role.ADMIN);
		if (adminAccount == null) {
			Member member = new Member();

			member.setEmail("admin@ecclesiaflow.com");
			member.setFirstName("admin");
			member.setLastName("admin");
			member.setPassword(new BCryptPasswordEncoder().encode("admin"));
			member.setRole(Role.ADMIN);
			memberRepository.save(member);
		}
	}
}