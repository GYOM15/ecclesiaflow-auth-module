package com.ecclesiaflow.springsecurity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SpringsecurityApplication implements CommandLineRunner {

//	@Autowired
//	private MemberRepository memberRepository;
//
//	@Autowired
//	private PasswordEncoderUtil passwordEncoderUtil;
//
//	@Value("${admin.email}")
//	private String adminEmail;
//
//	@Value("${admin.password}")
//	private String adminPassword;

	public static void main(String[] args) {
		SpringApplication.run(SpringsecurityApplication.class, args);
	}

	@Override
	//@Transactional
	public void run(String... args) {
//		Member adminAccount = memberRepository.findByRole(Role.ADMIN);
//		if (adminAccount == null) {
//			Member member = Member.builder().
//					email(adminEmail).
//					password(passwordEncoderUtil.encode(adminPassword)).
//					role(Role.ADMIN).
//					build();
//			memberRepository.save(member);
//		}
	}
}