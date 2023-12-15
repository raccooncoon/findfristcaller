package com.raccoon.findfristcaller;

import com.intellij.psi.*;

public class CallerInfo {
    private final PsiClass psiClass;
    private final PsiMethod psiMethod;
    private final String url;

    public CallerInfo(PsiClass psiClass, PsiMethod psiMethod) {
        this.psiClass = psiClass;
        this.psiMethod = psiMethod;
        this.url = extractUrl(psiClass, psiMethod);
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

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if ("RestController".equals(qualifiedName) || "RequestMapping".equals(qualifiedName)) {
                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
                if (value == null) {
                    // "value" 속성이 없는 경우, 첫 번째 어노테이션 값을 사용
                    PsiAnnotationParameterList params = annotation.getParameterList();
                    PsiNameValuePair[] attributes = params.getAttributes();
                    if (attributes.length > 0) {
                        PsiAnnotationMemberValue firstValue = attributes[0].getValue();
                        if (firstValue != null) {
                            return firstValue.getText().replaceAll("^\"|\"$", ""); // 따옴표 제거
                        }
                    }
                } else {
                    // "value" 속성이 있는 경우, 해당 값을 사용
                    return value.getText().replaceAll("^\"|\"$", "");
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
