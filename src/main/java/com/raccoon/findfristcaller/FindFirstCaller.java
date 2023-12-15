package com.raccoon.findfristcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FindFirstCaller extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (editor == null || project == null || psiFile == null) {
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement selectedElement = psiFile.findElementAt(offset);
        PsiMethod selectedMethod = PsiTreeUtil.getParentOfType(selectedElement, PsiMethod.class);

        Set<CallerInfo> callers = getCallerInfos(selectedMethod, project);

        // 다이얼로그 생성 및 데이터 설정
        if (selectedMethod != null) {
            ResultsDialog dialog = new ResultsDialog(selectedMethod, callers.size());
            dialog.setResults(getObjects(callers));
            dialog.setVisible(true);
        }
    }

    @NotNull
    private Set<CallerInfo> getCallerInfos(PsiMethod selectedMethod, Project project) {
        return Optional.ofNullable(selectedMethod)
                .map(method -> {
                    Set<PsiMethod> initialCallers = new HashSet<>();
                    findInitialCallers(selectedMethod, initialCallers, project);
                    return initialCallers.stream()
                            .map(CallerInfo::new)
                            .collect(Collectors.toSet());
                })
                .orElse(Collections.emptySet());
    }

    @NotNull
    private static Object[][] getObjects(Set<CallerInfo> callers) {
        Object[][] data = new Object[callers.size()][3];
        int index = 0;
        for (CallerInfo caller : callers) {
            data[index][0] = caller.getPsiClass().getQualifiedName();
            data[index][1] = caller.getPsiMethod().getName();
            data[index][2] = caller.getUrl();
            index++;
        }
        return data;
    }

    private void findInitialCallers(PsiMethod method, Set<PsiMethod> initialCallers, Project project) {
        Query<PsiReference> references = MethodReferencesSearch.search(method, GlobalSearchScope.allScope(project), false);
        for (PsiReference reference : references) {
            PsiMethod referenceMethod = PsiTreeUtil.getParentOfType(reference.getElement(), PsiMethod.class);
            if (referenceMethod != null) {
                Query<PsiReference> higherReferences = MethodReferencesSearch.search(referenceMethod, GlobalSearchScope.allScope(project), false);
                if (higherReferences.findFirst() == null) {  // No higher reference found
                    initialCallers.add(referenceMethod);
                } else {
                    findInitialCallers(referenceMethod, initialCallers, project); // Recursive call
                }
            }
        }
    }
}
