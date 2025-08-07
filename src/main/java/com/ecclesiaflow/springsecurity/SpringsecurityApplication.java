package com.ecclesiaflow.springsecurity;

import com.ecclesiaflow.springsecurity.io.entities.Member;
import com.ecclesiaflow.springsecurity.io.entities.Role;
import com.ecclesiaflow.springsecurity.io.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.business.encryption.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
public class SpringsecurityApplication implements CommandLineRunner {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${admin.email}")
	private String adminEmail;

	@Value("${admin.password}")
	private String adminPassword;

	@Value("${admin.firstName:Admin}")
	private String adminFirstName;

	@Value("${admin.lastName:User}")
	private String adminLastName;

	public static void main(String[] args) {
		SpringApplication.run(SpringsecurityApplication.class, args);
	}

	@Override
	@Transactional
	public void run(String... args) {
		Member adminAccount = memberRepository.findByRole(Role.ADMIN);
		if (adminAccount == null) {
			Member member = new Member();

			member.setEmail(adminEmail);
			member.setFirstName(adminFirstName);
			member.setLastName(adminLastName);
			member.setPassword(passwordEncoder.encode(adminPassword));
			member.setRole(Role.ADMIN);
			memberRepository.save(member);
		}
	}
}