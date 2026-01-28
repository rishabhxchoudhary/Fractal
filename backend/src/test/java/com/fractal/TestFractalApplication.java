package com.fractal;

import org.springframework.boot.SpringApplication;

public class TestFractalApplication {

	public static void main(String[] args) {
		SpringApplication.from(FractalApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
