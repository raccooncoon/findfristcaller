package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

        if (DumbService.isDumb(project)) {
            // 인덱싱이 완료될 때까지 대기하거나, 작업을 취소하거나, 사용자에게 알림을 표시
            Messages.showDialog(project, "인덱싱이 완료 후 다시 실행 하세요.", "인덱싱 중", new String[]{"OK"}, 0, null);
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement selectedElement = psiFile.findElementAt(offset);
        PsiMethod selectedMethod = PsiTreeUtil.getParentOfType(selectedElement, PsiMethod.class);

        Set<CallerInfo> callers = getCallerInfos(selectedMethod, project);

        // 다이얼로그 생성 및 데이터 설정
        if (selectedMethod != null) {
            ResultsDialog dialog = new ResultsDialog(selectedMethod, callers);
            dialog.setResults(getObjects(callers));
            dialog.setVisible(true);
        }
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

    @NotNull
    public Set<CallerInfo> getCallerInfos(PsiMethod selectedMethod, Project project) {
        Set<PsiMethod> visitedMethods = new HashSet<>();
        return Optional.ofNullable(selectedMethod)
                .map(method -> {
                    Set<PsiMethod> initialCallers = new HashSet<>();
                    findInitialCallers(selectedMethod, initialCallers, visitedMethods, project);
                    return initialCallers.stream()
                            .map(CallerInfo::new)
                            .collect(Collectors.toSet());
                })
                .orElse(Collections.emptySet());
    }

    private void findInitialCallers(PsiMethod method, Set<PsiMethod> initialCallers, Set<PsiMethod> visitedMethods, Project project) {
        if (visitedMethods.contains(method)) {
            return;
        }
        visitedMethods.add(method);
        Query<PsiReference> references = MethodReferencesSearch.search(method, GlobalSearchScope.allScope(project), false);
        for (PsiReference reference : references) {
            PsiMethod referenceMethod = PsiTreeUtil.getParentOfType(reference.getElement(), PsiMethod.class);
            if (referenceMethod != null && !visitedMethods.contains(referenceMethod)) {
                Query<PsiReference> higherReferences = MethodReferencesSearch.search(referenceMethod, GlobalSearchScope.allScope(project), false);
                if (higherReferences.findFirst() == null) {
                    initialCallers.add(referenceMethod);
                } else {
                    findInitialCallers(referenceMethod, initialCallers, visitedMethods, project);
                }
            }
        }
    }
}
