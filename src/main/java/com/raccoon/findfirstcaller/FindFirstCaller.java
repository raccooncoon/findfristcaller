package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FindFirstCaller extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        PsiMethod selectedMethod = null;
        //String projectBasePath = null;
        String sourceFolderName = null;
        Module module = null;
        String xmlTagText = "";

        if (editor == null || project == null || psiFile == null) {
            return;
        }

        if (DumbService.isDumb(project)) {
            // 인덱싱이 완료될 때까지 대기하거나, 작업을 취소하거나, 사용자에게 알림을 표시
            Messages.showDialog(project, "인덱싱이 완료 후 다시 실행 하세요.", "인덱싱 중", new String[]{"OK"}, 0, null);
            return;
        }

        // 선택한 부분 정보
        PsiElement selectedElement = psiFile.findElementAt(editor.getCaretModel().getOffset());

        if (selectedElement != null) {
            // 선택된 요소가 속한 모듈 찾기
            module = ModuleUtil.findModuleForPsiElement(selectedElement);
            if (module != null) {
                sourceFolderName = new File(module.getModuleFilePath()).getName().replaceAll(".iml", "");
            }
        }

        //xml 파일 일 경우 xml 정보 출력
        XmlTag xmlTag = PsiTreeUtil.getParentOfType(selectedElement, XmlTag.class);
        if (xmlTag != null && xmlTag.getParent() != null) {

            // id 찾을 때 까지
            xmlTag = getXmlTag(xmlTag);

            String id = xmlTag.getAttributeValue("id");
//            String crud = xmlTag.getLocalName();
            String namespace = ((XmlTagImpl) xmlTag.getParent()).getAttributeValue("namespace");

            xmlTagText = xmlTag.getText();


            // iBATIS 지원, <sqlMap
            // xmlTag.getParent();
            // findIBatisMethod(id, module);

            // MyBatis 지원, <mapper
            selectedMethod = findAbstractMethod(id, namespace, module);

        }


        if (selectedMethod == null) {
            selectedMethod = PsiTreeUtil.getParentOfType(selectedElement, PsiMethod.class);
        }

        // selectedMethod 로 최상위 메소드 검색
        Set<CallerInfo> callers = getCallerInfos(selectedMethod, project);

        // 다이얼로그 생성 및 데이터 설정
        if (selectedMethod != null) {
            ResultsDialog dialog = new ResultsDialog(selectedMethod, callers);
            dialog.setResults(getObjects(callers, sourceFolderName, xmlTagText, selectedMethod ));
            dialog.setVisible(true);
        }
    }

/*    private List<PsiMethod> findIBatisMethod(String sqlId, Module module) {
        List<PsiMethod> foundMethods = new ArrayList<>();

        Project project = module.getProject();
        GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);


        return foundMethods;
    }*/

    @Nullable
    private XmlTag getXmlTag(XmlTag xmlTag) {
        return Stream.iterate(xmlTag, XmlTag::getParentTag)
                .limit(10)
                .filter(tag -> tag != null && tag.getAttributeValue("id") != null)
                .findFirst()
                .orElse(null);
    }

    private PsiMethod findAbstractMethod(String xmlId, String mapperInterfaceFullName, Module module) {
        //PsiClass mapperInterface = JavaPsiFacade.getInstance(project).findClass(mapperInterfaceFullName, GlobalSearchScope.allScope(project));

        GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(module);
        PsiClass mapperInterface = JavaPsiFacade.getInstance(module.getProject()).findClass(mapperInterfaceFullName, moduleScope);

        if (mapperInterface != null) {
            for (PsiMethod method : mapperInterface.getMethods()) {
                String name = method.getName();
                if (xmlId.equals(name)) {
                    return method;
                }
            }
        }
        return null; // 일치하는 메소드를 찾지 못한 경우
    }


    @NotNull
    private static Object[][] getObjects(Set<CallerInfo> callers, String sourceFolderName, String xmlTagText, PsiMethod selectedMethod) {
        Object[][] data = new Object[callers.size()][6];
        int index = 0;
        for (CallerInfo caller : callers) {
            data[index][0] = sourceFolderName;
            data[index][1] = selectedMethod.getName();
            data[index][2] = caller.getPsiClass().getQualifiedName();
            data[index][3] = caller.getPsiMethod().getName();
            data[index][4] = caller.getUrl();
            data[index][5] = xmlTagText;
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
