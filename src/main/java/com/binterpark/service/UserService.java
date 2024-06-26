package com.binterpark.service;

import com.binterpark.exception.PasswordsNotEqualException;
import com.binterpark.exception.UserAlreadyExistException;
import com.binterpark.exception.UserNotFoundException;
import com.binterpark.common.UserRole;
import com.binterpark.domain.User;
import com.binterpark.dto.JwtDto;
import com.binterpark.dto.UserRegistrationDto;
import com.binterpark.jwt.JwtTokenProvider;
import com.binterpark.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    Logger logger = LoggerFactory.getLogger(Target.class);

    // 로그인
    @Transactional
    public JwtDto login (String userEmail, String userPw) {
        // 1. Email/PW 를 기반으로 Authentication 객체 생성
        // 이때 authentication 는 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new
                UsernamePasswordAuthenticationToken(userEmail, userPw);
        logger.info("authenticationToken : "+authenticationToken);


        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        // authenticate 매서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드가 실행
        try {
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            log.debug("Authentication successful for user: {}", userEmail); // 인증 성공 로그

            // 3. 인증 정보를 기반으로 JWT 토큰 생성
            return jwtTokenProvider.generateToken(authentication);
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", userEmail, e); // 인증 실패 로그
            throw e;
        }
    }

    // 회원가입
    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {

        // 이메일 중복 확인
        if (userRepository.findByUserEmail(registrationDto.getEmail()).isPresent()) {
            throw new UserAlreadyExistException("이미 등록된 이메일입니다.");
        }

        User user = new User();
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new PasswordsNotEqualException("비밀번호가 일치하지 않습니다.");
        }
        user.setUserEmail(registrationDto.getEmail());
        user.setUserName(registrationDto.getName());
        user.setUserPw(passwordEncoder.encode(registrationDto.getPassword()));
        user.setSignupDate(LocalDateTime.now());
        user.setUserRole(UserRole.USER.getRoleName());

        return userRepository.save(user);
    }

    // 회원정보 가져오기
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // 회원정보수정
    @Transactional
    public User patchUser(Long id, Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        updates.forEach((key, value) -> {
            System.out.println("key:"+key+", value:"+value);
            switch (key) {
                case "password" -> user.setUserPw(passwordEncoder.encode((String) value));
                case "name" -> user.setUserName((String) value);

                // 필요한 다른 필드들
                default -> throw new IllegalArgumentException("잘못된 필드: " + key);
            }
        });
        return userRepository.save(user);
    }

    // 회원삭제
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        userRepository.deleteById(id);
    }


}
