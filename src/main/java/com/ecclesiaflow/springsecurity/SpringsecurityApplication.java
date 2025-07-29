package com.ecclesiaflow.springsecurity;

import com.ecclesiaflow.springsecurity.entities.Member;
import com.ecclesiaflow.springsecurity.entities.Role;
import com.ecclesiaflow.springsecurity.repository.MemberRepository;
import com.ecclesiaflow.springsecurity.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringsecurityApplication implements CommandLineRunner {

	@Autowired
	private MemberRepository memberRepository;

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
	public void run(String... args) {
		Member adminAccount = memberRepository.findByRole(Role.ADMIN);
		if (adminAccount == null) {
			Member member = new Member();

			member.setEmail(adminEmail);
			member.setFirstName(adminFirstName);
			member.setLastName(adminLastName);
			member.setPassword(EncryptionUtil.hashPassword(adminPassword));
			member.setRole(Role.ADMIN);
			memberRepository.save(member);
		}
	}
}