/*
 * @ (#) StudentSpecification .java     1.0     10/30/2026
 * Copyright (c) 2026 IUH.All Rights Reserved.
 */
package com.dormhub.specification;

import com.dormhub.entity.Student;
import org.springframework.data.jpa.domain.Specification;

/*
 *@description: this is a class to test the StudentSpecification class
 *@author: Bao Nguyen
 *@Date: 10/30/2026
 *@version:      1.0
 */
public class StudentSpecification {
        public static Specification<Student> hasName(String name){
            return (((root, query, criteriaBuilder) -> {
                if (name == null || name.trim().isEmpty()){
                    return criteriaBuilder.conjunction();
                }else return criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("user").get("fullName")),
                        "%" + name.trim().toLowerCase() + "%"
                );
            }));
        }
    public static Specification<Student> hasStudentCode(String studentCode){
        return ((root, query, criteriaBuilder) -> {
            if (studentCode==null || studentCode.trim().isEmpty()){
                return criteriaBuilder.conjunction();
            }else return criteriaBuilder.like(
                    root.get("studentCode"),
                    "%" + studentCode.trim() + "%"
            );
        });
    }
}
