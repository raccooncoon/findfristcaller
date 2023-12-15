package com.raccoon.findfristcaller;

import com.intellij.psi.*;

import java.util.Arrays;

public class CallerInfo {
    private final PsiClass psiClass;
    private final PsiMethod psiMethod;
    private final String url;

    public CallerInfo(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
        this.psiClass = psiMethod != null ? psiMethod.getContainingClass() : null;
        this.url = (psiClass != null) ? extractUrl(psiClass, psiMethod) : "";
    }

    private String extractUrl(PsiClass psiClass, PsiMethod psiMethod) {
        String classUrl = extractUrlFromAnnotation(psiClass.getModifierList());
        String methodUrl = extractUrlFromAnnotation(psiMethod.getModifierList());

        // 클래스 URL과 메서드 URL을 결합
        return classUrl + methodUrl;
    }

    private String extractUrlFromAnnotation(PsiModifierList modifierList) {
        if (modifierList == null) {
            return "";
        }

        String[] annotationNames = {
                "Controller",
                "RestController",
                "RequestMapping",
                "GetMapping",
                "PostMapping",
                "PutMapping",
                "DeleteMapping",
                "PatchMapping",
                "org.springframework.web.bind.annotation.Controller",
                "org.springframework.web.bind.annotation.RestController",
                "org.springframework.web.bind.annotation.RequestMapping",
                "org.springframework.web.bind.annotation.GetMapping",
                "org.springframework.web.bind.annotation.PostMapping",
                "org.springframework.web.bind.annotation.PutMapping",
                "org.springframework.web.bind.annotation.DeleteMapping",
                "org.springframework.web.bind.annotation.PatchMapping"
        };

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (Arrays.asList(annotationNames).contains(qualifiedName)) {
                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
                if (value != null) {
                    String text = value.getText();
                    return text.replaceAll("^\"|\"$", "");
                }
            }
        }
        return "";
    }


    public PsiClass getPsiClass() {
        return psiClass;
    }

    public PsiMethod getPsiMethod() {
        return psiMethod;
    }

    public String getUrl() {
        return url;
    }
}
