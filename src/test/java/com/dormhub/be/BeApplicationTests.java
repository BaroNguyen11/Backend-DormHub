
package com.dormhub.be;

import com.dormhub.repository.StudentRepository;
import com.dormhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BeApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testRepositories() {
        var manager = userRepository.findByEmail("manager@dormhub.com");

        long studentCount = studentRepository.count();

        System.out.println("Manager query result: " + manager);
        System.out.println("Student count: " + studentCount);
    }
}
