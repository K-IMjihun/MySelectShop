package com.sparta.myselectshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


// 테스트시 MyselectshopApplication에 @EnableJpaAuditing가 방해하므로 @EnableJpaAuditing 주석처리 후 JpaConfig 작성
@Configuration // 아래 설정을 등록하여 활성화 합니다.
@EnableJpaAuditing // 시간 자동 변경이 가능하도록 합니다.
public class JpaConfig {
}