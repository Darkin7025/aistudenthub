package com.example.swp391.aistudenthub;

import com.example.swp391.aistudenthub.feature.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AistudenthubApplicationTests {

	@Autowired
	private EmailService emailService;

	@Test
	void contextLoads() {
	}

	@Test
	void testSendEmail() throws InterruptedException {
		// Thay đổi email nhận bên dưới để test thực tế
		String testRecipient = "cuongntse172349@fpt.edu.vn";
		System.out.println("Đang gửi mail thử nghiệm tới: " + testRecipient);

		emailService.sendPasswordResetEmail(testRecipient, "test-token-abcdef123456");

		// Đợi 5 giây vì EmailService gửi bất đồng bộ (@Async)
		System.out.println("Đang đợi mail gửi đi...");
		Thread.sleep(5000);
		System.out.println("Hoàn thành test gửi mail!");
	}
}
