package io.github.hummelhose.desksharing;

import org.springframework.boot.SpringApplication;

public class TestDeskSharingApplication {

	public static void main(String[] args) {
		SpringApplication.from(DeskSharingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
