package com.raccoon.findfirstcaller;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            ResultsDialog dialog = new ResultsDialog(selectedMethod, callers);
            dialog.setResults(getObjects(callers));
            dialog.setVisible(true);
        }
    }

    public void saveCsvFile(Set<CallerInfo> callers, boolean append, PsiMethod selectedMethod) {
        String filePath = generateSavePath("result.csv", selectedMethod.getProject());
        // 파일이 존재하지 않거나, 새로 쓰기 모드인 경우 헤더를 추가합니다.
        boolean writeHeader = !new File(filePath).exists() || !append;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append))) {
            // 새 파일이거나 새로 쓰기 모드인 경우에만 헤더를 작성합니다.
            if (writeHeader) {
                writer.write("Class Name,Method Name,URL,Selected Class Name,Selected Method Name" + System.lineSeparator());
            }

            for (CallerInfo caller : callers) {
                writeCallerInfo(writer, caller, selectedMethod);
            }
        } catch (IOException ex) {
            throw new RuntimeException("파일 작성 중 오류 발생", ex);
        }
    }

    private void writeCallerInfo(BufferedWriter writer, CallerInfo caller, PsiMethod selectedMethod) throws IOException {
        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                caller.getPsiClass().getQualifiedName(),
                caller.getPsiMethod().getName(),
                caller.getUrl(),
                Objects.requireNonNull(selectedMethod.getContainingClass()).getQualifiedName(),
                selectedMethod.getName()
        );
        writer.write(line);
    }

    private String generateSavePath(String defaultFileName, Project project) {
        String projectName = project.getName(); // 프로젝트 이름을 얻습니다.
        String fileName = projectName + "_" + defaultFileName; // 파일 이름을 프로젝트 이름으로 설정합니다.

        Path directoryPath = Paths.get(System.getProperty("user.home"), "메소드 검색 결과");
        File directory = directoryPath.toFile();

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directoryPath.resolve(fileName).toString();
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
